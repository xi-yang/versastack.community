/*
 * Copyright (c) 2013-2016 University of Maryland
 * Created by: Miguel Uzcategui 2015
 * Modified by: Xi Yang 2015-2016

 * Permission is hereby granted, free of charge, to any person obtaining a copy 
 * of this software and/or hardware specification (the “Work”) to deal in the 
 * Work without restriction, including without limitation the rights to use, 
 * copy, modify, merge, publish, distribute, sublicense, and/or sell copies of 
 * the Work, and to permit persons to whom the Work is furnished to do so, 
 * subject to the following conditions:

 * The above copyright notice and this permission notice shall be included in 
 * all copies or substantial portions of the Work.

 * THE WORK IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR 
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, 
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL 
 * THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER 
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING 
 * FROM, OUT OF OR IN CONNECTION WITH THE WORK OR THE USE OR OTHER DEALINGS  
 * IN THE WORK.
 */
package net.maxgigapop.mrs.driver.aws;

import net.maxgigapop.mrs.driver.aws.AwsModelBuilder;
import net.maxgigapop.mrs.driver.aws.AwsPush;
import net.maxgigapop.mrs.driver.aws.AwsDriver;
import com.amazonaws.regions.Regions;
import com.hp.hpl.jena.ontology.OntModel;
import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.AsyncResult;
import javax.ejb.Asynchronous;
import javax.ejb.EJBException;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import net.maxgigapop.mrs.bean.DriverInstance;
import net.maxgigapop.mrs.bean.DriverModel;
import net.maxgigapop.mrs.bean.DriverSystemDelta;
import net.maxgigapop.mrs.bean.VersionItem;
import net.maxgigapop.mrs.bean.persist.DeltaPersistenceManager;
import net.maxgigapop.mrs.bean.persist.DriverInstancePersistenceManager;
import net.maxgigapop.mrs.bean.persist.ModelPersistenceManager;
import net.maxgigapop.mrs.bean.persist.VersionItemPersistenceManager;
import net.maxgigapop.mrs.common.ModelUtil;
import net.maxgigapop.mrs.driver.IHandleDriverSystemCall;

/**
 *
 * @author muzcategui
 */
//TODO make the stirng returned by the push.progate function to be in JSON format
// and adapt it to the driver
//TODO make request not to be in the database as a driver property, as they do not
//truly get deleted.
@Stateless
public class AwsDriver implements IHandleDriverSystemCall {

    Logger logger = Logger.getLogger(AwsDriver.class.getName());

    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    @Override
    public void propagateDelta(DriverInstance driverInstance, DriverSystemDelta aDelta) {

        aDelta = (DriverSystemDelta) DeltaPersistenceManager.findById(aDelta.getId());

        String access_key_id = driverInstance.getProperty("aws_access_key_id");
        String secret_access_key = driverInstance.getProperty("aws_secret_access_key");
        String r = driverInstance.getProperty("region");
        String topologyURI = driverInstance.getProperty("topologyUri");
        Regions region = Regions.fromName(r);

        String model = driverInstance.getHeadVersionItem().getModelRef().getTtlModel();
        String modelAdd = aDelta.getModelAddition().getTtlModel();
        String modelReduc = aDelta.getModelReduction().getTtlModel();

        AwsPush push = new AwsPush(access_key_id, secret_access_key, region, topologyURI);
        String requests = null;
        requests = push.pushPropagate(model, modelAdd, modelReduc);
        String requestId = driverInstance.getId().toString() + aDelta.getId().toString();
        driverInstance.putProperty(requestId, requests);
        DriverInstancePersistenceManager.merge(driverInstance);
        Logger.getLogger(AwsDriver.class.getName()).log(Level.INFO, "AWS driver delta models succesfully propagated");
    }

    // Use ID to avoid passing entity bean between threads, which breaks persistence session
    @Asynchronous
    @Override
    public Future<String> commitDelta(DriverSystemDelta aDelta) {
        DriverInstance driverInstance = DriverInstancePersistenceManager.findById(aDelta.getDriverInstance().getId());
        if (driverInstance == null) {
            throw new EJBException(String.format("commitDelta see null driverInance for %s", aDelta));
        }

        String access_key_id = driverInstance.getProperty("aws_access_key_id");
        String secret_access_key = driverInstance.getProperty("aws_secret_access_key");
        String r = driverInstance.getProperty("region");
        String topologyURI = driverInstance.getProperty("topologyUri");
        Regions region = Regions.fromName(r);
        String requestId = driverInstance.getId().toString() + aDelta.getId().toString();
        String requests = driverInstance.getProperty(requestId);
        if (requests == null || requests.isEmpty()) {
            throw new EJBException(String.format("commitDelta encounters empty requests data for %s", driverInstance));
        }
        AwsPush push = new AwsPush(access_key_id, secret_access_key, region, topologyURI);
        push.pushCommit(requests);
        driverInstance.getProperties().remove(requestId);
        DriverInstancePersistenceManager.merge(driverInstance);

        Logger.getLogger(AwsDriver.class.getName()).log(Level.INFO, "AWS driver delta models succesfully commited");
        return new AsyncResult<String>("SUCCESS");
    }

    // Use ID to avoid passing entity bean between threads, which breaks persistence session
    @Asynchronous
    @Override
    public Future<String> pullModel(Long driverInstanceId) {

        DriverInstance driverInstance = DriverInstancePersistenceManager.findById(driverInstanceId);
        if (driverInstance == null) {
            throw new EJBException(String.format("pullModel cannot find driverInstance(id=%d)", driverInstanceId));
        }

        try {
            String access_key_id = driverInstance.getProperty("aws_access_key_id");
            String secret_access_key = driverInstance.getProperty("aws_secret_access_key");
            String r = driverInstance.getProperty("region");
            String topologyURI = driverInstance.getProperty("topologyUri");
            Regions region = Regions.fromName(r);
            OntModel ontModel = AwsModelBuilder.createOntology(access_key_id, secret_access_key, region, topologyURI);

            if (driverInstance.getHeadVersionItem() == null || !driverInstance.getHeadVersionItem().getModelRef().getOntModel().isIsomorphicWith(ontModel)) {
                DriverModel dm = new DriverModel();
                dm.setCommitted(true);
                dm.setOntModel(ontModel);
                ModelPersistenceManager.save(dm);

                VersionItem vi = new VersionItem();
                vi.setModelRef(dm);
                vi.setReferenceUUID(UUID.randomUUID().toString());
                vi.setDriverInstance(driverInstance);
                VersionItemPersistenceManager.save(vi);
                driverInstance.setHeadVersionItem(vi);
            }

        } catch (IOException e) {
            throw new EJBException(String.format("pullModel on %s raised exception[%s]", driverInstance, e.getMessage()));
        } catch (Exception ex) {
            Logger.getLogger(AwsDriver.class.getName()).log(Level.SEVERE, ex.getMessage());
        }

        //Logger.getLogger(AwsDriver.class.getName()).log(Level.INFO, "AWS driver ontology model succesfully pulled");
        return new AsyncResult<>("SUCCESS");
    }

}

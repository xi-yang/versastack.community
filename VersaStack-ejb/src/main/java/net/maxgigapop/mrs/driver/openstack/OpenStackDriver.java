/*
 * Copyright (c) 2013-2016 University of Maryland
 * Created by: Zan Wang 2015
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
package net.maxgigapop.mrs.driver.openstack;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hp.hpl.jena.ontology.OntModel;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
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
import org.json.simple.JSONObject;

/**
 *
 * @author muzcategui
 */
@Stateless
public class OpenStackDriver implements IHandleDriverSystemCall {

    static final Logger logger = Logger.getLogger(OpenStackDriver.class.getName());

    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    //@Override
    public void propagateDelta(DriverInstance driverInstance, DriverSystemDelta aDelta) {

        aDelta = (DriverSystemDelta) DeltaPersistenceManager.findById(aDelta.getId());

        String username = driverInstance.getProperty("username");
        String password = driverInstance.getProperty("password");
        String tenant = driverInstance.getProperty("tenant");
        String adminUsername = driverInstance.getProperty("adminUsername");
        String adminPassword = driverInstance.getProperty("adminPassword");
        String adminTenant = driverInstance.getProperty("adminTenant");
        String topologyURI = driverInstance.getProperty("topologyUri");
        String url = driverInstance.getProperty("url");
        String NATServer = driverInstance.getProperty("NATServer");
        String defaultImage = driverInstance.getProperty("defaultImage");
        String defaultFlavor = driverInstance.getProperty("defaultFlavor");

        OntModel model = driverInstance.getHeadVersionItem().getModelRef().getOntModel();
        OntModel modelAdd = aDelta.getModelAddition().getOntModel();
        OntModel modelReduc = aDelta.getModelReduction().getOntModel();

        OpenStackPush push = new OpenStackPush(url,NATServer, username, password, tenant, adminUsername, adminPassword, adminTenant, topologyURI, defaultImage, defaultFlavor);
        List<JSONObject> requests = null;
        String requestId = driverInstance.getId().toString() + aDelta.getId().toString();
        requests = push.propagate(model, modelAdd, modelReduc);
        driverInstance.putProperty(requestId, requests.toString());
        DriverInstancePersistenceManager.merge(driverInstance);
        Logger.getLogger(OpenStackDriver.class.getName()).log(Level.INFO, "OpenStack driver delta models succesfully propagated");
    }

    // Use ID to avoid passing entity bean between threads, which breaks persistence session
    @Asynchronous
    //@Override
    public Future<String> commitDelta(DriverSystemDelta aDelta) {

        DriverInstance driverInstance = aDelta.getDriverInstance();
        if (driverInstance == null) {
            throw new EJBException(String.format("commitDelta see null driverInance for %s", aDelta));
        }

        String username = driverInstance.getProperty("username");
        String password = driverInstance.getProperty("password");
        String tenant = driverInstance.getProperty("tenant");
        String adminUsername = driverInstance.getProperty("adminUsername");
        String adminPassword = driverInstance.getProperty("adminPassword");
        String adminTenant = driverInstance.getProperty("adminTenant");
        String topologyURI = driverInstance.getProperty("topologyUri");
        String url = driverInstance.getProperty("url");
        String NATServer = driverInstance.getProperty("NATServer");
        String defaultImage = driverInstance.getProperty("defaultImage");
        String defaultFlavor = driverInstance.getProperty("defaultFlavor");
        String requestId = driverInstance.getId().toString() + aDelta.getId().toString();
        String requests = driverInstance.getProperty(requestId);

        OpenStackPush push = new OpenStackPush(url,NATServer, username, password, tenant, adminUsername, adminPassword, adminTenant, topologyURI, defaultImage, defaultFlavor);
        ObjectMapper mapper = new ObjectMapper();
        List<JSONObject> r = new ArrayList();
        try {
            r = mapper.readValue(requests, mapper.getTypeFactory().constructCollectionType(List.class, JSONObject.class));
        } catch (IOException ex) {
            throw new EJBException("failed to load JSON requests due to " + ex);
        }
        try {
            push.pushCommit(r, url, NATServer, username, password, tenant, topologyURI);
        } catch (InterruptedException ex) {
            throw new EJBException("failed to pushCommit due to " + ex);
        }

        driverInstance.getProperties().remove(requestId);
        DriverInstancePersistenceManager.merge(driverInstance);

        Logger.getLogger(OpenStackDriver.class.getName()).log(Level.INFO, "OpenStack driver delta models succesfully commited");
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
            String username = driverInstance.getProperty("username");
            String password = driverInstance.getProperty("password");
            String tenant = driverInstance.getProperty("tenant");
            String url = driverInstance.getProperty("url");
            String topologyUri = driverInstance.getProperty("topologyUri");
            String NATServer = driverInstance.getProperty("NATServer");
            String modelExtTtl = driverInstance.getProperty("modelExt");
            String adminUsername = driverInstance.getProperty("adminUsername");
            String adminPassword = driverInstance.getProperty("adminPassword");
            String adminTenant = driverInstance.getProperty("adminTenant");
            OntModel modelExt = null;
            if (modelExtTtl != null && !modelExtTtl.isEmpty()) {
                modelExt = ModelUtil.unmarshalOntModel(modelExtTtl);
            }

            OntModel ontModel = OpenStackNeutronModelBuilder.createOntology(url, NATServer, topologyUri, username, password, tenant, 
                    adminUsername, adminPassword, adminTenant, modelExt);
            
            // combine injected Model extension
            if (driverInstance.getHeadVersionItem() == null 
                    || !driverInstance.getHeadVersionItem().getModelRef().getOntModel().isIsomorphicWith(ontModel)) {
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
        } catch (Exception ex) {
            Logger.getLogger(OpenStackDriver.class.getName()).log(Level.SEVERE, ex.getMessage());
            throw new EJBException(String.format("pullModel on %s raised exception[%s]", driverInstance, ex.getMessage()));
        }
        return new AsyncResult<>("SUCCESS");
    }

}

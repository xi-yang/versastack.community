/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.maxgigapop.mrs.driver;

import com.hp.hpl.jena.ontology.OntModel;
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
import net.maxgigapop.mrs.bean.persist.PersistenceManager;
import net.maxgigapop.mrs.bean.persist.VersionItemPersistenceManager;
import net.maxgigapop.mrs.common.ModelUtil;

/**
 *
 * @author xyang
 */
//use properties: stubModelTtl
@Stateless
public class StubSystemDriver implements IHandleDriverSystemCall {

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void propagateDelta(DriverInstance driverInstance, DriverSystemDelta aDelta) {
        driverInstance = DriverInstancePersistenceManager.findById(driverInstance.getId());
        aDelta = (DriverSystemDelta) DeltaPersistenceManager.findById(aDelta.getId());
        String ttlModel = driverInstance.getProperty("stubModelTtl");
        if (ttlModel == null) {
            throw new EJBException(String.format("%s has no property key=stubModelTtl", driverInstance));
        }
        String ttlModelNew = driverInstance.getProperty("stubModelTtl_new");
        if (ttlModelNew != null && !ttlModelNew.isEmpty()) {
            throw new EJBException(String.format("%s has propagated but uncommitted change.", driverInstance));
        }
        try {
            OntModel ontModel = ModelUtil.unmarshalOntModel(ttlModel);
            DriverModel dm = new DriverModel();
            dm.setOntModel(ontModel);
            ontModel = dm.applyDelta(aDelta);
            ttlModel = ModelUtil.marshalModel(ontModel.getBaseModel());
            // the extra "new:" makes two ttls always different to avoid new version_item being created before commit   
            driverInstance.putProperty("stubModelTtl_new", "new:" + ttlModel);
            DriverInstancePersistenceManager.merge(driverInstance);
        } catch (Exception ex) {
            throw new EJBException(String.format("propagateDelta for %s with %s raised exception(%s)", driverInstance, aDelta, ex.getMessage()));
        }
    }

    @Override
    @Asynchronous
    public Future<String> commitDelta(DriverSystemDelta aDelta) {
        DriverInstance driverInstance = DriverInstancePersistenceManager.findById(aDelta.getDriverInstance().getId());
        if (driverInstance == null) {
            throw new EJBException(String.format("commitDelta see null driverInance for %s", aDelta));
        }
        String ttlModelNew = driverInstance.getProperty("stubModelTtl_new");
        if (ttlModelNew == null || ttlModelNew.isEmpty() || !ttlModelNew.startsWith("new:")) {
            throw new EJBException(String.format("commitDelta sees nothing new (null or empty stubModelTtl_new)"));
        }
        ttlModelNew = ttlModelNew.replaceFirst("new:", "");
        try {
            OntModel ontModelNew = ModelUtil.unmarshalOntModel(ttlModelNew);
            String ttlModel = driverInstance.getProperty("stubModelTtl");
            OntModel ontModel = ModelUtil.unmarshalOntModel(ttlModel);
            if (!ontModel.getBaseModel().isIsomorphicWith(ontModelNew.getBaseModel())) {
                driverInstance.putProperty("stubModelTtl", ttlModelNew);
                driverInstance.putProperty("stubModelTtl_new", ttlModelNew);
            } else {
                driverInstance.putProperty("stubModelTtl_new", "");
            }
            DriverInstancePersistenceManager.merge(driverInstance);
        } catch (Exception ex) {
            throw new EJBException(String.format("commitDelta for %s with %s raised exception(%s)", driverInstance, aDelta, ex.getMessage()));
        }
        return new AsyncResult<String>("SUCCESS");
    }

    @Override
    @Asynchronous
    public Future<String> pullModel(Long driverInstanceId) {
        DriverInstance driverInstance = DriverInstancePersistenceManager.findById(driverInstanceId);
        if (driverInstance == null) {
            throw new EJBException(String.format("pullModel cannot find driverInance(id=%d)", driverInstanceId));
        }
        String ttlModel = driverInstance.getProperty("stubModelTtl");
        if (ttlModel == null) {
            throw new EJBException(String.format("%s has no stubModelTtl property configured", driverInstance));
        }
        String ttlModelNew = driverInstance.getProperty("stubModelTtl_new");
        // The first time model is pulled, add an empty stubModelTtl_new property.
        // Next time, if stubModelTtl_new remains empty or different from stubModelTtl, skip. 
        if (ttlModelNew == null) {
            driverInstance.putProperty("stubModelTtl_new", "");
        } else if (ttlModelNew.isEmpty()
                || !ttlModelNew.equals(ttlModel)) {
            return new AsyncResult<String>("SUCCESS");
        }
        // If first time or a stubModelTtl_new has been commited to stubModelTtl, create VI
        try {
            OntModel ontModel = ModelUtil.unmarshalOntModel(ttlModel);
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
            driverInstance.putProperty("stubModelTtl_new", "");
            DriverInstancePersistenceManager.merge(driverInstance);
        } catch (Exception e) {
            throw new EJBException(String.format("pullModel on %s raised exception[%s]", driverInstance, e.getMessage()));
        }
        return new AsyncResult<String>("SUCCESS");
    }

}

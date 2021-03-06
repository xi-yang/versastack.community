/*
 * Copyright (c) 2013-2016 University of Maryland
 * Created by: Xi Yang 2014

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

package net.maxgigapop.mrs.service.orchestrate;

import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJBException;
import net.maxgigapop.mrs.bean.DeltaModel;
import net.maxgigapop.mrs.bean.ServiceDelta;
import net.maxgigapop.mrs.common.ModelUtil;
import net.maxgigapop.mrs.service.compile.CompilerBase;
import net.maxgigapop.mrs.service.compile.CompilerFactory;
import net.maxgigapop.mrs.service.compute.MCE_AwsDxStitching;

/**
 *
 * @author xyang
 */
public class SimpleWorker extends WorkerBase {

    private static final Logger log = Logger.getLogger(SimpleWorker.class.getName());

    @Override
    public void run() {
        retrieveSystemModel();
        try {
            CompilerBase simpleCompiler = CompilerFactory.createCompiler("net.maxgigapop.mrs.service.compile.SimpleCompiler");
            if (this.annoatedModelDelta == null) {
                throw new EJBException(SimpleWorker.class.getName() + " encounters null annoatedModelDelta");
            }
            simpleCompiler.setSpaDelta(this.annoatedModelDelta);
            simpleCompiler.compile(this);
            this.runWorkflow();
        } catch (Exception ex) {
            throw new EJBException(SimpleWorker.class.getName() + " caught exception: " + ex);
        }
    }
}

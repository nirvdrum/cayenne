/* ====================================================================
 * 
 * The ObjectStyle Group Software License, version 1.1
 * ObjectStyle Group - http://objectstyle.org/
 * 
 * Copyright (c) 2002-2004, Andrei (Andrus) Adamchik and individual authors
 * of the software. All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 * 
 * 3. The end-user documentation included with the redistribution, if any,
 *    must include the following acknowlegement:
 *    "This product includes software developed by independent contributors
 *    and hosted on ObjectStyle Group web site (http://objectstyle.org/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 * 
 * 4. The names "ObjectStyle Group" and "Cayenne" must not be used to endorse
 *    or promote products derived from this software without prior written
 *    permission. For written permission, email
 *    "andrus at objectstyle dot org".
 * 
 * 5. Products derived from this software may not be called "ObjectStyle"
 *    or "Cayenne", nor may "ObjectStyle" or "Cayenne" appear in their
 *    names without prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE OBJECTSTYLE GROUP OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 * 
 * This software consists of voluntary contributions made by many
 * individuals and hosted on ObjectStyle Group web site.  For more
 * information on the ObjectStyle Group, please see
 * <http://objectstyle.org/>.
 */
package org.objectstyle.cayenne.modeler.dialog.db;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;

import javax.swing.JOptionPane;

import org.apache.log4j.Logger;
import org.objectstyle.cayenne.access.DataNode;
import org.objectstyle.cayenne.access.DbGenerator;
import org.objectstyle.cayenne.conn.DataSourceInfo;
import org.objectstyle.cayenne.dba.DbAdapter;
import org.objectstyle.cayenne.map.DataMap;
import org.objectstyle.cayenne.map.DbAttribute;
import org.objectstyle.cayenne.map.DbEntity;
import org.objectstyle.cayenne.map.DbRelationship;
import org.objectstyle.cayenne.map.DerivedDbEntity;
import org.objectstyle.cayenne.map.Entity;
import org.objectstyle.cayenne.modeler.Application;
import org.objectstyle.cayenne.modeler.InteractiveLogin;
import org.objectstyle.cayenne.modeler.ModelerClassLoader;
import org.objectstyle.cayenne.modeler.dialog.validator.ValidationDisplayHandler;
import org.objectstyle.cayenne.project.ProjectDataSource;
import org.objectstyle.cayenne.project.validator.ValidationInfo;
import org.objectstyle.cayenne.project.validator.Validator;
import org.scopemvc.controller.basic.BasicController;
import org.scopemvc.core.Control;
import org.scopemvc.core.ControlException;

/**
 * Controller for DbEntityValidationDialog.
 * 
 * @author Andrei Adamchik
 */
public class GenerateDbController extends BasicController {
    private static Logger logObj = Logger.getLogger(GenerateDbController.class);

    public static final String CANCEL_CONTROL =
        "cayenne.modeler.dbentityvalidation.cancel.button";

    public static final String GENERATION_OPTIONS_CONTROL =
        "cayenne.modeler.dbentityvalidation.generateopts.button";

    protected DataMap dataMap;
    protected DataNode node;
    protected DbAdapter adapter;
    protected DataSourceInfo dataSourceInfo;

    public GenerateDbController(DataNode node, DataMap dataMap) {
        this.node = node;
        this.dataMap = dataMap;
    }

    public void startup() {
        Collection validationProblems = validateDbEntities();
        if (validationProblems.isEmpty()) {
            runDbGeneration();
            return;
        }
        else {
            initForValidationDialog(validationProblems);
        }

        super.startup();
    }

    /**
     * Processes incoming Control objects.
     */
    protected void doHandleControl(Control control) throws ControlException {
        if (control.matchesID(CANCEL_CONTROL)) {
            shutdown();
        }
        else if (control.matchesID(GENERATION_OPTIONS_CONTROL)) {
            shutdown();
            runDbGeneration();
        }
    }

    protected void initForValidationDialog(Collection validationProblems) {
        setModel(validationProblems);
        setView(new DbEntityValidationDialog());
    }

    protected void runDbGeneration() {
        collectDatabaseInfo();
        if (adapter == null || dataSourceInfo == null) {
            return;
        }

        Collection excludeEntities = Collections.EMPTY_LIST;
        Object validationProblems = getModel();

        // get a list of invalid DbEntities...
        if (validationProblems instanceof Collection) {
            Collection problems = (Collection) validationProblems;

            if (!problems.isEmpty()) {
                Iterator it = problems.iterator();
                excludeEntities = new ArrayList(problems.size());
                while (it.hasNext()) {
                    ValidationInfo info = (ValidationInfo) it.next();
                    excludeEntities.add(info.getValidatedObject());
                }
            }
        }

        // try to create DbGenerator. This may fail if the model is in invalid state
        DbGenerator generator = null;

        try {
            generator = new DbGenerator(adapter, dataMap, excludeEntities);
        }
        catch (Exception ex) {
            logObj.info("problem generating schema (missing map info?)", ex);
            JOptionPane.showMessageDialog(
            Application.getFrame(),
                "Error generating schema: " + ex.getMessage());
            return;
        }

        // check if there are any tables to generate
        if (generator.isEmpty(false)) {
            logObj.info("Nothing to generate");
            JOptionPane.showMessageDialog(Application.getFrame(), "Nothing to generate.");
            return;
        }

        // TODO: reimplement GenerateDbDialog with Scope..
        GenerateDbDialog dialog =
            new GenerateDbDialog(dataSourceInfo, adapter, generator);
        dialog.show();
        dialog.dispose();
    }

    /**
     * Collects information about database connection parameters.
     */
    protected void collectDatabaseInfo() {
        this.adapter = null;
        this.dataSourceInfo = null;

        // Get connection info
        while (true) {
            DataSourceInfo localDataSource = null;

            // init defaults if possible
            if (node != null) {
                localDataSource =
                    ((ProjectDataSource) node.getDataSource())
                        .getDataSourceInfo()
                        .cloneInfo();
                if (node.getAdapter() != null) {
                    localDataSource.setAdapterClassName(
                        node.getAdapter().getClass().getName());
                }
            }
            else {
                localDataSource = new DataSourceInfo();
            }

            // run DB login panel to allow user to modify defaults
            InteractiveLogin loginObj =
                InteractiveLogin.getGuiLoginObject(localDataSource);
            loginObj.collectLoginInfo();
            this.dataSourceInfo = loginObj.getDataSrcInfo();

            // nothing....
            if (dataSourceInfo == null) {
                return;
            }

            if (dataSourceInfo.getAdapterClassName() == null
                || dataSourceInfo.getAdapterClassName().trim().length() == 0) {
                JOptionPane.showMessageDialog(
                Application.getFrame(),
                    "Must specify DB Adapter");
                continue;
            }
            try {
                Class adapterClass = ModelerClassLoader.getClassLoader().loadClass(
                        dataSourceInfo.getAdapterClassName());

                this.adapter = (DbAdapter) adapterClass.newInstance();
                break;
            }
            catch (InstantiationException e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(
                Application.getFrame(),
                    e.getMessage(),
                    "Error creating DbAdapter",
                    JOptionPane.ERROR_MESSAGE);
                continue;
            }
            catch (IllegalAccessException e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(
                Application.getFrame(),
                    e.getMessage(),
                    "Error creating DbAdapter",
                    JOptionPane.ERROR_MESSAGE);
                continue;
            }
            catch (ClassNotFoundException e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(
                Application.getFrame(),
                    e.getMessage(),
                    "Error creating DbAdapter",
                    JOptionPane.ERROR_MESSAGE);
                continue;
            }
        }
    }

    /**
      * Performs validation of DbEntities in the current DataMap.
      * Returns a collection of ValidationInfo objects describing the
      * problems.
      */
    protected Collection validateDbEntities() {
        Validator validator = Application.getProject().getValidator();
        int validationCode = validator.validate();

        // if there were errors, filter out those related to
        // non-derived DbEntities...

        // TODO: this is inefficient.. we need targeted validation 
        // instead of doing it on the whole project
        if (validationCode >= ValidationDisplayHandler.WARNING) {
            Collection allEntities = dataMap.getDbEntities();
            Iterator it = validator.validationResults().iterator();
            Collection failed = new ArrayList();
            while (it.hasNext()) {
                ValidationInfo nextProblem = (ValidationInfo) it.next();
                Entity failedEntity = null;

                if (nextProblem.getValidatedObject() instanceof DbAttribute) {
                    DbAttribute failedAttribute =
                        (DbAttribute) nextProblem.getValidatedObject();
                    failedEntity = failedAttribute.getEntity();
                }
                else if (nextProblem.getValidatedObject() instanceof DbRelationship) {
                    DbRelationship failedRelationship =
                        (DbRelationship) nextProblem.getValidatedObject();
                    failedEntity = failedRelationship.getSourceEntity();
                }
                else if (nextProblem.getValidatedObject() instanceof DbEntity) {
                    failedEntity = (Entity) nextProblem.getValidatedObject();
                }

                if (failedEntity == null) {
                    continue;
                }

                if (failedEntity instanceof DerivedDbEntity) {
                    continue;
                }

                if (!allEntities.contains(failedEntity)) {
                    continue;
                }

                failed.add(nextProblem);
            }

            return failed;
        }

        return Collections.EMPTY_LIST;
    }
}

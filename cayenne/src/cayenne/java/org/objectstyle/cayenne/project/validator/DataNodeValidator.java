/* ====================================================================
 * 
 * The ObjectStyle Group Software License, Version 1.0 
 *
 * Copyright (c) 2002 The ObjectStyle Group 
 * and individual authors of the software.  All rights reserved.
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
 * 3. The end-user documentation included with the redistribution, if
 *    any, must include the following acknowlegement:  
 *       "This product includes software developed by the 
 *        ObjectStyle Group (http://objectstyle.org/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 *
 * 4. The names "ObjectStyle Group" and "Cayenne" 
 *    must not be used to endorse or promote products derived
 *    from this software without prior written permission. For written 
 *    permission, please contact andrus@objectstyle.org.
 *
 * 5. Products derived from this software may not be called "ObjectStyle"
 *    nor may "ObjectStyle" appear in their names without prior written
 *    permission of the ObjectStyle Group.
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
 * individuals on behalf of the ObjectStyle Group.  For more
 * information on the ObjectStyle Group, please see
 * <http://objectstyle.org/>.
 *
 */
package org.objectstyle.cayenne.project.validator;

import java.util.Iterator;

import org.apache.log4j.Logger;
import org.objectstyle.cayenne.access.DataDomain;
import org.objectstyle.cayenne.access.DataNode;
import org.objectstyle.cayenne.conf.DriverDataSourceFactory;
import org.objectstyle.cayenne.project.ProjectPath;
import org.objectstyle.cayenne.util.Util;

/**
 * @author Andrei Adamchik
 */
public class DataNodeValidator extends TreeNodeValidator {
    private static Logger logObj = Logger.getLogger(DataNodeValidator.class);

    /**
     * Constructor for DataNodeValidator.
     */
    public DataNodeValidator() {
        super();
    }

    public void validateObject(ProjectPath path, Validator validator) {
        DataNode node = (DataNode) path.getObject();
        validateName(node, path, validator);
        validateConnection(node, path, validator);
    }

    protected void validateConnection(
        DataNode node,
        ProjectPath path,
        Validator validator) {
        String factory = node.getDataSourceFactory();

        // If direct factory, make sure the location is a valid file name.
        if (Util.isEmptyString(factory)) {
            validator.registerError("No DataSource factory.", path);
        } else if(!DriverDataSourceFactory.class.getName().equals(factory)) {
            String location = node.getDataSourceLocation();
            if (Util.isEmptyString(location)) {
                validator.registerError("DataNode has no location parameter.", path);
            }
        }

        if (node.getAdapter() == null) {
            validator.registerWarning("DataNode has no DBAdapter.", path);
        }
    }

    protected void validateName(DataNode node, ProjectPath path, Validator validator) {
        String name = node.getName();

        if (Util.isEmptyString(name)) {
            validator.registerError("Unnamed DataNode.", path);
            return;
        }

        DataDomain domain = (DataDomain) path.getObjectParent();
        if (domain == null) {
            return;
        }

        // check for duplicate names in the parent context
        Iterator it = domain.getDataNodesAsList().iterator();
        while (it.hasNext()) {
            DataNode otherNode = (DataNode) it.next();
            if (otherNode == node) {
                continue;
            }

            if (name.equals(otherNode.getName())) {
                validator.registerError("Duplicate DataNode name: " + name + ".", path);
                break;
            }
        }
    }
}

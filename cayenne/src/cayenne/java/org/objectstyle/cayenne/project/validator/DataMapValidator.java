/* ====================================================================
 * 
 * The ObjectStyle Group Software License, Version 1.0 
 *
 * Copyright (c) 2002-2003 The ObjectStyle Group 
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

import org.objectstyle.cayenne.access.DataDomain;
import org.objectstyle.cayenne.access.DataNode;
import org.objectstyle.cayenne.map.DataMap;
import org.objectstyle.cayenne.project.ProjectPath;
import org.objectstyle.cayenne.util.Util;

/**
 * Validator for DataMaps.
 * 
 * @author Andrei Adamchik
 */
public class DataMapValidator extends TreeNodeValidator {

    /**
     * Constructor for DataMapValidator.
     */
    public DataMapValidator() {
        super();
    }

    public void validateObject(ProjectPath path, Validator validator) {
        DataMap map = (DataMap) path.getObject();
        validateName(map, path, validator);

        // check if data map is not attached to any nodes
        validateNodeLinks(map, path, validator);
    }

    protected void validateNodeLinks(DataMap map, ProjectPath path, Validator validator) {
        DataDomain domain = (DataDomain) path.getObjectParent();
        if (domain == null) {
            return;
        }
        
        boolean unlinked = true;
        int nodeCount = 0;
        Iterator it = domain.getDataNodes().iterator();
        while(it.hasNext()) {
        	DataNode node = (DataNode)it.next();
        	nodeCount++;
        	if(node.getDataMaps().contains(map)) {
        		unlinked = false;
        		break;
        	}
        }
        
        if(unlinked && nodeCount > 0) {
        	 validator.registerWarning("DataMap is not linked to any DataNodes.", path);
        }
    }

    protected void validateName(DataMap map, ProjectPath path, Validator validator) {
        String name = map.getName();

        if (Util.isEmptyString(name)) {
            validator.registerError("Unnamed DataMap.", path);
            return;
        }

        DataDomain domain = (DataDomain) path.getObjectParent();
        if (domain == null) {
            return;
        }

        // check for duplicate names in the parent context
        Iterator it = domain.getDataMaps().iterator();
        while (it.hasNext()) {
            DataMap otherMap = (DataMap) it.next();
            if (otherMap == map) {
                continue;
            }

            if (name.equals(otherMap.getName())) {
                validator.registerError("Duplicate DataMap name: " + name + ".", path);
                return;
            }
        }
    }
}

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

package org.objectstyle.cayenne.modeler.validator;

import javax.swing.JFrame;

import org.objectstyle.cayenne.modeler.Editor;
import org.objectstyle.cayenne.modeler.control.*;
import org.objectstyle.cayenne.modeler.event.*;
import org.objectstyle.cayenne.access.DataDomain;
import org.objectstyle.cayenne.map.DataMap;
import org.objectstyle.cayenne.map.Entity;
import org.objectstyle.cayenne.map.Attribute;
import org.objectstyle.cayenne.project.validator.ValidationResult;

/**
 * Attribute validation message.
 * 
 * @author Misha Shengaout
 * @author Andrei Adamchik
 */
public class AttributeErrorMsg extends ValidationDisplayHandler {
    protected DataMap map;
    protected Entity entity;
    protected Attribute attribute;

    /**
     * Constructor for AttributeErrorMsg.
     * @param result
     */
    public AttributeErrorMsg(ValidationResult result) {
        super(result);

        Object[] path = result.getTreeNodePath();
        int len = path.length;

        if (len >= 1) {
            attribute = (Attribute) path[len - 1];
        }

        if (len >= 2) {
            entity = (Entity) path[len - 2];
        }

        if (len >= 3) {
            map = (DataMap) path[len - 3];
        }

        if (len >= 4) {
            domain = (DataDomain) path[len - 4];
        }

    }

    public void displayField(EventController mediator, JFrame frame) {
        AttributeDisplayEvent event;
        event = new AttributeDisplayEvent(frame, attribute, entity, map, domain);
        if (entity instanceof org.objectstyle.cayenne.map.ObjEntity)
            mediator.fireObjAttributeDisplayEvent(event);
        else if (entity instanceof org.objectstyle.cayenne.map.DbEntity)
            mediator.fireDbAttributeDisplayEvent(event);
    }
}
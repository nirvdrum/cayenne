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
package org.objectstyle.cayenne.dataport;

import java.util.List;

import org.objectstyle.cayenne.map.DbEntity;
import org.objectstyle.cayenne.query.Query;

/**
 * Interface for callback and delegate methods allowing implementing classes
 * to control various aspects of data porting via DataPort. DataPort instance
 * will invoke appropriate delegate methods during different stages of porting
 * process.
 * 
 * @author Andrei Adamchik
 */
public interface DataPortDelegate {
    /**
     * Allows delegate to sort or otherwise alter a list of DbEntities
     * right before the port starts.
     */
    public List willPortEntities(DataPort portTool, List entities);

    /**
     * Invoked by DataPort right before the start of data port 
     * for a given entity. Allows delegate to handle such things like 
     * logging, etc. Also makes it possible to
     * alter the select query used to cleanup the data, e.g. set a limiting
     * qualifier.
     */
    public void willPortEntity(DataPort portTool, DbEntity entity, Query query);

    /**
     * Invoked by DataPort right after the end of data port 
     * for a given entity. Allows delegate to handle such things like 
     * logging, etc.
     */
    public void didPortEntity(DataPort portTool, DbEntity entity, int rowCount);

    /**
     * Allows delegate to sort or otherwise alter a list of DbEntities
     * right before data cleanup starts.
     */
    public List willCleanData(DataPort portTool, List entities);

    /**
     * Invoked by DataPort right before the start of data cleanup 
     * for a given entity. Allows delegate to handle such things like 
     * logging, etc. Also makes it possible to
     * alter the delete query used to cleanup the data, e.g. set a limiting
     * qualifier.
     */
    public void willCleanData(DataPort portTool, DbEntity entity, Query query);

    /**
     * Invoked by DataPort right after the end of data cleanup 
     * for a given entity. Allows delegate to handle such things like 
     * logging, etc.
     */
    public void didCleanData(DataPort portTool, DbEntity entity, int rowCount);
}

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
package org.objectstyle.cayenne.project;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.objectstyle.cayenne.access.DataDomain;
import org.objectstyle.cayenne.conf.Configuration;
import org.objectstyle.cayenne.map.DataMap;
import org.objectstyle.cayenne.map.Entity;

/**
 * ProjectTraversal allows to traverse Cayenne project tree in a
 * "depth-first" order starting from an arbitrary level to its children. 
 * 
 * <p><i>Current implementation is not very efficient
 * and would actually first read the whole tree, before returning 
 * the first element from the iterator.</i></p>
 * 
 * @author Andrei Adamchik
 */
public class ProjectTraversal {
	
	protected static final ProjectTraversal sharedInstance = new ProjectTraversal();
	
	/**
	 * Returns ProjectTraversal singleton.
	 */
	public static ProjectTraversal getInstance() {
		return sharedInstance;
	}
	
    public void addConfig(List list, Configuration config, Object[] path) {
        Object[] configPath = FlatProjectView.buildPath(config, path);
        list.add(configPath);
        
        addDomains(list, config.getDomainList(), path);
    }

    public void addDomains(List list, List domains, Object[] path) {
        Iterator it = domains.iterator();
        while (it.hasNext()) {
            DataDomain domain = (DataDomain) it.next();
            Object[] domainPath = FlatProjectView.buildPath(domain, path);
            list.add(domainPath);

            addNodes(list, domain.getDataNodeList(), domainPath);
            addMaps(list, domain.getMapList(), domainPath);
        }
    }

    public void addNodes(List list, List nodes, Object[] path) {
        Iterator it = nodes.iterator();
        while (it.hasNext()) {
            list.add(FlatProjectView.buildPath(it.next(), path));
        }
    }

    public void addMaps(List list, List maps, Object[] path) {
        Iterator it = maps.iterator();
        while (it.hasNext()) {
            DataMap map = (DataMap) it.next();
            Object[] mapPath = FlatProjectView.buildPath(map, path);
            list.add(mapPath);

            addEntities(list, map.getDbEntitiesAsList(), mapPath);
            addEntities(list, map.getObjEntitiesAsList(), mapPath);
        }
    }

    public void addEntities(List list, List entities, Object[] path) {
        Iterator it = entities.iterator();
        while (it.hasNext()) {
            Entity ent = (Entity) it.next();
            Object[] entPath = FlatProjectView.buildPath(ent, path);
            list.add(entPath);

            addAttributes(list, ent.getAttributeList(), entPath);
            addRelationships(list, ent.getRelationshipList(), entPath);
        }
    }

    public void addAttributes(List list, List attributes, Object[] path) {
        Iterator it = attributes.iterator();
        while (it.hasNext()) {
            list.add(FlatProjectView.buildPath(it.next(), path));
        }
    }

    public void addRelationships(List list, List relationships, Object[] path) {
        Iterator it = relationships.iterator();
        while (it.hasNext()) {
            list.add(FlatProjectView.buildPath(it.next(), path));
        }
    }
}

/*****************************************************************
 *   Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 ****************************************************************/
package org.apache.cayenne.access.reveng;

import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.DbRelationship;

/**
 * NamingStrategy is a strategy for creating names for entities, attributes, relationships
 * during reverse engineering.
 * 
 * @since 3.0
 * @author Andrey Razumovsky
 */
public interface NamingStrategy {
    /**
     * Creates new name for Obj Entity 
     */
    public String createObjEntityName(DbEntity entity);
    
    /**
     * Creates new name for Obj Attribute 
     */
    public String createObjAttributeName(DbAttribute attr);
    
    /**
     * Creates new name for Db Relationship 
     */
    public String createDbRelationshipName(
            ExportedKey key,
            boolean toMany);
    
    /**
     * Creates new name for Obj Relationship 
     */
    public String createObjRelationshipName(DbRelationship dbRel);
}
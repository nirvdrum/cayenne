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
package org.apache.cayenne.query;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.map.ObjEntity;

/**
 * @since 3.0
 * @author Andrus Adamchik
 */
class SQLTemplateMetadata extends BaseQueryMetadata {

    boolean resolve(Object root, EntityResolver resolver, SQLTemplate query) {

        if (super.resolve(root, resolver, null)) {

            // generate unique cache key...
            if (QueryMetadata.NO_CACHE.equals(getCachePolicy())) {

            }
            else if (query.getName() != null) {
                this.cacheKey = query.getName();
            }
            else {

                // create a unique key based on entity, SQL, and parameters

                StringBuffer key = new StringBuffer();
                ObjEntity entity = getObjEntity();
                if (entity != null) {
                    key.append(entity.getName());
                }
                else if (dbEntity != null) {
                    key.append("db:").append(dbEntity.getName());
                }

                if (query.getDefaultTemplate() != null) {
                    key.append('/').append(query.getDefaultTemplate());
                }

                Map parameters = query.getParameters();
                if (!parameters.isEmpty()) {

                    List keys = new ArrayList(parameters.keySet());
                    Collections.sort(keys);

                    Iterator it = keys.iterator();
                    while (it.hasNext()) {
                        Object parameterKey = it.next();
                        key.append('/').append(parameterKey).append('=').append(
                                parameters.get(parameterKey));
                    }
                }

                this.cacheKey = key.toString();
            }

            return true;
        }

        return false;
    }
}

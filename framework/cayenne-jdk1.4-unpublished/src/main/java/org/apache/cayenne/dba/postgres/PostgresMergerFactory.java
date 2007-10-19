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
package org.apache.cayenne.dba.postgres;

import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.merge.MergeDirection;
import org.apache.cayenne.merge.MergerFactory;
import org.apache.cayenne.merge.MergerToken;
import org.apache.cayenne.merge.SetColumnType;

public class PostgresMergerFactory extends MergerFactory {

    public MergerToken createSetColumnType(MergeDirection direction, final DbEntity entity, DbAttribute columnOriginal, final DbAttribute columnNew) {

        return new SetColumnType(direction, entity, columnOriginal, columnNew) {

            protected void appendPrefix(StringBuffer sqlBuffer) {
                // http://www.postgresql.org/docs/8.2/static/sql-altertable.html
                sqlBuffer.append("ALTER TABLE ");
                sqlBuffer.append(entity.getFullyQualifiedName());
                sqlBuffer.append(" ALTER ");
                sqlBuffer.append(columnNew.getName());
                sqlBuffer.append(" TYPE ");
            }
        };
    }
}
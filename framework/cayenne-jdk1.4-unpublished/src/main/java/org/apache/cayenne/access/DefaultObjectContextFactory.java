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
package org.apache.cayenne.access;

import java.util.Map;

import org.apache.cayenne.DataChannel;
import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.ObjectContextFactory;

/**
 * A default implementation of the {@link ObjectContextFactory} that builds contexts based
 * on the mapped DataDomain configuration.
 * 
 * @author Andrus Adamchik
 * @since 3.0
 */
class DefaultObjectContextFactory implements ObjectContextFactory {

    protected DataDomain domain;

    DefaultObjectContextFactory(DataDomain domain) {
        this.domain = domain;
    }

    public ObjectContext createObjectContext(Map properties) {
        return createObjectContext(domain, properties);
    }

    public ObjectContext createObjectContext(DataChannel parent, Map properties) {
        return null;
    }

}
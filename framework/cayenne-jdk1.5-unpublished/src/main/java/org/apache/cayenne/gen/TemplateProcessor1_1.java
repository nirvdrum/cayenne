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
package org.apache.cayenne.gen;

import java.io.Writer;

import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.ObjEntity;

/**
 * Encapsulates a legacy class generation template.
 * 
 * @deprecated since 3.0
 * @author Andrus Adamchik
 */
class TemplateProcessor1_1 extends TemplateProcessor {

    private ClassGenerationInfo classGenerationInfo;

    public TemplateProcessor1_1(String template) throws Exception {

        super(template, null);
        this.classGenerationInfo = new ClassGenerationInfo();
        velocityContext.put("classGen", classGenerationInfo);
    }

    @Override
    public void generateClass(
            Writer out,
            DataMap dataMap,
            ObjEntity entity,
            String fqnBaseClass,
            String fqnSuperClass,
            String fqnSubClass) throws Exception {

        classGenerationInfo.setObjEntity(entity);
        classTemplate.merge(velocityContext, out);
    }

    ClassGenerationInfo getClassGenerationInfo() {
        return classGenerationInfo;
    }
}
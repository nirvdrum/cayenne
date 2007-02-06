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
package org.apache.cayenne.jpa.itest.ch3.entity;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.PostLoad;
import javax.persistence.PostPersist;
import javax.persistence.PostRemove;
import javax.persistence.PostUpdate;
import javax.persistence.PrePersist;
import javax.persistence.PreRemove;
import javax.persistence.PreUpdate;
import javax.persistence.Transient;

@Entity
public class CallbackEntity {

    @Id
    protected int id;
    protected String property1;

    @Transient
    protected transient boolean prePersistCalled1;

    @Transient
    protected transient boolean prePersistCalled2;

    @Transient
    protected transient boolean postPersistCalled;

    @Transient
    protected transient boolean preRemoveCalled;

    @Transient
    protected transient boolean postRemoveCalled;

    @Transient
    protected transient boolean preUpdateCalled;

    @Transient
    protected transient boolean postUpdateCalled;

    @Transient
    protected transient boolean postLoadCalled;

    public int idField() {
        return id;
    }

    public void updateIdField(int id) {
        this.id = id;
    }

    public String getProperty1() {
        return property1;
    }

    public void setProperty1(String property1) {
        this.property1 = property1;
    }

    @PrePersist
    public void prePersistMethod1() {
        prePersistCalled1 = true;
    }

    @PrePersist
    public void prePersistMethod2() {
        prePersistCalled2 = true;
    }

    @PostPersist
    public void postPersistMethod() {
        postPersistCalled = true;
    }

    @PreRemove
    public void preRemoveMethod() {
        preRemoveCalled = true;
    }

    @PostRemove
    public void postRemoveMethod() {
        postRemoveCalled = true;
    }

    @PreUpdate
    public void preUpdateMethod() {
        preUpdateCalled = true;
    }

    @PostUpdate
    public void postUpdateMethod() {
        postUpdateCalled = true;
    }

    @PostLoad
    public void postLoadMethod() {
        postLoadCalled = true;
    }

    public int getId() {
        return id;
    }

    public boolean isPostLoadCalled() {
        return postLoadCalled;
    }

    public boolean isPostPersistCalled() {
        return postPersistCalled;
    }

    public boolean isPostRemoveCalled() {
        return postRemoveCalled;
    }

    public boolean isPostUpdateCalled() {
        return postUpdateCalled;
    }

    public boolean isPrePersistCalled1() {
        return prePersistCalled1;
    }

    public boolean isPrePersistCalled2() {
        return prePersistCalled2;
    }

    public boolean isPreRemoveCalled() {
        return preRemoveCalled;
    }

    public boolean isPreUpdateCalled() {
        return preUpdateCalled;
    }
}

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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.objectstyle.cayenne.conf.ConfigLoader;
import org.objectstyle.cayenne.conf.ConfigLoaderDelegate;
import org.objectstyle.cayenne.conf.ConfigSaverDelegate;
import org.objectstyle.cayenne.conf.ConfigStatus;

/**
 * PartialProject is a "lightweight" project implementation. It can work with
 * projects even when some of the resources are missing. It never instantiates
 * Cayenne stack objects, using other, lightweight, data structures instead.
 * 
 * @author Andrei Adamchik
 */
public class PartialProject extends Project {
    protected List domains;
    protected ConfigLoaderDelegate loadDelegate;

    /**
     * Constructor PartialProjectHandler.
     * @param projectFile
     */
    public PartialProject(File projectFile) {
        super(projectFile);
    }

    /**
     * Loads internal project and rewrites its nodes according to the list of
     * DataNodeConfigInfo objects. Only main project file gets updated, the rest
     * are assumed to be in place.
     */
    public void updateNodes(List list) throws ProjectException {

    }

    protected void postInit(File projectFile) {
        loadDelegate = new LoadDelegate();
        domains = new ArrayList();

        try {
            FileInputStream in = new FileInputStream(projectFile);
            try {
                new ConfigLoader(loadDelegate).loadDomains(in);
            } catch (Exception ex) {
                throw new ProjectException("Error creating PartialProject.", ex);
            } finally {
                in.close();
            }
        } catch (IOException ioex) {
            throw new ProjectException("Error creating PartialProject.", ioex);
        }

        super.postInit(projectFile);
    }

    public List getChildren() {
        return domains;
    }

    public void checkForUpgrades() {
        // do nothing...
    }
    

    /**
     * @see org.objectstyle.cayenne.project.Project#buildFileList()
     */
    public List buildFileList() {
        List list = new ArrayList();
        list.add(projectFileForObject(this));
        return list;
    }

    /**
     * @see org.objectstyle.cayenne.project.Project#getLoadStatus()
     */
    public ConfigStatus getLoadStatus() {
        return loadDelegate.getStatus();
    }

    public ProjectFile projectFileForObject(Object obj) {
        if (obj != this) {
            return null;
        }

        ApplicationProjectFile projectFile = new ApplicationProjectFile(this);
        projectFile.setSaveDelegate(new SaveDelegate());
        return projectFile;
    }

    class DomainMetaData {
        protected String name;
        protected List nodes = new ArrayList();
        protected List maps = new ArrayList();
        protected Map mapDependencies = new HashMap();

        public DomainMetaData(String name) {
            this.name = name;
        }
    }

    class LoadDelegate implements ConfigLoaderDelegate {
        protected ConfigStatus status = new ConfigStatus();

        public void finishedLoading() {
        }

        public ConfigStatus getStatus() {
            return status;
        }

        public boolean loadError(Throwable th) {
            status.getOtherFailures().add(th.getMessage());
            return false;
        }

        public void shouldLinkDataMap(
            String domainName,
            String nodeName,
            String mapName) {
        }

        public void shouldLoadDataDomain(String name) {
            domains.add(new DomainMetaData(name));
        }

        public void shouldLoadDataMap(
            String domainName,
            String mapName,
            String location,
            List depMapNames) {
        }

        public void shouldLoadDataNode(
            String domainName,
            String nodeName,
            String dataSource,
            String adapter,
            String factory) {
        }

        public void startedLoading() {
        }
    }

    class SaveDelegate implements ConfigSaverDelegate {
        public Iterator dependentMapNames(String domainName, String mapName) {
            return null;
        }

        public Iterator domainNames() {
            return null;
        }

        public Iterator linkedMapNames(String domainName, String nodeName) {
            return null;
        }

        public String mapLocation(String domainName, String mapName) {
            return null;
        }

        public Iterator mapNames(String domainName) {
            return null;
        }

        public String nodeAdapterName(String domainName, String nodeName) {
            return null;
        }

        public String nodeDataSourceName(String domainName, String nodeName) {
            return null;
        }

        public String nodeFactoryName(String domainName, String nodeName) {
            return null;
        }

        public Iterator nodeNames(String domainName) {
            return null;
        }
    }
}

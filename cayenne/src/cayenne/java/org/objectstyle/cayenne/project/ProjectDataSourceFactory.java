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
import java.io.FileNotFoundException;
import java.io.InputStream;

import javax.sql.DataSource;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.objectstyle.cayenne.ConfigException;
import org.objectstyle.cayenne.access.DataSourceInfo;
import org.objectstyle.cayenne.conf.DriverDataSourceFactory;

/** 
 * Factory of DataSource objects used by the project classes. 
 * Always tries to locate file with direct connection info.
 * 
 * @author Misha Shengaout
 * @author Andrei Adamchik
 */
public class ProjectDataSourceFactory extends DriverDataSourceFactory {
    static Logger logObj = Logger.getLogger(ProjectDataSourceFactory.class.getName());

    protected File projectDir;

    public ProjectDataSourceFactory(File projectDir) throws Exception {
        this.projectDir = projectDir;
    }

    public DataSource getDataSource(String location, Level logLevel) throws Exception {
        try {
            load(location);
        } catch (ConfigException e) {
            logObj.warn("No data source " + location, e);
        }
        return new ProjectDataSource(getDriverInfo());
    }

    protected DataSourceInfo getDriverInfo() {
        DataSourceInfo temp = super.getDriverInfo();
        if (null == temp) {
            temp = new DataSourceInfo();
        }
        return temp;
    }

    protected InputStream getInputStream(String location) {
        File absLocation =
            (projectDir != null) ? new File(projectDir, location) : new File(location);

        try {
            return new FileInputStream(absLocation);
        } catch (FileNotFoundException ex) {
            return super.getInputStream(location);
        }
    }

    /**
     * Returns the projectDir.
     * @return String
     */
    public File getProjectDir() {
        return projectDir;
    }
}
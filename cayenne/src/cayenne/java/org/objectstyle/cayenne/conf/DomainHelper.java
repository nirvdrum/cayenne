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
package org.objectstyle.cayenne.conf;

import java.io.InputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.objectstyle.cayenne.access.DataDomain;
import org.objectstyle.cayenne.access.DataSourceInfo;

/**
 * @deprecated Use ConfigLoader/ConfigSaver where appropriate.
 * @author Andrei Adamchik 
 */
public class DomainHelper extends ConfigLoader {
    private static Logger logObj = Logger.getLogger(DomainHelper.class);

    /** Creates new DomainHelper. */
    public DomainHelper(Configuration config) throws Exception {
        super(config.getLoaderDelegate());
    }

    /** Creates new DomainHelper that uses specified level of verbosity. */
    public DomainHelper(Configuration config, Level level) throws Exception {
        super(config.getLoaderDelegate());
    }

    /** @deprecated factory argument is ignored. */
    public boolean loadDomains(InputStream in, DataSourceFactory factory)
        throws Exception {
        return loadDomains(in);
    }

    /** @deprecated */
    public List getDomains() {
        return new ArrayList(
            ((RuntimeLoadDelegate) getDelegate()).getDomains().values());
    }

    public Map getFailedAdapters() {
        return getDelegate().getStatus().getFailedAdapters();
    }

    public Map getFailedMaps() {
        return getDelegate().getStatus().getFailedMaps();
    }

    public List getFailedMapRefs() {
        return getDelegate().getStatus().getFailedMapRefs();
    }

    public Map getFailedDataSources() {
        return getDelegate().getStatus().getFailedDataSources();
    }

    /** @deprecated */
    public static void storeDomains(PrintWriter pw, DataDomain[] domains) {
        logObj.warn(
            "Using this method is unsafe, since it ignored 'domains' "
                + "parameter and instead saves domain from shared config. Use ConfigSaver instead.");
        RuntimeSaveDelegate delegate =
            new RuntimeSaveDelegate(Configuration.getSharedConfig());
        new ConfigSaver(delegate).storeDomains(pw);
    }

    /** 
     * Stores DataSolurceInfo to the specified PrintWriter. 
     * <code>info</code> object may contain full or partial information.
     */
    public static void storeDataNode(PrintWriter out, DataSourceInfo info) {
        new ConfigSaver().storeDataNode(out, info);
    }
}

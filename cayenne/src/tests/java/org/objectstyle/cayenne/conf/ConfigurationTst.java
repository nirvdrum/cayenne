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

import org.objectstyle.cayenne.ConfigurationException;
import org.objectstyle.cayenne.access.DataDomain;
import org.objectstyle.cayenne.project.ProjectDataSourceFactory;
import org.objectstyle.cayenne.unittest.CayenneTestCase;
import org.objectstyle.cayenne.util.ResourceLocator;

public class ConfigurationTst extends CayenneTestCase {

    public void testDomain() throws java.lang.Exception {
        Configuration cfg = new Config();

        DataDomain d1 = new DataDomain("d1");
        cfg.addDomain(d1);
        assertSame(d1, cfg.getDomain(d1.getName()));

        cfg.removeDomain(d1.getName());
        assertNull(cfg.getDomain(d1.getName()));
    }
    
	public void testOverrideFactory() throws java.lang.Exception {
		Configuration cfg = new Config();

        assertNull(cfg.getDataSourceFactory());
		ProjectDataSourceFactory factory = new ProjectDataSourceFactory(null);
        cfg.setDataSourceFactory(factory);
        assertSame(factory, cfg.getDataSourceFactory());
	}

	public void testFileConfigurationConstructorWithNullFile() {
		try {
			new FileConfiguration(null);
			fail("expected ConfigurationException!");
		}
		catch (ConfigurationException ex) {
			// OK
		}
	}


    /** Concrete Configuration subclass used for tests. */
    public static class Config extends Configuration {

		protected boolean shouldInitialize() {
			return true;
		}

		protected void initialize() throws Exception {
		}

		protected void didInitialize() {
		}

		public ResourceLocator getResourceLocator() {
			return null;
		}

		protected InputStream getDomainConfiguration() {
            return null;
        }

		protected InputStream getMapConfiguration(String location) {
            return null;
        }
    }
}

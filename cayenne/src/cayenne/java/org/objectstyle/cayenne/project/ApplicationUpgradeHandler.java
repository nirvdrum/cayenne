/* ====================================================================
 * 
 * The ObjectStyle Group Software License, Version 1.0 
 *
 * Copyright (c) 2002-2004 The ObjectStyle Group 
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

import java.util.Collection;

import org.objectstyle.cayenne.conf.Configuration;

/**
 * @since 1.1
 * @author Andrei Adamchik
 */
abstract class ApplicationUpgradeHandler {
    private static final ApplicationUpgradeHandler sharedInstance =
        new UpgradeHandler_1_1();

    static ApplicationUpgradeHandler sharedHandler() {
        return sharedInstance;
    }

    abstract String supportedVersion();

    abstract boolean checkForUpgrades(Configuration project, Collection appendMessages);

    abstract void performUpgrade(ApplicationProject project) throws ProjectException;

    int compareVersion(String version) {
        double supported = decodeVersion(supportedVersion());
        double newVersion = decodeVersion(version);
        return supported < newVersion ? -1 : (supported == newVersion) ? 0 : 1;
    }

    static double decodeVersion(String version) {
        if (version == null || version.trim().length() == 0) {
            return 0;
        }

        // leave the first dot, and treat remaining as a fraction
        // remove all non digit chars
        StringBuffer buffer = new StringBuffer(version.length());
        boolean dotProcessed = false;
        for (int i = 0; i < version.length(); i++) {
            char nextChar = version.charAt(i);
            if (nextChar == '.' && !dotProcessed) {
                dotProcessed = true;
                buffer.append('.');
            }
            else if (Character.isDigit(nextChar)) {
                buffer.append(nextChar);
            }
        }

        return Double.parseDouble(buffer.toString());
    }

    static class UpgradeHandler_1_1 extends ApplicationUpgradeHandler {
        String supportedVersion() {
            return "1.1";
        }

        void performUpgrade(ApplicationProject project) throws ProjectException {
            project.setModified(true);
            project.getConfiguration().setProjectVersion(supportedVersion());
            project.save();
        }

        boolean checkForUpgrades(Configuration project, Collection appendMessages) {
            String loadedVersion = project.getProjectVersion();
            int versionState = compareVersion(loadedVersion);
            if (versionState < 0) {
                String versionLabel = (loadedVersion != null) ? loadedVersion : "?";
                appendMessages.add(
                    "Newer Project Version Detected: \"" + versionLabel + "\"");
                return true;
            }
            else if (versionState > 0) {
                String versionLabel = (loadedVersion != null) ? loadedVersion : "?";
                appendMessages.add(
                    "Older Project Version Detected: \"" + versionLabel + "\"");
                return true;
            }
            else {
                return false;
            }
        }
    }
}

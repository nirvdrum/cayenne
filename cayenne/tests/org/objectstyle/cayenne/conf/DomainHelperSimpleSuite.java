package org.objectstyle.cayenne.conf;
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

import java.util.*;
import java.io.*;
import org.objectstyle.util.*;
import org.objectstyle.cayenne.access.DataDomain;


/** Runs multiple domain config cases. */
public class DomainHelperSimpleSuite extends DomainHelperSuite {

    protected void buildCases() {
        buildCase1();
        buildCase2();
        buildCase3();
        buildCase4();
        buildCase5();
    }

    private void buildCase1() {
        StringBuffer buf = new StringBuffer();
        buf.append("<?xml version='1.0' encoding='utf-8'?>")
        .append("\n<domains>")
        .append("\n   <domain name='domain1'>")
        .append("\n   </domain>")
        .append("\n </domains>");

        DomainHelperCase aCase = new DomainHelperCase();
        aCase.setConfigInfo(buf.toString());
        aCase.setTotalDomains(1);
        cases.add(aCase);
    }

    private void buildCase2() {
        StringBuffer buf = new StringBuffer();
        buf.append("<?xml version='1.0' encoding='utf-8'?>")
        .append("\n<domains>")
        .append("\n   <domain name='domain1'>")
        .append("\n   <map name='m1' location='aaa'/>")
        .append("\n   </domain>")
        .append("\n </domains>");

        DomainHelperCase aCase = new DomainHelperCase();
        aCase.setConfigInfo(buf.toString());
        aCase.setTotalDomains(1);
        aCase.setFailedMaps(1);
        cases.add(aCase);
    }

    private void buildCase3() {
        StringBuffer buf = new StringBuffer();
        buf.append("<?xml version='1.0' encoding='utf-8'?>")
        .append("\n<domains>")
        .append("\n   <domain name='domain1'>")
        .append("\n   <map name='m1' location='test_resources/testmap.xml'/>")
        .append("\n   </domain>")
        .append("\n </domains>");

        DomainHelperCase aCase = new DomainHelperCase();
        aCase.setConfigInfo(buf.toString());
        aCase.setTotalDomains(1);
        cases.add(aCase);
    }
    
    
    private void buildCase4() {
        StringBuffer buf = new StringBuffer();
        buf.append("<?xml version='1.0' encoding='utf-8'?>")
        .append("\n<domains>")
        .append("\n   <domain name='domain1'>")
        .append("\n        <map name='m1' location='test_resources/testmap.xml'/>")
        .append("\n        <node name='db1' datasource='node.xml'")
        .append("\n              factory='org.objectstyle.cayenne.conf.DriverDataSourceFactory'")
        .append("\n              adapter='org.objectstyle.cayenne.dba.mysql.MySQLAdapter'>")
        .append("\n        </node>")
        .append("\n   </domain>")
        .append("\n </domains>");


        DomainHelperCase aCase = new DomainHelperCase();
        aCase.setConfigInfo(buf.toString());
        aCase.setFailedDataSources(1);
        aCase.setTotalDomains(1);
        cases.add(aCase);
    }
    
    private void buildCase5() {
        StringBuffer buf = new StringBuffer();
        buf.append("<?xml version='1.0' encoding='utf-8'?>")
        .append("\n<domains>")
        .append("\n   <domain name='domain1'>")
        .append("\n        <map name='m1' location='test_resources/testmap.xml'/>")
        .append("\n        <node name='db1' datasource='node.xml'")
        .append("\n              factory='org.objectstyle.cayenne.conf.DriverDataSourceFactory'")
        .append("\n              adapter='org.objectstyle.cayenne.dba.mysql.MySQLAdapter'>")
        .append("\n              <map-ref name='m1'/>")
        .append("\n        </node>")
        .append("\n   </domain>")
        .append("\n </domains>");


        DomainHelperCase aCase = new DomainHelperCase();
        aCase.setConfigInfo(buf.toString());
        aCase.setFailedDataSources(1);
        aCase.setTotalDomains(1);
        cases.add(aCase);
    }
}

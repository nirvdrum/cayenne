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
package org.objectstyle.cayenne.gui;

import java.util.logging.Logger;

import org.objectstyle.cayenne.CayenneTestCase;
import org.objectstyle.cayenne.access.DataSourceInfo;
import org.objectstyle.testui.TestEditorFrame;

public class DbLoginPanelTst extends CayenneTestCase {
    static Logger logObj = Logger.getLogger(DbLoginPanelTst.class.getName());

    protected TestEditorFrame frame;
    protected DbLoginPanel loginPanel;

    public DbLoginPanelTst(String name) {
        super(name);
    }

    protected void setUp() throws java.lang.Exception {
        frame = new TestEditorFrame();
        loginPanel = new DbLoginPanel(frame);
        frame.pack();
        loginPanel.pack();
    }

    protected void tearDown() throws java.lang.Exception {
        loginPanel.dispose();
        frame.dispose();
    }

    public void testWidgetsPresent() {
        assertNotNull(loginPanel.unInput);
        assertNotNull(loginPanel.pwdInput);
        assertNotNull(loginPanel.drInput);
        assertNotNull(loginPanel.urlInput);
        assertNotNull(loginPanel.adapterInput);
    }

    public void testInitialContents() {
        // according to docs, JTextField should contain a NULL string by default
        // but in fact it contains empty string "".. we need to deal with that
        assertTrue(
            loginPanel.unInput.getText() == null
                || loginPanel.unInput.getText().length() == 0);
        assertTrue(
            loginPanel.pwdInput.getPassword() == null
                || loginPanel.pwdInput.getPassword().length == 0);
        assertTrue(
            loginPanel.drInput.getText() == null
                || loginPanel.drInput.getText().length() == 0);
        assertTrue(
            loginPanel.urlInput.getText() == null
                || loginPanel.urlInput.getText().length() == 0);
        assertTrue(
            loginPanel.adapterInput.getText() == null
                || loginPanel.adapterInput.getText().length() == 0);
    }

    public void testSetDataSourceInfo() {
        DataSourceInfo info = new DataSourceInfo();
        info.setUserName("1");
        info.setPassword("2");
        // test null
        info.setJdbcDriver(null);
        info.setDataSourceUrl("4");
        info.setAdapterClass("5");
        loginPanel.setDataSrcInfo(info);

        assertEquals(info.getUserName(), loginPanel.unInput.getText());
        assertEquals(info.getPassword(), new String(loginPanel.pwdInput.getPassword()));
        assertEquals(info.getDataSourceUrl(), loginPanel.urlInput.getText());
        assertEquals(info.getAdapterClass(), loginPanel.adapterInput.getText());

        // maybe JTextField bug, but it sets nulls to empty strings
        assertTrue(
            loginPanel.drInput.getText() == null
                || loginPanel.drInput.getText().length() == 0);
    }

    public void testModifiedDataSourceInfo() {
        DataSourceInfo info = new DataSourceInfo();
        info.setUserName("1");
        info.setPassword("2");
        info.setJdbcDriver("3");
        info.setDataSourceUrl("4");
        info.setAdapterClass("5");
        loginPanel.setDataSrcInfo(info);

        // modify text fields
        loginPanel.unInput.setText("6");
        loginPanel.pwdInput.setText("7");
        loginPanel.drInput.setText("8");
        loginPanel.adapterInput.setText("9");
        // test empty string, should be converted to NULL in the model object..
        loginPanel.urlInput.setText("");

        // generate button click event. 
        loginPanel.ok.doClick();

        // check model values   
        assertEquals("6", info.getUserName());
        assertEquals("7", info.getPassword());
        assertEquals("8", info.getJdbcDriver());
        assertEquals("9", info.getAdapterClass());
        assertNull(info.getDataSourceUrl());
    }

    /** Test that login info has a valid number of min and max connections. */
    public void testConnectionCount() {
        DataSourceInfo info = new DataSourceInfo();
        loginPanel.setDataSrcInfo(info);

        // set to deliberatly invalid values

        info.setMinConnections(-1);
        info.setMaxConnections(0);

        // generate button click event. 
        loginPanel.ok.doClick();

        // check connection pool sizes 
        assertTrue(info.getMinConnections() > 0);
        assertTrue(info.getMinConnections() <= info.getMaxConnections());
    }

}
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


import java.io.IOException;

import org.apache.log4j.Logger;
import org.objectstyle.cayenne.access.DataSourceInfo;
import org.objectstyle.cayenne.gui.util.CmdPasswordField;
import org.objectstyle.cayenne.gui.util.CmdTextField;


/** Class that can collect login information via GUI or command line interface.  */
public abstract class InteractiveLogin {
    static Logger logObj = Logger.getLogger(InteractiveLogin.class.getName());

    protected DataSourceInfo dataSrcInfo;
    
    
    /** Creates login handler object for command line login */
    public static InteractiveLogin getLoginObject(DataSourceInfo dataSrcInfo) {
        InteractiveLogin loginObj = new CommandLineLogin();
        loginObj.setDataSrcInfo(dataSrcInfo);
        return loginObj;
    }
    
    
    /** Creates login handler object for GUI login */
    public static InteractiveLogin getGuiLoginObject(DataSourceInfo dataSrcInfo) {
        InteractiveLogin loginObj = new GuiLogin();
        loginObj.setDataSrcInfo(dataSrcInfo);
        return loginObj;
    }
    
    /** Presents user with an interface to collect login information,
     * saves this information in "dataSrcInfo" property. */
    public abstract void collectLoginInfo();
    
    
    public void setDataSrcInfo(DataSourceInfo dataSrcInfo) {
        this.dataSrcInfo = dataSrcInfo;
    }
    
    public DataSourceInfo getDataSrcInfo() {
        return dataSrcInfo;
    }
    
    
    
    protected static class CommandLineLogin extends InteractiveLogin {
        
        /** Save entered information in a DataSourceInfo object */
        protected void commitInfo(DataSourceInfo dsi) {
            // copy entered information
            dataSrcInfo.setUserName(dsi.getUserName());
            dataSrcInfo.setPassword(dsi.getPassword());
            dataSrcInfo.setJdbcDriver(dsi.getJdbcDriver());
            dataSrcInfo.setDataSourceUrl(dsi.getDataSourceUrl());
            dataSrcInfo.setAdapterClass(dsi.getAdapterClass());
            
            // fix connection counts
            if(dataSrcInfo.getMinConnections() <= 0)
                dataSrcInfo.setMinConnections(1);
            
            if(dataSrcInfo.getMaxConnections() < dataSrcInfo.getMinConnections())
                dataSrcInfo.setMaxConnections(dataSrcInfo.getMinConnections());
        }
        
        
        public void collectLoginInfo() {
            try {
                DataSourceInfo localDsi = new DataSourceInfo();
                
                CmdTextField textField = new CmdTextField("Enter user name");
                textField.setText(dataSrcInfo.getUserName());
                textField.readInput();
                localDsi.setUserName(textField.getText());
                
                CmdPasswordField pwdField = new CmdPasswordField("Enter password");
                pwdField.setText(dataSrcInfo.getPassword());
                pwdField.readInput();
                localDsi.setPassword(pwdField.getText());
                
                textField.setLabel("Enter JDBC Driver Class");
                textField.setText(dataSrcInfo.getJdbcDriver());
                textField.readInput();
                localDsi.setJdbcDriver(textField.getText());
                
                textField.setLabel("Enter Data Source URL");
                textField.setText(dataSrcInfo.getDataSourceUrl());
                textField.readInput();
                localDsi.setDataSourceUrl(textField.getText());
                
                commitInfo(localDsi);
            }
            catch(IOException ioex) {
                throw new RuntimeException("Error reading info from STDIN", ioex);
            }
        }
    }
    
    
    protected static class GuiLogin extends InteractiveLogin {
    	private Editor getFrame() {
    		Editor frame = Editor.getFrame();
    		return (frame != null) ? frame : new Editor();
    	}
    	
        public void collectLoginInfo() {
            Editor frame = getFrame();
            DbLoginPanel loginPanel = new DbLoginPanel(frame);
            loginPanel.setDataSrcInfo(dataSrcInfo);
            frame.pack();
            
            // call to show will block until user closes the dialog
            loginPanel.show();
            dataSrcInfo = loginPanel.getDataSrcInfo();
            loginPanel.dispose();
        }
    }
}

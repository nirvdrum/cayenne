package org.objectstyle.cayenne.tools;
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

import java.io.PrintWriter;
import java.sql.*;

import org.objectstyle.cayenne.access.DataSourceInfo;
import org.objectstyle.cayenne.gui.InteractiveLogin;
import org.objectstyle.cayenne.map.*;


/** Utility class for loading schema information from the specified
  * database into DataMap.
  * <p>This class is an frontend to DbLoader class. It can be used 
  * from command line as well as from GUI tools.</p> */
public class DbLoaderTool {
    public static final String NO_GUI_PROPERTY = "cayenne.nogui";


    /** Runs DbLoader against specified database. Prints generated data map XML to STDOUT. */
    public static void main (String args[]) {

        boolean noGui = "true".equalsIgnoreCase(System.getProperty(NO_GUI_PROPERTY));

        DataMap map = null;
        try {
            Connection conn = openConnection(getConnectionInfo(!noGui));
            if (null == conn) {
                System.out.println("Cancel loading...");
                return;
            }
            map = new DbLoader(conn).createDataMapFromDB(null);
            conn.close();
        } catch (Exception ex) {
            ex.printStackTrace();
            System.exit(1);
        }


        // Write out the map to the xml file
        MapLoader impl = new MapLoaderImpl();
        PrintWriter out = new PrintWriter(System.out);

        try {
            impl.storeDataMap(out, map);
        } catch(Exception ex) {
            ex.printStackTrace();
            System.exit(1);
        }
        out.flush();
    }

    /** Interactively collects database login info and opens database connection. */
    static Connection openConnection(DataSourceInfo dsi) throws Exception {
        if (null == dsi)
            return null;
        // connect
        Driver driver = (Driver)Class.forName(dsi.getJdbcDriver()).newInstance();
        return DriverManager.getConnection(
                   dsi.getDataSourceUrl(),
                   dsi.getUserName(),
                   dsi.getPassword());
    }


    static DataSourceInfo getConnectionInfo(boolean useGui) {
        // gather connection info
        DataSourceInfo dsi = new DataSourceInfo();
        InteractiveLogin loginObj = (useGui)
                                    ? InteractiveLogin.getGuiLoginObject(dsi)
                                    : InteractiveLogin.getLoginObject(dsi);

        if(!useGui) {
            System.out.println("*********************************************");
            System.out.println("Collecting database connection information...");
        }

        loginObj.collectLoginInfo();

        return loginObj.getDataSrcInfo();
    }
}

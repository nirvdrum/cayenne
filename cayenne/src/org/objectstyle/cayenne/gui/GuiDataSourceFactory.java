package org.objectstyle.cayenne.gui;

import java.util.logging.Level;
import java.util.logging.Logger;
import javax.sql.DataSource;

import org.objectstyle.cayenne.ConfigException;
import org.objectstyle.cayenne.conf.DriverDataSourceFactory;


/** Factory creating data sources for GUI. Always tries to 
  * locate file with direct connection info  and create data source.
  */
public class GuiDataSourceFactory extends DriverDataSourceFactory
{
	public GuiDataSourceFactory() throws Exception { 
		super(); 
	}

    public DataSource getDataSource(String location, Level logLevel) 
    throws Exception {
    	try {
        	load(location);
        } catch (ConfigException e) {
        	System.out.println("No data source: " + e.getMessage());
        }
        return new GuiDataSource(getDriverInfo());
    }
}
package org.objectstyle.cayenne.gui;

import java.util.logging.Level;
import java.util.logging.Logger;
import javax.sql.DataSource;

import org.objectstyle.cayenne.ConfigException;
import org.objectstyle.cayenne.access.DataSourceInfo;
import org.objectstyle.cayenne.conf.DriverDataSourceFactory;

import java.io.File;
import java.io.InputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;

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
        	System.out.println("No data source " + location + ": " + e.getMessage());
        }
        return new GuiDataSource(getDriverInfo());
    }
    
    protected DataSourceInfo getDriverInfo() {
        DataSourceInfo temp = super.getDriverInfo();
        if (null == temp) {
        	temp = new DataSourceInfo();
        }
        return temp;
    }
    
	protected InputStream getInputStream(String location) {
		String proj_dir = GuiConfiguration.getGuiConfig().getProjDir();
		String absolute_loc = null;;
		if (null != proj_dir)
			absolute_loc = proj_dir + File.separator + location;
		File file = new File(absolute_loc);
		if (file.exists()) {
			System.out.println("File " + absolute_loc + " exists");
		}
		try {
			return new FileInputStream(absolute_loc);
		} catch (FileNotFoundException e) {
			System.out.println(e.getMessage());
			return super.getInputStream(location);
		}
	}

}
package org.objectstyle.cayenne.gui;

import java.util.logging.Level;
import java.util.logging.Logger;
import javax.sql.DataSource;

import org.objectstyle.cayenne.conf.DriverDataSourceFactory;


public class GuiDataSourceFactory extends DriverDataSourceFactory
{
	public GuiDataSourceFactory() throws Exception { 
		super(); 
	}

    public DataSource getDataSource(String location, Level logLevel) throws Exception {
        load(location);
        return new GuiDataSource(getDriverInfo());
    }
}
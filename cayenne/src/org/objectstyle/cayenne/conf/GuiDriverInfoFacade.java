package org.objectstyle.cayenne.conf;

import org.objectstyle.cayenne.access.DataSourceInfo;

/** Facade class for accessing DataSourceInfo within DataSourceFactories.
 */
public class GuiDriverInfoFacade
{
	public static DataSourceInfo getDataSourceInfo(DriverDataSourceFactory factory)
	{
		return factory.getDriverInfo();		
	}
	
	public static DriverDataSourceFactory createDataSourceFactory(DataSourceInfo info) 
	{
		return null;
	}
}
package org.objectstyle.cayenne.gui;


import javax.naming.OperationNotSupportedException;
import java.io.PrintWriter;
import java.sql.*;
import javax.sql.DataSource;

import org.objectstyle.cayenne.access.DataSourceInfo;

public class GuiDataSource implements DataSource
{
	DataSourceInfo info;
	
	public GuiDataSource(DataSourceInfo temp_info)
	{
		info = temp_info;
	}
	
	public DataSourceInfo getDataSourceInfo() {
		return info;
	}
	
	public Connection getConnection() throws SQLException
	{
		throw new SQLException("Method not implemented");
	}


	public Connection getConnection(String username,String password) 
	throws SQLException
	{
		throw new SQLException("Method not implemented");
	}


	public PrintWriter getLogWriter() throws SQLException
	{
		return new PrintWriter(System.out);
	}
	
	
	public void setLogWriter(PrintWriter out) throws SQLException
	{ }


	public void setLoginTimeout(int seconds) throws SQLException
	{}


	public int getLoginTimeout() throws SQLException
	{
		throw new SQLException("Method not implemented");
	}

	
}
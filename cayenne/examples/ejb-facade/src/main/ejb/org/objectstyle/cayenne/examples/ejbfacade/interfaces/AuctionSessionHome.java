/*
 * Generated file - Do not edit!
 */
package org.objectstyle.cayenne.examples.ejbfacade.interfaces;

import java.lang.*;
import java.rmi.RemoteException;
import java.util.Collection;
import java.util.Date;
import javax.ejb.CreateException;
import javax.ejb.EJBException;
import javax.ejb.SessionBean;
import javax.ejb.SessionContext;
import org.objectstyle.cayenne.access.DataContext;
import org.objectstyle.cayenne.conf.Configuration;
import org.objectstyle.cayenne.examples.ejbfacade.model.Auction;
import org.objectstyle.cayenne.query.SelectQuery;

/**
 * Home interface for AuctionSession. Lookup using {1}
 * @author XDOCLET 1.1.2
 * @xdoclet-generated at Feb 5, 2004 2:08:24 AM
 */
public interface AuctionSessionHome
   extends javax.ejb.EJBHome
{
   public static final String COMP_NAME="java:comp/env/ejb/AuctionSession";
   public static final String JNDI_NAME="ejb/cayenne/examples/AuctionSession";

   public org.objectstyle.cayenne.examples.ejbfacade.interfaces.AuctionSession create() throws javax.ejb.CreateException, java.rmi.RemoteException;

}

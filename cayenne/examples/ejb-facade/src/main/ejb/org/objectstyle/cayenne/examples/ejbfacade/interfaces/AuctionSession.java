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
 * Remote interface for AuctionSession.
 * @author XDOCLET 1.1.2
 * @xdoclet-generated at Feb 5, 2004 5:42:46 PM
 */
public interface AuctionSession
   extends javax.ejb.EJBObject
{
   /**
    * Returns all active auctions in the system.
    */
   public void createAuction( java.lang.String name,java.util.Date starts,java.util.Date ends ) throws java.rmi.RemoteException;

   /**
    * Returns all active auctions in the system.
    */
   public java.util.Collection getActiveAuctions(  ) throws java.rmi.RemoteException;

}

/* ====================================================================
 * 
 * The ObjectStyle Group Software License, Version 1.0
 * 
 * Copyright (c) 2002-2004 The ObjectStyle Group and individual authors of the
 * software. All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *  1. Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 *  2. Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *  3. The end-user documentation included with the redistribution, if any,
 * must include the following acknowlegement: "This product includes software
 * developed by the ObjectStyle Group (http://objectstyle.org/)." Alternately,
 * this acknowlegement may appear in the software itself, if and wherever such
 * third-party acknowlegements normally appear.
 *  4. The names "ObjectStyle Group" and "Cayenne" must not be used to endorse
 * or promote products derived from this software without prior written
 * permission. For written permission, please contact andrus@objectstyle.org.
 *  5. Products derived from this software may not be called "ObjectStyle" nor
 * may "ObjectStyle" appear in their names without prior written permission of
 * the ObjectStyle Group.
 * 
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED WARRANTIES,
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * OBJECTSTYLE GROUP OR ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA,
 * OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE,
 * EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 * ====================================================================
 * 
 * This software consists of voluntary contributions made by many individuals
 * on behalf of the ObjectStyle Group. For more information on the ObjectStyle
 * Group, please see <http://objectstyle.org/> .
 *  
 */
package org.objectstyle.cayenne.examples.ejbfacade.ejb;

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
 * SessionBean Facade to the Auction business
 * 
 * @author Andrei Adamchik
 * 
 * @ejb:bean name="AuctionSession"
 *           display-name="EJB Facade to Art Auctions"
 *           type="Stateless"
 *           transaction-type="Container"
 *           jndi-name="ejb/cayenne/examples/AuctionSession"
 */
public class AuctionSessionEJB implements SessionBean {

    protected SessionContext ejbContext;
    protected DataContext cayenneContext;

    public AuctionSessionEJB() {
    }

    /**
     * Creates the AuctionSession Bean
     *
     * @ejb:create-method view-type="remote"
     */
    public void ejbCreate() throws CreateException {
        System.out.println("AuctionSessionEJB.ejbCreate()");

        // configure container transactions
        Configuration.getSharedConfiguration().getDomain().setUsingInternalTransactions(
            false);
        cayenneContext = DataContext.createDataContext();
    }

    /**
     * Returns all active auctions in the system.
     * 
     * @ejb:interface-method view-type="remote"
     */
    public Collection getActiveAuctions() throws RemoteException {
        System.out.println("AuctionSessionEJB.getActiveAuctions()");
        return cayenneContext.performQuery(new SelectQuery(Auction.class));
    }

    /**
     * Returns all active auctions in the system.
     * 
     * @ejb:interface-method view-type="remote"
     */
    public void createAuction(String name, Date starts, Date ends)
        throws RemoteException {
        System.out.println("AuctionSessionEJB.createAuction()");

        Auction auction =
            (Auction) cayenneContext.createAndRegisterNewObject(Auction.class);
        auction.setName(name);
        auction.setStartTime(starts);
        auction.setClosingTime(ends);

        // initialize with dummy defaults, real logic for start price 
        // is applied when paintings are added
        auction.setStartPrice(new Float(0.00));
        auction.setMinIncrement(new Float(50.00));

        // must commit Cayenne context explicitly
        // since Cayenne is running under container transaction control
        // this will store the data to DB, but will not commit the underlying connections
        cayenneContext.commitChanges();
    }

    public void ejbActivate() throws EJBException, RemoteException {
        // stateless bean - this method is noop
    }

    public void ejbPassivate() throws EJBException, RemoteException {
        // stateless bean - this method is noop
    }

    public void ejbRemove() throws EJBException, RemoteException {
        System.out.println("AuctionSessionEJB.ejbRemove()");

        // might as well reset the context
        cayenneContext = null;
    }

    public void setSessionContext(SessionContext ejbContext)
        throws EJBException, RemoteException {
        this.ejbContext = ejbContext;
    }
}

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
package org.objectstyle.cayenne.example.ejbfacade.web;

import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.naming.InitialContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.objectstyle.cayenne.examples.ejbfacade.interfaces.AuctionSession;
import org.objectstyle.cayenne.examples.ejbfacade.interfaces.AuctionSessionHome;

/**
 * Servlet that processes requests to create art auctions, passing them to EJB.
 * Note that this Servlet simply demonstrates communication with the EJB business
 * logic, and definitely does not demonstrate the best web app practices.
 * 
 * @author Andrei Adamchik
 */
public class AuctionServlet extends HttpServlet {
    private static DateFormat formatter = new SimpleDateFormat("MM/dd/yyyy");

    protected void doGet(HttpServletRequest arg0, HttpServletResponse arg1)
        throws ServletException, IOException {
        super.doPost(arg0, arg1);
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException {

        // read parameters and pass them to EJB

        // extract name
        String name = request.getParameter("name");

        // extract start date
        String startDateString = request.getParameter("startDate");
        Date startDate = null;
        if (startDateString != null) {
            try {
                startDate = formatter.parse(startDateString);
            }
            catch (ParseException ex) {

            }
        }

        // extract start date
        String endDateString = request.getParameter("endDate");
        Date endDate = null;
        if (endDateString != null) {
            try {
                endDate = formatter.parse(endDateString);
            }
            catch (ParseException ex) {

            }
        }

        // use default dates if none entered
        if (startDate == null) {
            startDate = new Date();
        }

        if (endDate == null) {
            endDate = startDate;
        }

        log("name: " + name + "; startDate: " + startDate + "; endDate: " + endDate);

        try {
            InitialContext context = new InitialContext();
            AuctionSessionHome auctionHome =
                (AuctionSessionHome) context.lookup(
                    "ejb/cayenne/examples/AuctionSession");
            AuctionSession auction = auctionHome.create();

            auction.createAuction(name, startDate, endDate);
            auction.remove();
        }
        catch (Exception e) {
            throw new ServletException("Error creating an auction", e);
        }

        forward("/index.jsp", request, response);
    }

    protected void forward(
        String url,
        HttpServletRequest request,
        HttpServletResponse response)
        throws ServletException, IOException {

        request.getRequestDispatcher(url).forward(request, response);
    }
}

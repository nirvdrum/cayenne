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
package org.objectstyle.cayenne.modeler.control;

import org.objectstyle.cayenne.modeler.view.StatusBarView;
import org.scopemvc.controller.basic.BasicController;

/**
 * @author Andrei Adamchik
 */
public class StatusBarController extends BasicController {

    public void startup(StatusBarView view) {
        setView(view);
        setModel(new StatusBarModel());
        showView();
    }

    public void projectOpened() {
        doUpdate("Opened Project.");
    }

    protected void doUpdate(String message) {
        StatusBarModel model = (StatusBarModel) getModel();
        if (model == null) {
            return;
        }

        synchronized (model) {
            model.setEventMessage(message);
            ((StatusBarView) getView()).refresh();
        }

        // start message cleanup thread that would remove the message after X seconds
        if (message != null && message.trim().length() > 0) {
            Thread cleanup = new ExpireThread(message, 7);
            cleanup.start();
        }

    }

    public class StatusBarModel {
        protected String eventMessage;

        public String getEventMessage() {
            return eventMessage;
        }

        public void setEventMessage(String eventMessage) {
            this.eventMessage = eventMessage;
        }
    }

    class ExpireThread extends Thread {
        protected int seconds;
        protected String message;

        public ExpireThread(String message, int seconds) {
            this.seconds = seconds;
            this.message = message;
        }

        public void run() {
            try {
                sleep(seconds * 1000);
            } catch (InterruptedException e) {
                // ignore exception
            }

            StatusBarModel model = (StatusBarModel) getModel();
            if (model == null) {
                return;
            }

            synchronized (model) {
                if (message.equals(model.getEventMessage())) {
                    doUpdate(null);
                }
            }
        }
    }
}

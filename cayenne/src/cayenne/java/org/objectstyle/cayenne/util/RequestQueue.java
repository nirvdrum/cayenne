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
package org.objectstyle.cayenne.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.log4j.Logger;

/**
 * RequestQueue implements a FIFO queue for threads waiting for a 
 * particular event, resource, etc. Each thread will wait 
 * in the queue until either of the following events happen:
 * 
 * <ul>
 *    <li>Thread is already #1 in the queue and an awaited event occurrs.</li>
 * 	  <li>Thread timeout interval expired.</li>
 *    <li>Thread was interrupted (this is a quick way to remove thread from the queue).</li>
 * </ul>
 * 
 * If any of the conditions above ocurrs, thread will be removed from the queue. 
 * 
 * @author Andrei Adamchik
 */
public class RequestQueue {
    static Logger logObj = Logger.getLogger(RequestQueue.class.getName());

    protected List queue;
    protected int maxSize;
    protected int timeout;

    /**
     * Constructor for RequestQueue.
     * 
     * @param maxSize - maximum allowed number of threads in the queue.
     * @param timeout - timeout in milliseconds that determines maximum possible 
     * wait time in the queue.
     */
    public RequestQueue(int maxSize, int timeout) {
        this.maxSize = maxSize;
        this.timeout = timeout;
        this.queue = Collections.synchronizedList(new ArrayList());
    }

    public int getSize() {
        synchronized (queue) {
            return queue.size();
        }
    }

    /**
     * Queues current thread. This will block 
     * the caller till the thread is dequeued as a result
     * of another thread calling <code>dequeueFirst</code>
     * or as a result of a timeout. 
     * 
     * @return an object that represents an event or resource that
     * caused 
     */
    public RequestDequeue queueThread() {
        RequestDequeue result = new RequestDequeue();

        // queue up request
        synchronized (queue) {
            if (maxSize > 0 && queue.size() >= maxSize) {
                result.setDequeueEventCode(RequestDequeue.QUEUE_FULL);
                return result;
            }

            queue.add(result);
        }

        // wait
        synchronized (result) {
            boolean interrupted = false;
            try {

                if (logObj.isDebugEnabled()) {
                    logObj.debug(
                        "thread ["
                            + Thread.currentThread().getName()
                            + "] will wait in the queue for "
                            + timeout);
                }

                // release lock and wait
                result.wait(timeout);
            } catch (InterruptedException e) {
                interrupted = true;
            }

            if (logObj.isDebugEnabled()) {
                logObj.debug(
                    "thread ["
                        + Thread.currentThread().getName()
                        + "] finished waiting in the queue.");
            }

            // wait is over, remove itself from the queue
            if (result.getDequeueEventCode() != RequestDequeue.DEQUEUE_SUCCESS) {

                // timeout or interrupted
                synchronized (queue) {
                    queue.remove(result);
                    int code =
                        (interrupted)
                            ? RequestDequeue.INTERRUPTED
                            : RequestDequeue.TIMED_OUT;
                    result.setDequeueEventCode(code);
                }
            }

            return result;
        }
    }

    /**
     * Releases the first thread in the queue. 
     */
    public boolean dequeueFirst(Object dequeuedObj) {
        RequestDequeue first = null;
        // Make sure we avoid nested locks - locking both queue & result object
        // at the same time - this is a potential deadlock.

        synchronized (queue) {
            if (queue.size() > 0) {
                first = (RequestDequeue) queue.get(0);
                queue.remove(0);
            }
        }

        // this is true when the queue contained at least one object
        if (first != null) {
            synchronized (first) {
                first.setDequeueEventObject(dequeuedObj);
                first.setDequeueEventCode(RequestDequeue.DEQUEUE_SUCCESS);
                first.notifyAll();
            }

            return true;
        } else {
            return false;
        }
    }
}

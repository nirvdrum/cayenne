package org.objectstyle.cayenne.access;
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

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Logs special events during query exectutions.
* DataNodes invoke its static methods to do logging.
*/
public class QueryLogger {
    static Logger logObj = Logger.getLogger(QueryLogger.class.getName());

    /** Utility method that appends SQL literal for the specified object to the buffer.
    *  This value will be quoted as needed. Conversion of the value is done based on Java class.
    *
    *  <p>Note: this is not intended to build SQL queries, rather this is used in logging routines only.</p> 
    *
    *  @param buf buffer to append value
    *  @param anObject object to be transformed to SQL literal.
    */
    public static void sqlLiteralForObject(StringBuffer buf, Object anObject) {
        // 0. Null
        if(anObject == null)
            buf.append("NULL");
        // 1. String literal
        else if(anObject instanceof String) {
            buf.append('\'');
            // lets escape quotes
            String literal = (String)anObject;
            int curPos = 0;
            int endPos = 0;

            while((endPos = literal.indexOf('\'', curPos)) >= 0) {
                buf.append(literal.substring(curPos, endPos + 1)).append('\'');
                curPos = endPos + 1;
            }

            if(curPos < literal.length())
                buf.append(literal.substring(curPos));

            buf.append('\'');
        }
        // 2. Numeric literal
        else if(anObject instanceof Number) {
            // process numeric value (do something smart in the future)
            buf.append(anObject);
        }
        // 3. Date
        else if(anObject instanceof java.sql.Date) {
            buf.append('\'').append(anObject).append('\'');
        }
        // 4. Date
        else if(anObject instanceof java.sql.Time) {
            buf.append('\'').append(anObject).append('\'');
        }
        // 5 Date
        else if(anObject instanceof java.util.Date) {
            long time = ((java.util.Date)anObject).getTime();
            buf.append('\'').append(new java.sql.Timestamp(time)).append('\'');
        }
        // 6. byte[]
        else if(anObject instanceof byte[]) {
            buf.append('\'');
            byte[] b = (byte[])anObject;
            for(int i = 0; i < b.length; i++) {
                buf.append(b[i]);
            }
            buf.append('\'');
        } else
            throw new org.objectstyle.cayenne.CayenneRuntimeException("Unsupported type : " + anObject.getClass().getName());
    }

    public static Level getLogLevel() {
        return logObj.getLevel();
    }

    public static void setLogLevel(Level level) {
        logObj.setLevel(level);
    }

    /** Log query content using Log4J Category with "INFO" priority.
     *
     *  @param queryStr Query SQL string
     *  @param params optional list of query parameters that are used when executing query in prepared statement.
     */
    public static void logQuery(Level logLevel, String queryStr, List params) {
        if(logObj.isLoggable(logLevel)) {
            StringBuffer buf = new StringBuffer(queryStr);
            if(params != null && params.size() > 0) {
                buf.append(" [params: ");
                sqlLiteralForObject(buf, params.get(0));

                int len = params.size();
                for(int i = 1; i < len; i++) {
                    buf.append(", ");
                    sqlLiteralForObject(buf, params.get(i));
                }

                buf.append(']');
            }
            logObj.log(logLevel, buf.toString());
        }
    }

    public static void logSelectCount(Level logLevel, int count) {
        String countStr = (count == 1)
                          ? "=== returned 1 row."
                          : "=== returned " + count + " rows.";
        logObj.log(logLevel, countStr);
    }

    public static void logUpdateCount(Level logLevel, int count) {
        String countStr = (count == 1)
                          ? "=== updated 1 row."
                          : "=== updated " + count + " rows.";
        logObj.log(logLevel, countStr);
    }

    public static void logCommitTransaction(Level logLevel) {
        logObj.log(logLevel, "+++ transaction committed.");
    }

    public static void logRollbackTransaction(Level logLevel) {
        logObj.log(logLevel, "*** transaction rolledback.");
    }
    
    public static void logQueryError(Level logLevel, Throwable th) {
        logObj.log(logLevel, "*** error.", th);
    }

    public static void logQueryStart(Level logLevel, int count) {
        String countStr = (count == 1)
                          ? "--- will run 1 query."
                          : "--- will run " + count + " queries.";
        logObj.log(logLevel, countStr);
    }
}

package org.objectstyle.util;
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

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/** ObjectStyle log formatter for more readable logs. */
public final class LogFormatter extends Formatter {
    private static final Pattern classNamePat = Pattern.compile("\\.(\\w+\\.\\w+)$");
    private static final long ts = System.currentTimeMillis();

   /** Will trim long class names to only the last 2 components
    * that should be enough to identify where the call originated from. */
    public static String trimClassName(String className) {
        if(className == null)
            return null;
        Matcher match = classNamePat.matcher(className);
        return (match.find()) ? match.group(1) : className;
    }
    
    
    /** Logs exception stack trace in provided StringBuffer. */
    public static void logThrown(StringBuffer buf, Throwable th) {
        StringWriter out = new StringWriter();
        PrintWriter pout = new PrintWriter(out);
        th.printStackTrace(pout);
        pout.flush();
        pout.close();
        
        buf.append(out.getBuffer());
    }
    
    
    /** Format output message */    
    public String format(LogRecord record) {
        StringBuffer buf = new StringBuffer();
        buf.append(record.getLevel().getName())
        .append(' ')
        .append(trimClassName(record.getSourceClassName()))
        .append(' ')
        .append(record.getMillis() - ts)
        .append(": ")
        .append(record.getMessage())
        .append('\n');
        
        Throwable th = record.getThrown();
        if(th != null)
            logThrown(buf, th);
        
        return buf.toString();
    }
}



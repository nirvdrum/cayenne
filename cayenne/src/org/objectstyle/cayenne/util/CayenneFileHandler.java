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

import java.io.*;
import java.util.logging.FileHandler;

/**
 * File logging handler used by CayenneModeler. Intersepts all calls
 * to <code>stdout</code> and <code>stderr</code> redirecting output 
 * to a file.
 * 
 * @author Andrei Adamchik
 */
public class CayenneFileHandler extends FileHandler {

	/**
	 * Constructor for CayenneFileHandler.
	 * @throws IOException
	 * @throws SecurityException
	 */
	public CayenneFileHandler() throws IOException, SecurityException {
		super();
	}

	/**
	 * Constructor for CayenneFileHandler.
	 * @param pattern
	 * @throws IOException
	 * @throws SecurityException
	 */
	public CayenneFileHandler(String pattern)
		throws IOException, SecurityException {
		super(pattern);
	}

	/**
	 * Constructor for CayenneFileHandler.
	 * @param pattern
	 * @param append
	 * @throws IOException
	 * @throws SecurityException
	 */
	public CayenneFileHandler(String pattern, boolean append)
		throws IOException, SecurityException {
		super(pattern, append);
	}

	/**
	 * Constructor for CayenneFileHandler.
	 * @param pattern
	 * @param limit
	 * @param count
	 * @throws IOException
	 * @throws SecurityException
	 */
	public CayenneFileHandler(String pattern, int limit, int count)
		throws IOException, SecurityException {
		super(pattern, limit, count);
	}

	/**
	 * Constructor for CayenneFileHandler.
	 * @param pattern
	 * @param limit
	 * @param count
	 * @param append
	 * @throws IOException
	 * @throws SecurityException
	 */
	public CayenneFileHandler(
		String pattern,
		int limit,
		int count,
		boolean append)
		throws IOException, SecurityException {
		super(pattern, limit, count, append);
	}

    /**
     * Will use supplied output stream to redirect all stdin and stderr logging.
     * This is a naive implementation. A better one should use a special PrintStream
     * that would use this logger for the output instead of writing 
     * unformatted information to the file.
     */
	protected synchronized void setOutputStream(OutputStream out)
		throws SecurityException {
			
		super.setOutputStream(out);
        PrintStream pout = new PrintStream(out);
        System.setOut(pout);
        System.setErr(pout);
	}

}

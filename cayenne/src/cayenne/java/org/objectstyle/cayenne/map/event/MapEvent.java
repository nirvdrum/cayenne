/* ====================================================================
 * 
 * The ObjectStyle Group Software License, Version 1.0 
 *
 * Copyright (c) 2002-2003 The ObjectStyle Group 
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
package org.objectstyle.cayenne.map.event;

import org.objectstyle.cayenne.event.CayenneEvent;
import org.objectstyle.cayenne.util.Util;

/**
 * Superclass of CayenneModeler events.
 * 
 * @author Andrei Adamchik
 */
public abstract class MapEvent extends CayenneEvent {

	/** Signifies a changed object. */
	public static final int CHANGE = 1;

	/** Signifies a new object. */
	public static final int ADD = 2;

	/** Signifies a removed object. */
	public static final int REMOVE = 3;

	protected int id = CHANGE;
	protected String oldName;

	/**
	 * Constructor for MapEvent.
	 * 
	 * @param source event source
	 */
	public MapEvent(Object source) {
		super(source);
	}
	
	/**
	 * Constructor for MapEvent.
	 * 
	 * @deprecated Since 1.0b4, use MapEvent(Object, String)
	 */
	public MapEvent(Object source, String oldName, String newName) {
		this(source, oldName);
	}
	
	/**
	 * Constructor for MapEvent.
	 * 
	 * @param source event source
	 */
	public MapEvent(Object source, String oldName) {
		super(source);
		setOldName(oldName);
	}
	
	
	public boolean isNameChange() {
		return !Util.nullSafeEquals(getOldName(), getNewName());
	}
	
	/**
	 * Returns the id.
	 * @return int
	 */
	public int getId() {
		return id;
	}


	/**
	 * Returns the newName of the object that caused this event.
	 */
	public abstract String getNewName();


	/**
	 * Returns the oldName.
	 * @return String
	 */
	public String getOldName() {
		return oldName;
	}


	/**
	 * Sets the id.
	 * @param id The id to set
	 */
	public void setId(int id) {
		this.id = id;
	}

	/**
	 * Sets the oldName.
	 * @param oldName The oldName to set
	 */
	public void setOldName(String oldName) {
		this.oldName = oldName;
	}
}

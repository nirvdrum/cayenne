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
package org.objectstyle.cayenne.gui.datamap;

import java.util.*;

import org.objectstyle.cayenne.access.types.DefaultType;
import org.objectstyle.cayenne.gui.event.Mediator;
import org.objectstyle.cayenne.map.DbAttribute;
import org.objectstyle.cayenne.map.DbEntity;


public class Util {
	
	/** 
	 * Returns array of db attribute names for DbEntity mapped to 
	 * current ObjEntity. 
	 */
	public static String[] getDbAttributeNames(Mediator mediator, DbEntity entity) {
		java.util.List list = entity.getAttributeList();
		int list_size = list.size() + 1;
		String[] arr = new String[list_size];
		arr[0] = "";
		for (int i = 1; i < list_size; i++) {
			DbAttribute attribute = (DbAttribute)list.get(i-1);
			arr[i] = attribute.getName();
		}
		
		Arrays.sort(arr);
		return arr;
	}
	
	public static String[] getRegisteredTypeNames()
	{
	    Iterator it = DefaultType.defaultTypes();
	    ArrayList list = new ArrayList();
	    while(it.hasNext()) {
	        list.add(it.next());
	    }
	    
	    // can't use this anymore, ExtendedTypes are no longer a singleton
		// String [] arr = ExtendedTypeMap.sharedInstance().getRegisteredTypeNames();
		
		String[] ret_arr = new String[list.size() + 1];
		ret_arr[0] = "";
		for (int i = 0; i < list.size(); i++) ret_arr[i+1] = (String)list.get(i);
		return ret_arr;
	}

	public static String[] getDatabaseTypes()
	{
		// FIXME!!! Need to have a reference TypesMapping instance
		//String [] arr;
		//arr = TypesMapping.getDatabaseTypes();
		//String[] ret_arr = new String[arr.length + 1];
		String[] ret_arr = new String[1];
		ret_arr[0] = "";
		//for (int i = 0; i < arr.length; i++) ret_arr[i+1] = arr[i];
		return ret_arr;
	}

}
package org.objectstyle.cayenne.gui.util;
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


import org.objectstyle.cayenne.map.*;

/** Class for generating "Untitled" names for Attributes and Relationships.
 *  Also does name verifications of the existing names. */
public class NameGenerator {
	private static int domainCounter = 1;
	private static int dataMapCounter = 1;
	private static int dataNodeCounter = 1;
	private static int objEntityCounter = 1;
	private static int dbEntityCounter = 1;
	private static int objAttributeCounter = 1;
	private static int dbAttributeCounter = 1;
	private static int objRelationshipCounter = 1;
	private static int dbRelationshipCounter = 1;

	public static String getDomainName() {
		return "UntitledDomain" + domainCounter++;
	}

	public static String getDataMapName() {
		return "UntitledMap" + dataMapCounter++;
	}

	public static String getDataNodeName() {
		return "UntitledDataSrc" + dataNodeCounter++;
	}

	public static String getObjEntityName() {
		return "UntitledObjEntity" + objEntityCounter++;
	}

	public static String getDbEntityName() {
		return "UntitledDbEntity" + dbEntityCounter++;
	}

	
	public static String getObjAttributeName() {
		return "UntitledObjAttr" + objAttributeCounter++;
	}

	public static String getDbAttributeName() {
		return "UntitledDbAttr" + dbAttributeCounter++;
	}


	public static String getObjRelationshipName() {
		return "UntitledObjRel" + objRelationshipCounter++;
	}

	public static String getDbRelationshipName() {
		return "UntitledDbRel" + dbRelationshipCounter++;
	}



	/** Returns generated name for the ObjRelationships. 
	  * For to-one case and entity name "xxxx" it generates name "toXxxx".
	  * For to-many case and entity name "Xxxx" it generates name "xxxxArray". */
	public static String getObjRelationshipName(Entity to_entity, boolean to_many) {
		String name = to_entity.getName();
		if (to_many) {
			String lower_case_name = Character.toLowerCase(name.charAt(0)) 
									+ name.substring(1);
			return lower_case_name + "Array";
		} else {
			String upper_case_name = Character.toUpperCase(name.charAt(0)) 
									+ name.substring(1);
			return "to" + upper_case_name;
		}
	}

	/** Returns generated name for the DbRelationships. 
	  * For to-one case it generates name "TO_XXXX".
	  * For to-many case it generates name "XXXX_ARRAY". */
	public static String getDbRelationshipName(Entity to_entity, boolean to_many) {
		String name = to_entity.getName();
		if (to_many) {
			return name + "_ARRAY";
		} else {
			return "TO_" + name;
		}
	}



} 
package org.objectstyle.cayenne.map;
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

import java.util.*;

/**
 * Validates an object of DataMap and
 * generates objects of DataMapValidator.Message to describe
 * warnings. It is intended for use in GUI modelers or in
 * developer's code.
 *
 * <p>Validation rules:</p>
 * <ol>
 * <li>DataMap, any Attribute descendants, any Entity descendants, any
 * Relationship descendants, TableInfo must have their names.
 * <li>For each object of ObjEntity, or ObjAttribute, or ObjRelationship
 * there must be present an object of DbEntity, or DbAttribute,
 * or DbRelationship correspondingly.
 * <li>If an object (A) of DbEntity is connected to another object (B)
 * of DbEntity with an object of DbRelationship then B must have
 * the reverse relationship to A (also an object of DbRelationship).
 * <li>All DbEntities must have at least one primary key attribute defined.
 * <li>DbAttributes must have their database type defined.
 * <li>ObjEntities and ObjAttributes must have their Java class (type)
 * defined.
 * <li>DbRelationship and its reverse relationship can't both have
 * "toDependentPK == true" (while they can both be "false" or just one
 * can be "true").
 * <li>If both DbRelationship and its reverse relationship are "to one",
 * one of them must have  "toDependentPK == true" and the other one
 * "toDependentPK == false"
 * </ol>
 *
 * @author Andriy Shapochka
 * @version 1.0
 */

public class DataMapValidator {
  private List messages;


  public DataMapValidator() {
  }

  /**
   * Validates DataMap according to the rules mentioned above.
   * @param map DataMap to get validated
   * @return whether List of objects of Message or empty List when map is valid.
   */
  public List validate(DataMap map) {
    messages = new ArrayList();
    SortedMap objMap = map.getObjMap();
    Iterator objs = objMap.values().iterator();
    while(objs.hasNext()) validate0((ObjEntity)objs.next());
    SortedMap dbMap = map.getDbMap();
    Iterator dbs = dbMap.values().iterator();
    while(dbs.hasNext()) validate0((DbEntity)dbs.next());
    List t = messages;
    messages = null;
    return t;
  }

  private void validate0(ObjEntity obj) {
    if(obj.getName() == null || obj.getName().length() == 0)
      messages.add(new Message(new ObjEntityWrapper(obj),MessageCode.MISSING_NAME));
    if(obj.getClassName() == null || obj.getClassName().length() == 0)
      messages.add(new Message(new ObjEntityWrapper(obj),MessageCode.NO_CLASS));
    if(obj.getDbEntity() == null)
      messages.add(new Message(new ObjEntityWrapper(obj),MessageCode.NO_DBENTITY));

    HashMap attrMap = obj.getAttributes();
    Iterator attrs = attrMap.values().iterator();
    while(attrs.hasNext()) validate0((ObjAttribute)attrs.next());

    HashMap relMap = obj.getAttributes();
    Iterator rels = relMap.values().iterator();
    while(rels.hasNext()) validate0((ObjAttribute)rels.next());
  }

  private void validate0(ObjAttribute obj) {
    if(obj.getName() == null || obj.getName().length() == 0)
      messages.add(new Message(new ObjAttributeWrapper(obj),MessageCode.MISSING_NAME));
    if(obj.getType() == null || obj.getType().length() == 0)
      messages.add(new Message(new ObjAttributeWrapper(obj),MessageCode.NO_CLASS));
    if(obj.getDbAttribute() == null)
      messages.add(new Message(new ObjAttributeWrapper(obj),MessageCode.NO_DBATTRIBUTE));
  }

  private void validate0(ObjRelationship obj) {
    if(obj.getName() == null || obj.getName().length() == 0)
      messages.add(new Message(new ObjRelationshipWrapper(obj),MessageCode.MISSING_NAME));
    if(obj.getDbRelationshipList().size() == 0)
      messages.add(new Message(new ObjRelationshipWrapper(obj),MessageCode.NO_DBRELATIONSHIP));
  }

  private void validate0(DbEntity db) {
    if(db.getName() == null || db.getName().length() == 0)
      messages.add(new Message(new DbEntityWrapper(db),MessageCode.MISSING_NAME));
    if(db.getPrimaryKey().size() == 0)
      messages.add(new Message(new DbEntityWrapper(db),MessageCode.MISSING_PRIMARY_KEY));

    HashMap attrMap = db.getAttributes();
    Iterator attrs = attrMap.values().iterator();
    while(attrs.hasNext()) validate0((DbAttribute)attrs.next());

    HashMap relMap = db.getAttributes();
    Iterator rels = relMap.values().iterator();
    while(rels.hasNext()) validate0((DbAttribute)rels.next());
  }

  private void validate0(DbAttribute db) {
    if(db.getName() == null || db.getName().length() == 0)
      messages.add(new Message(new DbAttributeWrapper(db),MessageCode.MISSING_NAME));
    if(db.getType() < 0)
      messages.add(new Message(new DbAttributeWrapper(db),MessageCode.NO_TYPE));
  }

  private void validate0(DbRelationship db) {
    if(db.getName() == null || db.getName().length() == 0)
      messages.add(new Message(new DbRelationshipWrapper(db),MessageCode.MISSING_NAME));
    DbRelationship revdb = db.getReverseRelationship();
    if(revdb == null)
      messages.add(new Message(new DbRelationshipWrapper(db),MessageCode.MISSING_REVERSE_DBRELATIONSHIP));
    else {
      if(db.isToDependentPK() && revdb.isToDependentPK())
        messages.add(new Message(new DbRelationshipWrapper(db),MessageCode.DEPENDENT_PRIMARY_KEYS));
      if(!(db.isToMany()||revdb.isToMany()) && !(db.isToDependentPK()^revdb.isToDependentPK()))
        messages.add(new Message(new DbRelationshipWrapper(db),MessageCode.ONE_TO_ONE_COLLISION));
    }
  }

  public static class Message {
    private MessageCode code;
    private ValidationFriendly messageSource;

    public Message(ValidationFriendly messageSource, MessageCode code) {
      Message.this.code = code != null ? code : MessageCode.EMPTY;
      Message.this.messageSource = messageSource;
    }

    public MessageCode getCode() {
      return code;
    }

    public Object getMessageSource() {
      return messageSource.getWrappee();
    }

    public String toString() {
      String s = code.toString();
      if(messageSource != null) {
        s = messageSource.getTypenameToDisplay() +
            " " + messageSource.getNameToDisplay() +
            ": " + s;
      }
      return s;
    }
  }

  public static class MessageCode {
    private static int nextIndex = 0;
    private String description;
    public final int INDEX = nextIndex++;

    private MessageCode(String description) {
      MessageCode.this.description = description;
    }

    public String toString() {
      return description;
    }

    public static final MessageCode EMPTY = new MessageCode("");
    public static final MessageCode GENERAL = new MessageCode("DataMap is invalid");
    public static final MessageCode MISSING_NAME = new MessageCode("Name is missing");
    public static final MessageCode NO_DBENTITY = new MessageCode("No DbEntity is attached");
    public static final MessageCode NO_DBATTRIBUTE = new MessageCode("No DbAttribute is attached");
    public static final MessageCode NO_DBRELATIONSHIP = new MessageCode("No DbRelationship is attached");
    public static final MessageCode MISSING_REVERSE_DBRELATIONSHIP = new MessageCode("Reverse DbRelationship is missing");
    public static final MessageCode MISSING_PRIMARY_KEY = new MessageCode("Primary key is missing");
    public static final MessageCode NO_DBATTRIBUTE_TYPE = new MessageCode("No DbAttribute type is defined");
    public static final MessageCode NO_TYPE = new MessageCode("No type is defined");
    public static final MessageCode NO_CLASS = new MessageCode("No Java class is defined");
    public static final MessageCode DEPENDENT_PRIMARY_KEYS = new MessageCode("DbRelationship and reverse DbRelationship have \"toDependentPK == true\"");
    public static final MessageCode ONE_TO_ONE_COLLISION = new MessageCode("toDependentPK must be true either for DbRelationship or its reverse one in one-to-one");

    public static final MessageCode CODES[] = {
      EMPTY,
      GENERAL,
      MISSING_NAME,
      NO_DBENTITY,
      NO_DBATTRIBUTE,
      NO_DBRELATIONSHIP,
      MISSING_REVERSE_DBRELATIONSHIP,
      MISSING_PRIMARY_KEY,
      NO_DBATTRIBUTE_TYPE,
      NO_TYPE,
      NO_CLASS,
      DEPENDENT_PRIMARY_KEYS,
      ONE_TO_ONE_COLLISION
    };
  }


  private static abstract class ValidationFriendly {
    abstract String getNameToDisplay();
    abstract String getTypenameToDisplay();
    abstract Object getWrappee();
  }

  private static class ObjEntityWrapper extends ValidationFriendly {
    private ObjEntity obj;

    ObjEntityWrapper(ObjEntity obj) {
      ObjEntityWrapper.this.obj = obj;
    }
    String getNameToDisplay() {
      return obj.getName() != null ? obj.getName() : "<noname>";
    }
    String getTypenameToDisplay() {
      return "ObjEntity";
    }
    Object getWrappee() {
      return obj;
    }
  }
  private static class DbEntityWrapper extends ValidationFriendly {
    private DbEntity db;

    DbEntityWrapper(DbEntity db) {
      DbEntityWrapper.this.db = db;
    }
    String getNameToDisplay() {
      return db.getName() != null ? db.getName() : "<noname>";
    }
    String getTypenameToDisplay() {
      return "DbEntity";
    }
    Object getWrappee() {
      return db;
    }
  }
  private static class ObjAttributeWrapper extends ValidationFriendly {
    private ObjAttribute obj;

    ObjAttributeWrapper(ObjAttribute obj) {
      ObjAttributeWrapper.this.obj = obj;
    }
    String getNameToDisplay() {
      String s = obj.getEntity() != null ? obj.getEntity().getName()+"." : "";
      return s + (obj.getName() != null ? obj.getName() : "<noname>");
    }
    String getTypenameToDisplay() {
      return "ObjAttribute";
    }
    Object getWrappee() {
      return obj;
    }
  }
  private static class DbAttributeWrapper extends ValidationFriendly {
    private DbAttribute db;

    DbAttributeWrapper(DbAttribute db) {
      DbAttributeWrapper.this.db = db;
    }
    String getNameToDisplay() {
      String s = db.getEntity() != null ? db.getEntity().getName()+"." : "";
      return s + (db.getName() != null ? db.getName() : "<noname>");
    }
    String getTypenameToDisplay() {
      return "DbAttribute";
    }
    Object getWrappee() {
      return db;
    }
  }
  private static class ObjRelationshipWrapper extends ValidationFriendly {
    private ObjRelationship obj;

    ObjRelationshipWrapper(ObjRelationship obj) {
      ObjRelationshipWrapper.this.obj = obj;
    }
    String getNameToDisplay() {
      String s = obj.getSourceEntity() != null ? obj.getSourceEntity().getName()+"." : "";
      return s + (obj.getName() != null ? obj.getName() : "<noname>");
    }
    String getTypenameToDisplay() {
      return "ObjRelationship";
    }
    Object getWrappee() {
      return obj;
    }
  }
  private static class DbRelationshipWrapper extends ValidationFriendly {
    private DbRelationship db;

    DbRelationshipWrapper(DbRelationship db) {
      DbRelationshipWrapper.this.db = db;
    }
    String getNameToDisplay() {
      String s = db.getSourceEntity() != null ? db.getSourceEntity().getName()+"." : "";
      return s + (db.getName() != null ? db.getName() : "<noname>");
    }
    String getTypenameToDisplay() {
      return "DbRelationship";
    }
    Object getWrappee() {
      return db;
    }
  }
}
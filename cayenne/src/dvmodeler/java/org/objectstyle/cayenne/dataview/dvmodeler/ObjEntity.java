/* ====================================================================
 *
 * The ObjectStyle Group Software License, Version 1.0
 *
 * Copyright (c) 2002-2004 The ObjectStyle Group
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

package org.objectstyle.cayenne.dataview.dvmodeler;

import java.util.*;
import org.jdom.*;

/**
 *
 * @author Nataliya Kholodna
 * @version 1.0
 */

public class ObjEntity extends DVObject {
  private DataMap dataMap;
  private String className;
  private List objAttributes = new ArrayList();
  private List objEntityViews = new ArrayList();
  private List loadErrors = new ArrayList();

  private Set objAttributesNames = new HashSet();

  public ObjEntity(DataMap dataMap, Element element) throws DVModelerException{
    this.dataMap = dataMap;
    String entityPath = "<b>" + dataMap.getName() + ".";
    String attributeValue = element.getAttributeValue("name");
    if ((attributeValue == null) || (attributeValue.length() == 0)){
      entityPath += "</b><br>";
      loadErrors.add(entityPath + " ObjEntity has no attribute \"name\" and cannot be loaded.<br><br>");
      throw new DVModelerException(entityPath + " Entity has no attribute \"name\".");
    } else {
      setName(attributeValue);
      entityPath += attributeValue + "</b><br>";
    }
    attributeValue = element.getAttributeValue("className");
    if ((attributeValue == null) || (attributeValue.length() == 0)){
      className = "";
      loadErrors.add(entityPath + " ObjEntity has no attribute \"class-name\"<br><br>");
    } else {
      className = attributeValue;
    }
    List children = element.getChildren();
    java.util.List attributeErrors = new ArrayList();
    Iterator itr = children.iterator();
    while(itr.hasNext()){
      Object o = itr.next();
      Element e = (Element)o;
      if (e.getName().equals("obj-attribute")){
        ObjAttribute objAttribute = new ObjAttribute(e);
        objAttributes.add(objAttribute);
        if (objAttributesNames.add(objAttribute.getName()) == false){
          String path = "<b>" + dataMap + "." + getName() + "</b><br>";
          loadErrors.add(path + "ObjAttribute \"" + objAttribute.getName()
              + "\" already exists in the ObjEntity\"" + getName() + "\"<br><br>");
        }

        attributeErrors.addAll(objAttribute.getLoadErrors());
      }
    }
    objAttributesNames.clear();
    for (Iterator j = attributeErrors.iterator(); j.hasNext();){
      String error = entityPath + ((String)j.next());
      loadErrors.add(error);
    }
    dataMap.addObjEntity(this);
  }

  public ObjEntity(DataMap dataMap){
    this.dataMap = dataMap;
    setName("ObjEntity");
    className = "";
    dataMap.addObjEntity(this);
  }

  public List getLoadErrors(){
    return Collections.unmodifiableList(loadErrors);
  }

  public void setDataMap(DataMap dataView){
    this.dataMap = dataMap;
  }

  public DataMap getDataMap(){
    return dataMap;
  }

  public void addObjEntityView(ObjEntityView objEntityView){
    objEntityViews.add(objEntityView);
  }

  public void removeObjEntityView(ObjEntityView objEntityView){
    objEntityViews.remove(objEntityView);
  }

  public List getObjAttributes(){
    return Collections.unmodifiableList(objAttributes);
  }

  public ObjAttribute getObjAttribute(String name){
  	Iterator itr = objAttributes.iterator();
  	while (itr.hasNext()){
  		Object o = itr.next();
  		ObjAttribute attribute = (ObjAttribute)o;
  		if(attribute.getName().equals(name)){
  		  return attribute;
  		}
  	}
  	return null;
  }

  public ObjAttribute getObjAttribute(int index){
    return (ObjAttribute)objAttributes.get(index);
  }

  public List getObjEntityViews(){
    return Collections.unmodifiableList(objEntityViews);
  }

  private void setClassName(String className){
    this.className = className;
  }

  public String getClassName(){
    return className;
  }

  public String toString(){
    return this.getName();
  }

  public ObjEntityView getObjEntityView(int index){
    return (ObjEntityView)(objEntityViews.get(index));
  }

  public int getObjEntityViewCount(){
    return objEntityViews.size();
  }

  public int getObjAttributeCount(){
    return objAttributes.size();
  }

  public int getIndexOfObjEntityView(ObjEntityView objEntityView){
    return objEntityViews.indexOf(objEntityView);
  }

}

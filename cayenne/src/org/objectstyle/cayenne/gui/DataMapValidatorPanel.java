package org.objectstyle.cayenne.gui;
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

import java.awt.*;
import org.objectstyle.cayenne.map.*;
import javax.swing.event.*;
import java.util.*;
import javax.swing.*;

/**
 * GUI to show and interact with DataMap validation messages
 *
 * @author Andriy Shapochka
 * @version 1.0
 */

public class DataMapValidatorPanel extends JPanel {
  BorderLayout borderLayout1 = new BorderLayout();
  private DataMap dataMap;
  private transient Vector listSelectionListeners;
  private DataMapValidator validator = new DataMapValidator();
  private java.util.List messages = new ArrayList();
  JScrollPane listScrollPane = new JScrollPane();
  JList messageList = new JList();

  public DataMapValidatorPanel() {
    try {
      jbInit();
    }
    catch(Exception ex) {
      ex.printStackTrace();
    }
  }
  void jbInit() throws Exception {
    messageList.addListSelectionListener(new javax.swing.event.ListSelectionListener() {
      public void valueChanged(ListSelectionEvent e) {
        messageList_valueChanged(e);
      }
    });
    messageList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    this.setLayout(borderLayout1);
    this.add(listScrollPane, BorderLayout.CENTER);
    listScrollPane.getViewport().add(messageList, null);
    setFont(getFont());
    setForeground(getForeground());
  }

  /**
   * sets current DataMap
   * @param dataMap - DataMap to be validated
   */
  public void setDataMap(DataMap dataMap) {
    this.dataMap = dataMap;
  }
  public DataMap getDataMap() {
    return dataMap;
  }
  public synchronized void removeListSelectionListener(ListSelectionListener l) {
    if (listSelectionListeners != null && listSelectionListeners.contains(l)) {
      Vector v = (Vector) listSelectionListeners.clone();
      v.removeElement(l);
      listSelectionListeners = v;
    }
  }
  public synchronized void addListSelectionListener(ListSelectionListener l) {
    Vector v = listSelectionListeners == null ? new Vector(2) : (Vector) listSelectionListeners.clone();
    if (!v.contains(l)) {
      v.addElement(l);
      listSelectionListeners = v;
    }
  }
  protected void fireValueChanged(ListSelectionEvent e) {
    if (listSelectionListeners != null) {
      Vector listeners = listSelectionListeners;
      int count = listeners.size();
      for (int i = 0; i < count; i++) {
        ((ListSelectionListener) listeners.elementAt(i)).valueChanged(e);
      }
    }
  }

  void messageList_valueChanged(ListSelectionEvent e) {
    fireValueChanged(new ListSelectionEvent(this,e.getFirstIndex(),e.getLastIndex(),e.getValueIsAdjusting()));
  }

  /**
   * @return current DataMapValidator
   */
  public DataMapValidator getValidator() {
    return validator;
  }

  /**
   *
   * @return list of objects of DataMapValidator.Message from last validation
   */
  public java.util.List getMessages() {
    return Collections.unmodifiableList(messages);
  }

  /**
   * validates and displays current list of messages (DataMapValidator.Message)
   */
  public void validateDataMap() {
    if(dataMap == null) return;
    messages = validator.validate(dataMap);
    messageList.setModel(new MessageListModel(messages));
  }
  public void setFont(Font font) {
    super.setFont( font);
    if(messageList != null) messageList.setFont(font);
  }
  public void setForeground(Color fg) {
    super.setForeground(fg);
    if(messageList != null) messageList.setForeground(fg);
  }
}

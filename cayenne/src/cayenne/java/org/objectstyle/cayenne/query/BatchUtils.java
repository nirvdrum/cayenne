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

package org.objectstyle.cayenne.query;

import java.util.*;
import org.apache.commons.lang.builder.*;
import org.objectstyle.cayenne.*;
import org.objectstyle.cayenne.map.*;

/**
 *
 * @author Andriy Shapochka
 */

public class BatchUtils {

  private BatchUtils() {
  }

  public static Map buildSnapshotForUpdate(DataObject o) {
    Map committedSnapshot = o.getCommittedSnapshot();
    Map currentSnapshot = o.getCurrentSnapshot();
    Map snapshot = null;
    if (committedSnapshot == null || committedSnapshot.isEmpty()) {
      snapshot = Collections.unmodifiableMap(currentSnapshot);
      return snapshot;
    } else snapshot = new HashMap(currentSnapshot.size());
    Iterator it = currentSnapshot.entrySet().iterator();
    while (it.hasNext()) {
      Map.Entry entry = (Map.Entry)it.next();
      String attrName = (String)entry.getKey();
      Object newValue = entry.getValue();
      // if snapshot exists, compare old values and new values,
      // only add attribute to the update clause if the value has changed
      Object oldValue = committedSnapshot.get(attrName);
      if ((newValue == null && oldValue != null) ||
          (newValue != null && !newValue.equals(oldValue))) snapshot.put(attrName, newValue);
    }

    // original snapshot can have extra keys that are missing in current snapshot
    // process those
    Iterator origit = committedSnapshot.entrySet().iterator();
    while (origit.hasNext()) {
      Map.Entry entry = (Map.Entry)origit.next();
      String attrName = (String) entry.getKey();
      Object oldValue = entry.getValue();
      if (oldValue == null || currentSnapshot.containsKey(attrName)) continue;
      snapshot.put(attrName, null);
    }

    return Collections.unmodifiableMap(snapshot);
  }

  public static int hashCode(Collection c) {
    HashCodeBuilder builder = new HashCodeBuilder();
    for (Iterator i = c.iterator(); i.hasNext();) builder.append(i.next());
    return builder.toHashCode();
  }

  public static Map buildFlattenedSnapshot(Map sourceId, Map destinationId, DbRelationship firstRelationship, DbRelationship secondRelationship) {
      Map snapshot = new HashMap(sourceId.size() + destinationId.size());
      List joins = firstRelationship.getJoins();
      for(int i=0; i<joins.size(); i++) {
          DbAttributePair thisJoin = (DbAttributePair) joins.get(i);
          DbAttribute sourceAttribute = thisJoin.getSource();
          DbAttribute targetAttribute = thisJoin.getTarget();
          snapshot.put(targetAttribute.getName(), sourceId.get(sourceAttribute.getName()));
      }
      joins=secondRelationship.getJoins();
      for(int i=0; i<joins.size(); i++) {
          DbAttributePair thisJoin = (DbAttributePair) joins.get(i);
          DbAttribute sourceAttribute = thisJoin.getSource();
          DbAttribute targetAttribute = thisJoin.getTarget();
          snapshot.put(sourceAttribute.getName(), destinationId.get(targetAttribute.getName()));
      }
      return snapshot;
  }
}
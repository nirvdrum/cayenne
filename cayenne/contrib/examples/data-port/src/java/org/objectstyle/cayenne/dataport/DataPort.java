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
package org.objectstyle.cayenne.dataport;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.objectstyle.cayenne.CayenneException;
import org.objectstyle.cayenne.access.DataNode;
import org.objectstyle.cayenne.access.QueryResult;
import org.objectstyle.cayenne.access.ResultIterator;
import org.objectstyle.cayenne.access.util.IteratedSelectObserver;
import org.objectstyle.cayenne.map.DbEntity;
import org.objectstyle.cayenne.query.DeleteQuery;
import org.objectstyle.cayenne.query.InsertBatchQuery;
import org.objectstyle.cayenne.query.SelectQuery;

/**
 * Engine to port data between two DataNodes. These nodes can potentially connect
 * to databases from different vendors. The only assumption is that all of the 
 * DbEntities (tables) being ported are present in both source and destination databases, 
 * and are adequately described by Cayenne mapping. 
 * 
 * <p>DataPort implements a Cayenne-based algorithm to read data from one data node
 * and write to another. It uses DataPortDelegate interface to decouple porting logic 
 * from such things like filtering entities (include/exclude from port based on some criteria),
 * logging the progress of port operation, qualifying the queries, etc. It is possible to build 
 * various configurable interfaces for this tool. E.g. Cayenne implements CayenneDataPort 
 * Ant task.
 * </p>
 * 
 * @author Andrei Adamchik
 */
public class DataPort
{
  public static final int INSERT_BATCH_SIZE = 1000;

  protected DataNode sourceNode;
  protected DataNode destinationNode;
  protected List entities;
  protected boolean cleaningDestination;
  protected DataPortDelegate delegate;

  /**
   * Creates new DataPort instance, initializing it 
   * with a DataPortDelegate.  
   */
  public DataPort(DataPortDelegate delegate)
  {
    super();
    this.delegate = delegate;
  }

  /**
   * Runs DataPort. The instance must be fully configured by the time this method
   * is invoked, having its delegate, source and destinatio nodes, and a list of entities 
   * set up. 
   */
  public void execute() throws Exception
  {
    // sanity check
    if (sourceNode == null)
    {
      throw new CayenneException("Can't port data, source node is null.");
    }

    if (destinationNode == null)
    {
      throw new CayenneException("Can't port data, destination node is null.");
    }

    // the simple equality check may actually detect problems with misconfigred nodes
    // it is not as dumb as it may look at first
    if (sourceNode == destinationNode)
    {
      throw new CayenneException("Can't port data, source and target nodes are the same.");
    }

    if (entities == null || entities.isEmpty())
    {
      return;
    }

    // sort entities for insertion
    List sorted = new ArrayList(entities);
    destinationNode.getDependencySorter().sortDbEntities(sorted, false);

    if (cleaningDestination)
    {
      // reverse insertion order for deletion
      List entitiesInDeleteOrder = new ArrayList(sorted.size());
      entitiesInDeleteOrder.addAll(sorted);
      Collections.reverse(entitiesInDeleteOrder);
      processDelete(entitiesInDeleteOrder);
    }

    processInsert(sorted);
  }

  /**
   * Cleans up destination tables data.
   */
  protected void processDelete(List entities)
  {
    // Allow delegate to modify the list of entities
    // any way it wants. For instance delegate may filter 
    // or sort the list (though it doesn't have to, and can simply
    // pass through the original list). 
    if (delegate != null)
    {
      entities = delegate.willCleanData(this, entities);
    }

    if (entities == null || entities.isEmpty())
    {
      return;
    }

    // Using QueryResult as observer for the data cleanup.
    // This allows to collect query statistics and pass it to the delegate.
    QueryResult observer = new QueryResult();

    // Delete data from entities one by one
    Iterator it = entities.iterator();
    while (it.hasNext())
    {
      DbEntity entity = (DbEntity) it.next();
      DeleteQuery query = new DeleteQuery();

      // using DbEntity as a query root is unusual but will still work with Cayenne
      query.setRoot(entity);

      // notify delegate that delete is about to happen
      if (delegate != null)
      {
        delegate.willCleanData(this, entity, query);
      }

      // perform delete query
      observer.clear();
      destinationNode.performQuery(query, observer);

      // notify delegate that delete just happened
      if (delegate != null)
      {
        // observer will store query statistics
        int count = observer.getFirstUpdateCount(query);
        delegate.didCleanData(this, entity, count);
      }
    }
  }

  /**
   * Reads source data from source, saving it to destination.
   */
  protected void processInsert(List entities) throws Exception
  {
    // Allow delegate to modify the list of entities
    // any way it wants. For instance delegate may filter 
    // or sort the list (though it doesn't have to, and can simply
    // pass through the original list). 
    if (delegate != null)
    {
      entities = delegate.willCleanData(this, entities);
    }

    if (entities == null || entities.isEmpty())
    {
      return;
    }

    // Create an observer for to get the iterated result
    // instead of getting each table as a list
    IteratedSelectObserver observer = new IteratedSelectObserver();

    // Using QueryResult as observer for the data cleanup.
    // This allows to collect query statistics and pass it to the delegate.
    QueryResult insertObserver = new QueryResult();

    // process ordered list of entities one by one
    Iterator it = entities.iterator();
    while (it.hasNext())
    {
      insertObserver.clear();

      DbEntity entity = (DbEntity) it.next();

      SelectQuery select = new SelectQuery();
      select.setRoot(entity);
      select.setFetchingDataRows(true);

      sourceNode.performQuery(select, observer);
      ResultIterator result = observer.getResultIterator();
      InsertBatchQuery insert = new InsertBatchQuery(entity, INSERT_BATCH_SIZE);

      if (delegate != null)
      {
        delegate.willPortEntity(this, entity, select);
      }

      try
      {
        int count = 0;

        // Split insertions into the same table into batches of 1000. 
        // This will allow to process tables of arbitrary big size
        // without running out of memory. 
        int currentRow = 0;

        while (result.hasNextRow())
        {
          if (currentRow > 0 && currentRow % INSERT_BATCH_SIZE == 0)
          {
            // end of the batch detected... commit and start a new insert query
            destinationNode.performQuery(insert, insertObserver);
            insert = new InsertBatchQuery(entity, INSERT_BATCH_SIZE);
            count += insertObserver.getFirstUpdateCount(insert);
            insertObserver.clear();
          }

          currentRow++;

          Map nextRow = result.nextDataRow();
          insert.add(nextRow);
        }

        // commit last batch if needed
        if (insert.size() > 0)
        {
          destinationNode.performQuery(insert, insertObserver);
          count += insertObserver.getFirstUpdateCount(insert);
        }

        if (delegate != null)
        {
          delegate.didPortEntity(this, entity, count);
        }
      }
      finally
      {
        try
        {
          // don't forget to close ResultIterator
          result.close();
        }
        catch (CayenneException ex)
        {
        }
      }
    }
  }

  public List getEntities()
  {
    return entities;
  }

  public DataNode getSourceNode()
  {
    return sourceNode;
  }

  public DataNode getDestinationNode()
  {
    return destinationNode;
  }

  /**
   * Sets the initial list of entities to process. This list can
   * be later modified by the delegate.
   */
  public void setEntities(List entities)
  {
    this.entities = entities;
  }

  /**
   * Sets the DataNode serving as a source of the ported data.
   */
  public void setSourceNode(DataNode sourceNode)
  {
    this.sourceNode = sourceNode;
  }

  /**
   * Sets the DataNode serving as a destination of the ported data.
   */
  public void setDestinationNode(DataNode destinationNode)
  {
    this.destinationNode = destinationNode;
  }

  public DataPortDelegate getDelegate()
  {
    return delegate;
  }

  public void setDelegate(DataPortDelegate delegate)
  {
    this.delegate = delegate;
  }

  public boolean isCleaningDestination()
  {
    return cleaningDestination;
  }

  /**
   * Defines whether DataPort should delete all data from destination
   * tables before doing the port. 
   */
  public void setCleaningDestination(boolean cleaningDestination)
  {
    this.cleaningDestination = cleaningDestination;
  }

}

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
package org.objectstyle.cayenne.dataport.ant;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;
import org.apache.log4j.varia.NullAppender;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.objectstyle.cayenne.access.DataDomain;
import org.objectstyle.cayenne.access.DataNode;
import org.objectstyle.cayenne.conf.Configuration;
import org.objectstyle.cayenne.conf.FileConfiguration;
import org.objectstyle.cayenne.dataport.DataPort;
import org.objectstyle.cayenne.map.DataMap;
import org.objectstyle.cayenne.util.Util;

/**
 * Ant task implementing a frontend to DataPort allowing
 * porting database data using Ant build scripts.
 * 
 * @author Andrei Adamchik
 */
public class CayenneDataPort extends Task
{
  protected File projectFile;
  protected String maps;
  protected String srcNode;
  protected String destNode;
  protected String includeTables;
  protected String excludeTables;
  protected boolean cleanDest = true;

  public void execute() throws BuildException
  {
    validateParameters();

    // suppress Cayenne logging
    // in the future do something smarter 
    // (like redirecting Log4J logging to the Ant log)
    Configuration.setLoggingConfigured(true);
    Logger.getRootLogger().removeAllAppenders();
    Logger.getRootLogger().addAppender(new NullAppender());

    FileConfiguration configuration = new FileConfiguration();
    configuration.addFilesystemPath(projectFile.getParentFile());
    Configuration.initializeSharedConfiguration(configuration);

    // perform project validation
    DataNode source = findNode(configuration, srcNode);
    if (source == null)
    {
      throw new BuildException("srcNode not found in the project: " + srcNode);
    }

    DataNode destination = findNode(configuration, destNode);
    if (destination == null)
    {
      throw new BuildException(
        "destNode not found in the project: " + destNode);
    }

    log("Porting from '" + srcNode + "' to '" + destNode + "'.");

    AntDataPortDelegate portDelegate =
      new AntDataPortDelegate(this, maps, includeTables, excludeTables);
    DataPort dataPort = new DataPort(portDelegate);
    dataPort.setEntities(getAllEntities(source));
    dataPort.setCleaningDestination(cleanDest);
    dataPort.setSourceNode(source);
    dataPort.setDestinationNode(destination);

    try
    {
      dataPort.execute();
    }
    catch (Exception e)
    {
      Throwable topOfStack = Util.unwindException(e);
      throw new BuildException(
        "Error porting data: " + topOfStack.getMessage(),
        topOfStack);
    }
  }

  protected DataNode findNode(Configuration configuration, String name)
  {
    Iterator domains = configuration.getDomains().iterator();
    while (domains.hasNext())
    {
      DataDomain domain = (DataDomain) domains.next();
      DataNode node = domain.getNode(name);
      if (node != null)
      {
        return node;
      }
    }

    return null;
  }

  protected List getAllEntities(DataNode node)
  {
    List allEntities = new ArrayList();

    Iterator maps = node.getDataMaps().iterator();
    while (maps.hasNext())
    {
      DataMap map = (DataMap) maps.next();
      allEntities.addAll(map.getDbEntities());
    }

    return allEntities;
  }

  protected void validateParameters() throws BuildException
  {
    if (projectFile == null)
    {
      throw new BuildException("Required 'projectFile' parameter is missing.");
    }

    if (!projectFile.exists())
    {
      throw new BuildException("'projectFile' does not exist: " + projectFile);
    }

    if (srcNode == null)
    {
      throw new BuildException("Required 'srcNode' parameter is missing.");
    }

    if (destNode == null)
    {
      throw new BuildException("Required 'destNode' parameter is missing.");
    }
  }

  public void setDestNode(String destNode)
  {
    this.destNode = destNode;
  }

  public void setExcludeTables(String excludeTables)
  {
    this.excludeTables = excludeTables;
  }

  public void setIncludeTables(String includeTables)
  {
    this.includeTables = includeTables;
  }

  public void setMaps(String maps)
  {
    this.maps = maps;
  }

  public void setProjectFile(File projectFile)
  {
    this.projectFile = projectFile;
  }

  public void setSrcNode(String srcNode)
  {
    this.srcNode = srcNode;
  }

  public void setCleanDest(boolean flag)
  {
    this.cleanDest = flag;
  }
}

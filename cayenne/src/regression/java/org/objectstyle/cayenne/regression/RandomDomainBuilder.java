package org.objectstyle.cayenne.regression;

import java.util.*;
import java.io.*;
import java.sql.*;
import org.apache.commons.lang.*;
import org.objectstyle.ashwood.dbutil.*;
import org.objectstyle.ashwood.graph.*;
import org.objectstyle.cayenne.*;
import org.objectstyle.cayenne.access.*;
import org.objectstyle.cayenne.map.*;
import org.objectstyle.cayenne.util.*;
import org.objectstyle.cayenne.dba.*;

/**
 * RandomDomainBuilder uses ASHWOOD utilities and algorithms to build complex
 * randomized Oracle schemas with referential constraints, pk dependencies,
 * automatic key generation. It also builds the corresponding DataMaps and
 * attaches them to preconfigured DataNodes and DataDomains.
 * DataModificationRobot operates on these schemas.
 *
 * @author Andriy Shapochka
 */

public class RandomDomainBuilder {
  private DataDomain domain;
  private RandomSchema randomSchema = new RandomSchema();
  private Map objEntitiesByTable;
  private List createTableStatements;
  private List createSequenceStatements;
  private List dropTableStatements;
  private List dropSequenceStatements;
  private boolean schemaGenerated;

  public RandomDomainBuilder(DataDomain domain) {
    this.domain = domain;
  }

  public void generate(File generatedScriptDir) throws CayenneException {
    generateSchema(generatedScriptDir);
  }

  public void drop() throws CayenneException {
    dropSchema();
  }

  private void generateSchema(File dir) throws CayenneException {
    if (schemaGenerated) throw new CayenneException("Schema already exists");
    randomSchema.generate();
    List tables = randomSchema.getTables();
    Map sequencesByTable = randomSchema.getSequencesByTable();
    createTableStatements = new ArrayList(tables.size());
    createSequenceStatements = new ArrayList(sequencesByTable.size());
    dropTableStatements = new ArrayList(tables.size());
    dropSequenceStatements = new ArrayList(sequencesByTable.size());
    for (Iterator i = tables.iterator(); i.hasNext();) {
      StringWriter buffer = new StringWriter();
      PrintWriter out = new PrintWriter(buffer);
      Table table = (Table)i.next();
      table.toCreateSQL(out);
      out.flush();
      createTableStatements.add(buffer.toString());
      Sequence sequence = (Sequence)sequencesByTable.get(table);
      if (sequence != null) {
        StringWriter buffer1 = new StringWriter();
        PrintWriter out1 = new PrintWriter(buffer1);
        sequence.toCreateSQL(out1);
        out1.flush();
        createSequenceStatements.add(buffer1.toString());
        buffer1 = new StringWriter();
        out1 = new PrintWriter(buffer1);
        sequence.toDropSQL(out1);
        out1.flush();
        dropSequenceStatements.add(buffer1.toString());
      }
    }
    for (ListIterator i = tables.listIterator(tables.size()); i.hasPrevious();) {
      StringWriter buffer = new StringWriter();
      PrintWriter out = new PrintWriter(buffer);
      Table table = (Table)i.previous();
      table.toDropSQL(out);
      out.flush();
      dropTableStatements.add(buffer.toString());
    }
    if (dir != null) {
      try {
        FileWriter createSchemaScript = new FileWriter(new File(dir, "create_schema.sql"));
        PrintWriter out = new PrintWriter(createSchemaScript);
        script(createSequenceStatements, out);
        script(createTableStatements, out);
        out.close();
        createSchemaScript.close();
        FileWriter dropSchemaScript = new FileWriter(new File(dir, "drop_schema.sql"));
        out = new PrintWriter(dropSchemaScript);
        script(dropSequenceStatements, out);
        script(dropTableStatements, out);
        out.close();
        dropSchemaScript.close();
      }
      catch (IOException ex) {
        throw new CayenneException(ex);
      }
    }
    generateDataMap(dir);
    Connection connection = null;
    try {
      DataNode[] nodes = domain.getDataNodes();
      if (nodes == null || nodes.length == 0) throw new CayenneException("No data nodes configured.");
      DataNode node = nodes[0];
      connection = node.getDataSource().getConnection();
      schemaGenerated = true;
      executeStatements(createTableStatements, connection);
      executeStatements(createSequenceStatements, connection);
      try {connection.close();}
      catch (Exception exc) {}
    }
    catch (SQLException ex) {
      try {connection.close();}
      catch (Exception exc) {}
      dropSchema();
      throw new CayenneException(ex);
    }
  }

  private void dropSchema() throws CayenneException {
    Connection connection = null;
    try {
      DataNode[] nodes = domain.getDataNodes();
      if (nodes == null || nodes.length == 0) throw new CayenneException("No data nodes configured.");
      DataNode node = nodes[0];
      connection = node.getDataSource().getConnection();
      executeAllStatements(dropTableStatements, connection);
      executeAllStatements(dropSequenceStatements, connection);
      schemaGenerated = false;
      try {connection.close();}
      catch (Exception exc) {}
    }
    catch (SQLException ex) {
      try {connection.close();}
      catch (Exception exc) {}
      throw new CayenneException(ex);
    }
  }

  private void executeStatements(List statements, Connection c) throws SQLException {
    for (Iterator i = statements.iterator(); i.hasNext();) {
      PreparedStatement ps = c.prepareStatement(i.next().toString());
      ps.execute();
      ps.close();
    }
  }

  private void executeAllStatements(List statements, Connection c) {
    for (Iterator i = statements.iterator(); i.hasNext();) {
      try {
        PreparedStatement ps = c.prepareStatement(i.next().toString());
        ps.execute();
        ps.close();
      }
      catch (SQLException ex) {
      }
    }
  }

  private void script(List statements, PrintWriter out) {
    for (Iterator i = statements.iterator(); i.hasNext();) {
      out.print(i.next());
      out.println(';');
    }
  }

  private void generateDataMap(File dir) throws CayenneException {
    DataNode[] nodes = domain.getDataNodes();
    if (nodes == null || nodes.length == 0) throw new CayenneException("No data nodes configured.");
    DataNode node = nodes[0];
    Digraph refDigraph = randomSchema.getSchemaGraph();
    List tables = randomSchema.getTables();
    Map sequencesByTable = randomSchema.getSequencesByTable();
    DataMap dataMap = new DataMap("DataMap-" + StringUtils.defaultString(randomSchema.getSchemaName()));
    List objEntities = new ArrayList(tables.size());
    objEntitiesByTable = new HashMap(tables.size());
    Map tablesToFlatten = new HashMap();
    for (Iterator i = tables.iterator(); i.hasNext();) {
      Table table = (Table)i.next();
      DbEntity dbEntity = createDbEntity(table, (Sequence)sequencesByTable.get(table));
      if (refDigraph.outgoingSize(table) == 0 && refDigraph.incomingSize(table) == 2) {
          tablesToFlatten.put(table, dbEntity);
          dataMap.addDbEntity(dbEntity);
          continue;
      }
      ObjEntity objEntity = createObjEntity(dbEntity);
      objEntities.add(objEntity);
      objEntitiesByTable.put(table, objEntity);
    }
    Collections.shuffle(objEntities);
    for (Iterator i = objEntities.iterator(); i.hasNext();) {
      ObjEntity entity = (ObjEntity)i.next();
      entity.getDbEntity().setParent(dataMap);
      dataMap.addDbEntity(entity.getDbEntity());
      entity.setParent(dataMap);
      dataMap.addObjEntity(entity);
    }
    Set usedFlattenedTables = new HashSet();
    for (Iterator i = tables.iterator(); i.hasNext();) {
      Table pkTable = (Table)i.next();
      for (ArcIterator j = refDigraph.outgoingIterator(pkTable); j.hasNext();) {
        j.next();
        Table fkTable = (Table)j.getDestination();
        if (!tablesToFlatten.containsKey(fkTable)) createRelationship(pkTable, fkTable);
        else if (usedFlattenedTables.add(fkTable)) {
            ArcIterator masterIt = refDigraph.incomingIterator(fkTable);
            masterIt.next();
            Table master1 = (Table)masterIt.getOrigin();
            masterIt.next();
            Table master2 = (Table)masterIt.getOrigin();
            flattenRelationship(master1,
                                master2,
                                fkTable,
                                (DbEntity)tablesToFlatten.get(fkTable));
        }
      }
    }

    node.getAdapter().getPkGenerator().reset();
    node.setDataMaps(Collections.singletonList(dataMap));
    domain.reset();
    domain.clearDataMaps();
    domain.addNode(node);

    if (dir != null) {
      try {
        FileWriter dataMapXml = new FileWriter(new File(dir, dataMap.getName() + ".xml"));
        PrintWriter out = new PrintWriter(dataMapXml);
        MapLoader mapSaver = new MapLoader();
        mapSaver.storeDataMap(out, dataMap);
        out.close();
        dataMapXml.close();
      }
      catch (IOException ex) {
        throw new CayenneException(ex);
      }
    }
  }

  private DbEntity createDbEntity(Table table, Sequence sequence) {
    DbEntity dbEntity = new DbEntity(table.getName());
    dbEntity.setCatalog(table.getCatalog());
    dbEntity.setSchema(table.getSchema());
    if (sequence != null) {
      DbKeyGenerator pkGenerator = new DbKeyGenerator();
      pkGenerator.setGeneratorName(sequence.getName());
      pkGenerator.setGeneratorType(DbKeyGenerator.ORACLE_TYPE);
      pkGenerator.setKeyCacheSize(new Integer(sequence.getIncrement()));
      dbEntity.setPrimaryKeyGenerator(pkGenerator);
    }
    Collection columns = table.getColumns();
    Collection primaryKeys = table.getPrimaryKeys();
    for (Iterator i = columns.iterator(); i.hasNext();) {
      Column column = (Column)i.next();
      DbAttribute attribute = new DbAttribute(column.getName(), Types.INTEGER, dbEntity);
      attribute.setMandatory(true);
      for (Iterator j = primaryKeys.iterator(); j.hasNext();) {
        PrimaryKey pk = (PrimaryKey)j.next();
        if (pk.getColumnName().equals(attribute.getName()))
          attribute.setPrimaryKey(true);
      }
      dbEntity.addAttribute(attribute);
    }
    return dbEntity;
  }

  private void createRelationship(Table pkTable, Table fkTable) {
    ObjEntity pkObjEntity = (ObjEntity)objEntitiesByTable.get(pkTable);
    ObjEntity fkObjEntity = (ObjEntity)objEntitiesByTable.get(fkTable);
    DbEntity pkEntity = pkObjEntity.getDbEntity();
    DbEntity fkEntity = fkObjEntity.getDbEntity();
    Collection foreignKeys = fkTable.getForeignKeys();
    Collection primaryKeys = fkTable.getPrimaryKeys();
    for (Iterator i = foreignKeys.iterator(); i.hasNext();) {
      ForeignKey fk = (ForeignKey)i.next();
      if (fk.getPkTableName().equals(pkTable.getName())) {
        DbRelationship forwardRelation = new DbRelationship(pkEntity.getName() + '_' + fkEntity.getName() + '_' + fk.getName());
        forwardRelation.setToMany(true);
        for (Iterator j = primaryKeys.iterator(); j.hasNext();) {
          PrimaryKey pk = (PrimaryKey)j.next();
          if (pk.getColumnName().equals(fk.getColumnName())) {
            forwardRelation.setToDependentPK(true);
            break;
          }
        }
        forwardRelation.setSourceEntity(pkEntity);
        forwardRelation.setTargetEntity(fkEntity);
        pkEntity.addRelationship(forwardRelation);
        DbRelationship backwardRelation = new DbRelationship(fkEntity.getName() + '_' + fk.getName() + '_' + pkEntity.getName());
        backwardRelation.setToMany(false);
        backwardRelation.setSourceEntity(fkEntity);
        backwardRelation.setTargetEntity(pkEntity);
        fkEntity.addRelationship(backwardRelation);
        DbAttribute pkAtt = (DbAttribute)pkEntity.getAttribute(fk.getPkColumnName());
        DbAttribute fkAtt = (DbAttribute)fkEntity.getAttribute(fk.getColumnName());
        forwardRelation.addJoin(new DbAttributePair(pkAtt, fkAtt));
        backwardRelation.addJoin(new DbAttributePair(fkAtt, pkAtt));

        ObjRelationship objForwardRel = new ObjRelationship(forwardRelation.getName());
        objForwardRel.addDbRelationship(forwardRelation);
        if (forwardRelation.isToDependentPK()) objForwardRel.setDeleteRule(DeleteRule.DENY);
        objForwardRel.setToMany(forwardRelation.isToMany());
        objForwardRel.setSourceEntity(pkObjEntity);
        objForwardRel.setTargetEntity(fkObjEntity);
        pkObjEntity.addRelationship(objForwardRel);
        ObjRelationship objBackwardRel = new ObjRelationship(backwardRelation.getName());
        objBackwardRel.addDbRelationship(backwardRelation);
        objBackwardRel.setToMany(backwardRelation.isToMany());
        objBackwardRel.setSourceEntity(fkObjEntity);
        objBackwardRel.setTargetEntity(pkObjEntity);
        fkObjEntity.addRelationship(objBackwardRel);
        //return;
      }
    }
  }

  private void flattenRelationship(Table master1, Table master2, Table flattenedTable, DbEntity flattenedEntity) {
      ObjEntity pk1ObjEntity = (ObjEntity)objEntitiesByTable.get(master1);
      ObjEntity pk2ObjEntity = (ObjEntity)objEntitiesByTable.get(master2);
      DbEntity pk1Entity = pk1ObjEntity.getDbEntity();
      DbEntity pk2Entity = pk2ObjEntity.getDbEntity();
      Collection foreignKeys = flattenedTable.getForeignKeys();
      ForeignKey fk = null;

      //master1 <-> flattened
      for (Iterator i = foreignKeys.iterator(); i.hasNext();) {
          fk = (ForeignKey)i.next();
          if (fk.getPkTableName().equals(master1.getName())) break;
      }
      DbRelationship firstRelation = new DbRelationship(pk1Entity.getName() + '_' + flattenedEntity.getName() + '_' + fk.getName());
      firstRelation.setToMany(true);
      firstRelation.setToDependentPK(true);
      firstRelation.setSourceEntity(pk1Entity);
      firstRelation.setTargetEntity(flattenedEntity);
      pk1Entity.addRelationship(firstRelation);
      DbRelationship firstBackwardRelation = new DbRelationship(flattenedEntity.getName() + '_' + fk.getName() + '_' + pk1Entity.getName());
      firstBackwardRelation.setToMany(false);
      firstBackwardRelation.setSourceEntity(flattenedEntity);
      firstBackwardRelation.setTargetEntity(pk1Entity);
      flattenedEntity.addRelationship(firstBackwardRelation);
      DbAttribute pkAtt = (DbAttribute)pk1Entity.getAttribute(fk.getPkColumnName());
      DbAttribute fkAtt = (DbAttribute)flattenedEntity.getAttribute(fk.getColumnName());
      firstRelation.addJoin(new DbAttributePair(pkAtt, fkAtt));
      firstBackwardRelation.addJoin(new DbAttributePair(fkAtt, pkAtt));

      //master2 <-> flattened
      for (Iterator i = foreignKeys.iterator(); i.hasNext();) {
          fk = (ForeignKey)i.next();
          if (fk.getPkTableName().equals(master2.getName())) break;
      }
      DbRelationship secondRelation = new DbRelationship(pk2Entity.getName() + '_' + flattenedEntity.getName() + '_' + fk.getName());
      secondRelation.setToMany(true);
      secondRelation.setToDependentPK(true);
      secondRelation.setSourceEntity(pk2Entity);
      secondRelation.setTargetEntity(flattenedEntity);
      pk2Entity.addRelationship(secondRelation);
      DbRelationship secondBackwardRelation = new DbRelationship(flattenedEntity.getName() + '_' + fk.getName() + '_' + pk2Entity.getName());
      secondBackwardRelation.setToMany(false);
      secondBackwardRelation.setSourceEntity(flattenedEntity);
      secondBackwardRelation.setTargetEntity(pk2Entity);
      flattenedEntity.addRelationship(secondBackwardRelation);
      pkAtt = (DbAttribute)pk2Entity.getAttribute(fk.getPkColumnName());
      fkAtt = (DbAttribute)flattenedEntity.getAttribute(fk.getColumnName());
      secondRelation.addJoin(new DbAttributePair(pkAtt, fkAtt));
      secondBackwardRelation.addJoin(new DbAttributePair(fkAtt, pkAtt));

      //master1 <-> master2
      ObjRelationship objForwardRel = new ObjRelationship(firstRelation.getName() + '_' + secondBackwardRelation.getName());
      objForwardRel.setToMany(true);
      objForwardRel.addDbRelationship(firstRelation);
      objForwardRel.addDbRelationship(secondBackwardRelation);
      objForwardRel.setSourceEntity(pk1ObjEntity);
      objForwardRel.setTargetEntity(pk2ObjEntity);
      pk1ObjEntity.addRelationship(objForwardRel);

      ObjRelationship objBackwardRel = new ObjRelationship(secondRelation.getName() + '_' + firstBackwardRelation.getName());
      objBackwardRel.setToMany(true);
      objBackwardRel.addDbRelationship(secondRelation);
      objBackwardRel.addDbRelationship(firstBackwardRelation);
      objBackwardRel.setSourceEntity(pk2ObjEntity);
      objBackwardRel.setTargetEntity(pk1ObjEntity);
      pk2ObjEntity.addRelationship(objBackwardRel);
  }

  private ObjEntity createObjEntity(DbEntity dbEntity) {
    ObjEntity objEntity = new ObjEntity(NameConverter.undescoredToJava(dbEntity.getName(), true));
    objEntity.setDbEntity(dbEntity);
    objEntity.setClassName(objEntity.getName());
    Iterator colIt = dbEntity.getAttributeMap().values().iterator();
    while (colIt.hasNext()) {
      DbAttribute dbAtt = (DbAttribute) colIt.next();
      if (dbAtt.isPrimaryKey()) continue;
      String attName = NameConverter.undescoredToJava(dbAtt.getName(), false);
      String type = TypesMapping.getJavaBySqlType(dbAtt.getType());
      ObjAttribute objAtt = new ObjAttribute(attName, type, objEntity);
      objAtt.setDbAttribute(dbAtt);
      objEntity.addAttribute(objAtt);
    }
    return objEntity;
  }
  public RandomSchema getRandomSchema() {
    return randomSchema;
  }
  public DataDomain getDomain() {
    return domain;
  }
  public boolean isSchemaGenerated() {
    return schemaGenerated;
  }
}
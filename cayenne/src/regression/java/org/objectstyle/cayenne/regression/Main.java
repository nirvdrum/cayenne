package org.objectstyle.cayenne.regression;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.Random;

import org.apache.log4j.Level;
import org.objectstyle.ashwood.dbutil.RandomSchema;
import org.objectstyle.cayenne.access.DataContext;
import org.objectstyle.cayenne.access.DataDomain;
import org.objectstyle.cayenne.conf.Configuration;
import org.objectstyle.cayenne.conf.DefaultConfiguration;

/**
 * Main configures and runs regression tests defined in RandomDomainBuilder and
 * DataModificationRobot. It is responsible for performance metering as well.
 *
 * @author Andriy Shapochka
 */

public class Main {
  public static void main(String[] args) {
    //QueryLogger.setLoggingLevel(Level.ALL);
    System.out.println("max memory, MB: " + Runtime.getRuntime().maxMemory()/(1024*1024));
    System.out.println("total memory, MB: " + Runtime.getRuntime().totalMemory()/(1024*1024));
    System.out.println("free memory, MB: " + Runtime.getRuntime().freeMemory()/(1024*1024));

    Preferences prefs;
    try {
      prefs = new Preferences(args);
    } catch (FileNotFoundException ex) {
      System.out.println("Fatal Error: " + ex.getMessage());
      System.out.println("Good-bye.");
      return;
    }
    FileWriter fileOut = null;
    PrintWriter out = null;
    PrintWriter console = new PrintWriter(System.out, true);
    try {
      if (prefs.getOutFile() != null && !prefs.getOutFile().isDirectory()) {
        fileOut = new FileWriter(prefs.getOutFile());
        out = new PrintWriter(fileOut);
      }
      printHeader(console);
      if (out != null) printHeader(out);
      printPrefs(console, prefs);
      if (out != null) printPrefs(out, prefs);
      ClassLoader loader = new DOStubClassLoader();
      Configuration.bootstrapSharedConfig(loader.loadClass("Table"));
      Configuration conf = new DefaultConfiguration(prefs.getCayenneProject());
      conf.init();
      DataDomain domain = conf.getDomain();
      RandomDomainBuilder domainBuilder = new RandomDomainBuilder(domain);
      RandomSchema rndSchema = domainBuilder.getRandomSchema();
      rndSchema.setSchemaName(prefs.getSchema());
      rndSchema.setTableCount(prefs.getTableCount());
      rndSchema.setMaxReferencesPerTable(prefs.getMaxReferencesPerTable());
      rndSchema.setMaxForeignKeysPerTable(prefs.getMaxForeignKeysPerTable());
      rndSchema.setLoopCount(prefs.getLoopCount());
      rndSchema.setMaxLoopsPerTable(prefs.getMaxLoopsPerTable());
      Random randomizer = new Random(prefs.getSeed());
      rndSchema.setRandomizer(randomizer);
      File workDir = prefs.getWorkDirectory();
      String dirPrefix = prefs.getSchemaDirPrefix() + "-";
      for (int i = 1; i <= prefs.getSchemaCount(); i++) {
        try {
          File schemaDir = new File(workDir, dirPrefix + System.currentTimeMillis());
          schemaDir.mkdirs();
          printSchemaStart(console, i, schemaDir);
          if (out != null) printSchemaStart(out, i, schemaDir);
          domainBuilder.generate(schemaDir);
          DataContext ctxt = domain.createDataContext();
          for (int j = 1; j <= prefs.getCommitsPerSchema(); j++) {
            printCommitStart(console, j);
            if (out != null) printCommitStart(out, j);
            long freeMem = Runtime.getRuntime().freeMemory();
            console.println("free memory before gc, MB: " + freeMem/(1024*1024));
            do System.gc();
            while (freeMem > Runtime.getRuntime().freeMemory());
            freeMem = Runtime.getRuntime().freeMemory();
            console.println("free memory after gc, MB: " + freeMem/(1024*1024));
            if (freeMem/(1024*1024) < 5) {
              console.println("Out of memory!");
              return;
            }
            DataModificationRobot robot = new DataModificationRobot(ctxt, randomizer, prefs.getNewObjectPerTableCount(), prefs.getDeleteObjectPerTableCount());
            robot.generate();
            try {
              long start = System.currentTimeMillis();
              ctxt.commitChanges(null);
              //ctxt.commitChanges(Level.ALL);
              long end = System.currentTimeMillis();
              printCommitSuccess(console, j, end - start);
              if (out != null) printCommitSuccess(out, j, end - start);
            }
            catch (Exception ex) {
              printCommitFailure(console, j, ex);
              if (out != null) printCommitFailure(out, j, ex);
            }
          }
          printSchemaSuccess(console, i);
          if (out != null) printSchemaSuccess(out, i);
        } catch (Exception ex) {
          printSchemaFailure(console, i, ex);
          if (out != null) printSchemaFailure(out, i, ex);
        } finally {
          domainBuilder.drop();
        }
      }
    } catch (Exception ex) {
      printFailure(console, ex);
      if (out != null) printFailure(out, ex);
    } finally {
      printFooter(console);
      if (out != null) printFooter(out);
      console.flush();
      try {out.close();} catch (Exception ex) {}
      try {fileOut.close();} catch (Exception ex) {}
    }
  }

  static void printHeader(PrintWriter out) {
    out.println("Test starting!");
  }

  static void printFooter(PrintWriter out) {
    out.println("Test finished!");
    out.println("Good-bye.");
  }

  static void printPrefs(PrintWriter out, Preferences prefs) {
    out.println("Loaded preferences - ");
    prefs.print(out);
  }

  static void printSchemaStart(PrintWriter out, int schemaIndex, File schemaDir) {
    out.println();
    out.println("Schema " + schemaIndex + " generating.");
    out.println("schema recording in " + schemaDir);
  }

  static void printCommitStart(PrintWriter out, int commitIndex) {
    out.println("  Commit " + commitIndex + " starting.");
  }

  static void printCommitSuccess(PrintWriter out, int commitIndex, long ms) {
    out.println("  Commit " + commitIndex + " succeeded. Time=" + ms + " ms");
  }

  static void printCommitFailure(PrintWriter out, int commitIndex, Exception e) {
    out.println("  Commit " + commitIndex + " failed.");
    e.printStackTrace(out);
    out.println();
  }

  static void printSchemaSuccess(PrintWriter out, int schemaIndex) {
    out.println("Schema " + schemaIndex + " succeeded.");
  }

  static void printSchemaFailure(PrintWriter out, int schemaIndex, Exception e) {
    out.println("Schema " + schemaIndex + " failed.");
    e.printStackTrace(out);
    out.println();
  }

  static void printFailure(PrintWriter out, Exception e) {
    out.println("Fatal Error: ");
    e.printStackTrace(out);
    out.println();
  }
}
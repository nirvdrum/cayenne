package org.objectstyle.cayenne.pref;

import java.io.File;
import java.io.IOException;

import javax.sql.DataSource;

import org.apache.log4j.Level;
import org.objectstyle.cayenne.CayenneRuntimeException;
import org.objectstyle.cayenne.access.QueryLogger;
import org.objectstyle.cayenne.access.util.ConnectionEventLogger;
import org.objectstyle.cayenne.conf.Configuration;
import org.objectstyle.cayenne.conf.DataSourceFactory;
import org.objectstyle.cayenne.conf.DefaultConfiguration;
import org.objectstyle.cayenne.conn.PoolManager;
import org.objectstyle.cayenne.query.SQLTemplate;
import org.objectstyle.cayenne.util.Util;

/**
 * An implementation of preference service that stores the data using embedded HSQL DB
 * database with Cayenne.
 * 
 * @author Andrei Adamchik
 */
public class HSQLEmbeddedPreferenceService extends CayennePreferenceService {

    protected File dbDirectory;
    protected String baseName;
    protected String masterBaseName;
    protected String cayenneConfigPackage;

    /**
     * Creates a new PreferenceService that stores preferences using Cayenne and embedded
     * HSQLDB engine.
     * 
     * @param dbLocation path to an HSQL db.
     * @param cayenneConfigPackage a Java package that holds cayenne.xml for preferences
     *            access (can be null)
     * @param defaultDomain root domain name for this service.
     */
    public HSQLEmbeddedPreferenceService(String dbLocation, String cayenneConfigPackage,
            String defaultDomain) {
        super(defaultDomain);
        if (dbLocation == null) {
            throw new PreferenceException("Null DB location.");
        }

        File file = new File(dbLocation);

        this.dbDirectory = file.getParentFile();
        this.masterBaseName = file.getName();
        this.cayenneConfigPackage = cayenneConfigPackage;
    }

    /**
     * If true, this service updates a secondary HSQL instance that may need
     * synchronization with master.
     */
    public boolean isSecondaryDB() {
        return !Util.nullSafeEquals(masterBaseName, baseName);
    }

    public File getMasterLock() {
        return new File(dbDirectory, masterBaseName + ".lck");
    }

    /**
     * Creates a separate Cayenne stack used to work with preferences database only, so
     * that any other use of Cayenne in the app is not affected.
     */
    public void startService() {
        // use custom DataSourceFactory to prepare the DB...
        HSQLDataSourceFactory dataSourceFactory = new HSQLDataSourceFactory();

        DefaultConfiguration config = new DefaultConfiguration();
        config.setDataSourceFactory(dataSourceFactory);

        if (cayenneConfigPackage != null) {
            config.addClassPath(cayenneConfigPackage);
        }

        try {
            config.initialize();
        }
        catch (Exception ex) {
            throw new CayenneRuntimeException("Error connecting to preference DB.", ex);
        }

        config.didInitialize();
        dataContext = config.getDomain().createDataContext();

        // create DB if it does not exist...
        if (dataSourceFactory.needSchemaUpdate) {
            initSchema();
        }

        // bootstrap our own preferences...
        initPreferences();

        // start save timer...
        startTimer();
    }

    public void stopService() {

        if (saveTimer != null) {
            saveTimer.cancel();
        }

        if (dataContext != null) {

            // flush changes...
            savePreferences();

            // shutdown HSQL
            dataContext.performNonSelectingQuery(new SQLTemplate(
                    Domain.class,
                    "SHUTDOWN",
                    false));

            // shutdown Cayenne
            dataContext.getParentDataDomain().shutdown();
        }

        // attempt to sync primary DB...
        if (isSecondaryDB()) {
            File lock = getMasterLock();
            if (!lock.exists()) {

                // TODO: according to JavaDoc this is not reliable enough...
                // Investigate HSQL API for a better solution.
                try {
                    if (lock.createNewFile()) {
                        try {
                            moveDB(baseName, masterBaseName);
                        }
                        finally {
                            lock.delete();
                        }
                    }
                }
                catch (Throwable th) {
                    throw new PreferenceException(
                            "Error shutting down database. Preferences may be in invalid state.");
                }
            }
        }
    }

    /**
     * Copies one database to another. Caller must provide HSQLDB locks on target for this
     * to work reliably.
     */
    void moveDB(String masterBaseName, String targetBaseName) throws IOException {

        // move log
        File logSrc = new File(dbDirectory, masterBaseName + ".log");
        File logTarget = new File(dbDirectory, targetBaseName + ".log");
        if (logSrc.exists()) {
            logSrc.renameTo(logTarget);
        }
        else {
            logTarget.delete();
        }

        // move script
        File scriptSrc = new File(dbDirectory, masterBaseName + ".script");
        File scriptTarget = new File(dbDirectory, targetBaseName + ".script");
        if (scriptSrc.exists()) {
            scriptSrc.renameTo(scriptTarget);
        }
        else {
            scriptTarget.delete();
        }

        // move properties
        File propertiesSrc = new File(dbDirectory, masterBaseName + ".properties");
        File propertiesTarget = new File(dbDirectory, targetBaseName + ".properties");
        if (propertiesSrc.exists()) {
            propertiesSrc.renameTo(propertiesTarget);
        }
        else {
            propertiesTarget.delete();
        }
    }

    /**
     * Copies one database to another. Caller must provide HSQLDB locks for this to work
     * reliably.
     */
    void copyDB(String masterBaseName, String targetBaseName) throws IOException {

        // copy log
        File logSrc = new File(dbDirectory, masterBaseName + ".log");
        File logTarget = new File(dbDirectory, targetBaseName + ".log");
        if (logSrc.exists()) {
            Util.copy(logSrc, logTarget);
        }
        else {
            logTarget.delete();
        }

        // copy script
        File scriptSrc = new File(dbDirectory, masterBaseName + ".script");
        File scriptTarget = new File(dbDirectory, targetBaseName + ".script");
        if (logSrc.exists()) {
            Util.copy(scriptSrc, scriptTarget);
        }
        else {
            scriptTarget.delete();
        }

        // copy properties
        File propertiesSrc = new File(dbDirectory, masterBaseName + ".properties");
        File propertiesTarget = new File(dbDirectory, targetBaseName + ".properties");
        if (logSrc.exists()) {
            Util.copy(propertiesSrc, propertiesTarget);
        }
        else {
            propertiesTarget.delete();
        }
    }

    // addresses various issues with embedded database...
    final class HSQLDataSourceFactory implements DataSourceFactory {

        boolean needSchemaUpdate;
        String url;

        void prepareDB() throws IOException {

            // try master DB
            if (checkMainDB(masterBaseName)) {
                return;
            }

            // try last active DB
            if (baseName != null && checkMainDB(baseName)) {
                return;
            }

            // file locked... need to switch to a secondary DB
            // arbitrary big but finite number of attempts...
            for (int i = 1; i < 200; i++) {
                String name = masterBaseName + i;
                File lock = new File(dbDirectory, name + ".lck");
                if (!lock.exists()) {

                    // TODO: according to JavaDoc this is not reliable enough...
                    // Investigate HSQL API for a better solution.
                    if (!lock.createNewFile()) {
                        continue;
                    }

                    try {
                        copyDB(masterBaseName, name);
                    }
                    finally {
                        lock.delete();
                    }

                    needSchemaUpdate = false;
                    url = "jdbc:hsqldb:file:"
                            + Util.substBackslashes(new File(dbDirectory, name)
                                    .getAbsolutePath());
                    baseName = name;
                    return;
                }
            }

            throw new IOException("Can't create preferences DB");
        }

        boolean checkMainDB(String sessionBaseName) {
            File dbFile = new File(dbDirectory, sessionBaseName + ".properties");

            // no db file exists
            if (!dbFile.exists()) {
                needSchemaUpdate = true;
                url = "jdbc:hsqldb:file:"
                        + Util.substBackslashes(new File(dbDirectory, sessionBaseName)
                                .getAbsolutePath());
                baseName = sessionBaseName;
                return true;
            }

            // no lock exists... continue...
            File lockFile = new File(dbDirectory, sessionBaseName + ".lck");
            if (!lockFile.exists()) {
                needSchemaUpdate = false;
                url = "jdbc:hsqldb:file:"
                        + Util.substBackslashes(new File(dbDirectory, sessionBaseName)
                                .getAbsolutePath());
                baseName = sessionBaseName;
                return true;
            }

            return false;
        }

        public DataSource getDataSource(String location, Level logLevel) throws Exception {
            try {
                prepareDB();

                PoolManager pm = new PoolManager(
                        org.hsqldb.jdbcDriver.class.getName(),
                        url,
                        1,
                        1,
                        "sa",
                        null,
                        new ConnectionEventLogger(logLevel));

                return pm;
            }
            catch (Throwable th) {
                QueryLogger.logConnectFailure(logLevel, th);
                throw new PreferenceException("Error connecting to DB", th);
            }
        }

        public DataSource getDataSource(String location) throws Exception {
            return getDataSource(location, Level.INFO);
        }

        public void initializeWithParentConfiguration(Configuration conf) {
        }
    }

}
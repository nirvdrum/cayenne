package webtest;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.objectstyle.cayenne.conf.ServletConfiguration;

/** 
 * Special subclass of ServletConfiguration that enables 
 * logging of Cayenne queries and can also perform some 
 * custom tasks on servlet container startup.
 */
public class CustomConfiguration extends ServletConfiguration {
    private static Logger logObj = Logger.getLogger(CustomConfiguration.class);

    public CustomConfiguration() {
        super();
        configureLogging();
    }


    private void configureLogging() {
        // debug configuration
        setLoggingLevel(Level.WARN);
    }
}
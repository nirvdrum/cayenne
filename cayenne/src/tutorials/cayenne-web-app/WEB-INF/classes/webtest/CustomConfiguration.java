package webtest;

import org.apache.log4j.Level;
import org.objectstyle.cayenne.conf.ServletConfiguration;

/** 
 * Special subclass of ServletConfiguration that enables 
 * logging of Cayenne queries and can also perform some 
 * custom tasks on servlet container startup.
 */
public class CustomConfiguration extends ServletConfiguration {

    public CustomConfiguration() {
        super();
        this.configureLogging();
    }

    private void configureLogging() {
        // debug configuration
        setLoggingLevel(Level.WARN);
    }
}
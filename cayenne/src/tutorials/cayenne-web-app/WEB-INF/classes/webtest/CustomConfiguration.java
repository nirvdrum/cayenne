package webtest;

import org.apache.log4j.*;
import org.objectstyle.cayenne.conf.*;

/** 
 * Special subclass of ServletConfiguration that enables 
 * logging of Cayenne queries and can also perform some 
 * custom tasks on servlet container startup.
 */
public class CustomConfiguration extends ServletConfiguration {
    static Logger logObj = Logger.getLogger(CustomConfiguration.class);

    public CustomConfiguration() {
        super();
        configureLogging();
    }


    private void configureLogging() {
        // debug configuration
        setLoggingLevel(Level.WARN);
    }
}
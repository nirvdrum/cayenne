package test;

import java.util.logging.*;
import org.objectstyle.cayenne.conf.*;

/** 
 * Special subclass of ServletConfiguration that enables 
 * logging of Cayenne queries and can also perform some 
 * custom tasks on servlet container startup.
 */
public class CustomConfiguration extends ServletConfiguration {
    static Logger logObj = Logger.getLogger(CustomConfiguration.class.getName());

    public CustomConfiguration() {
        super();
        configureLogging();
    }


    private void configureLogging() {
        // debug configuration
        setLogLevel(Level.WARNING);
    }
}
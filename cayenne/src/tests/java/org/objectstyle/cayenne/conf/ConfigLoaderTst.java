package org.objectstyle.cayenne.conf;

import java.util.Iterator;

import org.apache.log4j.Logger;
import org.objectstyle.cayenne.unittest.CayenneTestCase;

/**
 * @author Andrei Adamchik
 */
public class ConfigLoaderTst extends CayenneTestCase {
    private static Logger logObj = Logger.getLogger(ConfigLoaderTst.class);

    /**
     * Constructor for ConfigLoaderTst.
     * @param name
     */
    public ConfigLoaderTst(String name) {
        super(name);
    }

    public void testLoadDomains() throws Exception {
        Iterator it = new ConfigLoaderSimpleSuite().getCases().iterator();
        while (it.hasNext()) {
            ConfigLoaderCase aCase = (ConfigLoaderCase) it.next();
            logObj.debug("Starting Case: " + aCase);
            ConfigLoader helper =
                new ConfigLoader(new EmptyConfiguration().getLoaderDelegate());
            aCase.test(helper);
        }
    }

}

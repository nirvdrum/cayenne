package org.objectstyle.cayenne.conf;

import org.objectstyle.cayenne.unittest.CayenneTestCase;

/**
 * @author Andrei Adamchik
 */
public class ConfigLoaderTst extends CayenneTestCase {

    /**
     * Constructor for ConfigLoaderTst.
     * @param name
     */
    public ConfigLoaderTst(String name) {
        super(name);
    }
    
    public void testLoadDomains() throws Exception {
        ConfigLoader helper = new ConfigLoader(new EmptyConfiguration());
        new ConfigLoaderSimpleSuite().test(helper);
    }

}

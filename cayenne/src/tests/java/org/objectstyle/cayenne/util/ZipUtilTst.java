package org.objectstyle.cayenne.util;

import java.io.File;
import java.util.zip.ZipFile;

import org.objectstyle.cayenne.unittest.CayenneTestCase;

/**
 * @author Andrei Adamchik
 */
public class ZipUtilTst extends CayenneTestCase {

    /**
     * Constructor for ZipUtilTest.
     * @param name
     */
    public ZipUtilTst(String name) {
        super(name);
    }

    public void testUnzip() throws Exception {
        File jar = new File(getTestResourceDir(), "jar-test.jar");
        File unjarDir = getTestDir();
        File unjarRootDir = new File(unjarDir, "jar-test");
        File manifest =
            new File(
                unjarRootDir.getParentFile(),
                "META-INF" + File.separator + "MANIFEST.MF");
        assertTrue(!unjarRootDir.exists());
        assertTrue(!manifest.exists());

        try {
            // try unzipping the JAR
            ZipUtil.unzip(jar, unjarDir);

            assertTrue(unjarRootDir.isDirectory());
            assertTrue(new File(unjarRootDir, "jar-test1.txt").length() > 0);
            assertTrue(new File(unjarRootDir, "jar-test2.txt").length() > 0);
            assertTrue(manifest.isFile());
        } finally {
            Util.delete(unjarRootDir.getPath(), true);
            Util.delete(new File(unjarDir, "META-INF").getPath(), true);
        }
    }

    public void testZip() throws Exception {
        File jar = new File(getTestResourceDir(), "jar-test.jar");
        File unjarDir = getTestDir();
        File unjarRootDir = new File(unjarDir, "jar-test");
        File newJarFile = new File(unjarDir, "new-jar.jar");

        try {
            // unzip existing jar and recreate
            assertTrue(!unjarRootDir.exists());
            ZipUtil.unzip(jar, unjarDir);

            ZipUtil.zip(
                newJarFile,
                unjarDir,
                new File[] { unjarRootDir, new File(unjarDir, "META-INF")},
                '/');

            assertTrue(newJarFile.isFile());

            // can't compare length, since different algorithms may have been used
            // assertEquals(jar.length(), newJarFile.length());

            // try unzipping it again
            Util.delete(unjarRootDir.getPath(), true);
            Util.delete(new File(unjarDir, "META-INF").getPath(), true);
            ZipUtil.unzip(newJarFile, unjarDir);
            
        } finally {
            Util.delete(unjarRootDir.getPath(), true);
            Util.delete(new File(unjarDir, "META-INF").getPath(), true);
            newJarFile.delete();
        }
    }
}

package org.objectstyle.cayenne.modeler.action;

import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.io.File;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;

import org.apache.log4j.Logger;
import org.objectstyle.cayenne.access.DataDomain;
import org.objectstyle.cayenne.map.DataMap;
import org.objectstyle.cayenne.map.DataMapException;
import org.objectstyle.cayenne.map.MapLoader;
import org.objectstyle.cayenne.modeler.Application;
import org.objectstyle.cayenne.modeler.ModelerPreferences;
import org.objectstyle.cayenne.modeler.swing.CayenneAction;
import org.objectstyle.cayenne.modeler.util.FileFilters;
import org.objectstyle.cayenne.project.NamedObjectFactory;
import org.objectstyle.cayenne.util.ResourceLocator;

/**
 * Modeler action that imports a DataMap into a project from an 
 * arbitrary location.
 * 
 * @author Andrei Adamchik
 * @since 1.1
 */
public class ImportDataMapAction extends CayenneAction {
    private static Logger logObj = Logger.getLogger(ImportDataMapAction.class);

    public static String getActionName() {
        return "Import DataMap";
    }

    public ImportDataMapAction(Application application) {
        super(getActionName(), application);
    }

    public void performAction(ActionEvent e) {
        importDataMap();
    }

    protected void importDataMap() {
        File dataMapFile = selectDataMap(Application.getFrame());
        if (dataMapFile == null) {
            return;
        }

        try {
            // configure resource locator to take absolute path
            MapLoader mapLoader = new MapLoader() {
                protected ResourceLocator configLocator() {
                    ResourceLocator locator = new ResourceLocator();
                    locator.setSkipAbsolutePath(false);
                    locator.setSkipClasspath(true);
                    locator.setSkipCurrentDirectory(true);
                    locator.setSkipHomeDirectory(true);
                    return locator;
                }
            };

            DataMap newMap = mapLoader.loadDataMap(dataMapFile.getAbsolutePath());
            DataDomain domain = getProjectController().getCurrentDataDomain();

            if (newMap.getName() != null) {
                newMap.setName(
                    NamedObjectFactory.createName(
                        DataMap.class,
                        domain,
                        newMap.getName()));
            }
            else {
                newMap.setName(NamedObjectFactory.createName(DataMap.class, domain));
            }

            getProjectController().addDataMap(this, newMap);
        }
        catch (DataMapException ex) {
            logObj.info("Error importing DataMap.", ex);
            JOptionPane.showMessageDialog(
                Application.getFrame(),
                "Error reading DataMap: " + ex.getMessage(),
                "Can't Open DataMap",
                JOptionPane.OK_OPTION);
        }
    }

    protected File selectDataMap(Frame f) {
        ModelerPreferences pref = ModelerPreferences.getPreferences();
        String startDirStr = (String) pref.getProperty(ModelerPreferences.LAST_DIR);
        File startDir = null;
        if (startDirStr != null) {
            startDir = new File(startDirStr);
        }

        // configure dialog
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        if (startDir != null) {
            chooser.setCurrentDirectory(startDir);
        }

        chooser.addChoosableFileFilter(FileFilters.getDataMapFilter());

        int status = chooser.showDialog(f, "Select DataMap");
        return (status == JFileChooser.APPROVE_OPTION) ? chooser.getSelectedFile() : null;
    }
}

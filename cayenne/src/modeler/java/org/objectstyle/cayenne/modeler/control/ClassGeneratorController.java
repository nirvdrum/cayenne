package org.objectstyle.cayenne.modeler.control;

import java.awt.Component;
import java.io.File;
import java.util.List;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;

import org.objectstyle.cayenne.gen.DefaultClassGenerator;
import org.objectstyle.cayenne.map.DataMap;
import org.objectstyle.cayenne.map.ObjEntity;
import org.objectstyle.cayenne.modeler.ModelerPreferences;
import org.objectstyle.cayenne.modeler.model.ClassGeneratorModel;
import org.objectstyle.cayenne.modeler.view.ClassGeneratorDialog;
import org.objectstyle.cayenne.project.Project;
import org.objectstyle.cayenne.project.validator.Validator;
import org.scopemvc.controller.basic.BasicController;
import org.scopemvc.core.Control;
import org.scopemvc.core.ControlException;

/**
 * @author Andrei Adamchik
 */
public class ClassGeneratorController extends BasicController {
    public static final String CANCEL_CONTROL =
        "cayenne.modeler.classgenerator.cancel.button";

    public static final String GENERATE_CLASSES_CONTROL =
        "cayenne.modeler.classgenerator.generate.button";

    public static final String CHOOSE_LOCATION_CONTROL =
        "cayenne.modeler.classgenerator.choose.button";

    public ClassGeneratorController(Project project, DataMap map, ObjEntity selectedEntity) {
        setModel(prepareModel(project, map, selectedEntity));
    }

    protected Object prepareModel(
        Project project,
        DataMap map,
        ObjEntity selectedEntity) {
        	
        // validate entities
        Validator validator = project.getValidator();
        validator.validate();

        ClassGeneratorModel model =
            new ClassGeneratorModel(
                map,
                selectedEntity,
                validator.validationResults());

        // by default generate pairs of classes
        // this may come from preferences later
        boolean setPairs = true;
        
        model.setPairs(setPairs);
        if(setPairs) {
        	model.updateDefaultSuperClassPackage();
        }

        // figure out default out directory
        ModelerPreferences pref = ModelerPreferences.getPreferences();
        String startDir =
            (String) pref.getProperty(
                ModelerPreferences.LAST_GENERATED_CLASSES_DIR);

        if (startDir != null) {
            model.setOutputDir(startDir);
        }

        return model;
    }

    /**
      * @see org.scopemvc.controller.basic.BasicController#startup()
      */
    public void startup() {
        setView(new ClassGeneratorDialog());
        super.startup();
    }

    /**
     * @see org.scopemvc.controller.basic.BasicController#doHandleControl(Control)
     */
    protected void doHandleControl(Control control) throws ControlException {
        if (control.matchesID(CANCEL_CONTROL)) {
            shutdown();
        } else if (control.matchesID(GENERATE_CLASSES_CONTROL)) {
            generateClasses();
        } else if (control.matchesID(CHOOSE_LOCATION_CONTROL)) {
            chooseLocation();
        }
    }

    protected void generateClasses() {
        ClassGeneratorModel model = (ClassGeneratorModel) getModel();
        File outputDir = model.getOutputDirectory();

        // no destination folder
        if (outputDir == null) {
            JOptionPane.showMessageDialog(
                (Component) this.getView(),
                "Select directory for source files.");
            return;
        }

        // no such folder
        if (!outputDir.exists() && !outputDir.mkdirs()) {
            JOptionPane.showMessageDialog(
                (Component) this.getView(),
                "Can't create directory "
                    + outputDir
                    + ". Select a different one.");
            return;
        }

        // not a directory
        if (!outputDir.isDirectory()) {
            JOptionPane.showMessageDialog(
                (Component) this.getView(),
                outputDir + " is not a valid directory.");
            return;
        }

        List selected = model.getSelectedEntities();
        DefaultClassGenerator generator = new DefaultClassGenerator(selected);
        generator.setDestDir(outputDir);
        generator.setMakePairs(model.isPairs());
        generator.setSuperPkg(model.getSuperClassPackage());

        try {
            generator.execute();
            JOptionPane.showMessageDialog(
                (Component) this.getView(),
                "Class generation finished");
            shutdown();
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(
                (Component) this.getView(),
                "Error generating classes - " + e.getMessage());
        }
    }

    protected void chooseLocation() {
        ClassGeneratorModel model = (ClassGeneratorModel) getModel();
        File startDir = model.getOutputDirectory();

        // guess start directory
        if (startDir == null) {
            String lastUsed =
                (String) ModelerPreferences.getPreferences().getProperty(
                    ModelerPreferences.LAST_GENERATED_CLASSES_DIR);
            if (lastUsed != null) {
                startDir = new File(lastUsed);
            }
        }

        // guess again
        if (startDir == null) {
            String lastUsed =
                (String) ModelerPreferences.getPreferences().getProperty(
                    ModelerPreferences.LAST_DIR);
            if (lastUsed != null) {
                startDir = new File(lastUsed);
            }
        }

        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setDialogType(JFileChooser.OPEN_DIALOG);

        if (startDir != null) {
            chooser.setCurrentDirectory(startDir);
        }

        int result = chooser.showOpenDialog((Component) this.getView());
        if (result == JFileChooser.APPROVE_OPTION) {
            File selected = chooser.getSelectedFile();
            // Set preferences
            ModelerPreferences.getPreferences().setProperty(
                ModelerPreferences.LAST_GENERATED_CLASSES_DIR,
                selected.getAbsolutePath());

            // update model
            model.setOutputDir(selected.getAbsolutePath());
        }
    }
}

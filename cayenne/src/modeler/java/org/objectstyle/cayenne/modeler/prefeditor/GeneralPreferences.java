package org.objectstyle.cayenne.modeler.prefeditor;

import java.awt.Component;

import javax.swing.InputVerifier;
import javax.swing.JComponent;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import org.objectstyle.cayenne.modeler.swing.CayenneController;
import org.objectstyle.cayenne.pref.CayennePreferenceEditor;
import org.objectstyle.cayenne.pref.CayennePreferenceService;
import org.objectstyle.cayenne.pref.PreferenceEditor;

/**
 * @author Andrei Adamchik
 */
public class GeneralPreferences extends CayenneController {

    protected GeneralPreferencesView view;
    protected CayennePreferenceEditor editor;

    public GeneralPreferences(PreferenceDialog parentController) {
        super(parentController);
        this.view = new GeneralPreferencesView();

        PreferenceEditor editor = parentController.getEditor();
        if (editor instanceof CayennePreferenceEditor) {
            this.editor = (CayennePreferenceEditor) editor;
            this.view.setEnabled(true);
            this.view.getSaveInterval().setText(this.editor.getSaveInterval() + "");
            initBindings();
        }
        else {
            this.view.setEnabled(false);
        }
    }

    public Component getView() {
        return view;
    }

    protected void initBindings() {
        final JTextField textField = view.getSaveInterval();
        textField.setInputVerifier(new InputVerifier() {

            public boolean verify(JComponent component) {
                String text = textField.getText();
                boolean resetText = false;
                if (text.length() == 0) {
                    resetText = true;
                }
                else {
                    try {
                        int interval = Integer.parseInt(text);
                        if (interval < CayennePreferenceService.MIN_SAVE_INTERVAL) {
                            interval = CayennePreferenceService.MIN_SAVE_INTERVAL;
                            textField.setText("" + interval);
                        }

                        editor.setSaveInterval(interval);
                    }
                    catch (NumberFormatException ex) {
                        resetText = true;
                    }
                }

                if (resetText) {
                    Runnable setText = new Runnable() {

                        public void run() {
                            textField.setText("" + editor.getSaveInterval());
                        }
                    };
                    SwingUtilities.invokeLater(setText);
                }
                
                return true;
            }
        });
    }
}
package org.objectstyle.cayenne.modeler.prefeditor;

import java.awt.BorderLayout;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.FormLayout;

/**
 * @author Andrei Adamchik
 */
public class GeneralPreferencesView extends JPanel {

    protected JTextField saveInterval;
    protected JLabel saveIntervalLabel;

    public GeneralPreferencesView() {
        this.saveInterval = new JTextField();

        FormLayout layout = new FormLayout(
                "right:max(50dlu;pref), 3dlu, fill:max(50dlu;pref):grow",
                "");
        DefaultFormBuilder builder = new DefaultFormBuilder(layout);
        builder.setDefaultDialogBorder();

        builder.appendSeparator("General Preferences");
        saveIntervalLabel = builder.append("Preferences Save Interval (ms.):", saveInterval);

        this.setLayout(new BorderLayout());
        this.add(builder.getPanel(), BorderLayout.CENTER);
    }

    public void setEnabled(boolean b) {
        super.setEnabled(b);
        saveInterval.setEnabled(b);
        saveIntervalLabel.setEnabled(b);
    }

    public JTextField getSaveInterval() {
        return saveInterval;
    }
}
package org.objectstyle.cayenne.modeler.prefeditor;

import java.awt.BorderLayout;
import java.awt.Component;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.FormLayout;

/**
 * @author Andrei Adamchik
 */
public class DataSourceEditorView extends JPanel {

    protected JComboBox adapters;
    protected JTextField driver;
    protected JTextField url;
    protected JTextField userName;
    protected JPasswordField password;

    protected Collection labels;

    public DataSourceEditorView() {
        adapters = new JComboBox();
        adapters.setEditable(true);

        driver = new JTextField();
        url = new JTextField();
        userName = new JTextField();
        password = new JPasswordField();
        labels = new ArrayList();

        // assemble
        FormLayout layout = new FormLayout(
                "right:pref, 3dlu, fill:max(50dlu;pref):grow",
                "");
        DefaultFormBuilder builder = new DefaultFormBuilder(layout);
        builder.setDefaultDialogBorder();

        builder.appendSeparator("DataSource Information");
        labels.add(builder.append("Adapter:", adapters));
        labels.add(builder.append("JDBC Driver:", driver));
        labels.add(builder.append("DB URL:", url));
        labels.add(builder.append("User Name:", userName));
        labels.add(builder.append("Password:", password));

        this.setLayout(new BorderLayout());
        this.add(builder.getPanel(), BorderLayout.CENTER);
    }

    public JComboBox getAdapters() {
        return adapters;
    }

    public JTextField getDriver() {
        return driver;
    }

    public JPasswordField getPassword() {
        return password;
    }

    public JTextField getUrl() {
        return url;
    }

    public JTextField getUserName() {
        return userName;
    }

    public void setEnabled(boolean enabled) {
        if (isEnabled() != enabled) {
            super.setEnabled(enabled);
            Iterator it = labels.iterator();
            while (it.hasNext()) {
                Component c = (Component) it.next();
                c.setEnabled(enabled);
            }

            adapters.setEnabled(enabled);
            driver.setEnabled(enabled);
            url.setEnabled(enabled);
            userName.setEnabled(enabled);
            password.setEnabled(enabled);
        }
    }
}
package org.objectstyle.cayenne.modeler.prefeditor;

import java.awt.BorderLayout;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

/**
 * @author Andrei Adamchik
 */
public class DataSourcePreferencesView extends JPanel {

    protected JButton addDataSource;
    protected JButton removeDataSource;
    protected JButton testDataSource;
    protected JComboBox dataSources;

    protected DataSourceEditorView dataSourceEditor;

    public DataSourcePreferencesView() {
        this.addDataSource = new JButton("New DataSource");
        this.removeDataSource = new JButton("Delete DataSource");
        this.testDataSource = new JButton("Test...");
        this.dataSources = new JComboBox();
        this.dataSourceEditor = new DataSourceEditorView();

        this.dataSourceEditor.setEnabled(false);

        // assemble
        CellConstraints cc = new CellConstraints();
        PanelBuilder builder = new PanelBuilder(new FormLayout(
                "fill:min(150dlu;pref)",
                "p, 3dlu, p, 5dlu, p, 3dlu, p, 3dlu, p"));
        builder.setDefaultDialogBorder();

        builder.add(new JLabel("Select DataSource"), cc.xy(1, 1));
        builder.add(dataSources, cc.xy(1, 3));
        builder.add(addDataSource, cc.xy(1, 5));
        builder.add(removeDataSource, cc.xy(1, 7));
        builder.add(testDataSource, cc.xy(1, 9));

        setLayout(new BorderLayout());
        add(new JScrollPane(dataSourceEditor), BorderLayout.CENTER);
        add(builder.getPanel(), BorderLayout.EAST);
    }

    public DataSourceEditorView getDataSourceEditor() {
        return dataSourceEditor;
    }

    public JComboBox getDataSources() {
        return dataSources;
    }

    public JButton getAddDataSource() {
        return addDataSource;
    }

    public JButton getRemoveDataSource() {
        return removeDataSource;
    }

    public JButton getTestDataSource() {
        return testDataSource;
    }
}
package org.objectstyle.cayenne.modeler.prefeditor;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.List;

import javax.swing.JFileChooser;
import javax.swing.filechooser.FileFilter;
import javax.swing.table.AbstractTableModel;

import org.objectstyle.cayenne.modeler.ModelerClassLoader;
import org.objectstyle.cayenne.modeler.pref.FSPath;
import org.objectstyle.cayenne.modeler.swing.CayenneController;
import org.objectstyle.cayenne.modeler.util.FileFilters;
import org.objectstyle.cayenne.pref.Domain;
import org.objectstyle.cayenne.pref.PreferenceDetail;
import org.objectstyle.cayenne.pref.PreferenceEditor;

/**
 * @author Andrei Adamchik
 */
public class ClasspathPreferences extends CayenneController {

    protected ClasspathPreferencesView view;
    protected PreferenceEditor editor;
    protected List classPathEntries;
    protected ClasspathTableModel tableModel;

    public ClasspathPreferences(PreferenceDialog parentController) {
        super(parentController);

        this.editor = parentController.getEditor();
        this.view = new ClasspathPreferencesView();
        this.classPathEntries = getClassLoaderDomain().getPreferenceDetails();
        this.tableModel = new ClasspathTableModel();

        initBindings();
    }

    public Component getView() {
        return view;
    }

    protected Domain getViewDomain() {
        return getApplication().getApplicationPreferences().getSubdomain(
                ClasspathPreferencesView.class);
    }

    protected Domain getClassLoaderDomain() {
        return editor
                .editableInstance(getApplication().getApplicationPreferences())
                .getSubdomain(ModelerClassLoader.class);
    }

    protected void initBindings() {
        view.getTable().setModel(tableModel);

        view.getAddDirButton().addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                addClassDirectoryAction();
            }
        });

        view.getRemoveEntryButton().addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                removeEntryAction();
            }
        });

        view.getAddJarButton().addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                addJarOrZipAction();
            }
        });
    }

    protected void addJarOrZipAction() {
        chooseClassEntry(
                FileFilters.getClassArchiveFilter(),
                "Select JAR or ZIP File.",
                JFileChooser.FILES_ONLY);
    }

    protected void addClassDirectoryAction() {
        chooseClassEntry(
                null,
                "Select Java Class Directory.",
                JFileChooser.DIRECTORIES_ONLY);
    }

    protected void removeEntryAction() {
        int selected = view.getTable().getSelectedRow();
        if (selected < 0) {
            return;
        }

        PreferenceDetail selection = (PreferenceDetail) classPathEntries.remove(selected);
        editor.deleteDetail(getClassLoaderDomain(), selection.getKey());
        tableModel.fireTableRowsDeleted(selected, selected);
    }

    protected void chooseClassEntry(FileFilter filter, String title, int selectionMode) {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(selectionMode);
        chooser.setDialogType(JFileChooser.OPEN_DIALOG);
        chooser.setAcceptAllFileFilterUsed(true);

        FSPath lastDir = FSPath.getPreference(getViewDomain());
        File startDir = lastDir.getExistingDirectory(false);

        if (startDir != null) {
            chooser.setCurrentDirectory(startDir);
        }

        if (filter != null) {
            chooser.addChoosableFileFilter(filter);
        }

        chooser.setDialogTitle(title);

        File selected = null;
        int result = chooser.showOpenDialog(view);
        if (result == JFileChooser.APPROVE_OPTION) {
            selected = chooser.getSelectedFile();
        }

        if (selected != null) {
            // store last dir in preferences
            lastDir.setPath(selected);

            int len = classPathEntries.size();
            String key = selected.getAbsolutePath();
            classPathEntries.add(editor.createDetail(getClassLoaderDomain(), key));
            tableModel.fireTableRowsInserted(len, len);
        }
    }

    class ClasspathTableModel extends AbstractTableModel {

        public int getColumnCount() {
            return 1;
        }

        public int getRowCount() {
            return classPathEntries.size();
        }

        public Object getValueAt(int rowIndex, int columnIndex) {
            PreferenceDetail preference = (PreferenceDetail) classPathEntries
                    .get(rowIndex);
            return preference.getKey();
        }

        public String getColumnName(int column) {
            return "Custom ClassPath";
        }
    }

}
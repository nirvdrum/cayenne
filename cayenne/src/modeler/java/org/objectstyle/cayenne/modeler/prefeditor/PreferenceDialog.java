package org.objectstyle.cayenne.modeler.prefeditor;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JDialog;
import javax.swing.JList;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.objectstyle.cayenne.modeler.pref.ComponentGeometry;
import org.objectstyle.cayenne.modeler.swing.CayenneController;
import org.objectstyle.cayenne.pref.Domain;
import org.objectstyle.cayenne.pref.PreferenceEditor;
import org.objectstyle.cayenne.pref.PreferenceException;

/**
 * A controller for editing Modeler preferences.
 * 
 * @author Andrei Adamchik
 */
public class PreferenceDialog extends CayenneController {

    private static final String[] preferenceMenus = new String[] {
            "General", "DataSource", "ClassPath"
    };

    protected PreferenceDialogView view;
    protected Map detailControllers;
    protected PreferenceEditor editor;

    public PreferenceDialog(CayenneController parent) {
        super(parent);
        this.view = new PreferenceDialogView();
        this.detailControllers = new HashMap();

        initBindings();
    }

    protected void initBindings() {
        final JList list = view.getList();
        list.setListData(preferenceMenus);
        list.addListSelectionListener(new ListSelectionListener() {

            public void valueChanged(ListSelectionEvent e) {
                Object selection = list.getSelectedValue();
                if (selection != null) {
                    showDetailViewAction(selection.toString());
                }
            }
        });

        view.getCancelButton().addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                cancelAction();
            }
        });

        view.getSaveButton().addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                savePreferencesAction();
            }
        });
    }

    public void cancelAction() {
        editor.revert();
        view.dispose();
    }

    public void savePreferencesAction() {
        editor.save();
        view.dispose();
    }

    public void showDetailViewAction(String name) {

        if (!detailControllers.containsKey(name)) {
            CayenneController c;

            if ("General".equals(name)) {
                c = new GeneralPreferences(this);
            }
            else if ("DataSource".equals(name)) {
                c = new DataSourcePreferences(this);
            }
            else if ("ClassPath".equals(name)) {
                c = new ClasspathPreferences(this);
            }
            else {
                throw new PreferenceException("Unknown detail key: " + name);
            }

            detailControllers.put(name, c);
            view.getDetailPanel().add(c.getView(), name);

            // this is needed to display freshly added panel...
            view.getDetailPanel().getParent().validate();
        }

        view.getDetailLayout().show(view.getDetailPanel(), name);
    }

    public void startupAction() {

        // setup peer DataContext for editing.
        this.editor = application.getPreferenceEditor();

        // bind own view preferences
        Domain prefDomain = application.getApplicationPreferences().getSubdomain(
                view.getClass());
        ComponentGeometry geometry = ComponentGeometry.getPreference(prefDomain);
        geometry.bind(view, 500, 350);
        geometry.bindIntProperty(view.getSplit(), "dividerLocation", view
                .getSplit()
                .getDividerLocation());

        // show
        centerView();
        makeCloseableOnEscape();
        
        view.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        view.setModal(true);
        view.show();

    }

    public Component getView() {
        return view;
    }

    public PreferenceEditor getEditor() {
        return editor;
    }
}
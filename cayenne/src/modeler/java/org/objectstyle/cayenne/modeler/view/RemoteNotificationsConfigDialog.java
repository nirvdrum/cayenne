package org.objectstyle.cayenne.modeler.view;

import java.awt.BorderLayout;
import java.awt.FlowLayout;

import javax.swing.JPanel;

import org.objectstyle.cayenne.modeler.control.RemoteNotificationsConfigController;
import org.objectstyle.cayenne.modeler.model.RemoteNotificationsConfigModel;
import org.scopemvc.core.Control;
import org.scopemvc.view.swing.SButton;
import org.scopemvc.view.swing.SComboBox;
import org.scopemvc.view.swing.SPanel;
import org.scopemvc.view.swing.SwingView;

import com.jgoodies.forms.extras.DefaultFormBuilder;
import com.jgoodies.forms.layout.FormLayout;

/**
 * @author Andrei Adamchik
 */
public class RemoteNotificationsConfigDialog extends SPanel {

    public RemoteNotificationsConfigDialog() {
        initView();
    }

    protected void initView() {
        setDisplayMode(SwingView.MODAL_DIALOG);
        this.setLayout(new BorderLayout());
        this.setTitle("Configure Remote Cache Synchronization");

        SComboBox type = new SComboBox();
        type.setSelector(RemoteNotificationsConfigModel.NOTIFICATION_TYPES_SELECTOR);
        type.setSelectionSelector(RemoteNotificationsConfigModel.SELECTED_TYPE_SELECTOR);

        SButton saveButton =
            new SButton("cayenne.modeler.remoteNotificationsConfig.save.button");
        SButton cancelButton =
            new SButton("cayenne.modeler.remoteNotificationsConfig.cancel.button");

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.add(saveButton);
        buttonPanel.add(cancelButton);

        FormLayout layout = new FormLayout("right:150, 3dlu, left:200", "");
        DefaultFormBuilder builder = new DefaultFormBuilder(layout);
        builder.setDefaultDialogBorder();
        builder.append("Notification Transport Type:", type);
        
        
        builder.append(buttonPanel, 3);

        this.add(builder.getPanel());
    }

    public Control getCloseControl() {
        return new Control(RemoteNotificationsConfigController.CANCEL_CONFIG_CONTROL);
    }
}

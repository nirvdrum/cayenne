package org.objectstyle.cayenne.dataview;

import java.util.*;
import java.text.*;
import javax.swing.*;
import javax.swing.text.*;

public class FieldComponentFactory {

  public FieldComponentFactory() {
  }

  public JComponent createFieldEditComponent(ObjEntityViewField field) {
    CellRenderers cellRenderers = new CellRenderers();
    JComponent editor = null;
    Format format = field.getEditFormat();
    int dataType = field.getDataType().getValue();
    boolean lookup = field.isLookup();
    int alignment;
    String nullText = "";
    String invalidText = "";

    switch (dataType) {
      case DataTypeEnum.INTEGER_TYPE_VALUE:
      case DataTypeEnum.DOUBLE_TYPE_VALUE:
      case DataTypeEnum.MONEY_TYPE_VALUE:
      case DataTypeEnum.PERCENT_TYPE_VALUE:
        alignment = JTextField.RIGHT;
        break;
      default:
        alignment = JTextField.LEFT;
        break;
    }

    if (lookup) {
      ComboBoxModel comboData =
          new DefaultComboBoxModel(field.getLookupValues());
      ListCellRenderer comboRenderer =
          cellRenderers.createListCellRenderer(field);
      JComboBox comboBox = new JComboBox(comboData);
      comboBox.setRenderer(comboRenderer);
      editor = comboBox;
    } else if (format != null) {
      if (format instanceof MapFormat) {
        MapFormat mapFormat = (MapFormat)format;
        ComboBoxModel comboData =
          new DefaultComboBoxModel((mapFormat).getValues());
        ListCellRenderer comboRenderer =
          cellRenderers.createFormatListCellRenderer(
          mapFormat, mapFormat.getNullFormat(), null, -1);
        JComboBox comboBox = new JComboBox(comboData);
        comboBox.setRenderer(comboRenderer);
        editor = comboBox;
      } else {
        JFormattedTextField textField = new JFormattedTextField(format);
        if (alignment >= 0)
          textField.setHorizontalAlignment(alignment);
        if (format instanceof DecimalFormat)
          textField.setToolTipText(((DecimalFormat)format).toPattern());
        else if (format instanceof SimpleDateFormat)
          textField.setToolTipText(((SimpleDateFormat)format).toPattern());
        editor = textField;
      }
    } else {
      if (dataType == DataTypeEnum.BOOLEAN_TYPE_VALUE) {
        JCheckBox checkBox = new JCheckBox();
        editor = checkBox;
      } else {
        JTextField textField = new JTextField();
        if (alignment >= 0)
          textField.setHorizontalAlignment(alignment);
        editor = textField;
      }
    }

    return editor;
  }
}
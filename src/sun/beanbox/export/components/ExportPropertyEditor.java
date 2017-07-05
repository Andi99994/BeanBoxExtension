package sun.beanbox.export.components;

import sun.beanbox.export.Exporter;
import sun.beanbox.export.datastructure.ExportBean;
import sun.beanbox.export.datastructure.ExportProperty;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.xml.soap.Text;
import java.awt.*;
import java.beans.PropertyDescriptor;
import java.beans.PropertyEditor;
import java.beans.PropertyVetoException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Created by Andreas on 22.06.2017.
 *
 * This class represents the view to customise the ExportProperties during exporting.
 */
public class ExportPropertyEditor extends JPanel {

    /**
     * This constructs all UI elements required to customise an ExportProperty.
     *
     * @param exporter the exporter component
     * @param exportProperty the ExportProperty to be customised
     * @param tree the TreeView to update name changes
     * @param treeNode the node to be updated on name changes
     */
    public ExportPropertyEditor(Exporter exporter, ExportProperty exportProperty, JTree tree, DefaultMutableTreeNode treeNode) {
        setLayout(new GridBagLayout());
        ExportBean exportBean = null;
        DefaultMutableTreeNode current = treeNode;
        while (exportBean == null) {
            current = (DefaultMutableTreeNode) current.getParent();
            if (current.getUserObject() instanceof ExportBean) {
                exportBean = (ExportBean) current.getUserObject();
            }
        }
        JLabel name = new JLabel("Name: ");
        name.setToolTipText("The name of the property. It must be unique among all configurable properties in this ExportBean and be a valid Java identifier.");
        TextField nameText = new TextField(exportProperty.getName());
        JLabel nameCheckLabel = new JLabel(exporter.checkIfValidPropertyName(exportBean, nameText.getText()) ? "Valid name" : "Invalid name");
        final ExportBean finalExportBean = exportBean;
        nameText.addTextListener(e -> {
            if (exporter.checkIfValidPropertyName(finalExportBean, nameText.getText())) {
                exportProperty.setName(nameText.getText());
                nameCheckLabel.setText("Valid name");
                DefaultTreeModel model = (DefaultTreeModel) tree.getModel();
                model.nodeChanged(treeNode);
            } else {
                nameCheckLabel.setText("Invalid name");
            }
        });
        nameText.setColumns(22);
        JLabel currentValue = new JLabel("Current value:");
        currentValue.setAlignmentX(Component.CENTER_ALIGNMENT);
        currentValue.setToolTipText("The currently configured value");
        JCheckBox configurable = new JCheckBox("Configurable");
        configurable.setAlignmentX(Component.CENTER_ALIGNMENT);
        configurable.setSelected(exportProperty.isExport());
        configurable.addActionListener(e -> exportProperty.setExport(configurable.isSelected()));
        configurable.setToolTipText("Select whether the property should still be configurable after export.");
        JCheckBox setDefaults = new JCheckBox("Set default value");
        setDefaults.setAlignmentX(Component.CENTER_ALIGNMENT);
        setDefaults.setSelected(exportProperty.isSetDefaultValue());
        setDefaults.addActionListener(e -> exportProperty.setSetDefaultValue(setDefaults.isSelected()));
        setDefaults.setToolTipText("Select whether the current value should be set as a default value on Bean instantiation. " +
                "Complex types have to be serialized which can lead to performance issues if you are not careful!");

        Component propertyDisplay;
        try {
            propertyDisplay = new PropertyDisplayCanvas(150, 50, exportProperty);
        } catch (InvocationTargetException | IllegalAccessException e) {
            propertyDisplay = new JLabel("Could not load editor or value");
        }

        GridBagConstraints c = new GridBagConstraints();
        c.weightx = 20;
        add(name, c);
        c.weightx = 80;
        c.gridx = 1;
        add(nameText, c);
        c.gridy = 1;
        add(nameCheckLabel, c);
        c.gridx = 0;
        c.gridy = 2;
        add(currentValue, c);
        c.gridx = 1;
        add(propertyDisplay, c);
        c.gridy = 3;
        c.gridx = 0;
        c.gridwidth = 2;
        add(setDefaults, c);
        c.gridy = 4;
        add(configurable, c);
    }
}

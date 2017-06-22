package sun.beanbox.export.components;

import sun.beanbox.export.Exporter;
import sun.beanbox.export.datastructure.ExportBean;
import sun.beanbox.export.datastructure.ExportProperty;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.xml.soap.Text;
import java.awt.*;
import java.beans.PropertyEditor;
import java.lang.reflect.InvocationTargetException;

/**
 * Created by Andi on 22.06.2017.
 */
public class ExportPropertyEditor extends JPanel {

    public ExportPropertyEditor(Frame owner, Exporter exporter, ExportProperty exportProperty, JTree tree, DefaultMutableTreeNode treeNode) {
        setLayout(new GridBagLayout());

        JLabel name = new JLabel("Name: ");
        name.setToolTipText("The name of the property. It must be unique among all configurable properties and be a valid Java identifier.");
        JLabel nameCheckLabel = new JLabel("Valid name");
        TextField nameText = new TextField();
        nameText.setText(exportProperty.getName());
        nameText.addTextListener(e -> {
            ExportBean exportBean = (ExportBean) ((DefaultMutableTreeNode) treeNode.getParent().getParent()).getUserObject();
            if (exporter.checkIfValidPropertyName(exportBean, nameText.getText())) {
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
        JLabel defaultValue = new JLabel("Default value:");
        defaultValue.setAlignmentX(Component.CENTER_ALIGNMENT);
        defaultValue.setToolTipText("Configure a default value to be set after exporting. By default this is equal " +
                "to the currently configured value. Be aware of any property bindings or property veto listeners!");
        JCheckBox configurable = new JCheckBox("Configurable");
        configurable.setAlignmentX(Component.CENTER_ALIGNMENT);
        configurable.setSelected(exportProperty.isExport());
        configurable.addActionListener(e -> exportProperty.setExport(configurable.isSelected()));
        configurable.setToolTipText("Select whether the property should still be configurable after export");

        Component propertyDisplay;
        try {
            propertyDisplay = new PropertyDisplayCanvas(150, 50, exportProperty, PropertyValueType.CURRENT_VALUE);
        } catch (InvocationTargetException | IllegalAccessException e) {
            propertyDisplay = new JLabel("Could not load editor or value");
        }

        Component propertyEditor;
        try {
            PropertyEditorComponent propertyEditorComponent = new PropertyEditorComponent(154, 54, exportProperty, PropertyValueType.DEFAULT_VALUE, owner);
            propertyEditorComponent.addPropertyChangeListener(evt -> {
                //TODO: handle property bindings and vetos
                if (evt.getSource() instanceof PropertyEditor) {
                    PropertyEditor editor = (PropertyEditor) evt.getSource();
                    exportProperty.setDefaultValue(editor.getValue());
                    propertyEditorComponent.repaintComponent();
                    System.out.println(editor.getValue());
                    /*for (int i = 0 ; i < editors.length; i++) {
                        if (editors[i] == editor) {
                            PropertyDescriptor property1 = properties[i];
                            Object value = editor.getValue();
                            values[i] = value;
                            Method setter = property1.getWriteMethod();
                            try {
                                Object args[] = { value };
                                args[0] = value;
                                setter.invoke(target, args);

                                // We add the changed property to the targets wrapper
                                // so that we know precisely what bean properties have
                                // changed for the target bean and we're able to
                                // generate initialization statements for only those
                                // modified properties at code generation time.
                                targetWrapper.getChangedProperties().addElement(properties[i]);

                            } catch (InvocationTargetException ex) {
                                if (ex.getTargetException()
                                        instanceof PropertyVetoException) {
                                    //warning("Vetoed; reason is: "
                                    //        + ex.getTargetException().getMessage());
                                    // temp dealock fix...I need to remove the deadlock.
                                    System.err.println("WARNING: Vetoed; reason is: "
                                            + ex.getTargetException().getMessage());
                                }
                                else
                                    error("InvocationTargetException while updating "
                                            + property1.getName(), ex.getTargetException());
                            } catch (Exception ex) {
                                error("Unexpected exception while updating "
                                        + property1.getName(), ex);
                            }
                            if (views[i] != null && views[i] instanceof PropertyCanvas) {
                                views[i].repaint();
                            }
                            break;
                        }
                    }
                }*/
                }
            });
            propertyEditor = propertyEditorComponent;
        } catch (InvocationTargetException | IllegalAccessException e) {
            propertyEditor = new JLabel("Could not load editor or value");
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
        add(defaultValue, c);
        c.gridx = 1;
        add(propertyEditor, c);
        c.gridy = 4;
        c.gridx = 0;
        c.gridwidth = 2;
        add(configurable, c);
    }
}

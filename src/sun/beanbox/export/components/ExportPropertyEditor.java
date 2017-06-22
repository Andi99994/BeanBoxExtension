package sun.beanbox.export.components;

import sun.beanbox.export.datastructure.ExportProperty;

import javax.swing.*;
import java.awt.*;
import java.beans.PropertyEditor;
import java.lang.reflect.InvocationTargetException;

/**
 * Created by Andi on 22.06.2017.
 */
public class ExportPropertyEditor extends JPanel {

    public ExportPropertyEditor(Frame owner, ExportProperty exportProperty) {
        JLabel currentValue = new JLabel("Current value:");
        currentValue.setAlignmentX(Component.CENTER_ALIGNMENT);
        currentValue.setToolTipText("The currently configured value");
        JLabel defaultValue = new JLabel("Default value:");
        defaultValue.setAlignmentX(Component.CENTER_ALIGNMENT);
        defaultValue.setToolTipText("Configure a default value to be set after exporting. By default this is equal to the currently configured value. Be aware of any property bindings or property veto listeners!");
        JCheckBox configurable = new JCheckBox("Configurable");
        configurable.setAlignmentX(Component.CENTER_ALIGNMENT);
        configurable.setSelected(exportProperty.isExport());
        configurable.addActionListener(e -> exportProperty.setExport(configurable.isSelected()));
        configurable.setToolTipText("Select whether the property should still be configurable after export");
        removeAll();
        repaint();
        add(currentValue);
        try {
            add(new PropertyDisplayCanvas(150, 50, exportProperty, PropertyValueType.CURRENT_VALUE));
        } catch (InvocationTargetException | IllegalAccessException e) {
            add(new JLabel("Could not load editor or value"));
        }
        add(defaultValue);
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
            add(propertyEditorComponent);
        } catch (InvocationTargetException | IllegalAccessException e) {
            add(new JLabel("Could not load editor or value"));
        }
        add(configurable);
    }
}

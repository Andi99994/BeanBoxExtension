package sun.beanbox.export.components;

import sun.beanbox.PropertyCanvas;
import sun.beanbox.PropertySelector;
import sun.beanbox.PropertyText;
import sun.beanbox.export.datastructure.ExportProperty;

import java.awt.*;
import java.beans.*;
import java.lang.reflect.InvocationTargetException;

/**
 * Created by Andi on 20.06.2017.
 */
public class PropertyEditorComponent extends Panel {

    private PropertyEditor editor;
    private Component component;

    public PropertyEditorComponent(int width, int height, ExportProperty property, PropertyValueType valueType, Frame owner) throws InvocationTargetException, IllegalAccessException {
        setMaximumSize(new Dimension(width, height));
        setSize(width, height);
        setPreferredSize(new Dimension(width, height));
        setMinimumSize(new Dimension(width, height));
        PropertyDescriptor propertyDescriptor = property.getPropertyDescriptor();
        Class pec = propertyDescriptor.getPropertyEditorClass();
        if (pec != null) {
            try {
                editor = (PropertyEditor) pec.newInstance();
            } catch (Exception ex) {
                // Drop through.
            }
        }
        if (editor == null) {
            editor = PropertyEditorManager.findEditor(propertyDescriptor.getPropertyType());
        }

        // If we can't edit this component, skip it.
        if (editor == null) {
            // If it's a user-defined property we give a warning.
            String getterClass = propertyDescriptor.getReadMethod().getDeclaringClass().getName();
            if (getterClass.indexOf("java.") != 0) {
                System.err.println("Warning: Can't find public property editor for property \""
                        + propertyDescriptor.getDisplayName() + "\".  Skipping.");
            }
            return;
        }

        Object value = null;
        try {
            value = valueType == PropertyValueType.CURRENT_VALUE ? property.getCurrentValue() : property.getDefaultValue();
        } catch (InvocationTargetException | IllegalAccessException e) {
            e.printStackTrace();
        }
        // Don't try to set null values:
        if (value == null) {
            // If it's a user-defined property we give a warning.
            String getterClass = property.getPropertyDescriptor().getReadMethod().getDeclaringClass().getName();
            if (getterClass.indexOf("java.") != 0) {
                System.err.println("Warning: Property \"" + property.getPropertyDescriptor().getDisplayName()
                        + "\" has null initial value.  Skipping.");
            }
            return;
        }
        editor.setValue(value);

        if (editor.isPaintable() && editor.supportsCustomEditor()) {
            PropertyCanvas propertyCanvas = new PropertyCanvas(owner, editor);
            propertyCanvas.setSize(width - 4, height - 4);
            propertyCanvas.setMinimumSize(new Dimension(width - 4, height - 4));
            propertyCanvas.setPreferredSize(new Dimension(width - 4, height - 4));
            propertyCanvas.setMaximumSize(new Dimension(width - 4, height - 4));
            add(propertyCanvas);
            component = propertyCanvas;
            return;
        } else if (editor.getTags() != null) {
            PropertySelector propertySelector = new PropertySelector(editor);
            add(propertySelector);
            component = propertySelector;
            return;
        } else if (editor.getAsText() != null) {
            PropertyText propertyText = new PropertyText(editor);
            add(propertyText);
            component = propertyText;
            return;
        }
        add(new Label("Unable to print value!"));
    }

    public void addPropertyChangeListener(PropertyChangeListener listener) {
        editor.addPropertyChangeListener(listener);
    }

    public void repaintComponent() {
        component.repaint();
    }
}

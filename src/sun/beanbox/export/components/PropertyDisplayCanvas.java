package sun.beanbox.export.components;

import sun.beanbox.export.datastructure.ExportProperty;

import java.awt.*;
import java.beans.PropertyDescriptor;
import java.beans.PropertyEditor;
import java.beans.PropertyEditorManager;
import java.lang.reflect.InvocationTargetException;

/**
 * Created by Andi on 20.06.2017.
 */
public class PropertyDisplayCanvas extends Canvas {

    private PropertyEditor editor;

    public PropertyDisplayCanvas(int width, int height, ExportProperty property, PropertyValueType valueType) throws InvocationTargetException, IllegalAccessException {
        setMaximumSize(new Dimension(width, height));
        setSize(width, height);
        setPreferredSize(new Dimension(width, height));
        setMinimumSize(new Dimension(width, height));
        PropertyDescriptor propertyDescriptor = property.getPropertyDescriptor();
        Object value = valueType == PropertyValueType.CURRENT_VALUE ? property.getCurrentValue() : property.getDefaultValue();
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

        // Don't try to set null values:
        if (value == null) {
            // If it's a user-defined property we give a warning.
            String getterClass = propertyDescriptor.getReadMethod().getDeclaringClass().getName();
            if (getterClass.indexOf("java.") != 0) {
                System.err.println("Warning: Property \"" + propertyDescriptor.getDisplayName()
                        + "\" has null initial value.  Skipping.");
            }
            return;
        }

        editor.setValue(value);
    }

    @Override
    public void paint(Graphics g) {
        super.paint(g);
        if (editor != null && editor.isPaintable() && editor.supportsCustomEditor()) {
            Rectangle box = new Rectangle(0, 0, getSize().width, getSize().height);
            editor.paintValue(g, box);
            return;
        } else if (editor != null && editor.getAsText() != null){
            String value = editor.getAsText();
            FontMetrics metrics = g.getFontMetrics(g.getFont());
            int x = (getSize().width - metrics.stringWidth(value)) / 2;
            int y = ((getSize().height - metrics.getHeight()) / 2) + metrics.getAscent();
            g.drawString(value, x, y);
            return;
        }
        g.drawString("Unable to print value!", 0,22);
    }
}
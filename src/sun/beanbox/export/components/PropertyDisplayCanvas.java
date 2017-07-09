package sun.beanbox.export.components;

import sun.beanbox.export.datastructure.ExportProperty;

import java.awt.*;
import java.beans.PropertyDescriptor;
import java.beans.PropertyEditor;
import java.beans.PropertyEditorManager;
import java.lang.reflect.InvocationTargetException;

/**
 * Created by Andreas Ertlschweiger on 20.06.2017.
 * <p>
 * This is a Canvas to print the values of properties. It uses the paintValue or
 * getAsText method of the PropertyDescriptor.
 */
class PropertyDisplayCanvas extends Canvas {

    private PropertyEditor editor;

    /**
     * Constructs all UI elements required to display a property.
     *
     * @param width    defines the width of the property display box
     * @param height   defines the height of the property display box
     * @param property the ExportProperty to be displayed
     * @throws InvocationTargetException if there is an error accessing the property
     * @throws IllegalAccessException    if there is an error accessing the property
     */
    PropertyDisplayCanvas(int width, int height, ExportProperty property) throws InvocationTargetException, IllegalAccessException {
        setMaximumSize(new Dimension(width, height));
        setSize(new Dimension(width, height));
        setPreferredSize(new Dimension(width, height));
        setMinimumSize(new Dimension(width, height));

        //code taken and adapted from PropertySheet. Unfortunately there was no re-usability.
        PropertyDescriptor propertyDescriptor = property.getPropertyDescriptor();
        Object value = property.getCurrentValue();
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
        } else if (editor != null && editor.getAsText() != null) {
            String value = editor.getAsText();
            FontMetrics metrics = g.getFontMetrics(g.getFont());
            int x = (getSize().width - metrics.stringWidth(value)) / 2;
            int y = ((getSize().height - metrics.getHeight()) / 2) + metrics.getAscent();
            g.drawString(value, x, y);
            return;
        }
        g.drawString("Unable to print value!", 0, 22);
    }
}
package sun.beanbox.export.datastructure;

import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;

/**
 * Created by Andi on 26.05.2017.
 */
public class ExportProperty {

    private PropertyDescriptor propertyDescriptor;
    private boolean export = true;
    private BeanNode node;
    private Object defaultValue;
    private String name;

    public ExportProperty(PropertyDescriptor propertyDescriptor, BeanNode node) throws InvocationTargetException, IllegalAccessException {
        this.propertyDescriptor = propertyDescriptor;
        this.node = node;
        this.defaultValue = propertyDescriptor.getReadMethod().invoke(node.getData());
    }

    public String getName() {
        if(name == null || name.isEmpty()) {
            return propertyDescriptor.getName();
        } else {
            return name;
        }
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isExport() {
        return export;
    }

    public void setExport(boolean export) {
        this.export = export;
    }

    public PropertyDescriptor getPropertyDescriptor() {
        return propertyDescriptor;
    }

    public BeanNode getNode() {
        return node;
    }

    public Object getCurrentValue() throws InvocationTargetException, IllegalAccessException {
        return propertyDescriptor.getReadMethod().invoke(node.getData());
    }

    public Object getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(Object defaultValue) {
        this.defaultValue = defaultValue;
    }

    public Class getPropertyType() {
        return propertyDescriptor.getPropertyType();
    }

    @Override
    public String toString() {
        return propertyDescriptor.getDisplayName() + " (" + getName() + ")";
    }

    public String uppercaseFirst() {
        char c[] = getName().toCharArray();
        if(Character.isLetter(c[0])) {
            c[0] = Character.toUpperCase(c[0]);
        }
        return new String(c);
    }
}

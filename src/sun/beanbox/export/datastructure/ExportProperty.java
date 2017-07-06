package sun.beanbox.export.datastructure;

import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;

/**
 * Created by Andi on 26.05.2017.
 */
public class ExportProperty { //TODO: add display name property

    private PropertyDescriptor propertyDescriptor;
    private boolean export = true;
    private BeanNode node;
    private String name;
    private boolean setDefaultValue = false;

    public ExportProperty(PropertyDescriptor propertyDescriptor, BeanNode node) {
        this.propertyDescriptor = propertyDescriptor;
        this.node = node;
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

    public boolean isSetDefaultValue() {
        return setDefaultValue;
    }

    public void setSetDefaultValue(boolean setDefaultValue) {
        this.setDefaultValue = setDefaultValue;
    }
}

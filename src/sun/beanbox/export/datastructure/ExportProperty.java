package sun.beanbox.export.datastructure;

import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;

/**
 * Created by Andreas Ertlschweiger on 26.05.2017.
 * <p>
 * This class describes a property of a BeanNode that can be exported.
 * <p>
 * TODO if time: if we want to also use this as a tool to generate BeanInfo classes for existing beans this class could
 * TODO if time: use properties like display name, short description etc to be set by the user to give full configurability
 */
public class ExportProperty extends ExportFeature {

    private final PropertyDescriptor propertyDescriptor;
    private boolean setDefaultValue = false;

    public ExportProperty(PropertyDescriptor propertyDescriptor, BeanNode beanNode) {
        super(beanNode);
        this.propertyDescriptor = propertyDescriptor;
        setName(propertyDescriptor.getName());
    }

    public PropertyDescriptor getPropertyDescriptor() {
        return propertyDescriptor;
    }

    public Object getCurrentValue() throws InvocationTargetException, IllegalAccessException {
        return propertyDescriptor.getReadMethod().invoke(getNode().getData());
    }

    public Class getPropertyType() {
        return propertyDescriptor.getPropertyType();
    }

    @Override
    public String toString() {
        return propertyDescriptor.getDisplayName() + " (" + getName() + ")";
    }

    public boolean isSetDefaultValue() {
        return setDefaultValue;
    }

    public void setSetDefaultValue(boolean setDefaultValue) {
        this.setDefaultValue = setDefaultValue;
    }
}

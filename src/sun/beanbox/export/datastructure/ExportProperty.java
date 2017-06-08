package sun.beanbox.export.datastructure;

import java.beans.PropertyDescriptor;

/**
 * Created by Andi on 26.05.2017.
 */
public class ExportProperty {

    private PropertyDescriptor propertyDescriptor;
    private boolean export = true;
    private BeanNode node;

    public ExportProperty(PropertyDescriptor propertyDescriptor, BeanNode node) {
        this.propertyDescriptor = propertyDescriptor;
        this.node = node;
    }

    public String getName() {
        return propertyDescriptor.getDisplayName();
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

    public String toString() {
        return getName();
    }
}

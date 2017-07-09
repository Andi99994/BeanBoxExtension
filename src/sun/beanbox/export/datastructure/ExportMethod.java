package sun.beanbox.export.datastructure;

import java.beans.MethodDescriptor;

/**
 * Created by Andi on 26.05.2017.
 */
public class ExportMethod {

    private MethodDescriptor methodDescriptor;
    private BeanNode node;
    private String name;
    private boolean include = true;
    private boolean implementInterface = true;
    private Class declaringClass;

    public ExportMethod(MethodDescriptor methodDescriptor, BeanNode node, Class declaringClass) {
        this.methodDescriptor = methodDescriptor;
        this.node = node;
        this.declaringClass = declaringClass;
    }

    public String getName() {
        if(name == null || name.isEmpty()) {
            return methodDescriptor.getName();
        } else {
            return name;
        }
    }

    public MethodDescriptor getMethodDescriptor() {
        return methodDescriptor;
    }

    public BeanNode getNode() {
        return node;
    }

    @Override
    public String toString() {
        return getName();
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isInclude() {
        return include;
    }

    public void setInclude(boolean include) {
        this.include = include;
    }

    public Class getDeclaringClass() {
        return declaringClass;
    }

    public boolean isImplementInterface() {
        return declaringClass != null && implementInterface;
    }

    public void setImplementInterface(boolean implementInterface) {
        this.implementInterface = implementInterface;
    }
}

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

    public ExportMethod(MethodDescriptor methodDescriptor, BeanNode node) {
        this.methodDescriptor = methodDescriptor;
        this.node = node;
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

    public String uppercaseFirst() {
        char c[] = getName().toCharArray();
        if(Character.isLetter(c[0])) {
            c[0] = Character.toUpperCase(c[0]);
        }
        return new String(c);
    }
}

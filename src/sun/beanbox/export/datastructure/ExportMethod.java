package sun.beanbox.export.datastructure;

import java.beans.MethodDescriptor;

/**
 * Created by Andi on 26.05.2017.
 */
public class ExportMethod {

    private MethodDescriptor methodDescriptor;
    private BeanNode node;

    public ExportMethod(MethodDescriptor methodDescriptor, BeanNode node) {
        this.methodDescriptor = methodDescriptor;
        this.node = node;
    }

    public String getName() {
        return methodDescriptor.getDisplayName();
    }

    public MethodDescriptor getMethodDescriptor() {
        return methodDescriptor;
    }

    public BeanNode getNode() {
        return node;
    }

    @Override
    public String toString() {
        return methodDescriptor.getDisplayName();
    }
}

package sun.beanbox.export.datastructure;

import java.beans.MethodDescriptor;

/**
 * Created by Andreas on 26.05.2017.
 * <p>
 * This class describes a method of a BeanNode that can be exported.
 */
public class ExportMethod extends ExportFeature {

    private final MethodDescriptor methodDescriptor;
    private final Class declaringClass;

    public ExportMethod(MethodDescriptor methodDescriptor, BeanNode beanNode, Class declaringClass) {
        super(beanNode);
        this.methodDescriptor = methodDescriptor;
        this.declaringClass = declaringClass;
        setName(methodDescriptor.getName());
    }

    public MethodDescriptor getMethodDescriptor() {
        return methodDescriptor;
    }

    public Class getDeclaringClass() {
        return declaringClass;
    }
}

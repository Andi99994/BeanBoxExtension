package bean;

import event.ImageProcessEvent;
import event.ImageProcessListener;

import java.beans.*;
import java.io.Serializable;

/**
 * Created by hellr on 11/21/2016.
 */
public class OpeningBeanBeanInfo extends SimpleBeanInfo implements Serializable {

    @Override
    public PropertyDescriptor[] getPropertyDescriptors() {
        try {
            Class cls = OpeningBean.class;
            PropertyDescriptor pdWidth = new PropertyDescriptor("radius", cls);
            pdWidth.setDisplayName("Radius");
            PropertyDescriptor pdHeight = new PropertyDescriptor("value", cls);
            pdHeight.setDisplayName("Matrixwert");
            PropertyDescriptor pdIter = new PropertyDescriptor("iterations", cls);
            pdIter.setDisplayName("Wiederholungen");
            return new PropertyDescriptor[]{pdWidth, pdHeight, pdIter};
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public EventSetDescriptor[] getEventSetDescriptors() {
        try {
            EventSetDescriptor esd = new EventSetDescriptor(OpeningBean.class, "ImageProcess", ImageProcessListener.class, "process");
            return new EventSetDescriptor[]{esd};
        } catch (IntrospectionException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public MethodDescriptor[] getMethodDescriptors() {
        try {
            MethodDescriptor md = new MethodDescriptor(OpeningBean.class.getMethod("process", new Class[]{ImageProcessEvent.class}), null);
            return new MethodDescriptor[]{md};
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
        return null;
    }
}

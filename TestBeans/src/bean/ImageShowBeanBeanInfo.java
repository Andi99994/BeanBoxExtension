package bean;

import event.ImageProcessEvent;
import event.ImageProcessListener;

import java.beans.*;
import java.io.Serializable;

/**
 * Created by hellr on 11/21/2016.
 */
public class ImageShowBeanBeanInfo extends SimpleBeanInfo implements Serializable {

    @Override
    public PropertyDescriptor[] getPropertyDescriptors() {
        return new PropertyDescriptor[]{};
    }

    @Override
    public EventSetDescriptor[] getEventSetDescriptors() {
        try {
            EventSetDescriptor esd = new EventSetDescriptor(ImageShowBean.class, "ImageProcess", ImageProcessListener.class, "process");
            return new EventSetDescriptor[]{esd};
        } catch (IntrospectionException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public MethodDescriptor[] getMethodDescriptors() {
        try {
            MethodDescriptor md = new MethodDescriptor(ImageShowBean.class.getMethod("process", new Class[]{ImageProcessEvent.class}), null);
            return new MethodDescriptor[]{md};
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
        return null;
    }
}

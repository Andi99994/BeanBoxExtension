package bean;

import event.ImageProcessEvent;
import event.ImageProcessListener;

import java.beans.*;
import java.io.Serializable;

/**
 * Created by hellr on 11/21/2016.
 */
public class ROIBeanBeanInfo extends SimpleBeanInfo implements Serializable {

    public PropertyDescriptor[] getPropertyDescriptors() {
        try {
            Class cls = ROIBean.class;
            PropertyDescriptor pdX = new PropertyDescriptor("x", cls);
            pdX.setDisplayName("X-Koordinate");
            PropertyDescriptor pdY = new PropertyDescriptor("y", cls);
            pdY.setDisplayName("Y-Koordinate");
            PropertyDescriptor pdWidth = new PropertyDescriptor("width", cls);
            pdWidth.setDisplayName("Breite");
            PropertyDescriptor pdHeight = new PropertyDescriptor("height", cls);
            pdHeight.setDisplayName("Höhe");
            return new PropertyDescriptor[]{pdX, pdY, pdWidth, pdHeight};
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }

    @Override
    public EventSetDescriptor[] getEventSetDescriptors() {
        try {
            EventSetDescriptor esd = new EventSetDescriptor(ROIBean.class, "ImageProcess", ImageProcessListener.class, "process");
            return new EventSetDescriptor[]{esd};
        } catch (IntrospectionException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public MethodDescriptor[] getMethodDescriptors() {
        try {
            MethodDescriptor md = new MethodDescriptor(ROIBean.class.getMethod("process", new Class[]{ImageProcessEvent.class}), null);
            return new MethodDescriptor[]{md};
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
        return null;
    }
}

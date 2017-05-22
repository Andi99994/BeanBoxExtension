package bean;

import event.ImageProcessEvent;
import event.ImageProcessListener;

import java.beans.*;
import java.io.Serializable;

/**
 * Created by hellr on 11/21/2016.
 */
public class ThresholdBeanBeanInfo extends SimpleBeanInfo implements Serializable {

    @Override
    public PropertyDescriptor[] getPropertyDescriptors() {
        try {
            Class cls = ThresholdBean.class;
            PropertyDescriptor pdLow = new PropertyDescriptor("low", cls);
            pdLow.setDisplayName("Unterer Grenzwert");
            PropertyDescriptor pdHigh = new PropertyDescriptor("high", cls);
            pdHigh.setDisplayName("Oberer Grenzwert");
            PropertyDescriptor pdSubColor = new PropertyDescriptor("substitute", cls);
            pdSubColor.setDisplayName("Ersetzungswert (Graustufen)");
            return new PropertyDescriptor[]{pdLow, pdHigh, pdSubColor};
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public EventSetDescriptor[] getEventSetDescriptors() {
        try {
            EventSetDescriptor esd = new EventSetDescriptor(ThresholdBean.class, "ImageProcess", ImageProcessListener.class, "process");
            return new EventSetDescriptor[]{esd};
        } catch (IntrospectionException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public MethodDescriptor[] getMethodDescriptors() {
        try {
            MethodDescriptor md = new MethodDescriptor(ThresholdBean.class.getMethod("process", new Class[]{ImageProcessEvent.class}), null);
            return new MethodDescriptor[]{md};
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
        return null;
    }
}

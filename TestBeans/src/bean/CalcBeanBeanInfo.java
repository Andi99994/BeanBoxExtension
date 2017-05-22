package bean;

import event.CalcResultListener;
import event.ImageProcessEvent;

import java.beans.EventSetDescriptor;
import java.beans.IntrospectionException;
import java.beans.MethodDescriptor;
import java.beans.SimpleBeanInfo;
import java.io.Serializable;

/**
 * Created by hellr on 11/22/2016.
 */
public class CalcBeanBeanInfo extends SimpleBeanInfo implements Serializable {

    @Override
    public EventSetDescriptor[] getEventSetDescriptors() {
        try {
            EventSetDescriptor esd = new EventSetDescriptor(CalcBean.class, "CalcResult", CalcResultListener.class, "process");
            return new EventSetDescriptor[]{esd};
        } catch (IntrospectionException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public MethodDescriptor[] getMethodDescriptors() {
        try {
            MethodDescriptor md = new MethodDescriptor(CalcBean.class.getMethod("process", new Class[]{ImageProcessEvent.class}), null);
            return new MethodDescriptor[]{md};
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
        return null;
    }
}

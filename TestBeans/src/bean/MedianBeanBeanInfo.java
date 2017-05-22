package bean;

import editor.MaskEditor;
import event.ImageProcessEvent;
import event.ImageProcessListener;

import java.beans.*;
import java.io.Serializable;

/**
 * Created by hellr on 11/21/2016.
 */
public class MedianBeanBeanInfo extends SimpleBeanInfo implements Serializable {

    @Override
    public PropertyDescriptor[] getPropertyDescriptors() {
        try {
            Class cls = MedianBean.class;
            PropertyDescriptor pdSize = new PropertyDescriptor("maskSize", cls);
            pdSize.setDisplayName("Maskengröße");
            PropertyDescriptor pdMaskShape = new PropertyDescriptor("shape", cls);
            pdMaskShape.setDisplayName("Maskenform");
            pdMaskShape.setPropertyEditorClass(MaskEditor.class);
            return new PropertyDescriptor[]{pdSize, pdMaskShape};
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public EventSetDescriptor[] getEventSetDescriptors() {
        try {
            EventSetDescriptor esd = new EventSetDescriptor(MedianBean.class, "ImageProcess", ImageProcessListener.class, "process");
            return new EventSetDescriptor[]{esd};
        } catch (IntrospectionException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public MethodDescriptor[] getMethodDescriptors() {
        try {
            MethodDescriptor md = new MethodDescriptor(MedianBean.class.getMethod("process", new Class[]{ImageProcessEvent.class}), null);
            return new MethodDescriptor[]{md};
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
        return null;
    }
}

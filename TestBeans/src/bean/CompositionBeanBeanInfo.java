package bean;

import editor.FilenameEditor;
import event.ImageProcessEvent;
import event.ImageProcessListener;

import java.beans.*;
import java.io.Serializable;

/**
 * Created by hellr on 11/21/2016.
 */
public class CompositionBeanBeanInfo extends SimpleBeanInfo implements Serializable {
    @Override
    public PropertyDescriptor[] getPropertyDescriptors() {
        try {
            Class cls = CompositionBean.class;
            PropertyDescriptor pdFilename = new PropertyDescriptor("filename", cls);
            pdFilename.setPropertyEditorClass(FilenameEditor.class);
            pdFilename.setDisplayName("Pfad");
            PropertyDescriptor pdX = new PropertyDescriptor("x", cls);
            pdX.setDisplayName("X-Koordinate");
            PropertyDescriptor pdY = new PropertyDescriptor("y", cls);
            pdY.setDisplayName("Y-Koordinate");
            PropertyDescriptor pdWidth = new PropertyDescriptor("width", cls);
            pdWidth.setDisplayName("Breite");
            PropertyDescriptor pdHeight = new PropertyDescriptor("height", cls);
            pdHeight.setDisplayName("Höhe");
            return new PropertyDescriptor[]{pdFilename, pdX, pdY, pdWidth, pdHeight};
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    // provided events by this bean (source of an event)
    @Override
    public EventSetDescriptor[] getEventSetDescriptors() {
        try {
            EventSetDescriptor esd = new EventSetDescriptor(CompositionBean.class, "ImageProcess", ImageProcessListener.class, "process"); // ImageProcess = event name which is parameter of 'process' method, process = listener method
            return new EventSetDescriptor[]{esd};
        } catch (IntrospectionException e) {
            e.printStackTrace();
        }
        return null;
    }

    // methods that can be called by other beans (used as 'target's of their events)
    @Override
    public MethodDescriptor[] getMethodDescriptors() {
        try {
            MethodDescriptor md = new MethodDescriptor(CompositionBean.class.getMethod("process", new Class[]{ImageProcessEvent.class}), null);
            return new MethodDescriptor[]{md};
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
        return null;
    }
}

package bean;

import editor.FileFormatEditor;
import editor.FilenameEditor;
import event.ImageProcessEvent;
import event.ImageProcessListener;

import java.beans.*;
import java.io.Serializable;

/**
 * Created by hellr on 11/21/2016.
 */
public class ImageSaveBeanBeanInfo extends SimpleBeanInfo implements Serializable {

    @Override
    public PropertyDescriptor[] getPropertyDescriptors() {
        try {
            Class cls = ImageSaveBean.class;
            PropertyDescriptor pdFilename = new PropertyDescriptor("filename", cls);
            pdFilename.setDisplayName("Dateiname");
            pdFilename.setPropertyEditorClass(FilenameEditor.class);
            PropertyDescriptor pdFormat = new PropertyDescriptor("format", cls);
            pdFormat.setDisplayName("Dateiformat");
            pdFormat.setPropertyEditorClass(FileFormatEditor.class);
            return new PropertyDescriptor[]{pdFilename, pdFormat};
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public EventSetDescriptor[] getEventSetDescriptors() {
        try {
            EventSetDescriptor esd = new EventSetDescriptor(ImageSaveBean.class, "ImageProcess", ImageProcessListener.class, "process");
            return new EventSetDescriptor[]{esd};
        } catch (IntrospectionException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public MethodDescriptor[] getMethodDescriptors() {
        try {
            MethodDescriptor md = new MethodDescriptor(ImageSaveBean.class.getMethod("process", new Class[]{ImageProcessEvent.class}), null);
            return new MethodDescriptor[]{md};
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
        return null;
    }
}

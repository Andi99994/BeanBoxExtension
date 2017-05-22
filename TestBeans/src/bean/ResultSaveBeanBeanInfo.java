package bean;

import editor.FilenameEditor;
import event.CalcResultEvent;
import event.CalcResultListener;

import java.beans.*;
import java.io.Serializable;

/**
 * Created by hellr on 11/21/2016.
 */
public class ResultSaveBeanBeanInfo extends SimpleBeanInfo implements Serializable {
    @Override
    public PropertyDescriptor[] getPropertyDescriptors() {
        try {
            Class cls = ResultSaveBean.class;
            PropertyDescriptor pdFilename = new PropertyDescriptor("filename", cls);
            pdFilename.setDisplayName("Dateiname");
            pdFilename.setPropertyEditorClass(FilenameEditor.class);
            return new PropertyDescriptor[]{pdFilename};
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public EventSetDescriptor[] getEventSetDescriptors() {
        try {
            EventSetDescriptor esd = new EventSetDescriptor(ResultSaveBean.class, "CalcResult", CalcResultListener.class, "process");
            return new EventSetDescriptor[]{esd};
        } catch (IntrospectionException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public MethodDescriptor[] getMethodDescriptors() {
        try {
            MethodDescriptor md = new MethodDescriptor(ResultSaveBean.class.getMethod("process", new Class[]{CalcResultEvent.class}), null);
            return new MethodDescriptor[]{md};
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
        return null;
    }
}

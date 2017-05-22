package bean;

import editor.FilenameEditor;
import event.CalcResultEvent;
import event.CalcResultListener;

import java.beans.*;
import java.io.Serializable;

/**
 * Created by hellr on 11/21/2016.
 */
public class EvaluationBeanBeanInfo extends SimpleBeanInfo implements Serializable {
    @Override
    public PropertyDescriptor[] getPropertyDescriptors() {
        try {
            Class cls = EvaluationBean.class;
            PropertyDescriptor pdDiamLower = new PropertyDescriptor("diameterToleranceLower", cls);
            pdDiamLower.setDisplayName("Unterer Durchmessergrenzwert");
            PropertyDescriptor pdDiamUpper = new PropertyDescriptor("diameterToleranceUpper", cls);
            pdDiamUpper.setDisplayName("Oberer Durchmessergrenzwert");
            PropertyDescriptor pdCoordToler = new PropertyDescriptor("coordinateTolerance", cls);
            pdCoordToler.setDisplayName("Maximaler Zentroidenabstand");
            PropertyDescriptor pdFile = new PropertyDescriptor("pathToFile", cls);
            pdFile.setPropertyEditorClass(FilenameEditor.class);
            pdFile.setDisplayName("Einstellungsfile");
            return new PropertyDescriptor[]{pdDiamLower, pdDiamUpper, pdCoordToler, pdFile};
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public EventSetDescriptor[] getEventSetDescriptors() {
        try {
            EventSetDescriptor esd = new EventSetDescriptor(EvaluationBean.class, "CalcResult", CalcResultListener.class, "process");
            return new EventSetDescriptor[]{esd};
        } catch (IntrospectionException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public MethodDescriptor[] getMethodDescriptors() {
        try {
            MethodDescriptor md = new MethodDescriptor(EvaluationBean.class.getMethod("process", new Class[]{CalcResultEvent.class}), null);
            return new MethodDescriptor[]{md};
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
        return null;
    }
}

package bean;

import event.CalcResultListener;
import event.ImageProcessEvent;
import event.ImageProcessListener;
import logic.CalcCentroidsLogic;

import javax.media.jai.PlanarImage;
import java.io.Serializable;


/**
 * Created by andreas on 21.11.2016.
 */

public class CalcBean extends AbstractEvaluationBean<CalcResultListener> implements ImageProcessListener, Serializable {

    private transient PlanarImage image;

    public CalcBean() {
    }

    @Override
    public void process(ImageProcessEvent event) {
        if (event != null) {
            image = event.getImage();
        }
        if (image != null) {
            notifyListeners(CalcCentroidsLogic.exec(image));
        }
    }
}

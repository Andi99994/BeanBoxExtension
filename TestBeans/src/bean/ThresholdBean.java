package bean;

import event.ImageProcessEvent;
import event.ImageProcessListener;
import logic.ThresholdLogic;

import javax.media.jai.PlanarImage;
import java.io.Serializable;

/**
 * Created by andreas on 21.11.2016.
 */
public class ThresholdBean extends AbstractImageProcessBean<ImageProcessListener> implements ImageProcessListener, Serializable {

    private transient PlanarImage image;
    private int low;
    private int high;
    private int substitute;

    public ThresholdBean() {
        low = 0;
        high = 50;
        substitute = 255;
    }

    @Override
    public void process(ImageProcessEvent event) {
        if (event != null) {
            image = event.getImage();
        }
        if (image != null) {
            notifyListeners(ThresholdLogic.exec(image, low, high, substitute));
        }
    }

    public int getLow() {
        return low;
    }

    public void setLow(int low) {
        this.low = low;
        process(null);
    }

    public int getHigh() {
        return high;
    }

    public void setHigh(int high) {
        this.high = high;
        process(null);
    }

    public int getSubstitute() {
        return substitute;
    }

    public void setSubstitute(int substitute) {
        this.substitute = substitute;
        process(null);
    }
}

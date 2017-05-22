package bean;

import datastructure.MedianMaskChoice;
import event.ImageProcessEvent;
import event.ImageProcessListener;
import logic.MedianLogic;

import javax.media.jai.PlanarImage;
import java.io.Serializable;

/**
 * Created by andreas on 21.11.2016.
 */
public class MedianBean extends AbstractImageProcessBean<ImageProcessListener> implements ImageProcessListener, Serializable {

    private transient PlanarImage image;
    private int maskSize;
    private String shape;

    public MedianBean() {
        maskSize = 5;
        shape = "Square";
    }

    @Override
    public void process(ImageProcessEvent event) {
        if (event != null) {
            image = event.getImage();
        }
        if (image != null) {
            notifyListeners(MedianLogic.exec(image, maskSize, MedianMaskChoice.getShape(shape)));
        }
    }

    public int getMaskSize() {
        return maskSize;
    }

    public void setMaskSize(int maskSize) {
        this.maskSize = maskSize;
        process(null);
    }

    public String getShape() {
        return shape;
    }

    public void setShape(String shape) {
        this.shape = shape;
        process(null);
    }
}

package bean;

import event.ImageProcessEvent;
import event.ImageProcessListener;
import logic.OpeningLogic;

import javax.media.jai.PlanarImage;
import java.io.Serializable;

/**
 * Created by andreas on 21.11.2016.
 */
public class OpeningBean extends AbstractImageProcessBean<ImageProcessListener> implements ImageProcessListener, Serializable {

    private transient PlanarImage image;
    private int radius;
    private float value;
    private int iterations;

    public OpeningBean() {
        radius = 1;
        value = 1.0f;
        iterations = 5;
    }

    @Override
    public void process(ImageProcessEvent event) {
        if (event != null) {
            image = event.getImage();
        }
        if (image != null) {
            notifyListeners(OpeningLogic.exec(image, radius, value, iterations));
        }
    }

    public int getIterations() {
        return iterations;
    }

    public void setIterations(int iterations) {
        this.iterations = iterations;
        process(null);
    }

    public int getRadius() {
        return radius;
    }

    public void setRadius(int radius) {
        this.radius = radius;
        process(null);
    }

    public float getValue() {
        return value;
    }

    public void setValue(float value) {
        this.value = value;
        process(null);
    }
}

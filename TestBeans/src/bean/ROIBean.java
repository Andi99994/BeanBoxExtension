package bean;

import event.ImageProcessEvent;
import event.ImageProcessListener;
import logic.ROILogic;

import javax.media.jai.PlanarImage;
import java.awt.*;
import java.io.Serializable;

/**
 * Created by andreas on 21.11.2016.
 */
public class ROIBean extends AbstractImageProcessBean<ImageProcessListener> implements ImageProcessListener, Serializable {

    private transient PlanarImage image;
    private int x;
    private int y;
    private int width;
    private int height;

    public ROIBean() {
        x = 0;
        y = 50;
        width = 5000;
        height = 100;
    }

    @Override
    public void process(ImageProcessEvent event) {
        if (event != null) {
            image = event.getImage();
        }
        if (image != null) {
            Rectangle rectangle = new Rectangle(x, y, width, height);
            notifyListeners(ROILogic.exec(image, rectangle));
        }
    }

    public int getX() {
        return x;
    }

    public void setX(int x) {
        this.x = x;
        process(null);
    }

    public int getY() {
        return y;
    }

    public void setY(int y) {
        this.y = y;
        process(null);
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
        process(null);
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
        process(null);
    }
}

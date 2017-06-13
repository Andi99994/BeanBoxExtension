package bean;

import event.ImageProcessEvent;
import event.ImageProcessListener;

import javax.media.jai.PlanarImage;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.EventListener;

/**
 * Created by Andi on 05.05.2017.
 */
public class CompositionBean implements ImageProcessListener, Serializable {

    private ImageReadBean imageReadBean = new ImageReadBean();
    private ROIBean roiBean = new ROIBean();

    public CompositionBean() {
        imageReadBean.addImageProcessListener(roiBean);
    }

    public void addImageProcessListener(ImageProcessListener listener) {
        roiBean.addImageProcessListener(listener);
    }

    public void removeImageProcessListener(ImageProcessListener listener) {
        roiBean.removeImageProcessListener(listener);
    }

    protected ArrayList<ImageProcessListener> getListeners() {
        return roiBean.getListeners();
    }

    protected void notifyImageProcessListeners(PlanarImage image) {
        ArrayList<ImageProcessListener> listens;

        synchronized (this) {
            listens = (ArrayList<ImageProcessListener>) getListeners().clone();
        }

        for (ImageProcessListener listener : listens) {
            listener.process(new ImageProcessEvent(this, image));
        }
    }

    @Override
    public void process(ImageProcessEvent event) {
        imageReadBean.process(event);
    }

    public int getX() {
        return roiBean.getX();
    }

    public void setX(int x) {
        roiBean.setX(x);
        process(null);
    }

    public int getY() {
        return roiBean.getY();
    }

    public void setY(int y) {
        roiBean.setY(y);
        process(null);
    }

    public int getWidth() {
        return roiBean.getWidth();
    }

    public void setWidth(int width) {
        roiBean.setWidth(width);
        process(null);
    }

    public int getHeight() {
        return roiBean.getHeight();
    }

    public void setHeight(int height) {
        roiBean.setHeight(height);
        process(null);
    }

    public String getFilename() {
        return imageReadBean.getFilename();
    }

    public void setFilename(String filename) {
        imageReadBean.setFilename(filename);
        process(null);
    }
}

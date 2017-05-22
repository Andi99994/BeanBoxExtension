package bean;

import event.ImageProcessEvent;
import event.ImageProcessListener;

import javax.media.jai.PlanarImage;
import javax.swing.*;
import java.io.Serializable;
import java.util.ArrayList;

/**
 * Created by andreas on 24.11.2016.
 */
public abstract class AbstractDisplayBean<T extends ImageProcessListener> extends JPanel implements Serializable {

    private ArrayList<T> listeners = new ArrayList<>();

    public void addImageProcessListener(T listener) {
        listeners.add(listener);
    }

    public void removeImageProcessListener(T listener) {
        listeners.remove(listener);
    }

    protected ArrayList<T> getListeners() {
        return listeners;
    }

    protected void notifyListeners(PlanarImage image) {
        ArrayList<ImageProcessListener> listens;

        synchronized (this) {
            listens = (ArrayList<ImageProcessListener>) getListeners().clone();
        }

        for (ImageProcessListener listener : listens) {
            listener.process(new ImageProcessEvent(this, image));
        }
    }
}

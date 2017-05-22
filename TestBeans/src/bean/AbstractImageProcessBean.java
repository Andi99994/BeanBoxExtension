package bean;

import event.ImageProcessEvent;
import event.ImageProcessListener;

import javax.media.jai.PlanarImage;
import java.io.Serializable;
import java.util.ArrayList;

/**
 * Created by andreas on 14.11.2016.
 */
public abstract class AbstractImageProcessBean<T extends ImageProcessListener> implements Serializable {

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
        ImageProcessEvent event = new ImageProcessEvent(this, image);

        synchronized (this) {
            listens = (ArrayList<ImageProcessListener>) getListeners().clone();
        }

        for (ImageProcessListener listener : listens) {
            listener.process(event);
        }
    }
}

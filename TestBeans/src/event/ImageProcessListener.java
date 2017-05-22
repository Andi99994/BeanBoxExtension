package event;

import java.io.Serializable;
import java.util.EventListener;

/**
 * Created by andreas on 14.11.2016.
 */
public interface ImageProcessListener extends EventListener, Serializable {

    void process(ImageProcessEvent event);
}

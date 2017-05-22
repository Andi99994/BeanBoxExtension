package event;

import javax.media.jai.PlanarImage;
import java.io.Serializable;
import java.util.EventObject;

/**
 * Created by andreas on 14.11.2016.
 */
public class ImageProcessEvent extends EventObject implements Serializable {

    private PlanarImage image;

    /**
     * Constructs a prototypical Event.
     *
     * @param source The object on which the Event initially occurred.
     * @throws IllegalArgumentException if source is null.
     */
    public ImageProcessEvent(Object source, PlanarImage image) {
        super(source);
        this.image = image;
    }

    public PlanarImage getImage() {
        return image;
    }
}

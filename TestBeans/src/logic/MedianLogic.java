package logic;

import javax.media.jai.PlanarImage;
import javax.media.jai.operator.MedianFilterDescriptor;
import javax.media.jai.operator.MedianFilterShape;
import java.io.Serializable;

/**
 * Created by hellr on 11/13/2016.
 */
public class MedianLogic implements Serializable {

    public MedianLogic() {
    }

    public static PlanarImage exec(PlanarImage entity, int maskSize, MedianFilterShape maskShape) {
        return MedianFilterDescriptor.create(entity, maskShape, maskSize, null);
    }
}

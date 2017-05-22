package logic;

import javax.media.jai.PlanarImage;
import java.awt.*;
import java.io.Serializable;

/**
 * Created by hellr on 11/13/2016.
 */
public class ROILogic implements Serializable {

    public ROILogic() {
    }

    public static PlanarImage exec(PlanarImage image, Rectangle roi) {
        PlanarImage planarImage = PlanarImage.wrapRenderedImage(image.getAsBufferedImage(roi, image.getColorModel()));
        planarImage.setProperty("ThresholdX", (int) roi.getX());
        planarImage.setProperty("ThresholdY", (int) roi.getY());
        return planarImage;
    }
}

package logic;

import javax.imageio.ImageIO;
import javax.media.jai.PlanarImage;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;

/**
 * Created by hellr on 11/13/2016.
 */
public class ImageSaveLogic implements Serializable {

    public ImageSaveLogic() {
    }

    public static void exec(PlanarImage entity, String filename, String format) {
        filename += "." + format;
        BufferedImage bi = entity.getAsBufferedImage();
        File out = new File(filename);
        try {
            ImageIO.write(bi, format, out);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

package logic;

import javax.media.jai.JAI;
import javax.media.jai.PlanarImage;
import java.io.Serializable;

/**
 * Created by hellr on 11/13/2016.
 */
public class ImageReadLogic implements Serializable {

    public ImageReadLogic() {
    }

    public static PlanarImage exec(String filename) {
        return JAI.create("fileload", filename);
    }
}

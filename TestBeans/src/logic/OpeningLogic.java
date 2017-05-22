package logic;

import javax.media.jai.KernelJAI;
import javax.media.jai.PlanarImage;
import javax.media.jai.operator.DilateDescriptor;
import javax.media.jai.operator.ErodeDescriptor;
import java.io.Serializable;

/**
 * Created by hellr on 11/13/2016.
 */
public class OpeningLogic implements Serializable {

    public OpeningLogic() {
    }

    public static PlanarImage exec(PlanarImage entity, int radius, float value, int iterations) {
        KernelJAI kernel = MatrixCreator.createConstantCircle(radius, value);
        PlanarImage planarImage = entity;

        for (int i = 0; i < iterations; i++) {
            planarImage = ErodeDescriptor.create(planarImage, kernel, null);
        }

        for (int i = 0; i < iterations; i++) {
            planarImage = DilateDescriptor.create(planarImage, kernel, null);
        }

        return planarImage;
    }


}

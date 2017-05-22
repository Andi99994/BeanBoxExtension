package logic;

import javax.media.jai.PlanarImage;
import javax.media.jai.operator.ThresholdDescriptor;
import java.io.Serializable;

/**
 * Created by hellr on 11/13/2016.
 */
public class ThresholdLogic implements Serializable {

    public ThresholdLogic() {
    }

    public static PlanarImage exec(PlanarImage entity, double low, double high, int substitute) {
        double[] lowArr, highArr, constantArr;
        lowArr = new double[]{low};
        highArr = new double[]{high};
        constantArr = new double[]{substitute};

        return ThresholdDescriptor.create(entity, lowArr, highArr, constantArr, null);
    }

}

package datastructure;

import javax.media.jai.operator.MedianFilterDescriptor;
import javax.media.jai.operator.MedianFilterShape;
import java.io.Serializable;

/**
 * Created by andreas on 22.11.2016.
 */
public class MedianMaskChoice implements Serializable {

    public static MedianFilterShape getShape(String shape) {
        String s = shape.toLowerCase();
        switch (s) {
            case "plus":
                return MedianFilterDescriptor.MEDIAN_MASK_PLUS;
            case "square":
                return MedianFilterDescriptor.MEDIAN_MASK_SQUARE;
            case "square separable":
                return MedianFilterDescriptor.MEDIAN_MASK_SQUARE_SEPARABLE;
            case "x":
                return MedianFilterDescriptor.MEDIAN_MASK_X;
        }
        return null;
    }
}

package logic;

import javax.media.jai.KernelJAI;
import java.io.Serializable;

/**
 * Created by andreas on 23.11.2016.
 * <p>
 * Algorithm source: https://github.com/mbedward/jaitools/blob/master/utils/src/main/java/org/jaitools/media/jai/kernel/KernelFactoryHelper.java
 */
public class MatrixCreator implements Serializable {

    public static KernelJAI createConstantCircle(int radius, float value) {
        if (radius <= 0) {
            throw new IllegalArgumentException(
                    "Invalid radius (" + radius + "); must be > 0");
        }

        float[] weights = makeCircle(radius);

        int w = 2 * radius + 1;
        rowFill(weights, w, w, value);

        return new KernelJAI(w, w, weights);
    }

    private static float[] makeCircle(int radius) {
        int w = 2 * radius + 1;
        float[] m = new float[w * w];

        int[] offset = new int[w];
        for (int i = 0, o = 0; i < w; i++, o += w) offset[i] = o;

        int x = radius, y = 0;
        int r2 = radius * radius;
        while (x > 0) {
            int ix = radius + x;
            int iy = radius + y;
            m[ix + offset[iy]] = 1f;
            m[w - 1 - ix + offset[iy]] = 1f;
            iy = w - 1 - iy;
            m[ix + offset[iy]] = 1f;
            m[w - 1 - ix + offset[iy]] = 1f;
            y--;
            x = (int) Math.sqrt(r2 - y * y);
        }

        m[radius] = 1f;
        m[radius + offset[2 * radius]] = 1f;

        return m;
    }

    private static void rowFill(float[] data, int w, int h, float value) {

        int k = 0;
        for (int y = 0; y < h; y++) {
            int left = -1, right = -1;
            for (int x = 0; x < w; x++, k++) {
                if (data[k] > 0) {
                    if (left < 0) {
                        left = k;
                    } else {
                        right = k;
                    }
                }
            }

            while (right > left + 1) {
                data[--right] = value;
            }
        }
    }
}

/* this filter expects the bonding discs to be completely white: pixel value of 255 on a scale of 0..255
 * all other pixels in the image are expected to have a pixel value < 255
 * use this filter adapting eventually the package name 
 */
package logic;

import datastructure.CentroidResult;
import datastructure.Coordinate;

import javax.media.jai.PlanarImage;
import java.awt.image.BufferedImage;
import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

public class CalcCentroidsLogic implements Serializable {

    private static HashMap<Coordinate, Boolean> _general;
    private static LinkedList<LinkedList<Coordinate>> _figures;
    private static PlanarImage _image;

    public static List<CentroidResult> exec(PlanarImage entity) {
        _general = new HashMap<>();
        _figures = new LinkedList<>();
        _image = entity;
        List<CentroidResult> res = new LinkedList<>();

        Coordinate[] centroids = processImage(entity);

        int i = 0;
        for (Coordinate c : centroids) {
            CentroidResult centroidResult = new CentroidResult();
            centroidResult.setCentroid(c);

            // magic calculation
            int xMax = Integer.MIN_VALUE;
            int xMin = Integer.MAX_VALUE;
            int yMax = Integer.MIN_VALUE;
            int yMin = Integer.MAX_VALUE;
            for (Coordinate cFig : _figures.get(i)) {
                if (cFig._x > xMax) {
                    xMax = cFig._x;
                }
                if (cFig._x < xMin) {
                    xMin = cFig._x;
                }
                if (cFig._y > yMax) {
                    yMax = cFig._y;
                }
                if (cFig._y < yMin) {
                    yMin = cFig._y;
                }
            }

            double diameter1 = xMax - xMin;
            double diameter2 = yMax - yMin;
            double diameterMax = Math.max(diameter1, diameter2);

            centroidResult.setDiameter(diameterMax);
            res.add(centroidResult);
            i++;
        }
        return res;
    }

    private static Coordinate[] processImage(PlanarImage entity) {
        BufferedImage bi = entity.getAsBufferedImage();

        for (int x = 0; x < bi.getWidth(); x++) {
            for (int y = 0; y < bi.getHeight(); y++) {
                int p = bi.getRaster().getSample(x, y, 0);
                if (p == 255 && !_general.containsKey(new Coordinate(x, y))) {
                    getNextFigure(bi, x, y);        //if there is a not visited white pixel, save all pixels belonging to the same figure
                }
            }
        }

        return calculateCentroids();    //calculate the centroids of all figures
    }

    private static void getNextFigure(BufferedImage img, int x, int y) {
        LinkedList<Coordinate> figure = new LinkedList<Coordinate>();
        _general.put(new Coordinate(x, y), true);
        figure.add(new Coordinate(x, y));

        addConnectedComponents(img, figure, x, y);

        _figures.add(figure);
    }

    private static void addConnectedComponents(BufferedImage img, LinkedList<Coordinate> figure, int x, int y) {
        if (x - 1 >= 0 && !_general.containsKey((new Coordinate(x - 1, y))) && img.getRaster().getSample(x - 1, y, 0) == 255) {
            _general.put(new Coordinate(x - 1, y), true);
            figure.add(new Coordinate(x - 1, y));
            addConnectedComponents(img, figure, x - 1, y);
        }
        if (x + 1 < img.getWidth() && !_general.containsKey((new Coordinate(x + 1, y))) && img.getRaster().getSample(x + 1, y, 0) == 255) {
            _general.put(new Coordinate(x + 1, y), true);
            figure.add(new Coordinate(x + 1, y));
            addConnectedComponents(img, figure, x + 1, y);
        }
        if (y - 1 >= 0 && !_general.containsKey((new Coordinate(x, y - 1))) && img.getRaster().getSample(x, y - 1, 0) == 255) {
            _general.put(new Coordinate(x, y - 1), true);
            figure.add(new Coordinate(x, y - 1));
            addConnectedComponents(img, figure, x, y - 1);
        }
        if (y + 1 < img.getHeight() && !_general.containsKey((new Coordinate(x, y + 1))) && img.getRaster().getSample(x, y + 1, 0) == 255) {
            _general.put(new Coordinate(x, y + 1), true);
            figure.add(new Coordinate(x, y + 1));
            addConnectedComponents(img, figure, x, y + 1);
        }
    }

    private static Coordinate[] calculateCentroids() {
        Coordinate[] centroids = new Coordinate[_figures.size()];
        int i = 0;
        for (LinkedList<Coordinate> figure : _figures) {
            LinkedList<Integer> xValues = new LinkedList<>();
            LinkedList<Integer> yValues = new LinkedList<>();

            for (Coordinate c : figure) {
                xValues.add(c._x);
                yValues.add(c._y);
            }

            Collections.sort(xValues);
            Collections.sort(yValues);

            int xMedian = xValues.get(xValues.size() / 2);
            int yMedian = yValues.get(yValues.size() / 2);

            centroids[i] = new Coordinate(xMedian + (int) _image.getProperty("ThresholdX"), yMedian + (int) _image.getProperty("ThresholdY"));
            i++;
        }
        return centroids;
    }
}

package logic;

import datastructure.CentroidResult;
import datastructure.Coordinate;
import datastructure.Interval;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by hellr on 11/13/2016.
 */
public class EvaluationLogic implements Serializable {

    public EvaluationLogic() {
    }

    public static void exec(List<CentroidResult> entity, Interval diameterTolerance, double coordinateTolerance, String pathToFile) {

        List<Coordinate> centroidsLocations = new ArrayList<>();
        try {
            BufferedReader reader = new BufferedReader(new FileReader(pathToFile));
            String line;
            while ((line = reader.readLine()) != null) {
                String[] numbersRaw = line.split(",");
                centroidsLocations.add(new Coordinate(Integer.parseInt(numbersRaw[0]), Integer.parseInt(numbersRaw[1])));
            }
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Error while reading file");
        } catch (NumberFormatException e) {
            e.printStackTrace();
            System.out.println("Error while parsing coordinates");
        }

        for (int i = 0; i < entity.size(); i++) {
            CentroidResult current = entity.get(i);
            Coordinate optimalCentroid = centroidsLocations.get(i);

            current.setIsInDiameterTolerance(current.getDiameter() <= diameterTolerance.getUpperBound() && current.getDiameter() >= diameterTolerance.getLowerBound());
            double distance = Math.sqrt((Math.pow(current.getCentroid()._x - optimalCentroid._x, 2) + Math.pow(current.getCentroid()._y - optimalCentroid._y, 2)));
            current.setIsInCentroidCoordinateTolerance(distance <= coordinateTolerance);
        }
    }
}

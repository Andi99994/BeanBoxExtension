package bean;

import datastructure.CentroidResult;
import datastructure.Interval;
import event.CalcResultEvent;
import event.CalcResultListener;
import logic.EvaluationLogic;
import logic.FilePathValidator;

import java.io.Serializable;
import java.util.List;


/**
 * Created by andreas on 21.11.2016.
 */

public class EvaluationBean extends AbstractEvaluationBean<CalcResultListener> implements CalcResultListener, Serializable {

    private List<CentroidResult> results;
    private int diameterToleranceLower;
    private int diameterToleranceUpper;
    private double coordinateTolerance;
    private String pathToFile;

    public EvaluationBean() {
        pathToFile = "";

        diameterToleranceLower = 18;
        diameterToleranceUpper = 21;
        coordinateTolerance = 2.0;
    }

    @Override
    public void process(CalcResultEvent event) {
        if (event != null) {
            results = event.getResults();
        }

        if (results != null && FilePathValidator.isValidFile(pathToFile, "txt")) {
            EvaluationLogic.exec(results, new Interval(diameterToleranceLower, diameterToleranceUpper), coordinateTolerance, pathToFile);
            notifyListeners(results);
        }
    }

    public double getCoordinateTolerance() {
        return coordinateTolerance;
    }

    public void setCoordinateTolerance(double coordinateTolerance) {
        this.coordinateTolerance = coordinateTolerance;
        process(null);
    }

    public int getDiameterToleranceLower() {
        return diameterToleranceLower;
    }

    public void setDiameterToleranceLower(int diameterToleranceLower) {
        this.diameterToleranceLower = diameterToleranceLower;
        process(null);
    }

    public int getDiameterToleranceUpper() {
        return diameterToleranceUpper;
    }

    public void setDiameterToleranceUpper(int diameterToleranceUpper) {
        this.diameterToleranceUpper = diameterToleranceUpper;
        process(null);
    }

    public String getPathToFile() {
        return pathToFile;
    }

    public void setPathToFile(String pathToFile) {
        this.pathToFile = pathToFile;
        process(null);
    }
}
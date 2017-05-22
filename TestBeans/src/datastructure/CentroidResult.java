package datastructure;

import java.io.Serializable;

/**
 * Created by hellr on 11/9/2016.
 */
public class CentroidResult implements Serializable {

    private Boolean _isInDiameterTolerance;
    private Boolean _isInCentroidCoordinateTolerance;
    private Coordinate _centroid;
    private Double _diameter;

    public CentroidResult() {
        _isInDiameterTolerance = null;
        _isInCentroidCoordinateTolerance = null;
        _centroid = null;
        _diameter = null;
    }

    public Boolean getIsInDiameterTolerance() {
        return _isInDiameterTolerance;
    }

    public void setIsInDiameterTolerance(Boolean isInDiameterTolerance) {
        this._isInDiameterTolerance = isInDiameterTolerance;
    }

    public Boolean getIsInCentroidCoordinateTolerance() {
        return _isInCentroidCoordinateTolerance;
    }

    public void setIsInCentroidCoordinateTolerance(Boolean isInCentroidCoordinateTolerance) {
        this._isInCentroidCoordinateTolerance = isInCentroidCoordinateTolerance;
    }

    public Coordinate getCentroid() {
        return _centroid;
    }

    public void setCentroid(Coordinate centroid) {
        this._centroid = centroid;
    }

    public Double getDiameter() {
        return _diameter;
    }

    public void setDiameter(Double diameter) {
        this._diameter = diameter;
    }

    @Override
    public String toString() {
        return "Center: [" + _centroid._x + "][" + _centroid._y + "], Avg. Diameter: " + _diameter + "px, Tolerable Center: "
                + _isInCentroidCoordinateTolerance + ", Tolerable Diameter: " + _isInDiameterTolerance;
    }
}

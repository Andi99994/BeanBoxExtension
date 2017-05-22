package datastructure;

import java.io.Serializable;

/**
 * Created by andreas on 09.11.2016.
 */
public class Interval implements Serializable {

    private double _lowerBound;
    private double _upperBound;

    public Interval(double lowerBound, double upperBound) {
        _lowerBound = lowerBound;
        _upperBound = upperBound;
    }

    public double getLowerBound() {
        return _lowerBound;
    }

    public void setLowerBound(double lowerBound) {
        this._lowerBound = lowerBound;
    }

    public double getUpperBound() {
        return _upperBound;
    }

    public void setUpperBound(double upperBound) {
        this._upperBound = upperBound;
    }
}

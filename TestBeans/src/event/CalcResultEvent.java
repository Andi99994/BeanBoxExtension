package event;

import datastructure.CentroidResult;

import java.io.Serializable;
import java.util.EventObject;
import java.util.List;

/**
 * Created by andreas on 21.11.2016.
 */
public class CalcResultEvent extends EventObject implements Serializable {

    private List<CentroidResult> results;

    /**
     * Constructs a prototypical Event.
     *
     * @param source The object on which the Event initially occurred.
     * @throws IllegalArgumentException if source is null.
     */
    public CalcResultEvent(Object source, List<CentroidResult> results) {
        super(source);
        this.results = results;
    }

    public List<CentroidResult> getResults() {
        return results;
    }
}

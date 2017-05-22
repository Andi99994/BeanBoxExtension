package bean;

import datastructure.CentroidResult;
import event.CalcResultEvent;
import event.CalcResultListener;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by andreas on 14.11.2016.
 */
public abstract class AbstractEvaluationBean<T extends CalcResultListener> implements Serializable {

    private ArrayList<T> listeners = new ArrayList<>();

    public void addCalcResultListener(T listener) {
        listeners.add(listener);
    }

    public void removeCalcResultListener(T listener) {
        listeners.remove(listener);
    }

    protected ArrayList<T> getListeners() {
        return listeners;
    }

    protected void notifyListeners(List<CentroidResult> results) {
        ArrayList<CalcResultListener> listens;
        CalcResultEvent event = new CalcResultEvent(this, results);

        synchronized (this) {
            listens = (ArrayList<CalcResultListener>) getListeners().clone();
        }

        for (CalcResultListener listener : listens) {
            listener.process(event);
        }
    }
}

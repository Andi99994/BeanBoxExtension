package bean;

import datastructure.CentroidResult;
import event.CalcResultEvent;
import event.CalcResultListener;
import logic.ResultWriteLogic;

import java.io.Serializable;
import java.util.List;


/**
 * Created by andreas on 21.11.2016.
 */

public class ResultSaveBean extends AbstractEvaluationBean<CalcResultListener> implements CalcResultListener, Serializable {
    private List<CentroidResult> results;
    private String filename;

    public ResultSaveBean() {
        filename = "";
    }

    @Override
    public void process(CalcResultEvent event) {
        if (event != null) {
            results = event.getResults();
        }
        if (results != null && !filename.equals("")) {
            ResultWriteLogic.exec(results, filename);
            notifyListeners(results);
        }
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
        process(null);
    }
}


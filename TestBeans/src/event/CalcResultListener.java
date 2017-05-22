package event;

import java.io.Serializable;
import java.util.EventListener;

/**
 * Created by andreas on 21.11.2016.
 */
public interface CalcResultListener extends EventListener, Serializable {

    void process(CalcResultEvent event);
}

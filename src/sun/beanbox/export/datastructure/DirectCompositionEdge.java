package sun.beanbox.export.datastructure;

import sun.beanbox.WrapperEventTarget;

import java.beans.EventSetDescriptor;

/**
 * Created by Andreas Ertlschweiger on 22.06.2017.
 * <p>
 * This class describes an edge between two BeanNodes that is given by a direct composition.
 */
public class DirectCompositionEdge extends BeanEdge {

    private final EventSetDescriptor eventSetDescriptor;

    public DirectCompositionEdge(BeanNode start, BeanNode end, WrapperEventTarget eventTarget) {
        super(start, end);
        this.eventSetDescriptor = eventTarget.getEventSetDescriptor();
    }

    public EventSetDescriptor getEventSetDescriptor() {
        return eventSetDescriptor;
    }
}

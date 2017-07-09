package sun.beanbox.export.datastructure;

import sun.beanbox.WrapperEventTarget;

import java.beans.EventSetDescriptor;

/**
 * Created by Andreas Ertlschweiger on 22.06.2017.
 * <p>
 * This class describes an edge between two BeanNodes that is given by a composition that uses an adapter class.
 */
public class AdapterCompositionEdge extends BeanEdge {

    private final Object hookup;
    private final String adapterClassPath;
    private final EventSetDescriptor eventSetDescriptor;

    public AdapterCompositionEdge(BeanNode start, BeanNode end, WrapperEventTarget eventTarget) {
        super(start, end);
        this.hookup = eventTarget.getTargetListener();
        this.adapterClassPath = eventTarget.getPath();
        this.eventSetDescriptor = eventTarget.getEventSetDescriptor();
    }

    public Object getHookup() {
        return hookup;
    }

    public String getAdapterClassPath() {
        return adapterClassPath;
    }

    public EventSetDescriptor getEventSetDescriptor() {
        return eventSetDescriptor;
    }
}

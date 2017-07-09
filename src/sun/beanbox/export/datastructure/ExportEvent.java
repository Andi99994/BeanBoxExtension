package sun.beanbox.export.datastructure;

import java.beans.EventSetDescriptor;

/**
 * Created by Andreas Ertlschweiger on 03.07.2017.
 * <p>
 * This class describes an event of a BeanNode that can be exported.
 */
public class ExportEvent extends ExportFeature {

    private final EventSetDescriptor eventSetDescriptor;

    public ExportEvent(EventSetDescriptor eventSetDescriptor, BeanNode beanNode) {
        super(beanNode);
        this.eventSetDescriptor = eventSetDescriptor;
        setName(eventSetDescriptor.getName());
    }

    public EventSetDescriptor getEventSetDescriptor() {
        return eventSetDescriptor;
    }
}

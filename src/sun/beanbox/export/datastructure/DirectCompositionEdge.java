package sun.beanbox.export.datastructure;

import sun.beanbox.WrapperEventTarget;

import java.beans.EventSetDescriptor;

/**
 * Created by Andi on 22.06.2017.
 */
public class DirectCompositionEdge extends BeanEdge{

    private String eventSetName;
    private EventSetDescriptor eventSetDescriptor;

    public DirectCompositionEdge(BeanNode start, BeanNode end, WrapperEventTarget eventTarget) {
        super(start, end);
        this.eventSetName = eventTarget.getEventSetName();
        this.eventSetDescriptor = eventTarget.getEventSetDescriptor();
    }

    public String getEventSetName() {
        char c[] = eventSetName.toCharArray();
        if(Character.isLetter(c[0])) {
            c[0] = Character.toUpperCase(c[0]);
        }
        return new String(c);
    }

    public EventSetDescriptor getEventSetDescriptor() {
        return eventSetDescriptor;
    }
}

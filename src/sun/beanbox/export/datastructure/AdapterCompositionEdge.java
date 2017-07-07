package sun.beanbox.export.datastructure;

import sun.beanbox.WrapperEventTarget;

import java.beans.EventSetDescriptor;

/**
 * Created by Andi on 22.06.2017.
 */
public class AdapterCompositionEdge extends BeanEdge {

    private Object hookup;
    private String eventSetName;
    private String adapterClassPath;
    private EventSetDescriptor eventSetDescriptor;

    public AdapterCompositionEdge(BeanNode start, BeanNode end, WrapperEventTarget eventTarget) {
        super(start, end);
        this.hookup = eventTarget.getTargetListener();
        this.eventSetName = eventTarget.getEventSetName();
        this.adapterClassPath = eventTarget.getPath();
        this.eventSetDescriptor = eventTarget.getEventSetDescriptor();
    }

    public Object getHookup() {
        return hookup;
    }

    public String getEventSetName() {
        char c[] = eventSetName.toCharArray();
        if(Character.isLetter(c[0])) {
            c[0] = Character.toUpperCase(c[0]);
        }
        return new String(c);
    }

    public String getAdapterClassPath() {
        return adapterClassPath;
    }

    public EventSetDescriptor getEventSetDescriptor() {
        return eventSetDescriptor;
    }
}

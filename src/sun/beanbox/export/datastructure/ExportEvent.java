package sun.beanbox.export.datastructure;

import java.beans.EventSetDescriptor;

/**
 * Created by Andi on 03.07.2017.
 */
public class ExportEvent {

    private EventSetDescriptor eventSetDescriptor;
    private BeanNode beanNode;
    private String name;
    private boolean include = true;

    public ExportEvent(EventSetDescriptor eventSetDescriptor, BeanNode beanNode) {
        this.eventSetDescriptor = eventSetDescriptor;
        this.beanNode = beanNode;
    }

    public String getName() {
        if(name == null || name.isEmpty()) {
            return eventSetDescriptor.getName();
        } else {
            return name;
        }
    }

    public BeanNode getBeanNode() {
        return beanNode;
    }

    public EventSetDescriptor getEventSetDescriptor() {
        return eventSetDescriptor;
    }

    @Override
    public String toString() {
        return getName();
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isInclude() {
        return include;
    }

    public void setInclude(boolean include) {
        this.include = include;
    }

    public String uppercaseFirst() {
        char c[] = getName().toCharArray();
        if(Character.isLetter(c[0])) {
            c[0] = Character.toUpperCase(c[0]);
        }
        return new String(c);
    }
}

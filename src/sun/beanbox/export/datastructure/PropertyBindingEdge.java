package sun.beanbox.export.datastructure;

import sun.beanbox.WrapperPropertyEventInfo;
import sunw.beanbox.PropertyHookup;

import java.lang.reflect.Method;

/**
 * Created by Andi on 22.06.2017.
 */
public class PropertyBindingEdge extends BeanEdge {

    private WrapperPropertyEventInfo wrapperPropertyEventInfo;
    private String adapterName;

    public PropertyBindingEdge(BeanNode start, BeanNode end, WrapperPropertyEventInfo wrapperPropertyEventInfo) {
        super(start, end);
        this.wrapperPropertyEventInfo = wrapperPropertyEventInfo;
    }

    public Method getTargetMethod() {
        return wrapperPropertyEventInfo.getSetterMethod();
    }

    public void setAdapterName(String adapterName) {
        this.adapterName = adapterName;
    }

    public String getAdapterName() {
        return adapterName;
    }

    public String getEventSetName() {
        return uppercaseFirst(wrapperPropertyEventInfo.getEventSetName());
    }

    private String uppercaseFirst(String in) {
        char c[] = in.toCharArray();
        if(Character.isLetter(c[0])) {
            c[0] = Character.toUpperCase(c[0]);
        }
        return new String(c);
    }
}

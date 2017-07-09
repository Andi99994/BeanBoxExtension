package sun.beanbox.export.datastructure;

import sun.beanbox.WrapperPropertyEventInfo;
import sun.beanbox.export.util.StringUtil;

import java.lang.reflect.Method;

/**
 * Created by Andreas Ertlschweiger on 22.06.2017.
 * <p>
 * This class describes an edge between two BeanNodes that is given by a property binding.
 */
public class PropertyBindingEdge extends BeanEdge {

    private final WrapperPropertyEventInfo wrapperPropertyEventInfo;
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
        return StringUtil.uppercaseFirst(wrapperPropertyEventInfo.getEventSetName());
    }

}

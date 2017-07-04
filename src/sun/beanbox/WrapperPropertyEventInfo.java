package sun.beanbox;

import sunw.beanbox.PropertyHookup;

import java.lang.reflect.Method;
import java.beans.EventSetDescriptor;

// Subclass of WrapperEventInfo for when the connection is really to
// a "bound property"

public class WrapperPropertyEventInfo extends WrapperEventInfo {

    public WrapperPropertyEventInfo(Object target,
                                    String pname,
                                    Method smethod, PropertyHookup hook) {
        super(target, "sunw.beanbox.PropertyHookup", "propertyChange");
        propertyName = pname;
        setterName = smethod.getName();
        setterTypes = initStringFromType(smethod.getParameterTypes());
        hookup = hook;
        setterMethod = smethod;
    }

    public String getPropertyName() {
        return propertyName;
    }

    public String getSetterName() {
        return setterName;
    }

    public String getSetterTypes() {
        return setterTypes;
    }

    public PropertyHookup getHookup() {
        return hookup;
    }

    public Method getSetterMethod() {
        return setterMethod;
    }

    private String initStringFromType(Class[] klass) {
        StringBuffer buf = new StringBuffer();
        buf.append("new String[] {");
        for (int i=0; i<klass.length; i++) {
            buf.append("\"").append(klass[i].getName()).append("\"");
            if (i != klass.length-1) {
                buf.append(", ");
            }
        }
        buf.append("}");
        return buf.toString();
    }

    private String setterName;
    private String setterTypes;
    private String propertyName;
    private PropertyHookup hookup;
    private Method setterMethod;
}


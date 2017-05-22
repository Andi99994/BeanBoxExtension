package sun.beanbox.export;

import sun.beanbox.Wrapper;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Andreas Ertlschweiger on 06.05.2017.
 */
public class Exporter {

    private HashMap<String, Wrapper> displayBeans = new HashMap<>();

    public Exporter(List<Wrapper> beans) {
        for (Wrapper wrapper : beans) {
            if(displayBeans.get(wrapper.getBeanLabel()) == null) {
                displayBeans.put(wrapper.getBeanLabel(), wrapper);
            } else {
                int counter = 2;
                while(displayBeans.get(wrapper.getBeanLabel()+"(" + counter + ")") != null) {
                    counter++;
                }
                displayBeans.put(wrapper.getBeanLabel()+"("+counter+")", wrapper);
            }
        }
    }

    public HashMap<String, List<String>> getProperties() {
        HashMap<String, List<String>> beans = new HashMap<>();
        for(Map.Entry<String, Wrapper> entry : displayBeans.entrySet()){
            List<String> properties = new ArrayList<>();
            try {
                BeanInfo beanInfo = Introspector.getBeanInfo(entry.getValue().getBean().getClass());
                for(PropertyDescriptor propertyDescriptor : beanInfo.getPropertyDescriptors()) {
                    if(!propertyDescriptor.isHidden() && !propertyDescriptor.isExpert() && propertyDescriptor.getReadMethod() != null && propertyDescriptor.getWriteMethod() != null) {
                        properties.add(propertyDescriptor.getDisplayName());
                    }
                }
            } catch (IntrospectionException e) {
                e.printStackTrace();
            }
            beans.put(entry.getKey(), properties);
        }
        return beans;
    }

    public List<String> getBeans() {
        return new ArrayList<>(displayBeans.keySet());
    }
}

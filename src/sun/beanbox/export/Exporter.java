package sun.beanbox.export;

import sun.beanbox.Wrapper;
import sun.beanbox.WrapperEventInfo;
import sun.beanbox.export.datastructure.BeanGraph;
import sun.beanbox.export.datastructure.BeanNode;
import sun.beanbox.export.datastructure.ExportBean;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by Andreas Ertlschweiger on 06.05.2017.
 */
public class Exporter {

    private HashMap<String, HashMap<String, Wrapper>> exportBeans = new HashMap<>();
    private HashMap<Object, Wrapper> wrapperBeanMap = new HashMap<>();

    public Exporter(List<Wrapper> beans) throws IllegalStateException{
        List<List<Wrapper>> groupedWrappers = createExportBeans(beans);

        //Temporary Code
        int counter = 0;
        for (List<Wrapper> group : groupedWrappers) {
            HashMap<String, Wrapper> wrappers = new HashMap<>();
            exportBeans.put("ExportBean" + counter, wrappers);
            for (Wrapper wrapper : group) {
                if(wrappers.get(wrapper.getBeanLabel()) == null) {
                    wrappers.put(wrapper.getBeanLabel(), wrapper);
                } else {
                    int beanCount = 2;
                    while(wrappers.get(wrapper.getBeanLabel()+"(" + beanCount + ")") != null) {
                        beanCount++;
                    }
                    wrappers.put(wrapper.getBeanLabel()+"("+beanCount+")", wrapper);
                }
            }
            counter++;
        }
        //End Temp
    }

    private List<List<Wrapper>> createExportBeans(List<Wrapper> wrappers) throws IllegalStateException {
        List<List<Wrapper>> groupedWrappers = groupWrappers(wrappers);
        List<ExportBean> exportBeans = new LinkedList<>();

        for (List<Wrapper> group : groupedWrappers) {
            exportBeans.add(assembleExportBean(group));
        }

        return groupedWrappers;
    }

    private ExportBean assembleExportBean(List<Wrapper> wrappers) {
        HashMap<Wrapper, BeanNode> createdNodes = new HashMap<>();
        for (Wrapper wrapper : wrappers) {
            createBeanNode(wrapper, createdNodes);
        }
        List<BeanNode> inputBeans = inferInputBeans(createdNodes);
        List<BeanNode> outputBeans = inferOutputBeans(createdNodes);
        BeanGraph beanGraph = new BeanGraph(inputBeans, outputBeans);
        return null;
    }

    private List<BeanNode> inferInputBeans(HashMap<Wrapper, BeanNode> createdNodes) {
        Set<BeanNode> availableNodes = new HashSet<>(createdNodes.values());
    }

    private BeanNode createBeanNode(Wrapper wrapper, HashMap<Wrapper, BeanNode> createdNodes) {
        if(createdNodes.get(wrapper) != null) {
            return createdNodes.get(wrapper);
        }
        BeanNode beanNode = new BeanNode(wrapper.getBean(), wrapper.getBeanLabel());
        createdNodes.put(wrapper, beanNode);
        List<BeanNode> endNodes = new LinkedList<>();
        for (Object end : wrapper.getListenerBeans()) {
            Wrapper beanWrapper = wrapperBeanMap.get(end);
            if (beanWrapper != null) {
                endNodes.add(createBeanNode(beanWrapper, createdNodes));
            }
        }
        beanNode.setEnd(endNodes);
        return beanNode;
    }

    private List<List<Wrapper>> groupWrappers(List<Wrapper> wrappers) {
        HashMap<Wrapper, Integer> groupMap = new HashMap<>();
        for (Wrapper wrapper : wrappers) {
            wrapperBeanMap.put(wrapper.getBean(), wrapper);
            groupMap.put(wrapper, null);
        }
        int groupCount = 0;
        for (Wrapper wrapper : wrappers) {
            Integer curGroup = groupMap.get(wrapper);
            if (curGroup == null) {
                for (Object bean : wrapper.getListenerBeans()) {
                    Wrapper beanWrapper = wrapperBeanMap.get(bean);
                    if (beanWrapper != null && groupMap.get(beanWrapper) != null) {
                        curGroup = groupMap.get(beanWrapper);
                    }
                }
            }
            if (curGroup == null) {
                curGroup = groupCount;
                groupCount++;
            }
            groupMap.replace(wrapper, curGroup);
            for (Object bean : wrapper.getListenerBeans()) {
                Wrapper beanWrapper = wrapperBeanMap.get(bean);
                if (beanWrapper != null) {
                    groupMap.replace(beanWrapper, curGroup);
                }
            }
        }
        HashMap<Integer, List<Wrapper>> groupedWrappers = new HashMap<>();
        for (Map.Entry<Wrapper, Integer> entry : groupMap.entrySet()) {
            if (groupedWrappers.containsKey(entry.getValue())) {
                groupedWrappers.get(entry.getValue()).add(entry.getKey());
            } else {
                groupedWrappers.put(entry.getValue(), new LinkedList<>());
                groupedWrappers.get(entry.getValue()).add(entry.getKey());
            }
        }
        return new ArrayList<>(groupedWrappers.values());
    }

    //Temporary Code
    public HashMap<String, HashMap<String, List<String>>> getProperties() {
        HashMap<String, HashMap<String, List<String>>> beans = new HashMap<>();
        for (Map.Entry<String, HashMap<String, Wrapper>> entry : exportBeans.entrySet()) {
            HashMap<String, List<String>> beanPropertyMap = new HashMap<>();
            beans.put(entry.getKey(), beanPropertyMap);
            for (Map.Entry<String, Wrapper> beanEntry : entry.getValue().entrySet()) {
                List<String> properties = new ArrayList<>();
                try {
                    BeanInfo beanInfo = Introspector.getBeanInfo(beanEntry.getValue().getBean().getClass());
                    for(PropertyDescriptor propertyDescriptor : beanInfo.getPropertyDescriptors()) {
                        if(!propertyDescriptor.isHidden() && !propertyDescriptor.isExpert() && propertyDescriptor.getReadMethod() != null && propertyDescriptor.getWriteMethod() != null) {
                            properties.add(propertyDescriptor.getDisplayName());
                        }
                    }
                } catch (IntrospectionException e) {
                    e.printStackTrace();
                }
                beanPropertyMap.put(beanEntry.getKey(), properties);
            }
        }
        return beans;
    }

    public HashMap<String, HashMap<String, Wrapper>> getBeans() {
        return exportBeans;
    }
    //End Temp
}

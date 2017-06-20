package sun.beanbox.export;

import sun.beanbox.Wrapper;
import sun.beanbox.WrapperEventInfo;
import sun.beanbox.export.components.NodeSelector;
import sun.beanbox.export.datastructure.*;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by Andreas Ertlschweiger on 06.05.2017.
 */
public class Exporter {

    private HashMap<Object, Wrapper> wrapperBeanMap = new HashMap<>();
    private List<ExportBean> exportBeans = new LinkedList<>();

    private File saveDirectory = new File(".");

    public Exporter(List<Wrapper> beans) throws IntrospectionException, IllegalArgumentException, InvocationTargetException, IllegalAccessException {
        for (List<Wrapper> group : groupWrappers(beans)) {
            exportBeans.add(assembleExportBean(group, "ExportBean" + exportBeans.size()));
        }
    }

    private ExportBean assembleExportBean(List<Wrapper> wrappers, String name) throws IntrospectionException, IllegalArgumentException, InvocationTargetException, IllegalAccessException {
        HashMap<Wrapper, BeanNode> createdNodes = new HashMap<>();
        for (Wrapper wrapper : wrappers) {
            createBeanNode(wrapper, createdNodes);
        }
        List<BeanNode> inputBeans = inferInputBeans(createdNodes);
        List<BeanNode> outputBeans = inferOutputBeans(createdNodes);
        BeanGraph beanGraph = new BeanGraph(inputBeans, outputBeans, createdNodes.values());
        return new ExportBean(beanGraph, name);
    }

    private List<BeanNode> inferOutputBeans(HashMap<Wrapper, BeanNode> createdNodes) throws IllegalArgumentException {
        List<BeanNode> availableNodes = new LinkedList<>();
        for (BeanNode node : createdNodes.values()) {
            if (node.getEdges().isEmpty()) {
                availableNodes.add(node);
            }
        }
        if(availableNodes.isEmpty()) {
            availableNodes.addAll(createdNodes.values());
            new NodeSelector(null, availableNodes, "Could not infer output Beans (maybe you have cyclic references in your composition?). Please select from the list below.").show();
        }
        if (availableNodes.isEmpty()) {
            throw new IllegalArgumentException("Cannot export without selection of at least one output node");
        }
        return availableNodes;
    }

    private List<BeanNode> inferInputBeans(HashMap<Wrapper, BeanNode> createdNodes) throws IllegalArgumentException {
        Set<BeanNode> availableNodes = new HashSet<>(createdNodes.values());
        for (BeanNode node : createdNodes.values()) {
            for(BeanEdge edge : node.getEdges()) {
                if(availableNodes.contains(edge.getEnd())) {
                    availableNodes.remove(edge.getEnd());
                }
            }
        }
        if(availableNodes.isEmpty()) {
            List<BeanNode> availableBeans = new LinkedList<>(createdNodes.values());
            new NodeSelector(null, availableBeans, "Could not infer input Beans (maybe you have cyclic references in your composition?). Please select from the list below.").show();
            availableNodes.addAll(availableBeans);
        }
        if (availableNodes.isEmpty()) {
            throw new IllegalArgumentException("Cannot export without selection of at least one input node");
        }
        return new ArrayList<>(availableNodes);
    }

    private BeanNode createBeanNode(Wrapper wrapper, HashMap<Wrapper, BeanNode> createdNodes) throws IntrospectionException, InvocationTargetException, IllegalAccessException {
        if(createdNodes.get(wrapper) != null) {
            return createdNodes.get(wrapper);
        }
        BeanNode beanNode = new BeanNode(wrapper.getBean(), wrapper.getBeanLabel());
        BeanInfo beanInfo = Introspector.getBeanInfo(beanNode.getData().getClass());
        for (PropertyDescriptor propertyDescriptor : beanInfo.getPropertyDescriptors()) {
            if (!propertyDescriptor.isHidden() && !propertyDescriptor.isExpert() && propertyDescriptor.getReadMethod() != null && propertyDescriptor.getWriteMethod() != null) {
                beanNode.getProperties().add(new ExportProperty(propertyDescriptor, beanNode));
            }
        }
        createdNodes.put(wrapper, beanNode);
        for (Object end : wrapper.getDirectTargets()) {
            Wrapper beanWrapper = wrapperBeanMap.get(end);
            if (beanWrapper != null) {
                BeanNode childNode = createBeanNode(beanWrapper, createdNodes);
                beanNode.addEdge(new BeanEdge(beanNode, childNode, BeanEdge.CompositionType.DIRECT));
            }
        }
        for (Object end : wrapper.getEventHookupTargets()) {
            Wrapper beanWrapper = wrapperBeanMap.get(end);
            if (beanWrapper != null) {
                BeanNode childNode = createBeanNode(beanWrapper, createdNodes);
                beanNode.addEdge(new BeanEdge(beanNode, childNode, BeanEdge.CompositionType.HOOKUP));
            }
        }
        for (Object end : wrapper.getPropertyTargets()) {
            Wrapper beanWrapper = wrapperBeanMap.get(end);
            if (beanWrapper != null) {
                BeanNode childNode = createBeanNode(beanWrapper, createdNodes);
                beanNode.addEdge(new BeanEdge(beanNode, childNode, BeanEdge.CompositionType.PROPERTY));
            }
        }
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
                for (Object bean : wrapper.getCompositionTargets()) {
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
            for (Object bean : wrapper.getCompositionTargets()) {
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

    public List<ExportBean> getBeans() {
        return exportBeans;
    }

    public File getSaveDirectory() {
        return saveDirectory;
    }

    public void setSaveDirectory(File saveDirectory) {
        this.saveDirectory = saveDirectory;
    }
}

package sun.beanbox.export.datastructure;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by Andreas Ertlschweiger on 26.05.2017.
 * <p>
 * This class describes a bean that is composed of one or more beans which are in relation to each other.
 */
public class ExportBean {

    private String name;
    private List<BeanNode> beans = new LinkedList<>();
    private boolean addPropertyChangeSupport = false;

    public ExportBean(Collection<BeanNode> inputNodes, Collection<BeanNode> outputNodes, Collection<BeanNode> allNodes, String name) {
        this.name = name;
        this.beans.addAll(allNodes);
        inputNodes.forEach(beanNode -> beanNode.setInputInterface(true));
        outputNodes.forEach(beanNode -> beanNode.setOutputInterface(true));
        Set<String> beanNames = new HashSet<>();
        for (BeanNode node : allNodes) {
            String beanName = node.getName();
            int counter = 2;
            if (beanNames.contains(beanName)) {
                while (beanNames.contains(beanName + counter)) {
                    counter++;
                }
                beanName += counter;
                node.setName(beanName);
            }
            beanNames.add(beanName);
        }
    }

    private List<BeanNode> getInputNodes() {
        return beans.stream().filter(BeanNode::isInputInterface).collect(Collectors.toList());
    }

    private List<BeanNode> getOutputNodes() {
        return beans.stream().filter(BeanNode::isOutputInterface).collect(Collectors.toList());
    }

    public List<BeanNode> getBeans() {
        return beans;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<ExportProperty> getProperties() {
        List<ExportProperty> exportProperties = new ArrayList<>();
        for (BeanNode node : beans) {
            for (ExportProperty property : node.getProperties()) {
                if (property.isExport()) {
                    exportProperties.add(property);
                }
            }
        }
        return exportProperties;
    }

    public List<ExportMethod> getMethods() {
        List<ExportMethod> exportMethods = new ArrayList<>();
        for (BeanNode node : getInputNodes()) {
            for (ExportMethod method : node.getMethods()) {
                if (method.isExport()) {
                    exportMethods.add(method);
                }
            }
        }
        return exportMethods;
    }

    public List<ExportEvent> getEvents() {
        List<ExportEvent> exportEvents = new ArrayList<>();
        for (BeanNode node : getOutputNodes()) {
            for (ExportEvent event : node.getEvents()) {
                if (event.isExport()) {
                    exportEvents.add(event);
                }
            }
        }
        return exportEvents;
    }

    public boolean isAddPropertyChangeSupport() {
        return addPropertyChangeSupport;
    }

    public void setAddPropertyChangeSupport(boolean addPropertyChangeSupport) {
        this.addPropertyChangeSupport = addPropertyChangeSupport;
    }

    public List<Class> getImplementedInterfaces() {
        return getMethods().stream().filter(exportMethod -> exportMethod.getDeclaringClass() != null).map(ExportMethod::getDeclaringClass).collect(Collectors.toList());
    }

    @Override
    public String toString() {
        return name;
    }
}

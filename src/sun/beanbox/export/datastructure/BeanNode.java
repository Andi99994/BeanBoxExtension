package sun.beanbox.export.datastructure;

import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by Andreas Ertlschweiger on 26.05.2017.
 * <p>
 * This class describes a node in the Bean graph. It contains information about the bean and various parameters regarding
 * the export process.
 */
public class BeanNode {

    private String name;
    private final Object data;
    private List<BeanEdge> edges = new LinkedList<>();
    private List<ExportProperty> properties = new LinkedList<>();
    private List<ExportMethod> methods = new LinkedList<>();
    private List<ExportEvent> events = new LinkedList<>();
    private boolean registerInManifest = false;
    private boolean inputInterface = false;
    private boolean outputInterface = false;
    private String jarPath;

    public BeanNode(Object data, String displayName) {
        this.name = displayName;
        this.data = data;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Object getData() {
        return data;
    }

    public List<BeanEdge> getEdges() {
        return edges;
    }

    public List<DirectCompositionEdge> getDirectCompositionEdges() {
        return edges.stream().filter(beanEdge -> beanEdge instanceof DirectCompositionEdge)
                .map(beanEdge -> (DirectCompositionEdge) beanEdge).collect(Collectors.toList());
    }

    public List<AdapterCompositionEdge> getAdapterCompositionEdges() {
        return edges.stream().filter(beanEdge -> beanEdge instanceof AdapterCompositionEdge)
                .map(beanEdge -> (AdapterCompositionEdge) beanEdge).collect(Collectors.toList());
    }

    public List<PropertyBindingEdge> getPropertyBindingEdges() {
        return edges.stream().filter(beanEdge -> beanEdge instanceof PropertyBindingEdge)
                .map(beanEdge -> (PropertyBindingEdge) beanEdge).collect(Collectors.toList());
    }

    public void addEdge(BeanEdge edge) {
        edges.add(edge);
    }

    public List<ExportProperty> getProperties() {
        return properties;
    }

    public boolean isRegisterInManifest() {
        return registerInManifest;
    }

    public void setRegisterInManifest(boolean registerInManifest) {
        this.registerInManifest = registerInManifest;
    }

    @Override
    public String toString() {
        return name;
    }

    public List<ExportMethod> getMethods() {
        return methods;
    }

    public String getJarPath() {
        return jarPath;
    }

    public void setJarPath(String jarPath) {
        this.jarPath = jarPath;
    }

    public List<ExportEvent> getEvents() {
        return events;
    }

    public boolean isInputInterface() {
        return inputInterface;
    }

    public void setInputInterface(boolean inputInterface) {
        this.inputInterface = inputInterface;
    }

    public boolean isOutputInterface() {
        return outputInterface;
    }

    public void setOutputInterface(boolean outputInterface) {
        this.outputInterface = outputInterface;
    }

    public boolean isAllPropertiesConfigurable() {
        for (ExportProperty property : properties) {
            if (!property.isExport()) {
                return false;
            }
        }
        return true;
    }

    public void setAllPropertiesConfigurable(boolean configurable) {
        for (ExportProperty property : properties) {
            property.setExport(configurable);
        }
    }

    public boolean isAllEventsInOutputInterface() {
        for (ExportEvent event : events) {
            if (!event.isExport()) {
                return false;
            }
        }
        return true;
    }

    public void setAllEventsInOutputInterface(boolean included) {
        for (ExportEvent event : events) {
            event.setExport(included);
        }
    }

    public boolean isAllMethodsInInputInterface() {
        for (ExportMethod method : methods) {
            if (!method.isExport()) {
                return false;
            }
        }
        return true;
    }

    public void setAllMethodsInInputInterface(boolean included) {
        for (ExportMethod method : methods) {
            method.setExport(included);
        }
    }

    /**
     * Sorts properties, methods and events alphabetically so it is easier for the user to find entries.
     */
    public void sortData() {
        properties.sort(Comparator.comparing(ExportFeature::getName));
        methods.sort(Comparator.comparing(ExportFeature::getName));
        events.sort(Comparator.comparing(ExportFeature::getName));
    }
}

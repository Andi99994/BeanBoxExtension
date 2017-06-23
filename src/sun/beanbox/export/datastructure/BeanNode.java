package sun.beanbox.export.datastructure;

import java.beans.IntrospectionException;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by Andi on 26.05.2017.
 */
public class BeanNode {

    private String name;
    private Object data;
    private List<BeanEdge> edges = new LinkedList<>();
    private List<ExportProperty> properties = new LinkedList<>();
    private List<ExportMethod> methods = new LinkedList<>();
    private boolean registerInManifest = false;

    public BeanNode(Object data, String displayName) throws IntrospectionException {
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
                .map(beanEdge -> (DirectCompositionEdge)beanEdge).collect(Collectors.toList());
    }

    public List<AdapterCompositionEdge> getAdapterCompositionEdges() {
        return edges.stream().filter(beanEdge -> beanEdge instanceof AdapterCompositionEdge)
                .map(beanEdge -> (AdapterCompositionEdge)beanEdge).collect(Collectors.toList());
    }

    public List<PropertyBindingEdge> getPropertyBindingEdges() {
        return edges.stream().filter(beanEdge -> beanEdge instanceof PropertyBindingEdge)
                .map(beanEdge -> (PropertyBindingEdge)beanEdge).collect(Collectors.toList());
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

    public String lowercaseFirst() {
        char c[] = getName().toCharArray();
        if(Character.isLetter(c[0])) {
            c[0] = Character.toLowerCase(c[0]);
        }
        return new String(c);
    }
}

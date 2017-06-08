package sun.beanbox.export.datastructure;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by Andi on 26.05.2017.
 */
public class BeanNode {

    private String displayName;
    private Object data;
    private List<BeanEdge> edges = new LinkedList<>();
    private List<ExportProperty> properties = new LinkedList<>();

    public BeanNode(Object data, String displayName) throws IntrospectionException {
        this.displayName = displayName;
        this.data = data;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public Object getData() {
        return data;
    }

    public List<BeanEdge> getEdges() {
        return edges;
    }

    public void addEdge(BeanEdge edge) {
        edges.add(edge);
    }

    public List<ExportProperty> getProperties() {
        return properties;
    }

    public String toString() {
        return displayName;
    }
}

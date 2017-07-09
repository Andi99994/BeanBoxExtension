package sun.beanbox.export.datastructure;

/**
 * Created by Andreas Ertlschweiger on 09.07.2017.
 * <p>
 * This class describes the basic fields and methods for any exportable feature(properties, events, methods) of a BeanNode.
 */
public abstract class ExportFeature {

    private String name;
    private final BeanNode node;
    private boolean export = true;

    ExportFeature(BeanNode node) {
        this.node = node;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public BeanNode getNode() {
        return node;
    }

    public boolean isExport() {
        return export;
    }

    public void setExport(boolean export) {
        this.export = export;
    }

    @Override
    public String toString() {
        return getName();
    }
}

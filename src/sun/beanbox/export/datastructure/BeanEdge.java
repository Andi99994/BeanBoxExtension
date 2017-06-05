package sun.beanbox.export.datastructure;

/**
 * Created by Andi on 05.06.2017.
 */
public class BeanEdge {

    private BeanNode start;
    private BeanNode end;
    private CompositionType compositionType;

    public BeanEdge(BeanNode start, BeanNode end, CompositionType type) {
        this.start = start;
        this.end = end;
        this.compositionType = type;
    }

    public BeanNode getStart() {
        return start;
    }

    public void setStart(BeanNode start) {
        this.start = start;
    }

    public BeanNode getEnd() {
        return end;
    }

    public void setEnd(BeanNode end) {
        this.end = end;
    }

    public CompositionType getCompositionType() {
        return compositionType;
    }

    public void setCompositionType(CompositionType compositionType) {
        this.compositionType = compositionType;
    }

    public enum CompositionType {
        DIRECT, HOOKUP, PROPERTY
    }
}

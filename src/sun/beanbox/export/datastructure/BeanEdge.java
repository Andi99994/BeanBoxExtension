package sun.beanbox.export.datastructure;

/**
 * Created by Andreas Ertlschweiger on 05.06.2017.
 * <p>
 * This class describes the basic fields and methods that every edge between two BeanNodes has.
 */
public abstract class BeanEdge {

    private final BeanNode start;
    private final BeanNode end;

    BeanEdge(BeanNode start, BeanNode end) {
        this.start = start;
        this.end = end;
    }

    public BeanNode getStart() {
        return start;
    }

    public BeanNode getEnd() {
        return end;
    }
}

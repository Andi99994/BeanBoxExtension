package sun.beanbox.export.datastructure;

/**
 * Created by Andi on 05.06.2017.
 */
public abstract class BeanEdge {

    private BeanNode start;
    private BeanNode end;

    public BeanEdge(BeanNode start, BeanNode end) {
        this.start = start;
        this.end = end;
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
}

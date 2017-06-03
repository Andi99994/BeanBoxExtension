package sun.beanbox.export.datastructure;

import java.util.List;

/**
 * Created by Andi on 26.05.2017.
 */
public class BeanNode {

    private String displayName;
    private Object data;
    private List<BeanNode> end;

    public BeanNode(Object start, String displayName, List<BeanNode> end){
        this.data = start;
        this.end = end;
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

    public void setData(Object data) {
        this.data = data;
    }

    public List<BeanNode> getEnd() {
        return end;
    }

    public void setEnd(List<BeanNode> end) {
        this.end = end;
    }
}

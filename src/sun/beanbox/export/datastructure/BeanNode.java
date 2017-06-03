package sun.beanbox.export.datastructure;

import java.util.List;

/**
 * Created by Andi on 26.05.2017.
 */
public class BeanNode {

    private String displayName;
    private Object data;
    private List<BeanNode> end;

    public BeanNode(Object data, String displayName){
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

    public List<BeanNode> getEnd() {
        return end;
    }

    public void setEnd(List<BeanNode> end) {
        this.end = end;
    }
}

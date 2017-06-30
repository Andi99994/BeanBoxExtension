package sun.beanbox;

import java.io.Serializable;

// Class to hold state on event listener hookup for which this
// Wrapper's bean is a source.
// Andreas Ertlschweiger 2017: moved
public class WrapperEventTarget implements Serializable {
    private static final long serialVersionUID = 4831901854891942741L;

    private String eventSetName;
    private Object targetBean;
    private Object targetListener;
    private String path;

    public static long getSerialVersionUID() {
        return serialVersionUID;
    }

    public String getEventSetName() {
        return eventSetName;
    }

    public void setEventSetName(String eventSetName) {
        this.eventSetName = eventSetName;
    }

    public Object getTargetBean() {
        return targetBean;
    }

    public void setTargetBean(Object targetBean) {
        this.targetBean = targetBean;
    }

    public Object getTargetListener() {
        return targetListener;
    }

    public void setTargetListener(Object targetListener) {
        this.targetListener = targetListener;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }
}
package sun.beanbox.export.datastructure;

import java.util.LinkedList;
import java.util.List;

/**
 * Created by Andi on 26.05.2017.
 */
public class ExportBean {

    private String beanName;
    private BeanGraph beans;
    private List<ExportProperty> properties = new LinkedList<>();
    private List<ExportMethod> methods = new LinkedList<>();

    public ExportBean(BeanGraph beans, String beanName) {
        this.beans = beans;
        this.beanName = beanName;
        beans.getAllNodes().forEach(bean -> properties.addAll(bean.getProperties()));
    }

    public BeanGraph getBeans() {
        return beans;
    }

    public void setBean(BeanGraph beans) {
        this.beans = beans;
    }

    public String getBeanName() {
        return beanName;
    }

    public void setBeanName(String beanName) {
        this.beanName = beanName;
    }

    public List<ExportProperty> getProperties() {
        return properties;
    }

    @Override
    public String toString() {
        return beanName;
    }
}

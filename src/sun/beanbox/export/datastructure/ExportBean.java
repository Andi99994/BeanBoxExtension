package sun.beanbox.export.datastructure;

import java.util.List;

/**
 * Created by Andi on 26.05.2017.
 */
public class ExportBean {

    private String beanName;
    private BeanGraph beans;
    private List<ExportProperty> properties;
    private List<ExportMethod> methods;

    public ExportBean(BeanGraph beans, String beanName, List<ExportProperty> properties, List<ExportMethod> methods) {
        this.beans = beans;
        this.beanName = beanName;
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
}

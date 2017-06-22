package sun.beanbox.export.datastructure;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by Andi on 26.05.2017.
 */
public class ExportBean {

    private String beanName;
    private BeanGraph beans;
    private List<ExportMethod> methods = new LinkedList<>();

    public ExportBean(BeanGraph beans, String beanName) {
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

    public List<ExportProperty> getProperties() {
        List<ExportProperty> exportProperties = new ArrayList<>();
        for (BeanNode node : beans.getAllNodes()) {
            for(ExportProperty property : node.getProperties()) {
                if (property.isExport()) {
                    exportProperties.add(property);
                }
            }
        }
        return exportProperties;
    }

    @Override
    public String toString() {
        return beanName;
    }
}

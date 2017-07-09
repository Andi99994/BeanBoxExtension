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
    private boolean addPropertyChangeSupport = false;

    public ExportBean(BeanGraph beans, String beanName) {
        this.beans = beans;
        this.beanName = beanName;
    }

    public List<BeanNode> getBeans() {
        return beans.getAllNodes();
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

    public List<ExportMethod> getMethods() {
        List<ExportMethod> exportMethods = new ArrayList<>();
        for (BeanNode node : beans.getInputNodes()) {
            for(ExportMethod method : node.getMethods()) {
                if (method.isInclude()) {
                    exportMethods.add(method);
                }
            }
        }
        return exportMethods;
    }

    public List<ExportEvent> getEvents() {
        List<ExportEvent> exportEvents = new ArrayList<>();
        for (BeanNode node : beans.getOutputNodes()) {
            for(ExportEvent event : node.getEvents()) {
                if (event.isInclude()) {
                    exportEvents.add(event);
                }
            }
        }
        return exportEvents;
    }

    public boolean isAddPropertyChangeSupport() {
        return addPropertyChangeSupport;
    }

    public void setAddPropertyChangeSupport(boolean addPropertyChangeSupport) {
        this.addPropertyChangeSupport = addPropertyChangeSupport;
    }

    @Override
    public String toString() {
        return beanName;
    }
}

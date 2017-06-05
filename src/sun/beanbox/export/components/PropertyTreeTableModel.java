package sun.beanbox.export.components;

import sun.beanbox.export.datastructure.BeanNode;
import sun.beanbox.export.datastructure.ExportBean;
import sun.beanbox.export.datastructure.ExportProperty;


/**
 * Created by Andi on 05.06.2017.
 */
public class PropertyTreeTableModel extends AbstractTreeTableModel {

    private String[] columnNames = {"Node", "Default Value", "Configurable"};
    private Class<?>[] columnTypes = { TreeTableModel.class, ExportProperty.class, Boolean.class};

    public PropertyTreeTableModel(ExportBean bean) {
        super(bean);
        root = bean;
    }

    public Object getChild(Object parent, int index) {
        if(parent instanceof ExportBean) {
            return ((ExportBean) parent).getBeans().getAllNodes().get(index);
        } else if (parent instanceof BeanNode) {
            return ((BeanNode) parent).getProperties().get(index);
        }
        return null;
    }


    public int getChildCount(Object parent) {
        if(parent instanceof ExportBean) {
            return ((ExportBean) parent).getBeans().getAllNodes().size();
        } else if (parent instanceof BeanNode) {
            return ((BeanNode) parent).getProperties().size();
        }
        return 0;
    }


    public int getColumnCount() {
        return columnNames.length;
    }


    public String getColumnName(int column) {
        return columnNames[column];
    }


    public Class<?> getColumnClass(int column) {
        return columnTypes[column];
    }

    public Object getValueAt(Object node, int column) {
        if(node instanceof ExportBean && column == 0) {
            return ((ExportBean) node).getBeanName();
        } else if (node instanceof BeanNode && column == 0) {
            return ((BeanNode) node).getDisplayName();
        } else if (node instanceof ExportProperty) {
            ExportProperty property = (ExportProperty) node;
            switch (column) {
                case 0:
                    return property.getName();
                case 1:
                    return property;
                case 2:
                    return property.isExport();
            }
        }
        return null;
    }

    public boolean isCellEditable(Object node, int column) {
        return column == 0; // Important to activate TreeExpandListener
    }

    public void setValueAt(Object aValue, Object node, int column) {
    }
}

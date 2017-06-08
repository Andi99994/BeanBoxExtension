package sun.beanbox.export.components;

import javafx.scene.control.Label;
import javafx.scene.control.TreeTableCell;
import javafx.scene.layout.Pane;
import sun.beanbox.export.datastructure.ExportProperty;

import java.lang.reflect.InvocationTargetException;

/**
 * Created by Andi on 08.06.2017.
 */
public class PropertyTreeTableCell extends TreeTableCell<Object, Object> {

    private Pane propertyPane;

    @Override
    protected void updateItem(Object item, boolean empty) {
        super.updateItem(item, empty);
        if (item == null || empty) {
            setGraphic(null);
            return;
        }
        if (getTreeTableRow() != null && getTreeTableRow().getTreeItem() != null
                && getTreeTableRow().getTreeItem().getValue() != null
                && getTreeTableRow().getTreeItem().getValue() instanceof ExportProperty) {
            propertyPane = new Pane();
            ExportProperty property = (ExportProperty) getTreeTableRow().getTreeItem().getValue();
            Label label = new Label("Could not read property");
            try {
                Object propertyValue = property.getPropertyDescriptor().getReadMethod().invoke(property.getNode().getData());
                label.setText(propertyValue.toString());
            } catch (IllegalAccessException | InvocationTargetException e) {
                e.printStackTrace();
            }
            propertyPane.getChildren().add(label);
            setGraphic(propertyPane);
        }
    }
}

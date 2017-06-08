package sun.beanbox.export;


import javafx.application.Platform;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.embed.swing.JFXPanel;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.*;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.util.Callback;
import sun.beanbox.export.components.PropertyTreeTableCell;
import sun.beanbox.export.datastructure.BeanNode;
import sun.beanbox.export.datastructure.ExportBean;
import sun.beanbox.export.datastructure.ExportProperty;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.List;


/**
 * Created by Andreas Ertlschweiger on 06.05.2017.
 */
public class ExportDialog extends JDialog {

    private Frame owner;

    private Exporter exporter;

    public ExportDialog(Frame owner, Exporter exporter) {
        super(owner, "Bean Export", true);
        this.owner = owner;
        this.exporter = exporter;
        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        setSize(new Dimension(800, 600));
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent windowEvent) {
                int result = showExitDialog();
                if (result == JOptionPane.YES_OPTION) {
                    dispose();
                }
            }
        });

        final JFXPanel fxPanel = new JFXPanel();
        add(fxPanel);
        final BorderPane root = new BorderPane();
        root.setPrefSize(Region.USE_COMPUTED_SIZE, Region.USE_COMPUTED_SIZE);
        root.setMaxSize(Region.USE_COMPUTED_SIZE, Region.USE_COMPUTED_SIZE);
        root.setMinSize(Region.USE_COMPUTED_SIZE, Region.USE_COMPUTED_SIZE);
        Platform.setImplicitExit(false);
        Platform.runLater(() -> {
            Scene scene = new Scene(root, Color.ALICEBLUE);
            fxPanel.setScene(scene);
            initUI(root);
        });
    }

    private int showExitDialog() {
        return JOptionPane.showConfirmDialog(null,
                "Do you want to cancel the export?", "Cancel", JOptionPane.YES_NO_OPTION);
    }

    private void initUI(BorderPane root) {
        TabPane tabPane = new TabPane();
        HBox controlBox = new HBox();
        controlBox.setPadding(new Insets(5, 5, 5, 5));
        root.setCenter(tabPane);
        root.setBottom(controlBox);

        tabPane.setPrefSize(Region.USE_COMPUTED_SIZE, Region.USE_COMPUTED_SIZE);
        tabPane.setMaxSize(Region.USE_COMPUTED_SIZE, Region.USE_COMPUTED_SIZE);
        tabPane.setMinSize(Region.USE_COMPUTED_SIZE, Region.USE_COMPUTED_SIZE);

        HBox locationBox = new HBox();
        locationBox.setSpacing(3);
        Label locationText = new Label();

        locationText.setStyle("-fx-border-color: black;");
        locationBox.getChildren().add(locationText);
        controlBox.getChildren().add(locationBox);

        HBox buttonBox = new HBox();
        buttonBox.setAlignment(Pos.CENTER_RIGHT);
        buttonBox.setSpacing(3);
        controlBox.getChildren().add(buttonBox);
        Button exportButton = new Button("Export");
        Button cancelButton = new Button("Cancel");
        buttonBox.getChildren().add(exportButton);
        buttonBox.getChildren().add(cancelButton);
        exportButton.setOnAction(event -> {
            //TODO: Export
        });
        cancelButton.setOnAction(event -> SwingUtilities.invokeLater(() -> {
            int result = showExitDialog();
            if (result == JOptionPane.YES_OPTION) {
                dispose();
            }
        }));

        HBox.setHgrow(locationBox, Priority.ALWAYS);
        HBox.setHgrow(buttonBox, Priority.ALWAYS);

        Tab beanTab = new Tab("Beans");
        beanTab.setClosable(false);
        beanTab.setContent(getBeanPane());
        Tab propertyTab = new Tab("Properties");
        propertyTab.setClosable(false);
        propertyTab.setContent(getPropertiesPane());
        Tab methodTab = new Tab("Methods");
        methodTab.setClosable(false);
        tabPane.getTabs().addAll(beanTab, propertyTab, methodTab);
    }

    private BorderPane getBeanPane() {
        BorderPane pane = new BorderPane();
        pane.setTop(new Label("The following Beans will be generated:"));

        TreeItem<Object> rootNode = new TreeItem<>();
        List<ExportBean> exportBeanList = new ArrayList<>(exporter.getBeans());
        for (ExportBean bean : exportBeanList) {
            TreeItem<Object> item = new TreeItem<>(bean);
            for (BeanNode node : bean.getBeans().getAllNodes()) {
                item.getChildren().add(new TreeItem<>(node));
            }
            rootNode.getChildren().add(item);
        }

        TreeView<Object> treeView = new TreeView<>(rootNode);
        treeView.setShowRoot(false);
        pane.setCenter(treeView);

        return pane;
    }

    private BorderPane getPropertiesPane() {
        BorderPane pane = new BorderPane();
        pane.setTop(new Label("The following Properties will be generated:"));

        TreeItem<Object> rootNode = new TreeItem<>();
        List<ExportBean> exportBeanList = new ArrayList<>(exporter.getBeans());
        for (ExportBean bean : exportBeanList) {
            TreeItem<Object> second = new TreeItem<>(bean);
            for (BeanNode node : bean.getBeans().getAllNodes()) {
                TreeItem<Object> third = new TreeItem<>(node);
                for (ExportProperty property : node.getProperties()) {
                    third.getChildren().add(new TreeItem<>(property));
                }
                second.getChildren().add(third);
            }
            rootNode.getChildren().add(second);
        }

        TreeTableColumn<Object, String> nameColumn = new TreeTableColumn<>("Node");
        nameColumn.setPrefWidth(150);
        nameColumn.setCellValueFactory((TreeTableColumn.CellDataFeatures<Object, String> param) -> {
            Object node = param.getValue().getValue();
            String result = null;
            if (node instanceof ExportBean) {
                result = ((ExportBean) node).getBeanName();
            } else if (node instanceof BeanNode) {
                result = ((BeanNode) node).getDisplayName();
            } else if (node instanceof ExportProperty) {
                result = ((ExportProperty) node).getName();
            }
            return result != null ? new ReadOnlyStringWrapper(result) : null;
        });

        TreeTableColumn<Object, Object> valueColumn = new TreeTableColumn<>();
        Label valueLabel = new Label("Default Value");
        valueLabel.setTooltip(new Tooltip("You can configure any default values you want to set"));
        valueColumn.setGraphic(valueLabel);
        valueColumn.setPrefWidth(150);
        valueColumn.setCellValueFactory((TreeTableColumn.CellDataFeatures<Object, Object> param) -> {
            Object node = param.getValue().getValue();
            if (node instanceof ExportProperty) {
                return new ReadOnlyObjectWrapper<>(node);
            }
            return null;
        });
        valueColumn.setCellFactory(param -> new PropertyTreeTableCell());

        TreeTableColumn<Object, Boolean> configColumn = new TreeTableColumn<>();
        Label configLabel = new Label("Configurable");
        configLabel.setTooltip(new Tooltip("If checked, the property will be configurable after export"));
        configColumn.setGraphic(configLabel);
        configColumn.setMinWidth(100);
        configColumn.setMaxWidth(100);
        configColumn.setCellValueFactory(param -> {
            Object node = param.getValue().getValue();
            if (node instanceof ExportProperty) {
                return new ReadOnlyBooleanWrapper(((ExportProperty) node).isExport());
            }
            return null;
        });
        configColumn.setCellFactory(new Callback<TreeTableColumn<Object, Boolean>, TreeTableCell<Object, Boolean>>() {
            @Override
            public TreeTableCell<Object, Boolean> call(TreeTableColumn<Object, Boolean> param) {
                return new TreeTableCell<Object, Boolean>() {
                    CheckBox checkBox = new CheckBox();

                    @Override
                    protected void updateItem(Boolean item, boolean empty) {
                        super.updateItem(item, empty);
                        if (item == null || empty) {
                            setGraphic(null);
                            return;
                        }
                        if (getTreeTableRow() != null && getTreeTableRow().getTreeItem() != null
                                && getTreeTableRow().getTreeItem().getValue() != null
                                && getTreeTableRow().getTreeItem().getValue() instanceof ExportProperty) {
                            ExportProperty property = (ExportProperty) getTreeTableRow().getTreeItem().getValue();
                            checkBox.setSelected(property.isExport());
                            checkBox.setOnAction(event -> property.setExport(checkBox.isSelected()));
                            setGraphic(checkBox);
                        }
                    }
                };
            }
        });

        TreeTableView<Object> treeView = new TreeTableView<>(rootNode);
        treeView.setShowRoot(false);
        treeView.getColumns().addAll(nameColumn, valueColumn, configColumn);
        treeView.setColumnResizePolicy(TreeTableView.CONSTRAINED_RESIZE_POLICY);
        pane.setCenter(treeView);

        return pane;
    }
}

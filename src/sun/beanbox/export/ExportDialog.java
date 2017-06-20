package sun.beanbox.export;

import sun.beanbox.PropertyCanvas;
import sun.beanbox.PropertyDialog;
import sun.beanbox.PropertySelector;
import sun.beanbox.PropertyText;
import sun.beanbox.export.components.PropertyDisplayCanvas;
import sun.beanbox.export.components.PropertyEditorComponent;
import sun.beanbox.export.components.PropertyValueType;
import sun.beanbox.export.datastructure.BeanNode;
import sun.beanbox.export.datastructure.ExportBean;
import sun.beanbox.export.datastructure.ExportProperty;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

/**
 * Created by Andreas Ertlschweiger on 06.05.2017.
 */
public class ExportDialog extends JDialog {

    private Exporter exporter;
    private Frame owner;

    public ExportDialog(Frame owner, Exporter exporter) {
        super(owner, "Bean Export", true);
        this.owner = owner;
        this.exporter = exporter;
        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent windowEvent) {
                int result = showExitDialog();
                if (result == JOptionPane.YES_OPTION) {
                    dispose();
                }
            }
        });
        this.setLayout(new BorderLayout());
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.addTab("Beans", getBeansPanel());
        tabbedPane.addTab("Properties", getPropertiesPanel());
        tabbedPane.addTab("Methods", getMethodsPanel());
        this.add(tabbedPane, BorderLayout.CENTER);
        JPanel directoryPanel = new JPanel();
        directoryPanel.setLayout(new FlowLayout(FlowLayout.LEFT));
        JLabel directoryLabel = new JLabel(exporter.getSaveDirectory().getAbsolutePath());
        directoryPanel.add(directoryLabel);
        JButton directoryButton = new JButton("Select");
        directoryButton.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser();
            chooser.setCurrentDirectory(exporter.getSaveDirectory());
            chooser.setDialogTitle("Select a save directory");
            chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            chooser.setAcceptAllFileFilterUsed(false);
            if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION && chooser.getSelectedFile() != null) {
                exporter.setSaveDirectory(chooser.getSelectedFile());
                System.out.println(exporter.getSaveDirectory().getAbsolutePath());
                directoryLabel.setText(exporter.getSaveDirectory().getAbsolutePath());
            }
        });
        directoryPanel.add(directoryButton);

        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new FlowLayout(FlowLayout.RIGHT));
        JButton exportButton = new JButton("Export");
        exportButton.addActionListener(e -> {
            //TODO:perform Export
        });

        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(e -> {
            int result = showExitDialog();
            if (result == JOptionPane.YES_OPTION) {
                dispose();
            }
        });
        buttonPanel.add(exportButton);
        buttonPanel.add(cancelButton);
        JSplitPane controlPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, directoryPanel, buttonPanel);
        controlPane.setResizeWeight(0.5);
        controlPane.setDividerSize(0);
        this.add(controlPane, BorderLayout.PAGE_END);
        this.setSize(new Dimension(800, 600));
    }

    private Component getMethodsPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());
        panel.add(new Label("The following Methods will be generated:"), BorderLayout.PAGE_START);
        DefaultMutableTreeNode top = new DefaultMutableTreeNode("Beans");
        createBeanNodes(top, exporter.getBeans());
        JTree tree = new JTree(top);
        JScrollPane scrollPane = new JScrollPane(tree);
        scrollPane.setPreferredSize(new Dimension(250, 80));
        panel.add(scrollPane, BorderLayout.CENTER);
        return panel;
    }

    private int showExitDialog() {
        return JOptionPane.showConfirmDialog(null, "Do you want to cancel the export?", "Cancel", JOptionPane.YES_NO_OPTION);
    }

    private JPanel getBeansPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());
        panel.add(new Label("The following Beans will be generated:"), BorderLayout.PAGE_START);

        JPanel editorPanel = new JPanel();
        editorPanel.setLayout(new BoxLayout(editorPanel, BoxLayout.Y_AXIS));
        editorPanel.add(new JLabel("Select a Bean to edit it"));

        DefaultMutableTreeNode top = new DefaultMutableTreeNode("Beans");
        createBeanNodes(top, exporter.getBeans());
        JTree tree = new JTree(top);
        tree.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                super.mouseClicked(e);
                setSelectedBean(tree.getSelectionPath().getLastPathComponent(), editorPanel);
            }
        });
        tree.setRootVisible(false);
        JScrollPane scrollPane = new JScrollPane(tree);
        scrollPane.setPreferredSize(new Dimension(130, 80));

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, scrollPane, editorPanel);
        splitPane.setResizeWeight(0.85);
        panel.add(splitPane);

        return panel;
    }

    private JPanel getPropertiesPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());
        panel.add(new Label("The following Properties will be generated:"), BorderLayout.PAGE_START);

        JPanel editorPanel = new JPanel();
        editorPanel.setLayout(new BoxLayout(editorPanel, BoxLayout.Y_AXIS));
        editorPanel.add(new JLabel("Select a Property to edit it"));

        DefaultMutableTreeNode top = new DefaultMutableTreeNode("Beans");
        createPropertyNodes(top, exporter.getBeans());
        JTree tree = new JTree(top);
        tree.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                super.mouseClicked(e);
                setSelectedProperty(tree.getSelectionPath().getLastPathComponent(), editorPanel);
            }
        });
        tree.setRootVisible(false);
        JScrollPane scrollPane = new JScrollPane(tree);
        scrollPane.setPreferredSize(new Dimension(130, 80));

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, scrollPane, editorPanel);
        splitPane.setResizeWeight(0.85);
        panel.add(splitPane);

        return panel;
    }

    private void createPropertyNodes(DefaultMutableTreeNode top, List<ExportBean> exportBeans) {
        for (ExportBean exportBean : exportBeans) {
            DefaultMutableTreeNode second = new DefaultMutableTreeNode(exportBean.getBeanName());
            second.setUserObject(exportBean);
            for (BeanNode node : exportBean.getBeans().getAllNodes()) {
                DefaultMutableTreeNode third = new DefaultMutableTreeNode(node.getDisplayName());
                third.setUserObject(node);
                for (ExportProperty property : node.getProperties()) {
                    DefaultMutableTreeNode fourth = new DefaultMutableTreeNode(property.getName());
                    fourth.setUserObject(property);
                    third.add(fourth);
                }
                second.add(third);
            }
            top.add(second);
        }
    }

    private void createBeanNodes(DefaultMutableTreeNode top, List<ExportBean> exportBeans) {
        for (ExportBean exportBean : exportBeans) {
            DefaultMutableTreeNode second = new DefaultMutableTreeNode(exportBean.getBeanName());
            for (BeanNode node : exportBean.getBeans().getAllNodes()) {
                second.add(new DefaultMutableTreeNode(node.getDisplayName()));
            }
            top.add(second);
        }
    }

    private void setSelectedProperty(Object selectedProperty, JPanel panel) {
        panel.removeAll();
        panel.repaint();
        panel.add(new JLabel("Select a Property to edit it"));
        panel.doLayout();
        if (!(selectedProperty instanceof DefaultMutableTreeNode)) return;
        DefaultMutableTreeNode treeNode = (DefaultMutableTreeNode) selectedProperty;
        if (!(treeNode.getUserObject() instanceof ExportProperty)) return;
        ExportProperty property = (ExportProperty) treeNode.getUserObject();
        JLabel currentValue = new JLabel("Current Value:");
        currentValue.setAlignmentX(Component.CENTER_ALIGNMENT);
        currentValue.setToolTipText("The currently configured value");
        JLabel defaultValue = new JLabel("Default Value:");
        defaultValue.setAlignmentX(Component.CENTER_ALIGNMENT);
        defaultValue.setToolTipText("Configure a default value to be set after exporting. By default this is equal to the currently configured value. Be aware of any Property Bindings or Property Veto Listeners!");
        JCheckBox configurable = new JCheckBox("Configurable");
        configurable.setAlignmentX(Component.CENTER_ALIGNMENT);
        configurable.setSelected(property.isExport());
        configurable.addActionListener(e -> property.setExport(configurable.isSelected()));
        configurable.setToolTipText("Select whether the property should still be configurable after export");
        panel.removeAll();
        panel.repaint();
        panel.add(currentValue);
        try {
            panel.add(new PropertyDisplayCanvas(150, 50, property, PropertyValueType.CURRENT_VALUE));
        } catch (InvocationTargetException | IllegalAccessException e) {
            panel.add(new JLabel("Could not load Editor or value"));
        }
        panel.add(defaultValue);
        try {
            PropertyEditorComponent propertyEditorComponent = new PropertyEditorComponent(154, 54, property, PropertyValueType.DEFAULT_VALUE, owner);
            propertyEditorComponent.addPropertyChangeListener(evt -> {
                //TODO: handle property bindings and vetos
                if (evt.getSource() instanceof PropertyEditor) {
                    PropertyEditor editor = (PropertyEditor) evt.getSource();
                    property.setDefaultValue(editor.getValue());
                    propertyEditorComponent.repaintComponent();
                    System.out.println(editor.getValue());
                    /*for (int i = 0 ; i < editors.length; i++) {
                        if (editors[i] == editor) {
                            PropertyDescriptor property1 = properties[i];
                            Object value = editor.getValue();
                            values[i] = value;
                            Method setter = property1.getWriteMethod();
                            try {
                                Object args[] = { value };
                                args[0] = value;
                                setter.invoke(target, args);

                                // We add the changed property to the targets wrapper
                                // so that we know precisely what bean properties have
                                // changed for the target bean and we're able to
                                // generate initialization statements for only those
                                // modified properties at code generation time.
                                targetWrapper.getChangedProperties().addElement(properties[i]);

                            } catch (InvocationTargetException ex) {
                                if (ex.getTargetException()
                                        instanceof PropertyVetoException) {
                                    //warning("Vetoed; reason is: "
                                    //        + ex.getTargetException().getMessage());
                                    // temp dealock fix...I need to remove the deadlock.
                                    System.err.println("WARNING: Vetoed; reason is: "
                                            + ex.getTargetException().getMessage());
                                }
                                else
                                    error("InvocationTargetException while updating "
                                            + property1.getName(), ex.getTargetException());
                            } catch (Exception ex) {
                                error("Unexpected exception while updating "
                                        + property1.getName(), ex);
                            }
                            if (views[i] != null && views[i] instanceof PropertyCanvas) {
                                views[i].repaint();
                            }
                            break;
                        }
                    }
                }

                // Now re-read all the properties and update the editors
                // for any other properties that have changed.
                for (int i = 0; i < properties.length; i++) {
                    Object o;
                    try {
                        Method getter = properties[i].getReadMethod();
                        Object args[] = { };
                        o = getter.invoke(target, args);
                    } catch (Exception ex) {
                        o = null;
                    }
                    if (o == values[i] || (o != null && o.equals(values[i]))) {
                        // The property is equal to its old value.
                        continue;
                    }
                    values[i] = o;
                    // Make sure we have an editor for this property...
                    if (editors[i] == null) {
                        continue;
                    }
                    // The property has changed!  Update the editor.
                    editors[i].setValue(o);
                    if (views[i] != null) {
                        views[i].repaint();
                    }*/
                }
            });
            panel.add(propertyEditorComponent);
        } catch (InvocationTargetException | IllegalAccessException e) {
            panel.add(new JLabel("Could not load Editor or value"));
        }
        panel.add(configurable);
        panel.doLayout();
    }

    private void setSelectedBean(Object selectedBean, JPanel panel) {
        panel.removeAll();
        panel.repaint();
        panel.add(new JLabel("Select a Bean to edit it"));
        panel.doLayout();
        if (!(selectedBean instanceof DefaultMutableTreeNode)) return;
        DefaultMutableTreeNode treeNode = (DefaultMutableTreeNode) selectedBean;
        if(!(treeNode.getUserObject() instanceof BeanNode) && ! (treeNode.getUserObject() instanceof ExportBean)) return;
        JLabel name = new JLabel("Name:");
        name.setAlignmentX(Component.CENTER_ALIGNMENT);


        if(treeNode.getUserObject() instanceof BeanNode) {
            BeanNode bean = (BeanNode) treeNode.getUserObject();
            name.setToolTipText("Configure the name of the Bean");

            JCheckBox manifest = new JCheckBox("Include in Manifest");
            manifest.setAlignmentX(Component.CENTER_ALIGNMENT);
            manifest.setSelected(property.isExport());
            manifest.addActionListener(e -> property.setExport(configurable.isSelected()));
            configurable.setToolTipText("Select whether the property should still be configurable after export");
...

        } else if(treeNode.getUserObject() instanceof ExportBean) {
            ExportBean bean = (ExportBean) treeNode.getUserObject();
...
        }



        panel.removeAll();
        panel.repaint();
        panel.add(name);
        ...
        panel.doLayout();
    }
}

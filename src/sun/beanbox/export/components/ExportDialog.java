package sun.beanbox.export.components;

import sun.beanbox.ErrorDialog;
import sun.beanbox.export.Exporter;
import sun.beanbox.export.datastructure.*;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.List;

/**
 * Created by Andreas Ertlschweiger on 06.05.2017.
 * <p>
 * This is the configurator window for Bean exporting.
 */
public class ExportDialog extends JDialog {

    private final Exporter exporter;

    private JTree tree;

    private static final String PROPERTY_NODE_LABEL = "Properties";
    private static final String EVENT_NODE_LABEL = "Events";
    private static final String METHOD_NODE_LABEL = "Methods";

    /**
     * This constructor sets up all UI components required for the export configuration.
     *
     * @param owner    the Window from which the Dialog is called.
     * @param exporter an instance of an Exporter component.
     */
    public ExportDialog(Frame owner, Exporter exporter) {
        super(owner, "Bean Export", true);
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
        setLayout(new BorderLayout());
        add(getBeansPanel(), BorderLayout.CENTER);

        JLabel statusLabel = new JLabel("");
        statusLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton exportButton = new JButton("Export");
        exportButton.addActionListener(e -> {
            FileDialog fd = new FileDialog(owner, "Save Export Bean", FileDialog.SAVE);
            fd.setDirectory(System.getProperty("user.dir"));
            fd.setFile("ExportBean.jar");
            fd.setVisible(true);
            if (fd.getFile() != null && fd.getDirectory() != null && !fd.getFile().isEmpty() && !fd.getDirectory().isEmpty()) {
                try {
                    final Component glassPane = getGlassPane();
                    //This is a long running process. Do not execute it on the UI thread.
                    Thread worker = new Thread(() -> {
                        //Display information about the running process to the user.
                        SwingUtilities.invokeLater(() -> {
                            final JPanel panel = new JPanel(new BorderLayout());
                            panel.add(new JLabel("Please wait a few moments.", SwingConstants.CENTER), BorderLayout.CENTER);
                            setGlassPane(panel);
                            panel.setVisible(true);
                            panel.setOpaque(false);
                            for (Component component : getComponents()) {
                                component.setEnabled(false);
                            }
                            statusLabel.setText("Generating...");
                        });
                        setEnabled(false);
                        //Start the exporting process.
                        try {
                            List<ExportConstraintViolation> violations = exporter.export(fd.getDirectory(), fd.getFile());
                            if (violations != null) {
                                SwingUtilities.invokeLater(() -> {
                                    setGlassPane(glassPane);
                                    for (Component component : getComponents()) {
                                        component.setEnabled(false);
                                    }
                                    setEnabled(true);
                                    statusLabel.setText("Failed!");
                                    StringBuilder errorText = new StringBuilder();
                                    for (ExportConstraintViolation violation : violations) {
                                        errorText.append(violation.getMessage()).append("\n");
                                    }
                                    JOptionPane.showMessageDialog(this, errorText, "Please resolve the following conflicts before exporting",
                                            JOptionPane.ERROR_MESSAGE);
                                });
                            } else {
                                SwingUtilities.invokeLater(() -> {
                                    setGlassPane(glassPane);
                                    for (Component component : getComponents()) {
                                        component.setEnabled(false);
                                    }
                                    setEnabled(true);
                                    statusLabel.setText("Finished!");
                                });
                            }
                        } catch (Exception ex) {
                            ex.printStackTrace();
                            SwingUtilities.invokeLater(() -> new ErrorDialog(owner, ex.getMessage()));
                        }
                    });

                    worker.start();
                } catch (Exception ex) {
                    ex.printStackTrace();
                    new ErrorDialog(owner, ex.getMessage());
                }
            }
        });

        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(e -> {
            if (showExitDialog() == JOptionPane.YES_OPTION) {
                dispose();
            }
        });

        JCheckBox keepSources = new JCheckBox("Keep sources");
        keepSources.setSelected(exporter.isKeepSources());
        keepSources.setToolTipText("Select whether the temporary folder that is created during export should be deleted afterwards or not.");
        keepSources.addActionListener(e -> exporter.setKeepSources(keepSources.isSelected()));
        buttonPanel.add(statusLabel);
        buttonPanel.add(keepSources);
        buttonPanel.add(exportButton);
        buttonPanel.add(cancelButton);
        add(buttonPanel, BorderLayout.PAGE_END);
        setSize(new Dimension(800, 600));
    }

    /**
     * Show a confirmation dialog to avoid accidental loss of the users configuration.
     *
     * @return returns an integer depending on the users choice.
     */
    private int showExitDialog() {
        return JOptionPane.showConfirmDialog(null, "Do you want to cancel the export?", "Cancel", JOptionPane.YES_NO_OPTION);
    }

    /**
     * Constructs the actual content for the configurator.
     *
     * @return returns a Panel that displays the Bean tree, configuration window and the button bar.
     */
    private JPanel getBeansPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(new Label("The following beans will be generated. You can customize any node by selecting it:"), BorderLayout.PAGE_START);

        JScrollPane editorPanel = new JScrollPane(new JLabel("Select a node to edit it"));

        DefaultMutableTreeNode top = new DefaultMutableTreeNode("Beans");
        createNodes(top, exporter.getBeans());
        tree = new JTree(top);
        tree.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                super.mouseClicked(e);
                setSelectedBean(editorPanel);
            }
        });
        tree.setRootVisible(false);
        DefaultMutableTreeNode currentNode = top.getNextNode();
        do {
            if (currentNode.getLevel() == 1) {
                tree.expandPath(new TreePath(currentNode.getPath()));
            }
            currentNode = currentNode.getNextNode();
        } while (currentNode != null);
        JScrollPane scrollPane = new JScrollPane(tree);
        scrollPane.setPreferredSize(new Dimension(130, 80));

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, scrollPane, editorPanel);
        splitPane.setResizeWeight(0.70);
        panel.add(splitPane);

        return panel;
    }

    /**
     * Assembles the TreeView that contains all the Beans, Properties, Events and Methods.
     *
     * @param top         the root node of the tree.
     * @param exportBeans a list of export beans that should be displayed.
     */
    private void createNodes(DefaultMutableTreeNode top, List<ExportBean> exportBeans) {
        for (ExportBean exportBean : exportBeans) {
            DefaultMutableTreeNode secondLevel = new DefaultMutableTreeNode(exportBean);
            for (BeanNode node : exportBean.getBeans()) {
                DefaultMutableTreeNode thirdLevel = new DefaultMutableTreeNode(node);
                DefaultMutableTreeNode fourthLevelProperties = new DefaultMutableTreeNode(PROPERTY_NODE_LABEL);
                thirdLevel.add(fourthLevelProperties);
                for (ExportProperty property : node.getProperties()) {
                    DefaultMutableTreeNode fifthLevelProperties = new DefaultMutableTreeNode(property);
                    fourthLevelProperties.add(fifthLevelProperties);
                }
                DefaultMutableTreeNode fourthLevelEvents = new DefaultMutableTreeNode(EVENT_NODE_LABEL);
                thirdLevel.add(fourthLevelEvents);
                for (ExportEvent event : node.getEvents()) {
                    DefaultMutableTreeNode fifthLevelEvents = new DefaultMutableTreeNode(event);
                    fourthLevelEvents.add(fifthLevelEvents);
                }
                DefaultMutableTreeNode fourthLevelMethods = new DefaultMutableTreeNode(METHOD_NODE_LABEL);
                thirdLevel.add(fourthLevelMethods);
                for (ExportMethod method : node.getMethods()) {
                    DefaultMutableTreeNode fifthLevelMethods = new DefaultMutableTreeNode(method);
                    fourthLevelMethods.add(fifthLevelMethods);
                }
                secondLevel.add(thirdLevel);
            }
            top.add(secondLevel);
        }
    }

    /**
     * This method changes the content of the configurator view depending on which node has been selected.
     * Unfortunately Swing is not generic and because of that we need to check for instances.
     *
     * @param panel the panel that should display the content.
     */
    private void setSelectedBean(JScrollPane panel) {
        Object selected = null;
        if (tree.getSelectionPath() != null && tree.getSelectionPath().getLastPathComponent() != null) {
            selected = tree.getSelectionPath().getLastPathComponent();
        }

        if (selected != null && selected instanceof DefaultMutableTreeNode) {
            DefaultMutableTreeNode treeNode = (DefaultMutableTreeNode) selected;
            if (treeNode.getUserObject() instanceof ExportBean) {
                panel.setViewportView(new ExportBeanEditor(exporter, (ExportBean) treeNode.getUserObject(), tree, treeNode));
                return;
            } else if (treeNode.getUserObject() instanceof BeanNode) {
                panel.setViewportView(new BeanNodeEditor(exporter, (BeanNode) treeNode.getUserObject(), tree, treeNode));
                return;
            } else if (treeNode.getUserObject() instanceof ExportProperty) {
                panel.setViewportView(new ExportPropertyEditor(exporter, (ExportProperty) treeNode.getUserObject(), tree, treeNode));
                return;
            } else if (treeNode.getUserObject() instanceof ExportMethod) {
                panel.setViewportView(new ExportMethodEditor(exporter, (ExportMethod) treeNode.getUserObject(), tree, treeNode));
                return;
            } else if (treeNode.getUserObject() instanceof ExportEvent) {
                panel.setViewportView(new ExportEventEditor(exporter, (ExportEvent) treeNode.getUserObject(), tree, treeNode));
                return;
            } else if (treeNode.getUserObject() instanceof String) {
                String text = (String) treeNode.getUserObject();
                BeanNode bean = null;
                DefaultMutableTreeNode current = treeNode;
                while (bean == null) {
                    current = (DefaultMutableTreeNode) current.getParent();
                    if (current.getUserObject() instanceof BeanNode) {
                        bean = (BeanNode) current.getUserObject();
                    }
                }
                switch (text) {
                    case PROPERTY_NODE_LABEL:
                        panel.setViewportView(new AllPropertiesEditor(bean));
                        return;
                    case EVENT_NODE_LABEL:
                        panel.setViewportView(new AllEventsEditor(bean));
                        return;
                    case METHOD_NODE_LABEL:
                        panel.setViewportView(new AllMethodsEditor(bean));
                        return;
                }
            }
        }
        panel.setViewportView(new JLabel("Select a node to edit it"));
    }
}

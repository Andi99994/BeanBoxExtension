package sun.beanbox.export;

import sun.beanbox.ErrorDialog;
import sun.beanbox.export.components.*;
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
import java.util.Observable;
import java.util.Observer;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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
        this.add(getBeansPanel(), BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new FlowLayout(FlowLayout.RIGHT));
        JButton exportButton = new JButton("Export");
        exportButton.addActionListener(e -> {
            FileDialog fd = new FileDialog(owner, "Save Export Bean", FileDialog.SAVE);
            fd.setDirectory(System.getProperty("user.dir"));
            fd.setFile("ExportBean.jar");
            fd.show();
            if (fd.getFile() == null || fd.getDirectory() == null || fd.getFile().isEmpty() ||fd.getDirectory().isEmpty()) {
                return;
            }
            try {
                final Component glassPane = getGlassPane();
                Thread worker = new Thread(() -> {
                    SwingUtilities.invokeLater(() -> {
                        final JPanel panel = new JPanel();
                        panel.setLayout(new BorderLayout());
                        panel.add(new JLabel("Generating...", SwingConstants.CENTER), BorderLayout.CENTER);
                        setGlassPane(panel);
                        panel.setVisible(true);
                        panel.setOpaque(false);
                        for (Component component : getComponents()) {
                            component.setEnabled(false);
                        }
                    });
                    setEnabled(false);
                    try {
                        exporter.export(fd.getDirectory(), fd.getFile());
                    } catch (Exception e1) {
                        SwingUtilities.invokeLater(() -> new ErrorDialog(owner, e1.getMessage()));
                    }

                    SwingUtilities.invokeLater(() -> {
                        setGlassPane(glassPane);
                        for (Component component : getComponents()) {
                            component.setEnabled(false);
                        }
                        dispose();
                    });
                });

                worker.start();
            } catch (Exception ex) {
                new ErrorDialog(owner, ex.getMessage());
            }
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
        this.add(buttonPanel, BorderLayout.PAGE_END);
        this.setSize(new Dimension(800, 600));
    }

    private int showExitDialog() {
        return JOptionPane.showConfirmDialog(null, "Do you want to cancel the export?", "Cancel", JOptionPane.YES_NO_OPTION);
    }

    private JPanel getBeansPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());
        panel.add(new Label("The following beans will be generated. You can customize any node by selecting it:"), BorderLayout.PAGE_START);

        JScrollPane editorPanel = new JScrollPane(new JLabel("Select a node to edit it"));

        DefaultMutableTreeNode top = new DefaultMutableTreeNode("Beans");
        createNodes(top, exporter.getBeans());
        JTree tree = new JTree(top);
        tree.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                super.mouseClicked(e);
                if (tree.getSelectionPath() != null && tree.getSelectionPath().getLastPathComponent() != null) {
                    setSelectedBean(tree.getSelectionPath().getLastPathComponent(), editorPanel, tree);
                }
            }
        });
        tree.setRootVisible(false);
        DefaultMutableTreeNode currentNode = top.getNextNode();
        do {
            if (currentNode.getLevel() == 1) {
                tree.expandPath(new TreePath(currentNode.getPath()));
            }
            currentNode = currentNode.getNextNode();
        }
        while (currentNode != null);
        JScrollPane scrollPane = new JScrollPane(tree);
        scrollPane.setPreferredSize(new Dimension(130, 80));

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, scrollPane, editorPanel);
        splitPane.setResizeWeight(0.70);
        panel.add(splitPane);

        return panel;
    }

    private void createNodes(DefaultMutableTreeNode top, List<ExportBean> exportBeans) {
        for (ExportBean exportBean : exportBeans) {
            DefaultMutableTreeNode secondLevel = new DefaultMutableTreeNode(exportBean.getBeanName());
            secondLevel.setUserObject(exportBean);
            for (BeanNode node : exportBean.getBeans().getAllNodes()) {
                DefaultMutableTreeNode thirdLevel = new DefaultMutableTreeNode(node.getName());
                thirdLevel.setUserObject(node);
                DefaultMutableTreeNode fourthLevelProperties = new DefaultMutableTreeNode("Properties");
                thirdLevel.add(fourthLevelProperties);
                for (ExportProperty property : node.getProperties()) {
                    DefaultMutableTreeNode fifthLevelProperties = new DefaultMutableTreeNode(property.toString());
                    fifthLevelProperties.setUserObject(property);
                    fourthLevelProperties.add(fifthLevelProperties);
                }
                DefaultMutableTreeNode fourthLevelEvents = new DefaultMutableTreeNode("Events");
                thirdLevel.add(fourthLevelEvents);
                for (ExportEvent event : node.getEvents()) {
                    DefaultMutableTreeNode fifthLevelEvents = new DefaultMutableTreeNode(event.toString());
                    fifthLevelEvents.setUserObject(event);
                    fourthLevelEvents.add(fifthLevelEvents);
                }
                DefaultMutableTreeNode fourthLevelMethods = new DefaultMutableTreeNode("Methods");
                thirdLevel.add(fourthLevelMethods);
                for (ExportMethod method : node.getMethods()) {
                    DefaultMutableTreeNode fifthLevelMethods = new DefaultMutableTreeNode(method.toString());
                    fifthLevelMethods.setUserObject(method);
                    fourthLevelMethods.add(fifthLevelMethods);
                }
                secondLevel.add(thirdLevel);
            }
            top.add(secondLevel);
        }
    }

    private void setSelectedBean(Object selected, JScrollPane panel, JTree tree) {
        if (selected instanceof DefaultMutableTreeNode) {
            DefaultMutableTreeNode treeNode = (DefaultMutableTreeNode) selected;
            if (treeNode.getUserObject() instanceof ExportBean) {
                panel.setViewportView(new ExportBeanEditor(exporter, (ExportBean) treeNode.getUserObject(), tree, treeNode));
                return;
            } else if (treeNode.getUserObject() instanceof BeanNode) {
                panel.setViewportView(new BeanNodeEditor((BeanNode) treeNode.getUserObject()));
                return;
            } else if (treeNode.getUserObject() instanceof ExportProperty) {
                panel.setViewportView(new ExportPropertyEditor(owner, exporter, (ExportProperty) treeNode.getUserObject(), tree, treeNode));
                return;
            } else if (treeNode.getUserObject() instanceof ExportMethod) {
                panel.setViewportView(new ExportMethodEditor(exporter, (ExportMethod) treeNode.getUserObject(), tree, treeNode));
                return;
            } else if (treeNode.getUserObject() instanceof ExportEvent) {
                panel.setViewportView(new ExportEventEditor(exporter, (ExportEvent) treeNode.getUserObject(), tree, treeNode));
                return;
            }
        }
        panel.setViewportView(new JLabel("Select a node to edit it"));
    }
}

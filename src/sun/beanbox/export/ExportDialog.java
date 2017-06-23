package sun.beanbox.export;

import sun.beanbox.ErrorDialog;
import sun.beanbox.export.components.BeanNodeEditor;
import sun.beanbox.export.components.ExportBeanEditor;
import sun.beanbox.export.components.ExportPropertyEditor;
import sun.beanbox.export.datastructure.BeanNode;
import sun.beanbox.export.datastructure.ExportBean;
import sun.beanbox.export.datastructure.ExportMethod;
import sun.beanbox.export.datastructure.ExportProperty;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
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
        tabbedPane.addTab("Methods", getMethodsPanel());
        this.add(tabbedPane, BorderLayout.CENTER);

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
                exporter.export(fd.getDirectory(), fd.getFile());
                dispose();
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

    private Component getMethodsPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());
        panel.add(new Label("The following methods will be generated:"), BorderLayout.PAGE_START);
        DefaultMutableTreeNode top = new DefaultMutableTreeNode("Methods");
        for (ExportBean exportBean : exporter.getBeans()) {
            DefaultMutableTreeNode second = new DefaultMutableTreeNode(exportBean.getBeanName());
            second.setUserObject(exportBean);
            for (BeanNode node : exportBean.getBeans().getAllNodes()) {
                DefaultMutableTreeNode third = new DefaultMutableTreeNode(node.getDisplayName());
                third.setUserObject(node);
                for (ExportMethod method : node.getMethods()) {
                    DefaultMutableTreeNode fourth = new DefaultMutableTreeNode(method.getName());
                    fourth.setUserObject(method);
                    third.add(fourth);
                }
                second.add(third);
            }
            top.add(second);
        }
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
            DefaultMutableTreeNode second = new DefaultMutableTreeNode(exportBean.getBeanName());
            second.setUserObject(exportBean);
            for (BeanNode node : exportBean.getBeans().getAllNodes()) {
                DefaultMutableTreeNode third = new DefaultMutableTreeNode(node.getDisplayName());
                third.setUserObject(node);
                for (ExportProperty property : node.getProperties()) {
                    DefaultMutableTreeNode fourth = new DefaultMutableTreeNode(property.toString());
                    fourth.setUserObject(property);
                    third.add(fourth);
                }
                second.add(third);
            }
            top.add(second);
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
            }
            panel.setViewportView(new JLabel("Unknown node type"));
            return;
        }
        panel.setViewportView(new JLabel("Select a node to edit it"));
    }
}

package sun.beanbox.export;

import sun.beanbox.export.components.*;
import sun.beanbox.export.datastructure.BeanNode;
import sun.beanbox.export.datastructure.ExportBean;
import sun.beanbox.export.datastructure.ExportProperty;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.Date;
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
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new FlowLayout(FlowLayout.RIGHT));
        Button exportButton = new Button("Export");
        exportButton.addActionListener(e -> {
            //TODO:perform Export
        });
        Button cancelButton = new Button("Cancel");
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
        DefaultMutableTreeNode top = new DefaultMutableTreeNode("Beans");
        createBeanNodes(top, exporter.getBeans());
        JTree tree = new JTree(top);
        JScrollPane scrollPane = new JScrollPane(tree);
        scrollPane.setPreferredSize(new Dimension(250, 80));
        panel.add(scrollPane, BorderLayout.CENTER);
        return panel;
    }

    private JPanel getPropertiesPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());
        panel.add(new Label("The following Properties will be generated:"), BorderLayout.PAGE_START);
        JPanel tablePanel = new JPanel(new GridBagLayout());
        tablePanel.setPreferredSize(new Dimension(250, 80));

        AbstractTreeTableModel model = new PropertyTreeTableModel(exporter.getBeans().get(0));
        JTreeTable table = new JTreeTable(model);
        table.setDefaultRenderer(ExportProperty.class, new PropertyCellRenderer(owner));
        panel.add(new JScrollPane(table));

        /*
        AbstractTreeTableModel treeTableModel = new MyDataModel(createDataStructure());
        JTreeTable myTreeTable = new JTreeTable(treeTableModel);
        panel.add(new JScrollPane(myTreeTable));
        JScrollPane scrollPane = new JScrollPane(tablePanel);
        scrollPane.setPreferredSize(new Dimension(250, 80));
        panel.add(scrollPane, BorderLayout.CENTER);
        GridBagConstraints c = new GridBagConstraints();
        c.anchor = GridBagConstraints.NORTHWEST;
        //c.insets = new Insets(0,5,0,5);
        c.gridy = 0;
        c.gridx = 0;
        for(ExportBean bean : exporter.getBeans()) {
            JTreeTable table = new JTreeTable(new PropertyTreeTableModel(bean));
            tablePanel.add(table.getTableHeader(),c);
            c.gridy += 1;
            tablePanel.add(table, c);
            c.gridy += 1;
        }*/
        return panel;
    }
    private static MyDataNode createDataStructure() {
        List<MyDataNode> children1 = new ArrayList<MyDataNode>();
        children1.add(new MyDataNode("N12", "C12", new Date(), Integer.valueOf(50), null));
        children1.add(new MyDataNode("N13", "C13", new Date(), Integer.valueOf(60), null));
        children1.add(new MyDataNode("N14", "C14", new Date(), Integer.valueOf(70), null));
        children1.add(new MyDataNode("N15", "C15", new Date(), Integer.valueOf(80), null));

        List<MyDataNode> children2 = new ArrayList<MyDataNode>();
        children2.add(new MyDataNode("N12", "C12", new Date(), Integer.valueOf(10), null));
        children2.add(new MyDataNode("N13", "C13", new Date(), Integer.valueOf(20), children1));
        children2.add(new MyDataNode("N14", "C14", new Date(), Integer.valueOf(30), null));
        children2.add(new MyDataNode("N15", "C15", new Date(), Integer.valueOf(40), null));

        List<MyDataNode> rootNodes = new ArrayList<MyDataNode>();
        rootNodes.add(new MyDataNode("N1", "C1", new Date(), Integer.valueOf(10), children2));
        rootNodes.add(new MyDataNode("N2", "C2", new Date(), Integer.valueOf(10), children1));
        rootNodes.add(new MyDataNode("N3", "C3", new Date(), Integer.valueOf(10), children2));
        rootNodes.add(new MyDataNode("N4", "C4", new Date(), Integer.valueOf(10), children1));
        rootNodes.add(new MyDataNode("N5", "C5", new Date(), Integer.valueOf(10), children1));
        rootNodes.add(new MyDataNode("N6", "C6", new Date(), Integer.valueOf(10), children1));
        rootNodes.add(new MyDataNode("N7", "C7", new Date(), Integer.valueOf(10), children1));
        rootNodes.add(new MyDataNode("N8", "C8", new Date(), Integer.valueOf(10), children1));
        rootNodes.add(new MyDataNode("N9", "C9", new Date(), Integer.valueOf(10), children1));
        rootNodes.add(new MyDataNode("N10", "C10", new Date(), Integer.valueOf(10), children1));
        rootNodes.add(new MyDataNode("N11", "C11", new Date(), Integer.valueOf(10), children1));
        rootNodes.add(new MyDataNode("N12", "C7", new Date(), Integer.valueOf(10), children1));
        rootNodes.add(new MyDataNode("N13", "C8", new Date(), Integer.valueOf(10), children1));
        rootNodes.add(new MyDataNode("N14", "C9", new Date(), Integer.valueOf(10), children1));
        rootNodes.add(new MyDataNode("N15", "C10", new Date(), Integer.valueOf(10), children1));
        rootNodes.add(new MyDataNode("N16", "C11", new Date(), Integer.valueOf(10), children1));
        MyDataNode root = new MyDataNode("R1", "R1", new Date(), Integer.valueOf(10), rootNodes);

        return root;
    }

    private void createPropertyNodes(DefaultMutableTreeNode top, List<ExportBean> exportBeans) {
        for (ExportBean exportBean : exportBeans) {
            DefaultMutableTreeNode second = new DefaultMutableTreeNode(exportBean.getBeanName());
            for (BeanNode node : exportBean.getBeans().getAllNodes()) {
                DefaultMutableTreeNode third = new DefaultMutableTreeNode(node.getDisplayName());
                for (ExportProperty property : node.getProperties()) {
                    third.add(new DefaultMutableTreeNode(property.getName()));
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

}

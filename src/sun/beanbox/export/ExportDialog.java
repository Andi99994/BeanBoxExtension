package sun.beanbox.export;

import sun.beanbox.Wrapper;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Andreas Ertlschweiger on 06.05.2017.
 */
public class ExportDialog extends JDialog {

    private Exporter exporter;

    public ExportDialog(Frame owner, Exporter exporter) {
        super(owner, "Bean Export", true);
        this.exporter = exporter;
        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent windowEvent){
                int result = showExitDialog();
                if (result == JOptionPane.YES_OPTION) {
                    dispose();
                }
            }
        });
        this.setLayout(new BorderLayout());
        JTabbedPane tabbedPane = new JTabbedPane();
        JPanel methodsPanel = new JPanel();
        tabbedPane.addTab("Beans", getBeansPanel());
        tabbedPane.addTab("Properties", getPropertiesPanel());
        tabbedPane.addTab("Methods", methodsPanel);
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
            if (result == JOptionPane.YES_OPTION ) {
                dispose();
            }
        });
        buttonPanel.add(exportButton);
        buttonPanel.add(cancelButton);
        this.add(buttonPanel, BorderLayout.PAGE_END);
        this.setSize(new Dimension(800, 600));
    }

    private int showExitDialog() {
        return JOptionPane.showConfirmDialog (null, "Do you want to cancel the export?","Cancel",JOptionPane.YES_NO_OPTION);
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

    private JScrollPane getPropertiesPanel() {
        DefaultMutableTreeNode top = new DefaultMutableTreeNode("Properties");
        createPropertyNodes(top, exporter.getProperties());
        JTree tree = new JTree(top);
        JScrollPane listScroller = new JScrollPane(tree);
        listScroller.setPreferredSize(new Dimension(250, 80));
        return listScroller;
    }

    private void createPropertyNodes(DefaultMutableTreeNode top, HashMap<String, HashMap<String, List<String>>> beans) {
        for (Map.Entry<String, HashMap<String, List<String>>> entry : beans.entrySet()) {
            DefaultMutableTreeNode second = new DefaultMutableTreeNode(entry.getKey());
            for(Map.Entry<String, List<String>> beanEntry : entry.getValue().entrySet()) {
                DefaultMutableTreeNode third = new DefaultMutableTreeNode(beanEntry.getKey());
                for(String property : beanEntry.getValue()) {
                    third.add(new DefaultMutableTreeNode(property));
                }
                second.add(third);
            }
            top.add(second);
        }
    }

    private void createBeanNodes(DefaultMutableTreeNode top, HashMap<String, HashMap<String, Wrapper>> beans) {
        for (Map.Entry<String, HashMap<String, Wrapper>> entry : beans.entrySet()) {
            DefaultMutableTreeNode second = new DefaultMutableTreeNode(entry.getKey());
            for(Map.Entry<String, Wrapper> beanEntry : entry.getValue().entrySet()) {
                second.add(new DefaultMutableTreeNode(beanEntry.getKey()));
            }
            top.add(second);
        }
    }

}

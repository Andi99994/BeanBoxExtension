package sun.beanbox.export;

import sun.beanbox.Wrapper;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
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
        exportButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                //TODO:perform Export
            }
        });
        Button cancelButton = new Button("Cancel");
        cancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int result = showExitDialog();
                if (result == JOptionPane.YES_OPTION ) {
                    dispose();
                }
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

    private JScrollPane getBeansPanel() {
        JList<String> list = new JList<>(exporter.getBeans().toArray(new String[exporter.getBeans().size()])); //data has type Object[]
        list.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
        list.setLayoutOrientation(JList.VERTICAL);
        list.setVisibleRowCount(-1);
        JScrollPane listScroller = new JScrollPane(list);
        listScroller.setPreferredSize(new Dimension(250, 80));
        return listScroller;
    }

    private JScrollPane getPropertiesPanel() {
        DefaultMutableTreeNode top = new DefaultMutableTreeNode("Properties");
        createNodes(top, exporter.getProperties());
        JTree tree = new JTree(top);
        JScrollPane listScroller = new JScrollPane(tree);
        listScroller.setPreferredSize(new Dimension(250, 80));
        return listScroller;
    }

    private void createNodes(DefaultMutableTreeNode top, HashMap<String, List<String>> beans) {
        for(Map.Entry<String, List<String>> entry : beans.entrySet()) {
            DefaultMutableTreeNode bean = new DefaultMutableTreeNode(entry.getKey());
            for(String property : entry.getValue()) {
                bean.add(new DefaultMutableTreeNode(property));
            }
            top.add(bean);
        }
    }

}

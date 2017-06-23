package sun.beanbox.export.components;

import sun.beanbox.export.datastructure.BeanNode;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by Andi on 05.06.2017.
 */
public class NodeSelector extends JDialog {

    public NodeSelector(Frame owner, List<BeanNode> availableNodes, String message) {
        super(owner, "Select Beans", true);
        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent windowEvent) {
                int result = showExitDialog();
                if (result == JOptionPane.YES_OPTION) {
                    availableNodes.clear();
                    dispose();
                }
            }
        });
        this.setLayout(new BorderLayout());
        this.add(new Label(message), BorderLayout.PAGE_START);
        JList list = new JList(availableNodes.toArray());
        list.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        list.setLayoutOrientation(JList.VERTICAL_WRAP);
        list.setVisibleRowCount(-1);
        list.setSelectedIndex(0);
        list.setCellRenderer(new BeanNodeCellRenderer());
        JScrollPane listScroller = new JScrollPane(list);
        listScroller.setPreferredSize(new Dimension(250, 80));
        this.add(listScroller, BorderLayout.CENTER);
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new FlowLayout(FlowLayout.RIGHT));
        Button exportButton = new Button("Accept");
        exportButton.addActionListener(e -> {
            List<BeanNode> selectedNodes = new LinkedList<>();
            for (Object item : list.getSelectedValuesList()) {
                selectedNodes.add((BeanNode) item);
            }
            availableNodes.clear();
            availableNodes.addAll(selectedNodes);
            dispose();
        });
        Button cancelButton = new Button("Cancel");
        cancelButton.addActionListener(e -> {
            int result = showExitDialog();
            if (result == JOptionPane.YES_OPTION) {
                availableNodes.clear();
                dispose();
            }
        });
        buttonPanel.add(exportButton);
        buttonPanel.add(cancelButton);
        this.add(buttonPanel, BorderLayout.PAGE_END);
        this.setSize(new Dimension(800, 600));
    }

    private int showExitDialog() {
        return JOptionPane.showConfirmDialog(null, "You must select your input and output Beans. Do you want to cancel the export?", "Cancel", JOptionPane.YES_NO_OPTION);
    }

    class BeanNodeCellRenderer extends JLabel implements ListCellRenderer {
        private final Color HIGHLIGHT_COLOR = new Color(0, 0, 128);

        BeanNodeCellRenderer() {
            setOpaque(true);
            setIconTextGap(12);
        }

        public Component getListCellRendererComponent(JList list, Object value,
                                                      int index, boolean isSelected, boolean cellHasFocus) {
            BeanNode entry = (BeanNode) value;
            setText(entry.getName());
            if (isSelected) {
                setBackground(HIGHLIGHT_COLOR);
                setForeground(Color.white);
            } else {
                setBackground(Color.white);
                setForeground(Color.black);
            }
            return this;
        }
    }

}

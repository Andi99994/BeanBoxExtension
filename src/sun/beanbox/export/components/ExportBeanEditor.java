package sun.beanbox.export.components;

import sun.beanbox.export.Exporter;
import sun.beanbox.export.datastructure.ExportBean;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import java.awt.*;

/**
 * Created by Andi on 22.06.2017.
 */
public class ExportBeanEditor extends JPanel {

    public ExportBeanEditor(Exporter exporter, ExportBean exportBean, JTree tree, DefaultMutableTreeNode treeNode) {
        setLayout(new GridBagLayout());

        JLabel name = new JLabel("Name: ");
        name.setToolTipText("Configure the name of the bean. The name must be a valid Java identifier and must not be a keyword.");
        TextField nameText = new TextField();
        nameText.setText(exportBean.getBeanName());
        JLabel nameCheckLabel = new JLabel(exporter.checkIfValidClassName(nameText.getText()) ? "Valid name" : "Invalid name");
        nameText.addTextListener(e -> {
            if (exporter.checkIfValidClassName(nameText.getText())) {
                exportBean.setBeanName(nameText.getText());
                nameCheckLabel.setText("Valid name");
                DefaultTreeModel model = (DefaultTreeModel) tree.getModel();
                model.nodeChanged(treeNode);
            } else {
                nameCheckLabel.setText("Invalid name");
            }
        });
        nameText.setColumns(30);
        JCheckBox propertyChange = new JCheckBox("Add PropertyChange Support");
        propertyChange.setAlignmentX(Component.CENTER_ALIGNMENT);
        propertyChange.setSelected(exportBean.isAddPropertyChangeSupport());
        propertyChange.addActionListener(e -> exportBean.setAddPropertyChangeSupport(propertyChange.isSelected()));
        propertyChange.setToolTipText("Select whether PropertyChange support should be added to be able to bind to Properties.");

        GridBagConstraints c = new GridBagConstraints();
        c.weightx = 20;
        add(name, c);
        c.weightx = 80;
        c.gridx = 1;
        add(nameText, c);
        c.gridy = 1;
        add(nameCheckLabel, c);
        c.gridy = 2;
        c.gridx = 0;
        c.gridwidth = 2;
        add(propertyChange, c);
    }
}

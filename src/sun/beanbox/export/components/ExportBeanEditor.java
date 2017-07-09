package sun.beanbox.export.components;

import sun.beanbox.export.Exporter;
import sun.beanbox.export.datastructure.ExportBean;
import sun.beanbox.export.datastructure.ExportConstraintViolation;
import sun.beanbox.export.util.StringUtil;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import java.awt.*;

/**
 * Created by Andreas Ertlschweiger on 22.06.2017.
 * <p>
 * This class represents the view to customise the ExportBeans during exporting.
 */
class ExportBeanEditor extends JPanel {

    /**
     * This constructs all UI elements required to customise an ExportBean.
     *
     * @param exporter   the exporter component
     * @param exportBean the ExportBean to be customised
     * @param tree       the TreeView to update name changes
     * @param treeNode   the node to be updated on name changes
     */
    ExportBeanEditor(Exporter exporter, ExportBean exportBean, JTree tree, DefaultMutableTreeNode treeNode) {
        setLayout(new GridBagLayout());

        JLabel name = new JLabel("Name: ");
        name.setToolTipText("Configure the name of the bean. The name must be a valid Java identifier and must not be a keyword. " +
                "Pay attention that it does not conflict with any beans or resources that you are including.");
        TextField nameText = new TextField(exportBean.getName());
        java.util.List<ExportConstraintViolation> violationList = exporter.checkIfValidClassName(exportBean, nameText.getText());
        JLabel nameCheckLabel = new JLabel(violationList == null ? "Valid name" : "Invalid name");
        nameCheckLabel.setToolTipText(violationList == null ? "No constraint violations found." : StringUtil.concatenateViolations(violationList));
        nameText.addTextListener(e -> {
            java.util.List<ExportConstraintViolation> violations = exporter.checkIfValidClassName(exportBean, nameText.getText());
            if (violations == null) {
                exportBean.setName(nameText.getText());
                nameCheckLabel.setText("Valid name");
                nameCheckLabel.setToolTipText("No constraint violations found.");
                DefaultTreeModel model = (DefaultTreeModel) tree.getModel();
                model.nodeChanged(treeNode);
            } else {
                nameCheckLabel.setText("Invalid name");
                nameCheckLabel.setToolTipText(StringUtil.concatenateViolations(violations));
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

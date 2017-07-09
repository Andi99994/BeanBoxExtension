package sun.beanbox.export.components;

import sun.beanbox.export.Exporter;
import sun.beanbox.export.datastructure.ExportBean;
import sun.beanbox.export.datastructure.ExportConstraintViolation;
import sun.beanbox.export.datastructure.ExportMethod;
import sun.beanbox.export.util.StringUtil;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import java.awt.*;

/**
 * Created by Andreas Ertlschweiger on 22.06.2017.
 * <p>
 * This class represents the view to customise the ExportMethods during exporting.
 */
class ExportMethodEditor extends JPanel {

    /**
     * This constructs all UI elements required to customise an ExportMethod.
     *
     * @param exporter     the exporter component
     * @param exportMethod the ExportMethod to be customised
     * @param tree         the TreeView to update name changes
     * @param treeNode     the node to be updated on name changes
     */
    ExportMethodEditor(Exporter exporter, ExportMethod exportMethod, JTree tree, DefaultMutableTreeNode treeNode) {
        setLayout(new GridBagLayout());
        ExportBean exportBean = null;
        DefaultMutableTreeNode current = treeNode;
        while (exportBean == null) {
            current = (DefaultMutableTreeNode) current.getParent();
            if (current.getUserObject() instanceof ExportBean) {
                exportBean = (ExportBean) current.getUserObject();
            }
        }
        JLabel name = new JLabel("Name: ");
        name.setToolTipText("The name of the method. It must be unique among all methods and be a valid Java identifier.");
        TextField nameText = new TextField(exportMethod.getName());
        java.util.List<ExportConstraintViolation> violationList = exporter.checkIfValidMethodName(exportBean, exportMethod, nameText.getText());
        JLabel nameCheckLabel = new JLabel(violationList == null ? "Valid name" : "Invalid name");
        nameCheckLabel.setToolTipText(violationList == null ? "No constraint violations found." : StringUtil.concatenateViolations(violationList));
        final ExportBean finalExportBean = exportBean;
        nameText.addTextListener(e -> {
            java.util.List<ExportConstraintViolation> violations = exporter.checkIfValidMethodName(finalExportBean, exportMethod, nameText.getText());
            if (violations == null) {
                exportMethod.setName(nameText.getText());
                nameCheckLabel.setText("Valid name");
                nameCheckLabel.setToolTipText("No constraint violations found.");
                DefaultTreeModel model = (DefaultTreeModel) tree.getModel();
                model.nodeChanged(treeNode);
            } else {
                nameCheckLabel.setText("Invalid name");
                nameCheckLabel.setToolTipText(StringUtil.concatenateViolations(violations));
            }
        });
        nameText.setColumns(22);
        JCheckBox include = new JCheckBox("Include in input interface");
        include.setEnabled(exportMethod.getNode().isInputInterface());
        include.setAlignmentX(Component.CENTER_ALIGNMENT);
        include.setSelected(exportMethod.isExport());
        include.addActionListener(e -> exportMethod.setExport(include.isSelected()));
        include.setToolTipText("Select whether the method should be included after export. A method can only be included if it's bean is part of the input interface.");

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
        add(include, c);
    }
}

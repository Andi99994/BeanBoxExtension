package sun.beanbox.export.components;

import sun.beanbox.export.Exporter;
import sun.beanbox.export.datastructure.*;
import sun.beanbox.export.util.StringUtil;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import java.awt.*;
import java.util.List;

/**
 * Created by Andreas Ertlschweiger on 22.06.2017.
 * <p>
 * This class represents the view to customise the BeanNodes during exporting.
 */
class BeanNodeEditor extends JPanel {

    /**
     * This constructs all UI elements required to customise a BeanNode.
     *
     * @param exporter the exporter component
     * @param beanNode the BeanNode to be customised
     * @param tree     the TreeView to update name changes
     * @param treeNode the node to be updated on name changes
     */
    BeanNodeEditor(Exporter exporter, BeanNode beanNode, JTree tree, DefaultMutableTreeNode treeNode) {
        setLayout(new GridBagLayout());

        Font plainFont = new Font("Dialog", Font.PLAIN, 12);
        ExportBean exportBean = null;
        DefaultMutableTreeNode current = treeNode;
        while (exportBean == null) {
            current = (DefaultMutableTreeNode) current.getParent();
            if (current.getUserObject() instanceof ExportBean) {
                exportBean = (ExportBean) current.getUserObject();
            }
        }
        JLabel name = new JLabel("Name: ");
        name.setToolTipText("The name of the bean. It must be unique among all beans in this ExportBean and be a valid Java identifier.");
        TextField nameText = new TextField(beanNode.getName());
        java.util.List<ExportConstraintViolation> violationList = exporter.checkIfValidNodeName(exportBean, beanNode, nameText.getText());
        JLabel nameCheckLabel = new JLabel(violationList == null ? "Valid name" : "Invalid name");
        nameCheckLabel.setToolTipText(violationList == null ? "No constraint violations found." : StringUtil.concatenateViolations(violationList));
        final ExportBean finalExportBean = exportBean;
        nameText.addTextListener(e -> {
            java.util.List<ExportConstraintViolation> violations = exporter.checkIfValidNodeName(finalExportBean, beanNode, nameText.getText());
            if (violations == null) {
                beanNode.setName(nameText.getText());
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
        JLabel directCompositionsLabel = new JLabel("Direct Compositions:");
        JLabel hookupCompositions = new JLabel("Adapter Compositions:");
        JLabel propertyBindings = new JLabel("Property Bindings:");
        JCheckBox manifest = new JCheckBox("Include in manifest");
        manifest.setAlignmentX(Component.CENTER_ALIGNMENT);
        manifest.setSelected(beanNode.isRegisterInManifest());
        manifest.addActionListener(e -> beanNode.setRegisterInManifest(manifest.isSelected()));
        manifest.setToolTipText("Select whether the bean should still be visible as a bean after export by registering it in the manifest.");

        JCheckBox input = new JCheckBox("Input interface");
        input.setAlignmentX(Component.CENTER_ALIGNMENT);
        input.setSelected(beanNode.isInputInterface());
        input.addActionListener(e -> beanNode.setInputInterface(input.isSelected()));
        input.setToolTipText("Select whether the bean should be part of the input interface.");

        JCheckBox output = new JCheckBox("Output interface");
        output.setAlignmentX(Component.CENTER_ALIGNMENT);
        output.setSelected(beanNode.isOutputInterface());
        output.addActionListener(e -> beanNode.setOutputInterface(output.isSelected()));
        output.setToolTipText("Select whether the bean should be part of the output interface.");

        GridBagConstraints c = new GridBagConstraints();
        Insets topPadding = new Insets(15, 0, 0, 0);
        Insets noPadding = new Insets(0, 0, 0, 0);
        c.weightx = 20;
        add(name, c);
        c.gridx = 1;
        c.weightx = 80;
        add(nameText, c);
        c.gridy = 1;
        add(nameCheckLabel, c);
        c.gridy = 2;
        c.gridx = 0;
        c.gridwidth = 2;
        c.insets = topPadding;
        add(manifest, c);
        c.gridy = 3;
        add(input, c);
        c.gridy = 4;
        add(output, c);
        //print all direct compositions
        List<DirectCompositionEdge> directCompositions = beanNode.getDirectCompositionEdges();
        if (directCompositions != null && directCompositions.size() > 0) {
            ++c.gridy;
            add(directCompositionsLabel, c);
            for (DirectCompositionEdge edge : directCompositions) {
                JLabel labelStart = new JLabel(edge.getStart().toString());
                labelStart.setFont(plainFont);
                ++c.gridy;
                add(labelStart, c);
                c.insets = noPadding;
                JLabel labelArrow = new JLabel(">");
                labelArrow.setFont(plainFont);
                ++c.gridy;
                add(labelArrow, c);
                JLabel labelEnd = new JLabel(edge.getEnd().toString());
                labelEnd.setFont(plainFont);
                ++c.gridy;
                add(labelEnd, c);
                c.insets = topPadding;
            }
        }

        //print all compositions that use an adapter
        List<AdapterCompositionEdge> hookupCompositionEdges = beanNode.getAdapterCompositionEdges();
        if (hookupCompositionEdges != null && hookupCompositionEdges.size() > 0) {
            ++c.gridy;
            add(hookupCompositions, c);
            c.insets = noPadding;
            for (AdapterCompositionEdge edge : hookupCompositionEdges) {
                JLabel labelStart = new JLabel(edge.getStart().toString());
                labelStart.setFont(plainFont);
                ++c.gridy;
                add(labelStart, c);
                c.insets = noPadding;
                JLabel labelArrow = new JLabel(">");
                labelArrow.setFont(plainFont);
                ++c.gridy;
                add(labelArrow, c);
                JLabel labelEnd = new JLabel(edge.getEnd().toString());
                labelEnd.setFont(plainFont);
                ++c.gridy;
                add(labelEnd, c);
                c.insets = topPadding;
            }
            c.insets = topPadding;
        }

        //print all property bindings
        List<PropertyBindingEdge> propertyBindingEdges = beanNode.getPropertyBindingEdges();
        if (propertyBindingEdges != null && propertyBindingEdges.size() > 0) {
            ++c.gridy;
            add(propertyBindings, c);
            c.insets = noPadding;
            for (PropertyBindingEdge edge : propertyBindingEdges) {
                JLabel labelStart = new JLabel(edge.getStart().toString());
                labelStart.setFont(plainFont);
                ++c.gridy;
                add(labelStart, c);
                c.insets = noPadding;
                JLabel labelArrow = new JLabel(">");
                labelArrow.setFont(plainFont);
                ++c.gridy;
                add(labelArrow, c);
                JLabel labelEnd = new JLabel(edge.getEnd().toString());
                labelEnd.setFont(plainFont);
                ++c.gridy;
                add(labelEnd, c);
                c.insets = topPadding;
            }
            c.insets = topPadding;
        }
    }
}

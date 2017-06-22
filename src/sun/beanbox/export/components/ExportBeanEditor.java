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

    private static final String NAME_LABEL_TEXT = "Name: ";
    private static final String NAME_LABEL_TOOLTIP_TEXT = "Configure the name of the bean. The name must be a valid Java identifier and must not be a keyword";
    private static final String NAME_CHECK_LABEL_TEXT_VALID = "Valid name";
    private static final String NAME_CHECK_LABEL_TEXT_INVALID = "Invalid name";

    public ExportBeanEditor(Exporter exporter, ExportBean exportBean, JTree tree, DefaultMutableTreeNode treeNode) {
        setLayout(new GridBagLayout());

        JLabel name = new JLabel(NAME_LABEL_TEXT);
        name.setToolTipText(NAME_LABEL_TOOLTIP_TEXT);
        JLabel nameCheckLabel = new JLabel(NAME_CHECK_LABEL_TEXT_VALID);
        TextField nameText = new TextField();
        nameText.setText(exportBean.getBeanName());
        nameText.addTextListener(e -> {
            if (exporter.checkIfValidClassName(nameText.getText())) {
                exportBean.setBeanName(nameText.getText());
                nameCheckLabel.setText(NAME_CHECK_LABEL_TEXT_VALID);
                DefaultTreeModel model = (DefaultTreeModel) tree.getModel();
                model.nodeChanged(treeNode);
            } else {
                nameCheckLabel.setText(NAME_CHECK_LABEL_TEXT_INVALID);
            }
        });
        nameText.setColumns(30);

        GridBagConstraints c = new GridBagConstraints();
        c.weightx = 20;
        add(name, c);
        c.weightx = 80;
        c.gridx = 1;
        add(nameText, c);
        c.gridy = 1;
        add(nameCheckLabel, c);
    }
}

package sun.beanbox.export.components;

import sun.beanbox.export.Exporter;
import sun.beanbox.export.datastructure.ExportBean;
import sun.beanbox.export.datastructure.ExportMethod;
import sun.beanbox.export.datastructure.ExportProperty;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.xml.soap.Text;
import java.awt.*;
import java.beans.PropertyDescriptor;
import java.beans.PropertyEditor;
import java.beans.PropertyVetoException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Created by Andi on 22.06.2017.
 */
public class ExportMethodEditor extends JPanel {

    public ExportMethodEditor(Exporter exporter, ExportMethod exportMethod, JTree tree, DefaultMutableTreeNode treeNode) {
        setLayout(new GridBagLayout());
        ExportBean exportBean = (ExportBean) ((DefaultMutableTreeNode) treeNode.getParent().getParent().getParent()).getUserObject();
        JLabel name = new JLabel("Name: ");
        name.setToolTipText("The name of the method. It must be unique among all methods and be a valid Java identifier.");
        TextField nameText = new TextField();
        nameText.setText(exportMethod.getName());
        JLabel nameCheckLabel = new JLabel(exporter.checkIfValidPropertyName(exportBean, nameText.getText()) ? "Valid name" : "Invalid name");
        nameText.addTextListener(e -> {
            if (exporter.checkIfValidPropertyName(exportBean, nameText.getText())) {
                exportMethod.setName(nameText.getText());
                nameCheckLabel.setText("Valid name");
                DefaultTreeModel model = (DefaultTreeModel) tree.getModel();
                model.nodeChanged(treeNode);
            } else {
                nameCheckLabel.setText("Invalid name");
            }
        });
        nameText.setColumns(22);
        JCheckBox include = new JCheckBox("Include in input interface");
        include.setEnabled(exportMethod.getNode().isInputInterface());
        include.setAlignmentX(Component.CENTER_ALIGNMENT);
        include.setSelected(exportMethod.isInclude());
        include.addActionListener(e -> exportMethod.setInclude(include.isSelected()));
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

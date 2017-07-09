package sun.beanbox.export.components;

import sun.beanbox.export.datastructure.BeanNode;

import javax.swing.*;
import java.awt.*;

/**
 * Created by Andreas Ertlschweiger on 22.06.2017.
 * <p>
 * This class represents the view to customise all methods at the same time during exporting.
 */
class AllMethodsEditor extends JPanel {

    /**
     * This constructs all UI elements required to customise all methods.
     *
     * @param node the node to update methods
     */

    AllMethodsEditor(final BeanNode node) {
        setLayout(new GridBagLayout());

        JCheckBox include = new JCheckBox("Include all in input interface");
        include.setEnabled(node.isInputInterface());
        include.setAlignmentX(Component.CENTER_ALIGNMENT);
        include.setSelected(node.isAllMethodsInInputInterface());
        include.addActionListener(e -> node.setAllMethodsInInputInterface(include.isSelected()));
        include.setToolTipText("Include or exclude all methods of this Bean in the input interface.");

        GridBagConstraints c = new GridBagConstraints();
        c.weightx = 20;
        c.gridy = 0;
        c.gridx = 0;
        c.gridwidth = 2;
        add(include, c);
    }
}

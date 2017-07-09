package sun.beanbox.export.components;

import sun.beanbox.export.datastructure.BeanNode;

import javax.swing.*;
import java.awt.*;

/**
 * Created by Andreas Ertlschweiger on 22.06.2017.
 * <p>
 * This class represents the view to customise all Properties at the same time during exporting.
 */
class AllPropertiesEditor extends JPanel {

    /**
     * This constructs all UI elements required to customise all Properties.
     *
     * @param node the node update Properties
     */

    AllPropertiesEditor(final BeanNode node) {
        setLayout(new GridBagLayout());

        JCheckBox configurable = new JCheckBox("Enable configurability for all properties");
        configurable.setAlignmentX(Component.CENTER_ALIGNMENT);
        configurable.setSelected(node.isAllPropertiesConfigurable());
        configurable.addActionListener(e -> node.setAllPropertiesConfigurable(configurable.isSelected()));
        configurable.setToolTipText("Enable/Disable configurability for all properties of this Bean.");

        GridBagConstraints c = new GridBagConstraints();
        c.weightx = 20;
        c.gridy = 0;
        c.gridx = 0;
        c.gridwidth = 2;
        add(configurable, c);
    }
}

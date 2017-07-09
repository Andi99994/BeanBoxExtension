package sun.beanbox.export.components;

import sun.beanbox.export.datastructure.BeanNode;

import javax.swing.*;
import java.awt.*;

/**
 * Created by Andreas Ertlschweiger on 22.06.2017.
 * <p>
 * This class represents the view to customise all events at the same time during exporting.
 */
class AllEventsEditor extends JPanel {

    /**
     * This constructs all UI elements required to customise all events.
     *
     * @param node the node to update events
     */

    AllEventsEditor(final BeanNode node) {
        setLayout(new GridBagLayout());

        JCheckBox include = new JCheckBox("Include all in output interface");
        include.setEnabled(node.isOutputInterface());
        include.setAlignmentX(Component.CENTER_ALIGNMENT);
        include.setSelected(node.isAllEventsInOutputInterface());
        include.addActionListener(e -> node.setAllEventsInOutputInterface(include.isSelected()));
        include.setToolTipText("Include or exclude all events of this Bean in the output interface.");

        GridBagConstraints c = new GridBagConstraints();
        c.weightx = 20;
        c.gridy = 0;
        c.gridx = 0;
        c.gridwidth = 2;
        add(include, c);
    }
}

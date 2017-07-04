package sun.beanbox.export.components;

import sun.beanbox.export.datastructure.BeanNode;
import sun.beanbox.export.datastructure.DirectCompositionEdge;
import sun.beanbox.export.datastructure.AdapterCompositionEdge;
import sun.beanbox.export.datastructure.PropertyBindingEdge;

import javax.swing.*;
import java.awt.*;
import java.util.List;

/**
 * Created by Andreas on 22.06.2017.
 *
 * This class represents the view to customise the BeanNodes during exporting.
 */
public class BeanNodeEditor extends JPanel {

    /**
     * This constructs all UI elements required to customise a BeanNode.
     *
     * @param beanNode the BeanNode to be customised
     */
    public BeanNodeEditor(BeanNode beanNode) {
        setLayout(new GridBagLayout());

        Font plainFont = new Font("Dialog", Font.PLAIN, 12);
        JLabel name = new JLabel("Name: ");
        JLabel nameText = new JLabel(beanNode.getName());
        nameText.setFont(plainFont);
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
        Insets topPadding = new Insets(15,0,0,0);
        Insets noPadding = new Insets(0,0,0,0);
        c.weightx = 20;
        add(name, c);
        c.gridx = 1;
        c.weightx = 80;
        add(nameText, c);
        c.gridy = 1;
        c.gridx = 0;
        c.gridwidth = 2;
        c.insets = topPadding;
        add(manifest, c);
        c.gridy = 2;
        add(input, c);
        c.gridy = 3;
        add(output, c);
        //print all direct compositions
        List<DirectCompositionEdge> directCompositions = beanNode.getDirectCompositionEdges();
        if(directCompositions != null && directCompositions.size() > 0) {
            ++c.gridy;
            add(directCompositionsLabel, c);
            for(DirectCompositionEdge edge : directCompositions) {
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
        if(hookupCompositionEdges != null && hookupCompositionEdges.size() > 0) {
            ++c.gridy;
            add(hookupCompositions, c);
            c.insets = noPadding;
            for(AdapterCompositionEdge edge : hookupCompositionEdges) {
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
        if(propertyBindingEdges != null && propertyBindingEdges.size() > 0) {
            ++c.gridy;
            add(propertyBindings, c);
            c.insets = noPadding;
            for(PropertyBindingEdge edge : propertyBindingEdges) {
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

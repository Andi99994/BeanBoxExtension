package sun.beanbox.export;

import sun.beanbox.export.datastructure.BeanGraph;
import sun.beanbox.export.datastructure.BeanNode;

import javax.swing.*;
import java.awt.*;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by Andi on 26.05.2017.
 */
public class GraphPanel extends JPanel {

    private BeanGraph graph;

    private int startXOffset = 10;
    private int startYOffset = 10;
    private int nodeXOffset = 20;
    private int nodeYOffset = 20;
    private int textSize = 10;

    public GraphPanel(BeanGraph objectGraph){
        graph = objectGraph;
    }

    @Override
    public void paint(Graphics g){
        int graphBreadth = graph.getBreadth();
        Set<BeanNode> printedNodes = new HashSet<>();
        for (int i = 0; i < graph.getStartNodes().size(); i++) {
            BeanNode current = graph.getStartNodes().get(i);
            g.drawString(current.getDisplayName(), startXOffset, startYOffset);
            for (BeanNode node : current.getEnd()) {

            }
        }
    }
}

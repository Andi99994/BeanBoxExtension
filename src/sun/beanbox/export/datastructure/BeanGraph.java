package sun.beanbox.export.datastructure;

import java.util.*;

/**
 * Created by Andi on 26.05.2017.
 */
public class BeanGraph {

    private List<BeanNode> startNodes;

    public BeanGraph(List<BeanNode> startNodes){
        this.startNodes = startNodes;
    }

    public List<BeanNode> getStartNodes() {
        return startNodes;
    }

    public void setStartNodes(List<BeanNode> startNodes) {
        this.startNodes = startNodes;
    }
    public int getBreadth(){
        int breadth = startNodes.size();
        List<BeanNode> currentLevel = new ArrayList<>();
        Set<BeanNode> visitedNodes = new HashSet<>();
        currentLevel.addAll(startNodes);
        while (!currentLevel.isEmpty()){
            breadth = currentLevel.size() > breadth ? currentLevel.size() : breadth;
            List<BeanNode> nextLevel = new ArrayList<>();
            for (BeanNode node : currentLevel) {
                for (BeanNode childNode : node.getEnd()){
                    if (!visitedNodes.contains(childNode)){
                        nextLevel.add(childNode);
                        visitedNodes.add(childNode);
                    }
                }
            }
            currentLevel = nextLevel;
        }
        return breadth;
    }
}

package sun.beanbox.export.datastructure;

import java.util.*;

/**
 * Created by Andi on 26.05.2017.
 */
public class BeanGraph {

    private List<BeanNode> inputNodes;
    private List<BeanNode> outputNodes;

    public BeanGraph(List<BeanNode> inputNodes, List<BeanNode> outputNodes){
        this.inputNodes = inputNodes;
        this.outputNodes = outputNodes;
    }

    public int getBreadth(){
        int breadth = inputNodes.size();
        List<BeanNode> currentLevel = new ArrayList<>();
        Set<BeanNode> visitedNodes = new HashSet<>();
        currentLevel.addAll(inputNodes);
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

    public List<BeanNode> getInputNodes() {
        return inputNodes;
    }

    public void setInputNodes(List<BeanNode> inputNodes) {
        this.inputNodes = inputNodes;
    }

    public List<BeanNode> getOutputNodes() {
        return outputNodes;
    }

    public void setOutputNodes(List<BeanNode> outputNodes) {
        this.outputNodes = outputNodes;
    }
}

package sun.beanbox.export.datastructure;

import java.util.*;

/**
 * Created by Andi on 26.05.2017.
 */
public class BeanGraph {

    private List<BeanNode> inputNodes = new LinkedList<>();
    private List<BeanNode> outputNodes = new LinkedList<>();
    private List<BeanNode> allNodes = new LinkedList<>();

    public BeanGraph(Collection<BeanNode> inputNodes, Collection<BeanNode> outputNodes, Collection<BeanNode> allNodes){
        this.inputNodes.addAll(inputNodes);
        this.outputNodes.addAll(outputNodes);
        this.allNodes.addAll(allNodes);
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

    public List<BeanNode> getAllNodes() {
        return allNodes;
    }

    public void setAllNodes(List<BeanNode> allNodes) {
        this.allNodes = allNodes;
    }
}

package sun.beanbox.export.datastructure;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by Andreas on 26.05.2017.
 */
public class BeanGraph {

    private List<BeanNode> allNodes = new LinkedList<>();

    public BeanGraph(Collection<BeanNode> inputNodes, Collection<BeanNode> outputNodes, Collection<BeanNode> allNodes){
        for(BeanNode node : inputNodes) {
            node.setInputInterface(true);
        }
        for(BeanNode node : outputNodes) {
            node.setOutputInterface(true);
        }
        this.allNodes.addAll(allNodes);
        Set<String> beanNames = new HashSet<>();
        for (BeanNode node : allNodes) {
            String beanName = node.getName();
            int counter = 2;
            if(beanNames.contains(beanName)) {
                while (beanNames.contains(beanName + counter)) {
                    counter++;
                }
                beanName+= counter;
                node.setName(beanName);
            }
            beanNames.add(beanName);
        }
    }

    List<BeanNode> getInputNodes() {
        return allNodes.stream().filter(BeanNode::isInputInterface).collect(Collectors.toList());
    }

    List<BeanNode> getOutputNodes() {
        return allNodes.stream().filter(BeanNode::isOutputInterface).collect(Collectors.toList());
    }

    List<BeanNode> getAllNodes() {
        return allNodes;
    }
}

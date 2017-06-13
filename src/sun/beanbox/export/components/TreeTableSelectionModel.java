package sun.beanbox.export.components;

import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.tree.DefaultTreeSelectionModel;

public class TreeTableSelectionModel extends DefaultTreeSelectionModel {

    public TreeTableSelectionModel() {
        super();

        getListSelectionModel().addListSelectionListener(e -> {

        });
    }

    ListSelectionModel getListSelectionModel() {
        return listSelectionModel;
    }
}

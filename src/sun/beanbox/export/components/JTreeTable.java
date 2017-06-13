package sun.beanbox.export.components;

import java.awt.Dimension;

import javax.swing.JTable;

public class JTreeTable extends JTable {

    private TreeTableCellRenderer tree;


    public JTreeTable(AbstractTreeTableModel treeTableModel) {
        super();

        // JTree erstellen.
        tree = new TreeTableCellRenderer(this, treeTableModel);

        // Modell setzen.
        super.setModel(new TreeTableModelAdapter(treeTableModel, tree));

        // Gleichzeitiges Selektieren fuer Tree und Table.
        TreeTableSelectionModel selectionModel = new TreeTableSelectionModel();
        tree.setSelectionModel(selectionModel); //For the tree
        setSelectionModel(selectionModel.getListSelectionModel()); //For the table


        // Renderer fuer den Tree.
        setDefaultRenderer(TreeTableModel.class, tree);
        // Editor fuer die TreeTable
        setDefaultEditor(TreeTableModel.class, new TreeTableCellEditor(tree, this));

        // Kein Grid anzeigen.
        setShowGrid(false);

        // Keine Abstaende.
        setIntercellSpacing(new Dimension(0, 0));

    }
}

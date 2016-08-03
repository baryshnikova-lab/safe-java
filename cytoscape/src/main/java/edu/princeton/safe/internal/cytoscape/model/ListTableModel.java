package edu.princeton.safe.internal.cytoscape.model;

import java.util.List;

import javax.swing.table.AbstractTableModel;

@SuppressWarnings("serial")
public abstract class ListTableModel<T> extends AbstractTableModel {
    protected List<T> rows;

    public ListTableModel(List<T> rows) {
        this.rows = rows;
    }

    @Override
    public int getRowCount() {
        return rows.size();
    }

    public T getRow(int index) {
        return rows.get(index);
    }
}
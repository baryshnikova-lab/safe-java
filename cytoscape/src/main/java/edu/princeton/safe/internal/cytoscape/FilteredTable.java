package edu.princeton.safe.internal.cytoscape;

import java.awt.Component;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.concurrent.TimeUnit;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.RowFilter;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;

import net.miginfocom.swing.MigLayout;
import rx.Observable;
import rx.Observable.OnSubscribe;
import rx.Subscriber;
import rx.functions.Action1;

public class FilteredTable<T> {

    Component panel;
    private JTextField queryField;
    private JTable table;
    private TableRowSorter<TableModel> sorter;

    public FilteredTable(ListTableModel<T> model) {
        queryField = new JTextField();

        table = new JTable(model);
        table.setFillsViewportHeight(true);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);

        RegexRowFilter filter = new RegexRowFilter() {
            @Override
            public boolean include(RowFilter.Entry<? extends TableModel, ? extends Integer> entry) {
                String value = (String) model.getValueAt(entry.getIdentifier(), 0);
                return value != null && predicate.test(value);
            }
        };

        sorter = new TableRowSorter<TableModel>(model) {
            @Override
            public void sort() {
                filter.setQuery(queryField.getText());
                super.sort();
            }
        };

        sorter.setRowFilter(filter);
        table.setRowSorter(sorter);

        Observable<String> queryObservable = Observable.create(new OnSubscribe<String>() {
            @Override
            public void call(Subscriber<? super String> subscriber) {
                queryField.addKeyListener(new KeyAdapter() {
                    @Override
                    public void keyReleased(KeyEvent e) {
                        subscriber.onNext(queryField.getText());
                    }
                });
            }
        });

        queryObservable.debounce(300, TimeUnit.MILLISECONDS)
                       .distinctUntilChanged()
                       .subscribe(new Action1<String>() {
                           @Override
                           public void call(String text) {
                               sorter.sort();
                           }
                       });

        JScrollPane scrollPane = new JScrollPane(table);

        JPanel panel = UiUtil.createJPanel();
        panel.setLayout(new MigLayout("fill, insets 0", "[min!, grow 0][grow]", "[][grow]"));

        panel.add(SafeUtil.createIconLabel(SafeUtil.SEARCH_ICON));
        panel.add(queryField, "growx, wrap");
        panel.add(scrollPane, "span 2, grow");

        this.panel = panel;
    }

    public TableRowSorter<TableModel> getSorter() {
        return sorter;
    }

    public Component getPanel() {
        return panel;
    }

    public JTable getTable() {
        return table;
    }
}

package edu.princeton.safe.internal.cytoscape;

import java.util.function.Predicate;
import java.util.regex.Pattern;

import javax.swing.RowFilter;
import javax.swing.table.TableModel;

public abstract class RegexRowFilter extends RowFilter<TableModel, Integer> {
    protected Predicate<String> predicate;
    String query;

    public void setQuery(String value) {
        if (value != null && value.equals(query)) {
            return;
        }

        query = value;
        predicate = Pattern.compile(Pattern.quote(value), Pattern.CASE_INSENSITIVE)
                           .asPredicate();
    }
}
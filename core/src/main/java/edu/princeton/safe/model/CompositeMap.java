package edu.princeton.safe.model;

import java.util.List;

public interface CompositeMap {

    List<? extends Domain> getDomains(int typeIndex);

    boolean isTop(int attributeIndex,
                  int typeIndex);

    void setTop(int attributeIndex,
                int typeIndex,
                boolean isTop);

}

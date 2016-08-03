package edu.princeton.safe.internal.cytoscape.model;

public class NameValuePair<T> {

    String name;
    T value;

    public NameValuePair(String name,
                         T value) {
        this.name = name;
        this.value = value;
    }

    public String getName() {
        return name;
    }

    public T getValue() {
        return value;
    }

    @Override
    public String toString() {
        return name;
    }
}

package edu.princeton.safe.internal.cytoscape;

public class Factory<T> {
    String id;
    FactoryMethod<T> method;

    public Factory(String id,
                   FactoryMethod<T> method) {
        this.id = id;
        this.method = method;
    }

    public String getId() {
        return id;
    }

    public T create() {
        return method.create();
    }
}
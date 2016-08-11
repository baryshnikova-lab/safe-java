package edu.princeton.safe.internal.io;

@FunctionalInterface
public interface LineHandler {
    boolean handle(String[] parts);
}
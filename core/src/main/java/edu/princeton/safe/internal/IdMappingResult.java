package edu.princeton.safe.internal;

import java.util.function.Consumer;

import com.carrotsearch.hppc.IntIntHashMap;
import com.carrotsearch.hppc.IntIntMap;
import com.carrotsearch.hppc.ObjectIntHashMap;
import com.carrotsearch.hppc.ObjectIntMap;
import com.carrotsearch.hppc.cursors.ObjectIntCursor;

public class IdMappingResult {
    static final String DEFAULT_COMMENT_CHARACTER = "#";

    public IntIntMap coverage;
    public int totalNetworkNodes;
    public int totalAnnotationNodes;

    IntIntMap columnFrequencies;
    IntIntMap firstLineOfColumnWidth;
    ObjectIntMap<String> firstCharacterFrequencies;

    public IdMappingResult() {
        columnFrequencies = new IntIntHashMap();
        firstCharacterFrequencies = new ObjectIntHashMap<>();
        firstLineOfColumnWidth = new IntIntHashMap();
    }

    public String getCommentCharacter() {
        int[] topCount = { 0 };
        String[] commentCharacter = { null };
        firstCharacterFrequencies.forEach((Consumer<? super ObjectIntCursor<String>>) c -> {
            if (c.value > topCount[0]) {
                commentCharacter[0] = c.key;
                topCount[0] = c.value;
            }
        });

        if (commentCharacter[0] == null) {
            return DEFAULT_COMMENT_CHARACTER;
        }

        return commentCharacter[0];
    }

    public int getLabelLineIndex() {
        int columnCount = getColumnCount();
        return firstLineOfColumnWidth.getOrDefault(columnCount, 0);
    }

    public int getColumnCount() {
        return Util.getTopKey(columnFrequencies, 0);
    }

}

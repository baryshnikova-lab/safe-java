package edu.princeton.safe.internal;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import com.carrotsearch.hppc.ObjectIntHashMap;
import com.carrotsearch.hppc.ObjectIntMap;

import edu.princeton.safe.AnnotationProvider;

public class DomainLabeller {

    static final Set<String> stopWords;
    static final Pattern goAccessionPattern;

    static {
        stopWords = new HashSet<>();
        for (String word : new String[] { "to", "or", "and", "the", "a", "an", "via", "of", "from", "into", "in", "by",
                                          "process" }) {
            stopWords.add(word);
        }
        goAccessionPattern = Pattern.compile("GO:\\d+", Pattern.CASE_INSENSITIVE);
    }

    static boolean isStopWord(String word) {
        if (word == null) {
            return true;
        }
        if (stopWords.contains(word.toLowerCase())) {
            return true;
        }
        return goAccessionPattern.matcher(word)
                                 .matches();
    }

    static void assignLabels(AnnotationProvider annotationProvider,
                             List<DefaultDomain> domains) {

        domains.stream()
               .forEach(domain -> {
                   ObjectIntMap<String> wordCounts = new ObjectIntHashMap<>();

                   domain.forEachAttribute(attributeIndex -> {
                       String label = annotationProvider.getAttributeLabel(attributeIndex);
                       if (label == null) {
                           return;
                       }

                       countWords(wordCounts, label);
                   });

                   List<String> topWords = computeTopWords(wordCounts);
                   domain.name = String.join(" ", topWords);
               });
    }

    static void countWords(ObjectIntMap<String> wordCounts,
                           String label) {
        String[] words = label.split(" ");
        Arrays.stream(words)
              .filter(word -> !isStopWord(word))
              .distinct()
              .forEach(word -> {
                  int count = wordCounts.getOrDefault(word, 0);
                  wordCounts.put(word, count + 1);
              });
    }

    static List<String> computeTopWords(ObjectIntMap<String> wordCounts) {
        return StreamSupport.stream(wordCounts.spliterator(), false)
                            .map(c -> new WordCount(c.key, c.value))
                            .sorted((w1,
                                     w2) -> {
                                // Sort by count (descending) first
                                int result = w2.count - w1.count;
                                if (result != 0) {
                                    return result;
                                }

                                // ...then by label
                                return w1.word.compareToIgnoreCase(w2.word);
                            })
                            .map(w -> w.word)
                            .limit(5)
                            .collect(Collectors.toList());
    }

    static class WordCount {
        String word;
        int count;

        WordCount(String word,
                  int count) {

            this.word = word;
            this.count = count;
        }
    }

}

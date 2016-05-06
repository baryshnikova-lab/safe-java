#!/usr/bin/env python3

import sys

from scipy.io import loadmat

def main(session_path, scores_path):
    """Print listing of neighborhoods and attributes with worst correlations
    between MATLAB result and given score file.
    """

    minimumDelta = 0.05

    # Neighborhood score text file only has 3 significant digits so we round
    # MATLAB session to same value
    significantDigits = 3

    root = loadmat(session_path, struct_as_record=False, squeeze_me=True)
    layout = root['layout']

    label_indexes = {}
    for index, label in zip(range(len(layout.label)), layout.label):
        label = label.replace('&apos;', "'")
        label_indexes[label] = index

    node_indexes = {}
    attribute_indexes = {}
    with open(scores_path) as scores_file:
        header = scores_file.readline()
        attributes = header.split('\t')[1:]
        for line in scores_file:
            parts = line.split('\t')
            label = parts[0]
            node_index = label_indexes[label]
            for attribute_index, value in zip(range(len(parts) - 1), map(float, parts[1:])):
                y = round(value, significantDigits)
                x = round(layout.opacity[node_index][attribute_index], significantDigits)
                if abs(x - y) > minimumDelta:
                    increment(node_indexes, label)
                    increment(attribute_indexes, attribute_index)

    for d in [node_indexes, attribute_indexes]:
        print_sorted(d, lambda x: -x[1])

def increment(map, key):
    value = map.get(key)
    if value is None:
        value = 0
    value += 1
    map[key] = value

def print_sorted(map, key):
    items = list(map.items())
    items.sort(key=key)
    for pair in items:
        print(*pair, sep='\t')

if __name__ == '__main__':
    session_path = sys.argv[1]
    scores_path = sys.argv[2]
    main(session_path, scores_path)

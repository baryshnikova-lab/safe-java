#!/usr/bin/env python3

import matplotlib.pyplot as plt

import sys
import hist
import numpy

from scipy.io import loadmat

def main(session_path, scores_path, output_path, title):
    """Make scatter plot of MATLAB vs Java neighborhood scores.
    """

    root = loadmat(session_path, struct_as_record=False, squeeze_me=True)
    layout = root['layout']

    label_indexes = {}
    for index, label in zip(range(len(layout.label)), layout.label):
        label = label.replace('&apos;', "'")
        label_indexes[label] = index

    x = []
    y = []
    with open(scores_path) as scores_file:
        header = scores_file.readline()
        for line in scores_file:
            parts = line.split('\t')
            label = parts[0]
            node_index = label_indexes[label]
            for attribute_index, value in zip(range(len(parts) - 1), map(float, parts[1:])):
                y.append(round(value, 3))
                x.append(round(layout.opacity[node_index][attribute_index], 3))
    plot(output_path, x, y)

    d = numpy.array(x) - y
    print(numpy.nanstd(d))

def plot(output_path, x, y):
    # the histogram of the data
    plt.scatter(x, y, alpha=0.1, linewidth=0)

    plt.xlim(xmin=0, xmax=1)
    plt.ylim(ymin=0, ymax=1)
    plt.xlabel('MATLAB')
    plt.ylabel('Java')
    plt.title('Neighborhood Scores - %s' % title)
    plt.grid(True)

    plt.savefig(output_path)

if __name__ == '__main__':
    session_path = sys.argv[1]
    scores_path = sys.argv[2]
    title = sys.argv[3]
    output_path = sys.argv[4]
    main(session_path, scores_path, output_path, title)

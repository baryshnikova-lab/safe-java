#!/usr/bin/env python3

import matplotlib.pyplot as plt

import sys

def main(input_path, output_path, title):
    """Plot histogram of neighborhood score file.
    """

    data = read(input_path)
    plot(output_path, title, data)

def plot(output_path, title, data):
    n, bins, patches = plt.hist(data, 100, facecolor='green', alpha=0.75, linewidth=0)

    plt.yscale('log')
    plt.ylim(ymin=1, ymax=1e6)
    plt.xlabel('Score')
    plt.ylabel('Occurrences')
    plt.title('Neighborhood Scores - ' + title)
    plt.grid(True)

    plt.savefig(output_path)

def read(input_path):
    x=[]
    with open(input_path) as file:
        for line in file:
            parts = line.split('\t')
            if parts[0] == 'ORF':
                break

        for line in file:
            parts = line.split('\t')
            for value in parts[1:]:
                x.append(float(value))

    print(len(x))
    return x

if __name__ == '__main__':
    input_path = sys.argv[1]
    output_path = sys.argv[2]
    title = sys.argv[3]
    main(input_path, output_path, title)

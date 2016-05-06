#!/usr/bin/env python3

import sys
import csv

from bs4 import BeautifulSoup

def main(network_path, view_path, node_table_path, output_node_path, output_edge_path):
    """Extract tab-delimited node and edge files from a Cytoscape network.
    """
    
    cynode_indexes = {}
    node_labels = []
    node_aliases = []

    with open(node_table_path) as node_table_file:
        reader = csv.reader(node_table_file)
        for i in range(5):
            next(reader)

        index = 0
        for row in reader:
            node_id = row[0]
            node_label = row[1]
            node_alias = row[3]

            cynode_indexes[node_id] = index
            node_labels.append(node_label)
            node_aliases.append(node_alias)
            index += 1

    with open(output_node_path, 'w') as node_file:
        with open(view_path) as view_file:
            soup = BeautifulSoup(view_file, 'xml')
            for node in soup.find_all('node'):
                graphics = node.graphics
                node_id = node.get('cy:nodeId')
                label = node.get('label')
                alias = node_aliases[cynode_indexes[node_id]]
                print(label, alias, graphics.get('x'), graphics.get('y'), sep='\t', file=node_file)

    with open(output_edge_path, 'w') as edge_file:
        with open(network_path) as network_file:
            soup = BeautifulSoup(network_file, 'xml')
            for edge in soup.find_all('edge'):
                source_id = edge.get('source')
                target_id = edge.get('target')
                source = node_labels[cynode_indexes[source_id]]
                target = node_labels[cynode_indexes[target_id]]
                print(source, target, sep='\t', file=edge_file)

if __name__ == '__main__':
    network_path = sys.argv[1]
    view_path = sys.argv[2]
    node_table_path = sys.argv[3]
    output_node_path = sys.argv[4]
    output_edge_path = sys.argv[5]
    main(network_path, view_path, node_table_path, output_node_path, output_edge_path)

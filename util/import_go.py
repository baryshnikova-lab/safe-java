#!/usr/bin/env python3

import sys

from scipy.io import loadmat

def main(input_path):
    """Extract GO annotations from GO MATLAB data.
    """
    
    root = loadmat(input_path, struct_as_record=False, squeeze_me=True)
    go = root['go']

    header = ['ORF'] + list(go.term_names)
    print(*header, sep='\t')
    for i in range(len(go.orfs)):
        labels = go.term2orf[:, i]
        values = [go.orfs[i]] + list(labels)
        print(*values, sep='\t')

if __name__ == '__main__':
    input_path = sys.argv[1]
    main(input_path)

#!/usr/bin/env python3

import sys
import hist

from scipy.io import loadmat

def main(input_path):
    """Extract neighborhood scores from SAFE MATLAB session.
    """
    
    root = loadmat(input_path, struct_as_record=False, squeeze_me=True)
    layout = root['layout']

    print('\t'.join(['ORF'] + list(layout.group_names)))
    for i in range(len(layout.label)):
        values = list(map(lambda x: '%.3f' % x, layout.opacity[i]))
        print('\t'.join([layout.label[i]] + values))

if __name__ == '__main__':
    input_path = sys.argv[1]
    main(input_path)

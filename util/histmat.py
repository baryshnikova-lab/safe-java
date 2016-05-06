#!/usr/bin/env python3

import sys
import hist

from scipy.io import loadmat

def main(input_path, output_path, title):
    """Plot histogram of neighborhood scores from SAFE MATLAB session.
    """
    
    root = loadmat(input_path, struct_as_record=False, squeeze_me=True)
    layout = root['layout']
    hist.plot(output_path, title, layout.opacity.reshape(-1))

if __name__ == '__main__':
    input_path = sys.argv[1]
    output_path = sys.argv[2]
    title = sys.argv[3]
    main(input_path, output_path, title)

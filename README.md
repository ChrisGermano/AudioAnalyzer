# Audio Analyzer
This audio analyzer takes an input file or file path, and a target file or file path. The input file(s) are compared against the target file(s), and for each comparison the program returns whether the input was likely derived from the target. Transformations include, but are not limited to, cropping, volume adjustments, and pitch bending.

## Compilation
Code will be automatically compiled when running the included p4500 file.
NOTE: The p4500 file must be in the same directory as the source files when run.


## Running the Program:
While in the directory containing the source and p4500 files
run the following command:

./p4500 -f/-d <filepath1> -f/-d <filepath2>

Where <filepath1> and <filepath2> are absolute paths of the audio files to be
compared.

The -f tag must be used before a filepath referencing a single test
file.

The -d tag must be used before a filepath referencing a directory
containing any number of test files.


## Acknowledgements:
Eclipse was used to develop and test our code.

The FFT algorithm detailed in this link was used to analyze audio files -
  http://support.ircam.fr/docs/AudioSculpt/3.0/co/FFT%20Size.html

//Import relevant libraries
import java.io.File;
import java.util.ArrayList;
import java.util.Hashtable;

//The main class containing the core program functionality
public class p4500 {

    //Input paths given by the user
    ArrayList<Directory> dirs;

    boolean ErrorFound;

    Hashtable<String, ArrayList<Integer>> cachedFFT;

    //The generated tmp directory
    static String TMPDIR = "/tmp/p4500HGM/";
    static boolean DEBUG = false;

    public p4500() {
        dirs = new ArrayList<Directory>();
        cachedFFT = new Hashtable<String, ArrayList<Integer>>();
        ErrorFound = false;
    }


    // Check whether an absolute path returns a valid directory
    //Param: Absolute path of an input directory
    //Return: Whether a directory exists
    private boolean directoryCheck(String type, String dir) {
        try {
            File folder = new File(dir);
            if (!folder.exists())
                return false;
            return !(folder.isDirectory() ^ type.equals("-d"));
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    //Main loop handling audio matching
    //Param: Audio files to be compared
    public static void main(String[] args) {

        p4500 prog = new p4500();

        //Determine basic validity of user given input
        if (args.length < 4) {
            System.err.println("ERROR - Too few inputs!");
            System.exit(1);
        }

        String inputOne = args[0];
        String inputTwo = args[1];
        String inputThree = args[2];
        String inputFour = args[3];

        if (!((inputOne.equals("-f") || inputOne.equals("-d")) &&
                (inputThree.equals("-f") || inputThree.equals("-d")))) {
            System.err.println("ERROR - Invalid type parameter");
            System.exit(1);
        }

        //If the first input is invalid, exit with appropriate message
        if (prog.directoryCheck(inputOne, inputTwo)) {
            prog.dirs.add(new Directory(inputTwo));
        } else {
            String type = inputOne.equals("-d") ? "folder" : "file";
            System.err.println("ERROR - Derived " + type + " \"" + inputTwo
                    + "\" does not exist! Terminating.");
            System.exit(1);
        }



        //If the second input is invalid, exit with appropriate message
        if (prog.directoryCheck(inputThree, inputFour)) {
            prog.dirs.add(new Directory(inputFour));
        } else {
            String type = inputThree.equals("-d") ? "folder" : "file";
            System.err.println("ERROR - Source " + type + " \"" + inputFour
                    + "\" does not exist! Terminating.");
            System.exit(1);
        }

        //If any of the accepted files are of the wrong type, exit with
        //appropriate message
        for (Directory d : prog.dirs) {
            d.getFiles();
            if (d.audioFiles.size() == 0 && d.files.length == 0) {
                System.err.println("ERROR - " + d.absolutePath
                        + " is invalid!");
                System.exit(1);
            }
        }

        //Check all files in the first directory with the second
        Directory d1 = prog.dirs.get(0);
        Directory d2 = prog.dirs.get(1);
        for (int i = 0; i < d1.files.length; i++) {
            //generate audio file and validate file
            AudioFile f1 = d1.ValidateFile(d1.files[i], null);

            //if file couldn't be validated, throw error
            if(f1 == null){
                prog.ErrorFound = true;
                System.err.println("Invalid File");
                continue;
            }
            String f1name = f1.getBaseName();

            String f1path = f1.getAbsolutePath();
            //if the file has already been fft'd, dont add it
            //to the cache
            if(!prog.cachedFFT.containsKey(f1path)){
                f1.Initialize();
                //otherwise generate fft and add to cache
                prog.cachedFFT.put(f1.getAbsolutePath(),
                                   f1.calculateFFTofData());
            }

            for (int j = 0; j < d2.files.length; j++) {
                //generate audio file and validate file
                AudioFile f2 = d2.ValidateFile(d2.files[j], null);

                if(f2 == null){
                    prog.ErrorFound = true;
                    System.err.println("Invalid File");
                    continue;
                }

                String f2name = f2.getBaseName();

                String f2path = f2.getAbsolutePath();

                //if the file has already been fft'd, dont add it
                //to the cache
                if(!prog.cachedFFT.containsKey(f2path)){

                    //otherwise generate fft and add to cache
                    f2.Initialize();
                    prog.cachedFFT.put(f2.getAbsolutePath(),
                                       f2.calculateFFTofData());
                }
                // get the fft from the cache
                ArrayList<Integer> FFT1 = prog.cachedFFT.get(f1path);
                ArrayList<Integer> FFT2 = prog.cachedFFT.get(f2path);
                String subset_name;
                String parent_name;
                boolean match;

                //set the shorter file as a subset
                if(FFT1.size() > FFT2.size()){
                    subset_name = f2name;
                    parent_name = f1name;
                    //check match logic
                    match = prog.checkMatch(55, prog, FFT1, FFT2);
                } else {
                    subset_name = f1name;
                    parent_name = f2name;
                    //check match logic
                    match =  prog.checkMatch(55, prog, FFT2, FFT1);
                }
                if (match) {
                    System.out.println("MATCH " + subset_name + " " + parent_name);
                } else {
                    // TODO Change to valid output
                    System.out.println("NO MATCH");
                }
            }
        }

        if(!prog.ErrorFound){
            System.exit(0);
        } else {
            System.exit(1);
        }
    }

    private boolean checkMatch(float threshold, p4500 prog,
            ArrayList<Integer> FFT1, ArrayList<Integer> FFT2) {

        float percent_match;
        //compare the files and return a match percentages
        percent_match = prog.compareFiles( FFT1, FFT2);
        if (DEBUG) System.out.print(percent_match + ": ");
        return (percent_match > threshold);
    }

    //Looks for exact match of data between two AudioFiles
    //Param: Two AudioFiles to be compared
    //Return
    private float compareFiles(ArrayList<Integer> first,
            ArrayList<Integer> second) {
        // Only looks for an exact match of the data for now
        // boolean same = false;
        int currentHit1 = 0;                 //Current byte of parent array
        int currentHit2 = 0;                          //Current byte of derived array
        float diff_count = 0;                //Longest match array value diff.
        float curr_diff_count = 0;           //Current match array value diff.
        boolean match = false;               //Whether the FFTs match
        boolean cont = false;                //Should the comparison proceed
        boolean breakloop = false;           //Should the comparison break

        int parent_start_index = 1;                  //FFT array index to begin on

        int difference_threshold = 100;      //Value difference padding

        int parent_index = 0;                //Current FFT index
        float longest_match_chain = 0;       //Length of longest match
        float current_match_chain = 0;       //Length of current match

        int miss_count = 0;

        int secondSize = second.size();

        // If there is no data, assume not a match
        if (secondSize <= 1){
            return 0;
        }

        while (!match && !breakloop) {
            for (int i = 1; i < secondSize; i++) {
                cont = false;


                //Retrieve current fft values
                currentHit2 = second.get(i);
                currentHit1 = first.get(parent_start_index + parent_index);

                //If the difference between this and the previous values are
                //similar enough, add to the current match
                int diff = currentHit2 - currentHit1;

                if (-1 * difference_threshold < diff &&
                        diff < difference_threshold) {
                    current_match_chain +=1;
                    curr_diff_count += Math.abs(diff);
                    miss_count = 0;
                    cont = true;
                } else {
                    // Allow up to 4 sequential misses without breaking chain
                    if (current_match_chain > 1 && miss_count <= 4) {
                        current_match_chain++;
                        cont = true;
                        miss_count++;
                    } else {
                        miss_count = 0;
                    }
                }

                if (cont) {
                    //If the current match is longest, current match becomes
                    //longest match
                    if (i == secondSize - 1) {
                        longest_match_chain = current_match_chain;
                        diff_count = curr_diff_count;
                        match = true;
                    } else {
                        parent_index++;
                        if (parent_start_index + parent_index >=
                                first.size()) {
                            return i / first.size() * 100;
                        }
                    }
                } else {
                    //If the current match isn't longest record reset
                    parent_index = 0;
                    parent_start_index += 1;
                    if(current_match_chain > longest_match_chain){
                        longest_match_chain = current_match_chain;
                        diff_count = curr_diff_count;
                    }
                    if (parent_start_index + secondSize >= first.size()) {
                        breakloop = true;
                        break;
                    }
                    current_match_chain = 0;
                    curr_diff_count = 0;
                    break;
                }

            }
        }

        //Return clean difference results
        float p = longest_match_chain / secondSize;
        float x = (float)(p * (1 - ( diff_count /
                (difference_threshold * secondSize))));
        if (p4500.DEBUG) System.out.println("Chain: " +
                longest_match_chain + " Size:" + secondSize);
        if (p4500.DEBUG) System.out.println("DIFFCOUNT: " + diff_count);
        if (p4500.DEBUG) System.out.println("p" + p);
        return x * 100;
    }
}

//Import relevant libraries
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;

//The main class containing the core program functionality
public class p4500 {

    //Input paths given by the user
    ArrayList<Directory> dirs;
	
    //The log file generated in /tmp for recording results
    File logFile;
	FileWriter fWrite;
	BufferedWriter bWrite;
	
	//Timestamp formats for logging
	DateFormat dForm;
	DateFormat logDForm;
	Date date;
	
	//The generated log file name
	String logName;

	// Initialize logging variables and create log file
	public p4500() {
		date = new Date();
		dForm = new SimpleDateFormat("MMddyyyy_HH-mm-ss"); //Log file naming
		logDForm = new SimpleDateFormat("HH:mm:ss"); //Used within the log file
		logName = dForm.format(date) + "_logfile.txt";
		dirs = new ArrayList<Directory>();
		initLog();
	}

	// Create a new log file in /tmp
	private void initLog() {
		logFile = new File("/tmp", logName);
		if (!logFile.exists()) {
			try {
				logFile.createNewFile();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	// Helper method to write text in a uniform fashion
	//Param: Desired text to be written to the external log file
	private void writeToFile(String line) {
		try {
			fWrite = new FileWriter(logFile, true);
			bWrite = new BufferedWriter(fWrite);
			date = new Date();
			bWrite.write(logDForm.format(date) + " - " + line);
			bWrite.newLine();
			bWrite.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	// Check whether an absolute path returns a valid directory
	//Param: Absolute path of an input directory
	//Return: Whether a directory exists
	private boolean directoryCheck(String dir) {
		try {
			File folder = new File(dir);
			if (!folder.exists())
				return false;
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}

		return true;
	}

	//Main loop handling audio matching
	//Param: Audio files to be compared
	public static void main(String[] args) {

		p4500 prog = new p4500();
		
		//Determine basic validity of user given input
		if (args.length < 4) {
			System.err.println("ERROR - Too few inputs!");
			System.exit(1);
		} else if (!(args[0].equals("-f") && args[2].equals("-f"))) {
		    System.err.println("ERROR - Invalid parameter type");
		    System.exit(1);
		}
		
		//Extract file paths from input
		String derivedDir = args[1];
		String sourceDir = args[3];
		
		//If the first input is invalid, exit with appropriate message
		if (prog.directoryCheck(derivedDir)) {
			prog.dirs.add(new Directory(derivedDir));
		} else {
			System.err.println("ERROR - Derived file \"" + derivedDir
					+ "\" does not exist! Terminating.");
			System.exit(1);
		}

		//If the second input is invalid, exit with appropriate message
		if (prog.directoryCheck(sourceDir)) {
			prog.dirs.add(new Directory(sourceDir));
		} else {
			System.err.println("ERROR - Source file \"" + sourceDir
					+ "\" does not exist! Terminating.");
			System.exit(1);
		}

		//If any of the accepted files are of the wrong type, exit with
		//appropriate message
		for (Directory d : prog.dirs) {
			d.getFiles();
			if (d.audioFiles.size() == 0) {
				System.err
						.println("ERROR - " + d.absolutePath + " is invalid!");
				System.exit(1);
			}
		}

		//Downsample all files to common sampling rate
		try {
			prog.downsample();
		} catch (Exception e) {
			e.printStackTrace();
		}

		//Determine if the files are a match
		AudioFile file_1 = prog.dirs.get(0).audioFiles.get(0);
		AudioFile file_2 = prog.dirs.get(1).audioFiles.get(0);
		boolean match = false;
		float match_threshold = 60;
		if(file_1.fft.size() > file_2.fft.size()){
			float percent_match = prog.compareFiles(file_1, file_2);
			if(percent_match > match_threshold){
				match = true;
			}
		} else {
			float percent_match = prog.compareFiles(file_2, file_1);
			if(percent_match > match_threshold){
				match = true;
			}
		}
		
		if (match) {
			System.out.println("MATCH");
			prog.writeToFile("[MATCH]");
		} else {
            System.out.println("NO MATCH");
            prog.writeToFile("[NO MATCH]");
		}
		
		System.exit(0);
	}

	//Looks for exact match of data between two AudioFiles
	//Param: Two AudioFiles to be compared
	//Return
	private float compareFiles(AudioFile first, AudioFile second) {
		// Only looks for an exact match of the data for now
		// boolean same = false;
		int currentHit1 = 0;				//Current byte of parent array
		int currentHit2 = 0;				//Current byte of derived array
		float diff_count = 0;				//Longest match array value diff.
		float curr_diff_count = 0;			//Current match array value diff.
		boolean match = false;				//Whether the FFTs match
		boolean cont = false;				//Should the comparison proceed
		boolean breakloop = false;			//Should the comparison break
		int parent_start_index = 0;			//FFT array index to begin on
		int difference_threshold = 2000;	//Value difference padding
		int parent_index = 0;				//Current FFT index
		float longest_match_end_index = 0;	//Beginning of longest match
		float longest_match_chain = 0;		//Length of longest match
		float current_match_chain = 0;		//Length of current match
		
		while (!match && !breakloop) {
			for (int i = 0; i < second.fft.size(); i++) {
				cont = false;
				
				//Retrieve current fft values
				currentHit2 = second.fft.get(i);
				currentHit1 = first.fft.get(parent_start_index + parent_index);
				
				//If the values are similar enough, add to the current match
				//and continue
				int diff = currentHit2 - currentHit1;
				if (-1 * difference_threshold < diff && diff < 
						difference_threshold) {
					current_match_chain +=1;
					curr_diff_count += Math.abs(diff);
					cont = true;
				}

				if (cont) {
					//If the current match is longest, current match becomes
					//longest match
					if (i == second.fft.size() - 1) {
						longest_match_chain = current_match_chain;
						diff_count = curr_diff_count;
						match = true;
					} else {
						parent_index++;
						if (parent_start_index + parent_index >= 
								first.fft.size()) {
							return i / first.fft.size() * 100;
						}
					}
				} else {
					//If the current match isn't longest record reset
					parent_index = 0;
					parent_start_index += 1;
					if(current_match_chain > longest_match_chain){
						longest_match_chain = current_match_chain;
						longest_match_end_index = parent_start_index + 
								parent_index;
						diff_count = curr_diff_count;
					}
					if (parent_start_index + second.fft.size() >= 
							first.fft.size()) {
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
		int sze = second.fft.size();
		float p = longest_match_chain / sze;
		float x = (float)(p * (1 - ( diff_count / 
				(difference_threshold * second.fft.size()))));
		return x * 100;
	}

	//Downsample all files in the given directories to a common sampling rate
	private void downsample() throws Exception {
		float lowestSampling = 44100f;
		for (Directory d : dirs) {
			for (AudioFile f : d.audioFiles) {
				float tempSample = f.audioInputStream.getFormat()
						.getSampleRate();
				if (tempSample < lowestSampling)
					lowestSampling = tempSample;
			}
		}

		//For each given file, grab existing audio format and set the new
		//sampling/frame rate
		for (Directory d : dirs) {
			for (AudioFile f : d.audioFiles) {
				AudioInputStream tempStream = f.audioInputStream;
				AudioFormat tempFormat = tempStream.getFormat();
				// http://stackoverflow.com/questions/1088216/whats-wrong-
				// with-using-to-compare-floats-in-java
				if (!(Math.abs(tempFormat.getSampleRate() - lowestSampling) < 
						0.00000001)) {
					AudioFormat newFormat = new AudioFormat(
							tempFormat.getEncoding(),
							lowestSampling, // Sampling rate
							tempFormat.getSampleSizeInBits(),
							tempFormat.getChannels(),
							tempFormat.getFrameSize(), lowestSampling,
							tempFormat.isBigEndian());
					AudioInputStream tempStreamTwo = AudioSystem
							.getAudioInputStream(newFormat, tempStream);
					f.setAudioInputStream(tempStreamTwo);
					byte[] fileArray = fileToByteArray(f);
				}
			}
		}
	}

	//Converts a given file to a byte array
	//Param: File to be converted
	//Return: Byte array of File data
	private byte[] fileToByteArray(File f) {
		byte[] fileBytes = new byte[0];

		try {
			FileInputStream fis = new FileInputStream(f);
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			// Should be larger buffer?
			byte[] buffer = new byte[4];

			try {
				for (int i = 0; (i = fis.read(buffer)) != -1;) {
					baos.write(buffer, 0, i);
				}
			} catch (IOException e) {
				e.printStackTrace();
			}

			fileBytes = baos.toByteArray();

			fis.close();
			baos.close();

		} catch (IOException e) {
			e.printStackTrace();
		}

		return fileBytes;
	}
}

//Import relevant libraries
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;

// TODO rename to AudioSet 
public class Directory {

	String absolutePath;				//The absolute path of the directory
	ArrayList<AudioFile> audioFiles;	//Audio files within this directory

	//Create a directory with a given path
	public Directory(String path) {
		absolutePath = path;
		audioFiles = new ArrayList<AudioFile>();
	}

	//Set the absolute path of this directory
	//Param: The new absolute path
	public void setPath(String p) {
		absolutePath = p;
	}

	//Add a new audio file to the directory
	//Param: Absolute path of the desired file
	public void addFile(String f) {
		audioFiles.add(new AudioFile(f, ""));
	}

	//Get all files within this directory and add them to audioFiles
	//If this directory is a file path, add a new file with this
	//directory's absolute path
	public void getFiles() {
		File folder = new File(absolutePath);
		if (!folder.isDirectory()) {
			validateFile(folder);
		} else {
			File[] files = folder.listFiles();
			for (File f : files) {
				validateFile(f);
			}
		}
	}

	//Validate a given file within this directory
	//Param: File to be validated
	private void validateFile(File f) {
		String type = getAudioType(f);
		if (type == "MP3" || type == "MP3 ID3v2") {
			// Convert to WAV -> create new file -> use that file
			try {
				// This path will need to be updated on submission
				String lamePath = "/usr/local/bin/lame";
				// String lamePath = "/course/cs4500f13/bin/lame"; 
				
				// Run lame to create the tmp file
				Process p = Runtime.getRuntime().exec(
						new String[]{ lamePath, "--decode", "--quiet",
								f.getAbsolutePath(), "/tmp/tmp.wav"});
				p.waitFor();

				File newWave = new File("/tmp/tmp.wav");
				// Validate newly created file instead
				validateFile(newWave);
			} catch (IOException e) {
				e.printStackTrace();
			}
			catch (InterruptedException e) {
				e.printStackTrace();
			}
		} else if (type == "WAVE") {
			AudioFile file = new AudioFile(f.getAbsolutePath(), type);
			if (file.isValid()) {
				audioFiles.add(file);
				file.initializeValues();
			}
		} else {
			//File is neither MP3 nor WAVE
			return;
		}
	}

	//Based on WAVE and MP3 headers, get the given file type
	//Param: File to be identified
	//Return: Basic audio file type, INVALID if unsupported
	public String getAudioType(File f) {
		try {
			FileInputStream fis = new FileInputStream(f);
			byte[] buff = new byte[12];
			int bytes = 0, pos = 0;

			while (pos < buff.length &&
					(bytes = fis.read(buff, pos, buff.length - pos)) > 0) {
				pos += bytes;
			}

			fis.close();

			//If the first byte is 11111111 it's the start of an MP3 frame sync
			if ((buff[0] & 0x000000ff) == 255) {
				return "MP3";
			}
			
			//Magic number for mp3 file signature
			if (73 == buff[0] && 68 == buff[1] && 51 == buff[2]) {
				return "MP3 ID3v2";
			}
			
			if ('W' == (char)buff[8] && 
					'A' == (char)buff[9] && 
						'V' == (char)buff[10] && 
							'E' == (char)buff[11]) {
				return "WAVE";
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return "INVALID";
	}
}

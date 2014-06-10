//Import relevant libraries
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;

public class Directory {

    String absolutePath;                //The absolute path of the directory
    ArrayList<AudioFile> audioFiles;    //Audio files within this directory

    File[] files;

    //Create a directory with a given path
    public Directory(String path) {
        absolutePath = path;
        audioFiles = new ArrayList<AudioFile>();
        files = new File[0];
    }

    //Set the absolute path of this directory
    //Param: The new absolute path
    public void setPath(String p) {
        absolutePath = p;
    }

    //Get all files within this directory and add them to audioFiles
    //If this directory is a file path, add a new file with this
    //directory's absolute path
    public void getFiles() {
        File folder = new File(absolutePath);
        if (!folder.isDirectory()) {
            files = new File[1];
            files[0] = folder;
        } else {
            files = folder.listFiles();

        }
    }

    //Validate a given file within this directory
    //Param: File to be validated
    public AudioFile ValidateFile(File f, String baseName) {
        if (baseName == null) {
            String filePath = f.getAbsolutePath();
            int slashInd = filePath.lastIndexOf("/");
            baseName = filePath.substring(slashInd + 1);
        }

        if (f.isDirectory()) {
            System.err.print("[WARNING] - Subdirectory " + f.getName() +
                    " will be skipped.");
            return null;
        }
        String type = getAudioType(f);
        if (type == "MP3" || type == "MP3 ID3v2") {
            // Convert to WAV -> create new file -> use that file
            try {
                // This path will need to be updated on submission
                String lamePath = "/usr/local/bin/lame";
                // String lamePath = "/course/cs4500f13/bin/lame";

                String filePath = f.getAbsolutePath();
                int slashInd = filePath.lastIndexOf("/");
                String fileName = filePath.substring(slashInd + 1);
                int typeInd = fileName.lastIndexOf(".");

                // Make a temporary file with the mp3 extension so lame works
                if (typeInd == -1 ||
                        !fileName.substring(typeInd + 1).equals("mp3")) {
                    Process p = Runtime.getRuntime().exec(
                            new String[]{ p4500.TMPDIR + "make-tmp-mp3",
                                    filePath});
                    p.waitFor();
                    filePath = p4500.TMPDIR + "tmp.mp3";
                }

                String tmpPath = p4500.TMPDIR + "/" + fileName + ".wav";


                // Run lame to create the tmp file
                Process p = Runtime.getRuntime().exec(
                        new String[]{ lamePath, "--decode", "--quiet",
                                filePath, tmpPath});
                p.waitFor();

                File newWave = new File(tmpPath);
                // Validate newly created file instead
                return ValidateFile(newWave, baseName);
            } catch (IOException e) {
                e.printStackTrace();
            }
            catch (InterruptedException e) {
                e.printStackTrace();
            }
        } else if (type == "WAVE") {
            AudioFile file = new AudioFile(f.getAbsolutePath(), type, baseName);
            //if (file.isValid()) {
                audioFiles.add(file);
                return file;
           // }
        } else {
            //File is neither MP3 nor WAVE
            return null;
        }

        return null;
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

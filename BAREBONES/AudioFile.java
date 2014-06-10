//Import relevant libraries
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Formatter;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.Locale;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;


//A class representing a supported audio file
public class AudioFile extends File {
	private static final long serialVersionUID = 1L;

	//Variables used for reading and storing file data
	AudioInputStream audioInputStream;
	byte[] fileBytes;
	byte[] dataBytes; //Audio data without header data
	
	//Variables used for fast fourier transformations
	//and non-byte audio information
	ArrayList<Integer> fft;
	String hash;
	String format;
	int sampleRate;
	int numChannels;
	int bps;
	int nonZeroValues;
	
	Hashtable<Integer, LinkedList<Integer>> fftHash;
	
	private static final int LEFT_CHANNEL = 0;
	private static final int RIGHT_CHANNEL = 1;

	//Path-based constructor for WAVE files
	//Param: Absolute path of the file and desired audio format
	public AudioFile(String pathname, String type) {
		super(pathname);
			try {
				this.getByteArray();
				setAudioInputStream(AudioSystem.getAudioInputStream(this));
				fftHash = new Hashtable<Integer, LinkedList<Integer>>();
				format = type;
		} catch (IOException e) {
			e.printStackTrace();
		} catch (UnsupportedAudioFileException e) {
			e.printStackTrace();
		}
	}
	
	//MP3-based constructor, to be given data from an MP3Builder
	//Param: Absolute path of the file, file data, sampling rate, and channels
	public AudioFile(String path, byte[] data, float sRate, int channels,
			String type) {
	    super(path);
	    try {
	        format = type;
	        fileBytes = data;
	        dataBytes = data;
	        sampleRate = (int)sRate;
	        numChannels = channels;
            fftHash = new Hashtable<Integer, LinkedList<Integer>>();
	    } catch (Exception e) {
	        
	    }
	}

	//Get the byte array of data from this audiofile
	private void getByteArray() throws IOException {
		ByteArrayOutputStream baos = null;
		InputStream is = null;
		
		try {
			byte[] buff = new byte[4096];
			baos = new ByteArrayOutputStream();
			is = new FileInputStream(this);
			int r = 0;
			while ((r = is.read(buff)) != -1) {
				baos.write(buff, 0, r);
			}
		} finally {
			try {
				if (baos != null) {
					baos.close();
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			try {
				if (is != null) {
					is.close();
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		this.fileBytes = baos.toByteArray();
	}
	
	//Simple check if the file is valid based on the format
	//Return: Whether this file is of a supported format
	public boolean isValid() {
		if ("WAVE".equals(this.format)) {
			setAttrs();
			return true;
		}
		return false;
	}
	
	//Generate values for file comparing based off the FFT and hash data
	public void initializeValues() {
		try {
			this.calculateFFTofData();
			this.hashData();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	//Convert littleEndian bytes to ints
	//Param: Byte array to be converted
	//Return: Int representation of the input
	private int littleEndianToInt(byte[] bytes) {
		int i;
		if (bytes.length == 4) {
			i = ((0xFF & bytes[3]) << 24) | ((0xFF & bytes[2]) << 16) |
		            ((0xFF & bytes[1]) << 8) | (0xFF & bytes[0]);
		} else if (bytes.length == 2) {
			i = ((0xFF & bytes[1]) << 8) | (0xFF & bytes[0]);
		} else {
			return 0;
		}

		return i;
	}
	
	//Update this audiofile's byte array
	//Param: The desired byte array
	//Return: Whether the desired byte array is valid
	public boolean updateByteArray(byte[] byteArray) {
		this.fileBytes = byteArray;
		return isValid();
	}
	
	//Set this audiofile's attributes and data
	private void setAttrs() {
		// Format
		char[] format = {(char)fileBytes[8], (char)fileBytes[9], 
		        (char)fileBytes[10], (char)fileBytes[11]};
		
		// Sample Rate
		this.sampleRate = littleEndianToInt(Arrays.copyOfRange(this.fileBytes, 
		        24, 28));
		
		// Number of Channels
		this.numChannels = littleEndianToInt(Arrays.copyOfRange(this.fileBytes, 
		        22, 24));
		
		this.bps = littleEndianToInt(Arrays.copyOfRange(
				this.fileBytes, 34, 36));
		
		this.dataBytes = Arrays.copyOfRange(this.fileBytes, 44, 
		        this.fileBytes.length);
		
	}
	
	//Converts a byte to an array of ints representing bits
	//Param: Byte to be converted to int array
	//Return: Int array representing the given byte
	private int[] byteToBits(byte b) {
		int[] bits = new int[8];
		String byteString = String.format("%8s", 
				Integer.toBinaryString(b & 0xFF)).replace(' ', '0');
		for (int i = 0; i < 7; i++) {
			bits[i] = Integer.parseInt(byteString.substring(i,i+1));
		}
		return bits;
	}

	//Get this audiofile's input stream
	//Return: This audiofile's input stream
	public AudioInputStream getAudioInputStream() {
		return audioInputStream;
	}

	//Set this audiofile's input stream
	//Param: The desired new audio input stream
	public void setAudioInputStream(AudioInputStream audioInputStream) {
		this.audioInputStream = audioInputStream;
	}

	//Based on this audiofile's data, run a fast fourier transform and
	//store the results
	public void calculateFFTofData() {
		int bytesPerSample = this.bps / 8;
		int step = bytesPerSample * numChannels;
		int k;

		float[][] sampleArray = new float[numChannels][dataBytes.length / step];

		// Read and store the sample data
		for (int channel = 0; channel < numChannels; channel++) {
			k = 0;
			for (int i = 0 + (channel * bytesPerSample); i <= 
					this.dataBytes.length - 8; i += step) {
				Short v = ((short) littleEndianToInt(Arrays.copyOfRange(
						dataBytes, i, i + bytesPerSample)));
				sampleArray[channel][k] = (v != null ? v : Float.NaN);
				k++;
			}
		}
		
		
		ArrayList<Integer> fft = new ArrayList<Integer>();
		int sampleRate = getNextHighestPower(this.sampleRate);
		int lngth = sampleArray[0].length;
		int windowSize = 4096;
		int stepDim = windowSize / 2;
		for (int j = 0; j < lngth; j += stepDim) {
			// TODO: Handle channels stored in floatsArray (currently computes
			// FFT of the left channel only)
			int singleFft = FFT.calculateFFT(
					Arrays.copyOfRange(sampleArray[LEFT_CHANNEL], j, j
							+ windowSize), windowSize, sampleRate, 1);
			fft.add(singleFft);
		}
		this.fft = fft;
		//System.out.println("FFT: " + this.fft);
	}
	
	//Store a hashed value of fft result data
	//Param: The fft value and index location
	private void storeHashValue(int fftVal, int location) {
		if (fftHash.containsKey(fftVal)) {
			LinkedList<Integer> tmp = fftHash.get(fftVal);
			tmp.add(location);
			fftHash.put(fftVal, tmp);	
		} else {
			//rename
			LinkedList<Integer> tmp = new LinkedList<Integer>();
			tmp.add(location);
			fftHash.put(fftVal, tmp);
		}
	}
	
	//Get the next higher power of two than the given value
	//Param: Value to get a power of two higher than
	//Return: Power of two that's closest and higher than the given value
	private int getNextHighestPower(float sRate) {
		int start = 2;
		while (start < sRate) {
			start = (int)Math.pow(start,2);
		}
		return start;
	}
	
	//Hash this audiofile's data
	public void hashData() throws NoSuchAlgorithmException{
		MessageDigest md = MessageDigest.getInstance("SHA-256");
		Formatter formatter = new Formatter();
	    for (byte b : md.digest(this.dataBytes)) {
	        formatter.format("%02x", b);
	    }
	    this.hash = formatter.toString();
	    formatter.close();
	}

	//For testing/debugging, print the audiofile's wave attributes
	private void printWaveAttributes() {
		byte[] file = this.fileBytes;
		boolean showBits = false;
		System.out.println("RIFF Chunk Descriptor");
		System.out.println("Chunk ID: " + (char)file[0] + (char)file[1] + 
		    (char)file[2] +  (char)file[3] + (showBits ? ": " + file[0] + 
		        " " + file[1] + " " + file[2] + " " + file[3] : ""));
		System.out.println("Chunk Data Size: " + 
		    NumberFormat.getNumberInstance(Locale.US).format(
		        littleEndianToInt(Arrays.copyOfRange(
		            file, 4, 8))) + (showBits ? ": " + 
		            file[7] + " " + file[6] + " " + file[5] + " " + 
		            file[4] : ""));
		System.out.println("Chunk Format: " + (char)file[8] + (char)file[9] + 
		    (char)file[10] +  (char)file[11] + (showBits ? ": " + file[8] + 
		    " " + file[9] + " " + file[10] + " " + file[11] : ""));
		
		System.out.println("\nFMT Sub-Chunk");
		System.out.println("Subchunk1 ID: " + (char)file[12] + 
		    (char)file[13] + (char)file[14] + (char)file[15] + 
		    (showBits ? ": " + file[12] + " " + file[13] + " " + file[14] + 
		    " " + file[15] : ""));
		System.out.println("Subchunk1 Size: " + 
		    NumberFormat.getNumberInstance(Locale.US).format(
		        littleEndianToInt(Arrays.copyOfRange(
		            file, 16, 20))) + 
		            (showBits ? ": " + file[19] + " " + 
		            file[18] + " " + file[17] + " " + 
		            file[16] : ""));
		System.out.println("Audio Format: " + littleEndianToInt(
		    Arrays.copyOfRange(file, 20, 22)) + (showBits ? ": " + 
		    file[21] + " " + file[20] : ""));
		System.out.println("Num Channels: " + littleEndianToInt(
		    Arrays.copyOfRange(file, 22, 24)) + (showBits ? ": " + 
		    file[23] + " " + file[22] : ""));
		System.out.println("Sample Rate: " + 
		    NumberFormat.getNumberInstance(Locale.US).format(littleEndianToInt(
		        Arrays.copyOfRange(file, 24, 28))) + (showBits ? ": " + 
		        file[27] + " " + file[26] + " " + file[25] + " " + 
		        file[24] : ""));
		System.out.println("Byte Rate: " + 
		    NumberFormat.getNumberInstance(Locale.US).format(
		        littleEndianToInt(Arrays.copyOfRange(file, 28, 32))) + 
		        (showBits ? ": " + file[31] + " " + file[30] + " " + file[29] + 
		        " " + file[28] : ""));
		System.out.println("Block Align: " + 
		    littleEndianToInt(Arrays.copyOfRange(file, 32, 34)) + 
		    (showBits ? ": " + file[33] + " " + file[32] : ""));
		System.out.println("Bits per Sample: " + 
		    littleEndianToInt(Arrays.copyOfRange(file, 34, 36)) + 
		    (showBits ? ": " + file[33] + " " + file[34] : ""));
		System.out.println("\nData Sub-Chunk");
		
		System.out.println("Subchunk2 ID: " + (char)file[36] + (char)file[37] + 
		    (char)file[38] + (char)file[39] + (showBits ? ": " + file[36] + 
		    " " + file[37] + " " + file[38] + " " + file[39] : ""));
		System.out.println("Subchunk2 Size: " + 
		    NumberFormat.getNumberInstance(Locale.US).format(littleEndianToInt(
		        Arrays.copyOfRange(file, 40, 44))) + (showBits ? ": " + 
		        file[43] + " " + file[42] + " " + file[41] + " " + 
		        file[40] : ""));
		System.out.println();
	}
}

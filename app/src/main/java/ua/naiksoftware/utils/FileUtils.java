package ua.naiksoftware.utils;
import java.io.*;

public class FileUtils {
	
	public static void write(String data, File file) throws IOException {
		FileWriter writer = null;
		try {
			writer = new FileWriter(file);
			writer.write(data);
			writer.flush();
		} catch (IOException e) {
			throw new IOException(e);
		} finally {
			if (writer != null) {
		    	writer.close();
			}
		}
	}
}

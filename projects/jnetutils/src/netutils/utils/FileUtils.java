package netutils.utils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;

/**
 * Common Files and Library's functions.
 * 
 * @author roni bar-yanai
 *
 */
public class FileUtils
{
	/**
	 * Get all file names under directory.
	 * called recursively.
	 * @param path
	 * @return array of all file names.
	 */
	public static String[] getAllFiles(String path)
	{
		File dir = new File(path);
		if (!dir.exists() || !dir.isDirectory())
			return new String[0];

		ArrayList<String> files = new ArrayList<String>();

		File dirFiles[] = dir.listFiles();
		for (int i = 0; i < dirFiles.length; i++)
		{
			if (dirFiles[i].isDirectory())
			{
				// could be done more cleverly, but will do the job.
				String toAdd[] = getAllFiles(dirFiles[i].getAbsolutePath());
				for (int j = 0; j < toAdd.length; j++)
				{
					files.add(toAdd[j]);
				}
			} else
			{
				files.add(dirFiles[i].getAbsolutePath());
			}
		}

		return files.toArray(new String[files.size()]);
	}

	/**
	 * read entire file content into string.
	 * (file must have reasonable size as it is being read to memory).
	 * @param theFileName
	 * @return
	 * @throws IOException
	 */
	public static String readFileToString(String theFileName) throws IOException
	{
		StringBuffer buff = new StringBuffer();
		BufferedReader rd = new BufferedReader(new FileReader(theFileName));
		String line = null;
		while ((line = rd.readLine()) != null)
		{
			buff.append(line);
			buff.append('\n');
		}
		return buff.toString();
	}
	
	/**
	 * write string to file.
	 * Open the file, write the string and close the file in one action
	 * @param theString
	 * @param theFileName
	 * @throws IOException
	 */
	public static void writeStringToFile(String theString,String theFileName) throws IOException
	{
		BufferedWriter wr = new BufferedWriter(new FileWriter(theFileName));
		wr.write(theString);
		wr.close();
	}

	public static void copyfile(String src, String dst) throws IOException
	{
		File f1 = new File(src);
		File f2 = new File(dst);

		InputStream in = new FileInputStream(f1);
		OutputStream out = new FileOutputStream(f2);

		byte[] buf = new byte[8096];
		int n = 0;
		
		while ((n = in.read(buf)) > 0)
		{
			out.write(buf, 0, n);
		}

		in.close();
		out.close();

	}
}

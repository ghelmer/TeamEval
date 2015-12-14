import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;

import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.sax.BodyContentHandler;
import org.xml.sax.SAXException;

/**
 * Extract the evaluation data from the submitted spreadsheets.
 * @author ghelmer
 *
 */
public class ExtractEvalsFromFiles
{
	public static void main(String[] args) throws IOException, SAXException, TikaException
	{
		if (args.length < 1)
		{
			System.err.println("Usage: java -classpath .:tika-app-1.7.jar ExtractEvalsFromFiles input-directory [output-filename]");
		}
		PrintStream output;
		boolean doClose = false;
		ArrayList<File> errorFiles = new ArrayList<File>();
		if (args.length > 1)
		{
			output = new PrintStream(args[1]);
			doClose = true;
		}
		else
		{
			output = System.out;
		}
		File[] inputFiles;
		File inputFile = new File(args[0]);
		if (inputFile.isDirectory())
		{
			inputFiles = inputFile.listFiles();
		}
		else
		{
			inputFiles = new File[1];
			inputFiles[0] = inputFile;
		}
		for (File f : inputFiles)
		{
			if (f.getName().startsWith("."))
			{
				// Skip hidden files.
				continue;
			}
			try
			{
				String result = getText(f);
			   	ArrayList<String> lineList = new ArrayList<String>(Arrays.asList(result.split("[\\r\\n]+")));
			   	int i = 0;
			   	while (i < lineList.size())
			   	{
			   		if (lineList.get(i).matches("\\s*This self and peer eval.*"))
			   		{
			   			lineList.remove(i);
			   		}
			   		else if (lineList.get(i).matches("\\s*Scoring Guide.*"))
			   		{
			   			boolean found = false;
			   			int j = i + 1;
			   			while (j < lineList.size() && !found)
			   			{
			   				// Look for the line ending the scoring guide.
			   				if (lineList.get(j).matches("\\s*Justification.*"))
			   				{
			   					found = true;
			   				}
			   				else
			   				{
			   					j++;
			   				}
			   			}
			   			if (found)
			   			{
			   				for (int count = 0; count < j - i; count++)
			   				{
			   					lineList.remove(i);
			   				}
			   			}
			   			else
			   			{
			   				i++;
			   			}
			   		}
			   		else
			   		{
			   			i++;
			   		}
			   	}
				output.println(f.getName());
				for (String s : lineList)
				{
					output.println(s);
				}
			}
			catch (TikaException e)
			{
				output.println(f.getName());
				output.print("TikaException: " + e.getMessage());
				errorFiles.add(f);
			}
			catch (SAXException e)
			{
				output.println(f.getName());
				output.print("SAXException: " + e.getMessage());
				errorFiles.add(f);
			}
			catch (IOException e)
			{
				output.println(f.getName());
				output.print("IOException: " + e.getMessage());
				errorFiles.add(f);
			}
		}
		if (doClose) {
			output.close();
		}
		if (errorFiles.size() > 0)
		{
			System.err.println("Errors encountered in these files:");
			for (File f : errorFiles)
			{
				System.err.println(f.getPath());
			}
		}
	}

	public static String getText(File inputFile) throws IOException, SAXException, TikaException {
		if (!inputFile.exists() || !inputFile.isFile()) {
			throw new IllegalArgumentException(inputFile.toString() + " is not a valid file");
		}
		FileInputStream stream = new FileInputStream(inputFile);
		String output;

		BodyContentHandler handler = new BodyContentHandler();
		AutoDetectParser parser = new AutoDetectParser();
		Metadata metadata = new Metadata();
		try {
			parser.parse(stream, handler, metadata);
			output = handler.toString();
		} finally {
			stream.close();
		}
		return output;
		//pw.print(output);
		//return "The file " + inputFile.getName() + " contains:\n";
	}
}

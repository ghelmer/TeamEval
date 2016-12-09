import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
	private File[] inputFiles;
	
	/**
	 * Construct a new extractor for the given path (single file or directory of files).
	 * @param inputPath - File or directory of input files
	 * @throws IOException on error
	 */
	public ExtractEvalsFromFiles(String inputPath) throws IOException
	{
		File inputFile = new File(inputPath);
		if (!inputFile.exists())
		{
			throw new IOException("Path " + inputPath + " does not exist");
		}
		if (inputFile.isFile())
		{
			inputFiles = new File[1];
			inputFiles[0] = inputFile;
		}
		else
		{
			inputFiles = inputFile.listFiles();
		}
	}
	
	/**
	 * Process the input files. Store successful input results from files
	 * into the results map indexed by reporting student ID + ':' + evaluated student name. 
	 * Store exceptions into the exceptions map indexed by filename.
	 * @param results - indexed by reporting student ID + ':' + evaluated student name
	 * @param exceptions - indexed by input filename
	 */
	public void processFiles(TreeMap<String, double[]> results, TreeMap<String, String> exceptions)
	{
		Pattern reportingStudentIDPattern = Pattern.compile("\t([a-z][0-9a-z]+)\tCategories");
		Pattern reportedScores = Pattern.compile("\t([A-Za-z., ]+)\t(\\d+)\t(\\d+)\t(\\d+)\t(\\d+)\t(\\d+)\t\\d+");
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
			   	String reportingStudentID = "";
			   	for (int i = 0; i < lineList.size(); i++)
			   	{
			   		if (reportingStudentID.length() == 0)
			   		{
				   		// Do we have the reporting student yet?
			   			Matcher matcher = reportingStudentIDPattern.matcher(lineList.get(i));
			   			if (matcher.find())
			   			{
			   				reportingStudentID = matcher.group(1);
			   			}
			   		}
			   		else
			   		{
			   			// Look for scores.
			   			Matcher matcher = reportedScores.matcher(lineList.get(i));
			   			if (matcher.find())
			   			{
			   				double[] scores = new double[5];
			   				String evaluatedStudent = matcher.group(1);
			   				for (int j = 0; j < 5; j++)
			   				{
			   					scores[j] = Double.parseDouble(matcher.group(2 + j));
			   				}
			   				results.put(reportingStudentID + ":" + evaluatedStudent, scores);
			   			}
			   		}
			   	}
			}
			catch (TikaException e)
			{
				exceptions.put(f.getName(), "TikaException: " + e.getMessage());
			}
			catch (SAXException e)
			{
				exceptions.put(f.getName(), "SaxException: " + e.getMessage());
			}
			catch (IOException e)
			{
				exceptions.put(f.getName(), "IOException: " + e.getMessage());
			}
		}
	}

	public static void main(String[] args) throws IOException, SAXException, TikaException
	{
		if (args.length < 1)
		{
			System.err.println("Usage: java -classpath .:tika-app-1.13.jar ExtractEvalsFromFiles input-directory");
		}
		ExtractEvalsFromFiles extractor = new ExtractEvalsFromFiles(args[0]);
		TreeMap<String, double[]> results = new TreeMap<>();
		TreeMap<String, String> exceptions = new TreeMap<>();
		extractor.processFiles(results, exceptions);
		for (String key : results.keySet())
		{
			System.out.println(key + "\t" + Arrays.toString(results.get(key)));
		}
		if (exceptions.size() > 0)
		{
			System.err.println("Errors encountered in these files:");
			for (String key : exceptions.keySet())
			{
				System.err.println(exceptions.get(key));
			}
		}
	}

	/**
	 * Extract text from the specified file and return the text as a String.
	 * @param inputFile - file to read
	 * @return the captured text
	 * @throws IOException
	 * @throws SAXException
	 * @throws TikaException
	 */
	public static String getText(File inputFile) throws IOException, SAXException, TikaException {
		if (!inputFile.exists() && !inputFile.isFile()) {
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

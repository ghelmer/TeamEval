import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Scanner;

/**
 * Manage a Student in the database.
 * @author ghelmer
 *
 */
public class Student implements Comparable<Student> {
	private String studentId;
	
	/**
	 * Create the empty Teams table in the database. If the Teams table previously
	 * existed, it will be dropped and re-created.
	 * @param db TeamDB connection
	 * @throws SQLException
	 */
	public static void initializeTable(TeamDB db) throws SQLException
	{
		Statement stat = db.getConnection().createStatement();
		try
		{
			try
			{
				stat.execute("DROP TABLE Students");
			}
			catch (SQLException e)
			{
				System.out.println("Notice: Exception during DROP TABLE Students: " + e.getMessage());
			}
			stat.execute("CREATE TABLE Students (StudentId VARCHAR(16) PRIMARY KEY, StudentName VARCHAR(64))");
		}
		finally
		{
			stat.close();
		}

	}
	
	/**
	 * Load all of the students from the input file into the Students table in the database.
	 * Input file format is student ID and student name separated by \t. '#' comments are allowed.
	 * @param db TeamDB connection
	 * @param filename Input file containing student data
	 * @throws FileNotFoundException
	 * @throws SQLException
	 */
	public static void loadTable(TeamDB db, String filename) throws FileNotFoundException, SQLException
	{
		File f = new File(filename);
		Scanner in = null;
		try
		{
			in = new Scanner(f);
			int lineNum = 0;
			while (in.hasNextLine())
			{
				lineNum++;
				String line = in.nextLine();
				int hashAt = line.indexOf('#');
				if (hashAt != -1)
				{
					line = line.substring(0, hashAt);
				}
				line.trim();
				if (line.length() == 0)
				{
					continue;
				}
				String[] tokens = line.split("\t");
				if (tokens.length != 2)
				{
					throw new IllegalArgumentException("File " + filename + " Line " + lineNum + ": Two tokens expected, but " + tokens.length + " found");
				}
				Student s = new Student(tokens[0]);
				s.setName(db, tokens[1]);
			}
		}
		finally
		{
			if (in != null)
			{
				in.close();
			}
		}
	}
	
	/**
	 * Construct the Student object with the specified student ID.
	 * @param id
	 */
	public Student(String id)
	{
		studentId = id;
	}
	
	/**
	 * Set the name in the Students table for this student ID. If the student with
	 * this ID does not exist in the table, the student will be inserted rather than
	 * updated.
	 * @param db TeamDB connection
	 * @param name Student name
	 * @throws SQLException
	 */
	public void setName(TeamDB db, String name) throws SQLException
	{
		PreparedStatement stat = db.getConnection().prepareStatement(
				"UPDATE Students SET StudentName = ? WHERE StudentId = ?");
		stat.setString(1, name);
		stat.setString(2, studentId);
		if (stat.executeUpdate() == 1)
		{
			System.out.println("Student " + studentId + " name set to " + name);
		}
		else
		{
			stat = db.getConnection().prepareStatement(
					"INSERT INTO Students (StudentId, StudentName) VALUES(?, ?)");
			stat.setString(1, studentId);
			stat.setString(2, name);
			stat.executeUpdate();
			System.out.println("Added student " + studentId + " name " + name);
		}
	}
	
	/**
	 * Get the name for this student.
	 * @param db TeamDB connection
	 * @return Name
	 * @throws SQLException
	 */
	public String getName(TeamDB db) throws SQLException
	{
		String result = null;
		PreparedStatement stat = db.getConnection().prepareStatement(
				"SELECT StudentName FROM Students WHERE StudentId = ?");
		stat.setString(1, studentId);
		ResultSet rs = stat.executeQuery();
		if (rs.next())
		{
			result = rs.getString(1);
		}
		stat.close();
		return result;

	}
	
	/**
	 * Get the Id for the student.
	 * @return studentId
	 */
	public String getId()
	{
		return studentId;
	}
	
	/**
	 * Get the name for this student when the TeamDB connection is not available.
	 * @return Name
	 * @throws IOException 
	 * @throws ClassNotFoundException 
	 * @throws SQLException
	 */
	public String getName() throws ClassNotFoundException, IOException, SQLException
	{
		String result;
		TeamDB db = new TeamDB();
		try
		{
			result = getName(db);
		}
		finally
		{
			db.close();
		}
		return result;
	}
	
	/**
	 * Return a string representation of the Student.
	 */
	public String toString()
	{
		String name = "(name unknown)";
		try
		{
			name = getName();
		}
		catch (Exception e)
		{
			System.err.println("Student.toString(): Exception seen: " + e.getMessage());
		}
		return "Student ID: " + studentId + " Student Name: " + name;
	}
	
	/**
	 * Return a string representation of the Student.
	 * @param db TeamDB connection
	 * @return String representing the student
	 */
	public String toString(TeamDB db)
	{
		String name = "(name unknown)";
		try
		{
			name = getName(db);
		}
		catch (Exception e)
		{
			System.err.println("Student.toString(): Exception seen: " + e.getMessage());
		}
		return "Student ID: " + studentId + " Student Name: " + name;		
	}

	/**
	 * Compare this student object to another.
	 * @return less than 0: this student is less than the other
	 * 			greater than 0: this student is greater than the other
	 * 			0: this student is equal to the other
	 */
	public int compareTo(Student other) {
		try
		{
			String thisName;
			String otherName;
			thisName = this.getName();
			otherName = other.getName();
			return thisName.compareTo(otherName);
		}
		catch (Exception e)
		{
			e.printStackTrace();
			System.exit(1);
		}
		return 0;
	}
}

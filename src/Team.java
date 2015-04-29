import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Scanner;

/**
 * Manage a Team in the database.
 * @author ghelmer
 *
 */
public class Team {
	private String teamName;
	
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
				stat.execute("DROP TABLE Teams");
			}
			catch (SQLException e)
			{
				System.out.println("Notice: Exception during DROP TABLE Teams: " + e.getMessage());
			}
			stat.execute("CREATE TABLE Teams (TeamName VARCHAR(32), StudentId VARCHAR(16))");
		}
		finally
		{
			stat.close();
		}

	}
	
	/**
	 * Load all of the teams and their students from the input file.
	 * Input file format is:
	 * LastName\tFirstName\tID\tTeam
	 * '#' comments are allowed.
	 * @param db TeamDB connection
	 * @param filename Name of file containing team member data.
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
				if (tokens.length != 4)
				{
					throw new IllegalArgumentException("File " + filename + " Line " + lineNum + ": Four tokens expected, but " + tokens.length + " found");
				}
				Team t = new Team(tokens[3]);
				t.addStudent(db, tokens[2]);
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
	 * Get the team for the specified student.
	 * @param db TeamDB connection
	 * @return Array of Student objects
	 * @throws SQLException
	 */
	public static Team getTeamByStudent(TeamDB db, Student s) throws SQLException
	{
		Team t = null;

		PreparedStatement stat = db.getConnection().prepareStatement(
				"SELECT TeamName FROM Teams WHERE StudentId = ?");
		try
		{
			stat.setString(1, s.getId());
			ResultSet rs = stat.executeQuery();
			if (rs.next())
			{
				t = new Team(rs.getString(1));
			}
		}
		finally
		{
			stat.close();
		}
		return t;
	}
	
	/**
	 * Construct the Team object with the name of this team.
	 * @param name
	 */
	public Team(String name)
	{
		teamName = name;
	}
	
	/**
	 * Add the specified student to the team.
	 * @param db TeamDB connection
	 * @param studentId ID of the student to add (foreign key to Students table)
	 * @throws SQLException
	 */
	public void addStudent(TeamDB db, String studentId) throws SQLException
	{
		PreparedStatement stat = db.getConnection().prepareStatement(
				"SELECT COUNT(*) FROM Teams WHERE TeamName = ? AND StudentId = ?");
		stat.setString(1, teamName);
		stat.setString(2, studentId);
		ResultSet rs = stat.executeQuery();
		rs.next();
		if (rs.getInt(1) == 1)
		{
			System.out.println("Student " + studentId + " is already in team " + teamName);
			rs.close();
		}
		else
		{
			stat.close();
			stat = db.getConnection().prepareStatement(
					"INSERT INTO Teams (TeamName, StudentId) VALUES(?, ?)");
			stat.setString(1, teamName);
			stat.setString(2, studentId);
			stat.executeUpdate();
			System.out.println("Added student " + studentId + " to team " + teamName);
		}
		stat.close();
	}
	
	/**
	 * Get the students for the team.
	 * @param db TeamDB connection
	 * @return Array of Student objects
	 * @throws SQLException
	 */
	public Student[] getStudents(TeamDB db) throws SQLException
	{
		ArrayList<Student> students = new ArrayList<Student>();
		
		PreparedStatement stat = db.getConnection().prepareStatement(
				"SELECT Teams.StudentId FROM Teams, Students WHERE TeamName = ? AND Teams.StudentId = Students.StudentId ORDER BY Students.StudentName");
		try
		{
			stat.setString(1, teamName);
			ResultSet rs = stat.executeQuery();
			while (rs.next())
			{
				students.add(new Student(rs.getString(1)));
			}
		}
		finally
		{
			stat.close();
		}
		Student[] result = new Student[students.size()];
		students.toArray(result);
		return result;
	}
	
	/**
	 * Get the students for the team when the TeamDB is not available.
	 * @return Array of Student objects
	 * @throws SQLException
	 * @throws IOException 
	 * @throws ClassNotFoundException 
	 */
	public Student[] getStudents() throws SQLException, ClassNotFoundException, IOException
	{
		Student[] students;
		TeamDB db = new TeamDB();
		try
		{
			students = getStudents(db);
		}
		finally
		{
			db.close();
		}
		return students;
	}
	
	/**
	 * Get all of the known teams from the Teams table.
	 * @param db TeamDB connection
	 * @return Array of Team objects
	 * @throws SQLException
	 */
	public static Team[] getAllTeams(TeamDB db) throws SQLException
	{
		ArrayList<Team>teams = new ArrayList<Team>();
		Statement stat = db.getConnection().createStatement();
		try
		{
			ResultSet rs = stat.executeQuery("SELECT DISTINCT TeamName FROM Teams ORDER BY TeamName");
			while (rs.next())
			{
				teams.add(new Team(rs.getString(1)));
			}
		}
		finally
		{
			stat.close();
		}
		Team[] result = new Team[teams.size()];
		teams.toArray(result);
		return result;
	}
	
	/**
	 * Return a string representation of the Team.
	 * @return Team information
	 */
	public String toString()
	{
		Student[] students = new Student[0];
		try
		{
			students = getStudents();
		}
		catch (Exception e)
		{
			System.err.println("Student.toString(): Exception seen: " + e.getMessage());
		}
		return "Team: " + teamName + " Members: " + Arrays.toString(students);
	}
	
	/**
	 * Return a string representation of the team when the TeamDB connection is available.
	 * @param db TeamDB connection
	 * @return Team information
	 */
	public String toString(TeamDB db)
	{
		Student[] students = new Student[0];
		try
		{
			students = getStudents(db);
		}
		catch (Exception e)
		{
			System.err.println("Student.toString(): Exception seen: " + e.getMessage());
		}
		return "Team: " + teamName + " Members: " + Arrays.toString(students);
		
	}
	
	/**
	 * Get the name of the team.
	 * @return name
	 */
	public String getName()
	{
		return teamName;
	}
}

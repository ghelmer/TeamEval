import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.NoSuchElementException;
import java.util.Scanner;


public class Eval {
	public static final double DEFAULT_SCORE = 3.0;
	private boolean exists; // Record exists in Evals table
	@SuppressWarnings("unused")
	private Team team;
	private Student evaluatingStudent;
	private Student evaluatedStudent;
	private double contributing;
	private double interacting;
	private double onTrack;
	private double expectQuality;
	private double relevance;
	
	public static void initializeTable(TeamDB db) throws SQLException
	{
		Statement stat = db.getConnection().createStatement();
		try
		{
			try
			{
				stat.execute("DROP TABLE Evals");
			}
			catch (SQLException e)
			{
				System.out.println("Notice: Exception during DROP TABLE Evals: " + e.getMessage());
			}
			stat.execute("CREATE TABLE Evals (EvaluatingStudentId VARCHAR(16), EvaluatedStudentId VARCHAR(16), Contributing DOUBLE, Interacting DOUBLE, OnTrack DOUBLE, ExpectingQuality DOUBLE, Relevance DOUBLE)");
		}
		finally
		{
			stat.close();
		}
	}
	
	/**
	 * Allow the user to update the scores for this evaluation object.
	 * @param in Scanner from which to read
	 * @param db TeamDB connection
	 * @throws SQLException
	 */
	public boolean enterScores(Scanner in, TeamDB db, boolean enterIndividually) throws SQLException
	{
		System.out.println("Entering evaulation reported by " + evaluatingStudent.getName(db) + " for student " +
				evaluatedStudent.getName(db) + ":");
		String prompt;
		String response;
		boolean changed = false;
		
		if (enterIndividually)
		{
			System.out.println("  (Press <Enter> to keep current value)");
			prompt = String.format("  Contributing score (%.1f)", contributing);
			response = Menu.prompt(in, prompt).trim();
			if (response.length() > 0)
			{
				contributing = Double.parseDouble(response);
				changed = true;
			}
			prompt = String.format("  Interacting score (%.1f)", interacting);
			response = Menu.prompt(in, prompt).trim();
			if (response.length() > 0)
			{
				interacting = Double.parseDouble(response);
				changed = true;
			}		
			prompt = String.format("  Keeping on track (%.1f)", onTrack);
			response = Menu.prompt(in, prompt).trim();
			if (response.length() > 0)
			{
				onTrack = Double.parseDouble(response);
				changed = true;
			}		
			prompt = String.format("  Expecting quality (%.1f)", expectQuality);
			response = Menu.prompt(in,  prompt).trim();
			if (response.length() > 0)
			{
				expectQuality = Double.parseDouble(response);
				changed = true;
			}
			prompt = String.format("  Relevance (%.1f)", relevance);
			response = Menu.prompt(in, prompt).trim();
			if (response.length() > 0)
			{
				relevance = Double.parseDouble(response);
				changed = true;
			}
		}
		else
		{
			// Enter five numbers as batch.
			System.out.printf("  Current scores: Contrib %.1f, Interact %.1f, OnTrack %.1f, Quality %.1f, Relevance %.1f\n",
					contributing, interacting, onTrack, expectQuality, relevance);
			System.out.print("  Enter five new scores (or <Enter> for no change): ");
			String line = in.nextLine();
			if (line.length() > 0)
			{
				Scanner lineScanner = new Scanner(line);
				try
				{
					double newContributing = lineScanner.nextDouble();
					double newInteracting = lineScanner.nextDouble();
					double newOnTrack = lineScanner.nextDouble();
					double newExpectQuality = lineScanner.nextDouble();
					double newRelevance = lineScanner.nextDouble();
					lineScanner.close();
					// Save changes.
					changed = true;
					contributing = newContributing;
					interacting = newInteracting;
					onTrack = newOnTrack;
					expectQuality = newExpectQuality;
					relevance = newRelevance;
				}
				catch (NoSuchElementException e)
				{
					System.out.println("  No change (emtpy or invalid entry).");
				}
				finally
				{
					lineScanner.close();
				}
			}
		}
		if (changed)
		{
			updateScores(db);
		}
		return changed;
	}

	/**
	 * Store the current set of scores from the evaluating student for the evaluated
	 * students into the Evals table. An UPDATE is attempted first, and if that fails,
	 * do an INSERT.
	 * @param db TeamDB connection
	 * @throws SQLException
	 */
	private void updateScores(TeamDB db) throws SQLException
	{
		PreparedStatement stat = db.getConnection().prepareStatement(
				"UPDATE Evals SET Contributing = ?, Interacting = ?, OnTrack = ?, ExpectingQuality = ?, " +
				"Relevance = ? WHERE EvaluatingStudentId = ? AND EvaluatedStudentId = ?");
		try
		{
			stat.setDouble(1, contributing);
			stat.setDouble(2, interacting);
			stat.setDouble(3, onTrack);
			stat.setDouble(4, expectQuality);
			stat.setDouble(5, relevance);
			stat.setString(6, evaluatingStudent.getId());
			stat.setString(7, evaluatedStudent.getId());
			int updated = stat.executeUpdate();
			if (updated == 0)
			{
				stat = db.getConnection().prepareStatement(
						"INSERT INTO Evals (EvaluatingStudentId, EvaluatedStudentId, Contributing, " +
						"Interacting, OnTrack, ExpectingQuality, Relevance) VALUES (?, ?, ?, ?, ?, ?, ?)");
				stat.setString(1, evaluatingStudent.getId());
				stat.setString(2, evaluatedStudent.getId());
				stat.setDouble(3, contributing);
				stat.setDouble(4, interacting);
				stat.setDouble(5, onTrack);
				stat.setDouble(6, expectQuality);
				stat.setDouble(7, relevance);
				updated = stat.executeUpdate();
				if (updated == 1)
				{
					exists = true;
				}
			}
		}
		finally
		{
			stat.close();
		}
	}
	
	/**
	 * Create a new Eval object for the given students.
	 * @param db TeamDB connection
	 * @param _evaluating Student evaluating
	 * @param _evaluated Student evaluated
	 * @throws SQLException 
	 */
	public Eval(TeamDB db, Team t, Student evaluating, Student evaluated) throws SQLException
	{
		exists = false;
		team = t;
		evaluatingStudent = evaluating;
		evaluatedStudent = evaluated;
		contributing = DEFAULT_SCORE;
		interacting = DEFAULT_SCORE;
		onTrack = DEFAULT_SCORE;
		expectQuality = DEFAULT_SCORE;
		relevance = DEFAULT_SCORE;

		PreparedStatement stat = db.getConnection().prepareStatement(
				"SELECT Contributing, Interacting, OnTrack, ExpectingQuality, Relevance FROM Evals WHERE EvaluatingStudentId = ? AND EvaluatedStudentId = ?");
		try
		{
			stat.setString(1, evaluatingStudent.getId());
			stat.setString(2, evaluatedStudent.getId());
			ResultSet rs = stat.executeQuery();
			if (rs.next())
			{
				contributing = rs.getDouble(1);
				interacting = rs.getDouble(2);
				onTrack = rs.getDouble(3);
				expectQuality = rs.getDouble(4);
				relevance = rs.getDouble(5);
				exists = true;
			}
		}
		finally
		{
			stat.close();
		}
	}
	
	public boolean exists()
	{
		return exists;
	}
	
	public double getContributing()
	{
		return contributing;
	}
	public double getInteracting()
	{
		return interacting;
	}
	public double getOnTrack()
	{
		return onTrack;
	}
	public double getExpectQuality()
	{
		return expectQuality;
	}
	public double getRelevance()
	{
		return relevance;
	}
	public double getAverage()
	{
		return (contributing + interacting + onTrack + expectQuality + relevance) / 5.0;
	}
}

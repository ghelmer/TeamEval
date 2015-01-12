import java.io.PrintStream;
import java.sql.SQLException;

/**
 * Run the Team List report.
 * Output is the list of team names and students in each team.
 * @author ghelmer
 *
 */
public class ReportTeamList {
	private TeamDB teamDB;

	/**
	 * Prepare to run the Team List report.
	 * @param db Database connection for team data.
	 */
	public ReportTeamList(TeamDB db)
	{
		teamDB = db;
	}
	
	/**
	 * Run the Team List report.
	 * @param out Stream to which the report data is written.
	 */
	public void ExecuteReport(PrintStream out) throws SQLException
	{
		// SELECT TeamName, StudentName FROM teams, students WHERE Teams.StudentID = Students.StudentID ORDER BY TeamName, StudentName
		out.printf("%-32s %-24s\n", "Team", "Student");
		Team[] teams = Team.getAllTeams(teamDB);
		for (Team t : teams)
		{
			Student[] members = t.getStudents(teamDB);
			for (Student s : members)
			{
				out.printf("%-32s %-24s\n", t.getName(), s.getName(teamDB));
			}
		}
	}
}

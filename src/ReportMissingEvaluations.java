import java.io.PrintStream;
import java.sql.SQLException;
import java.util.TreeMap;

/**
 * Run the Missing Evaluations report.
 * Output the list of teams and members that have not turned in evaluations. 
 * @author ghelmer
 *
 */
public class ReportMissingEvaluations {
	private TeamDB teamDB;

	/**
	 * Prepare to run the Missing Evaluations report.
	 * @param db Database connection for team data.
	 */
	public ReportMissingEvaluations(TeamDB db)
	{
		teamDB = db;
	}
	
	/**
	 * Run the Missing Evaluations report.
	 * @param out Stream to which the report data is written.
	 */
	public void ExecuteReport(PrintStream out) throws SQLException
	{
		TreeMap<String, String> missing = new TreeMap<String, String>();
		Team[] teams = Team.getAllTeams(teamDB);
		out.printf("%-32s %-24s %-8s\n", "Team", "ID", "Missing", "Num Eval");
		for (Team t : teams)
		{
			Student[] members = t.getStudents(teamDB);
			for (Student evaluating : members)
			{
				int numEvaluated = Eval.countEvaluating(teamDB, evaluating);
				if (numEvaluated != members.length)
				{
					missing.put(evaluating.getId(),
							String.format("%-32s %-16s %-24s %d",
							t.getName(), evaluating.getId(), evaluating.getName(teamDB), numEvaluated));
				}
			}
		}
		for (String key : missing.keySet())
		{
		    out.println(missing.get(key));
		}
		out.printf("Total: %d missing\n\n", missing.size());
	}
}

import java.io.PrintStream;
import java.sql.SQLException;

/**
 * Run the Evaluations of a Student report.
 * Output the list of teams and members that have not turned in evaluations. 
 * @author ghelmer
 *
 */
public class ReportEvaluatedStudent {
	private TeamDB teamDB;
	private Student evaluated;

	/**
	 * Prepare to run the Missing Evaluations report.
	 * @param db Database connection for team data.
	 * @param s Student on whom to report
	 */
	public ReportEvaluatedStudent(TeamDB db, Student s)
	{
		teamDB = db;
		evaluated = s;
	}
	
	/**
	 * Run the Evaluations of a Student report.
	 * @param out Stream to which the report data is written.
	 */
	public void ExecuteReport(PrintStream out) throws SQLException
	{
		String evaluatedName = evaluated.getName(teamDB);
		if (evaluatedName == null)
		{
			out.println("Student not found.");
			return;
		}

		Team t = Team.getTeamByStudent(teamDB, evaluated);
		Student[] members = t.getStudents(teamDB);
		out.printf("%-32s %-24s %-24s %-10s %-10s %-10s %-10s %-10s %-10s\n",
				"Team", "Evaluated", "Evaluating", "Contrib", "Expect Quality", "Interacting",
				"On Track", "Relevance", "Average");
		double totalOfAverages = 0;
		int evalCount = 0;
		for (Student evaluating : members)
		{
			Eval eval = new Eval(teamDB, t, evaluating, evaluated);
			if (eval.exists())
			{
				out.printf("%-32s %-24s %-24s %10.1f %10.1f %10.1f %10.1f %10.1f %10.1f\n",
						t.getName(), evaluatedName, evaluating.getName(teamDB),
						eval.getContributing(), eval.getExpectQuality(), eval.getInteracting(),
						eval.getOnTrack(), eval.getRelevance(), eval.getAverage());
				totalOfAverages += eval.getAverage();
				evalCount++;
			}
		}
		if (evalCount > 0)
		{
			out.printf("%-32s %s %.1f\n\n", t.getName(),
					"Average for " + evaluatedName, totalOfAverages / evalCount);
		}
	}
}

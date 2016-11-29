import java.io.IOException;
import java.io.PrintStream;
import java.sql.SQLException;
import java.util.TreeMap;

/**
 * Run the Evaluation Scores report.
 * Output is all evaluation score for each student.
 * @author ghelmer
 *
 */
public class ReportEvalScores {
	private TeamDB teamDB;
	private double multiplier;

	/**
	 * Prepare to run the Evaluation Score report.
	 * @param db Database connection for team data.
	 */
	public ReportEvalScores(TeamDB db, double mult)
	{
		teamDB = db;
		multiplier = mult;
	}

	/**
	 * Run the All Evaluations report.
	 * @param out Stream to which the report data is written.
	 * @throws IOException 
	 * @throws ClassNotFoundException 
	 */
	public void ExecuteReport(PrintStream out) throws SQLException, IOException
	{
		TreeMap<String, String> evalScores = new TreeMap<String, String>();
		Team[] teams = Team.getAllTeams(teamDB);
		out.printf("%-32s %-24s %-32s %-8s\n", "Team", "ID", "Name", "EvalScore");
		for (Team t : teams)
		{
			Student[] members = t.getStudents(teamDB);
			for (Student e1 : members)
			{
				double totalOfAverages = 0;
				int evalCount = 0;
				for (Student e2: members)
				{
					Eval e = new Eval(teamDB, t, e2, e1);
					if (e.exists())
					{
						totalOfAverages += e.getAverage();
						evalCount++;
					}
				}
				if (evalCount > 0)
				{
					evalScores.put(e1.getName(teamDB), String.format("%-32s %-24s %-32s %8.1f", t.getName(),
							 e1.getId(), e1.getName(teamDB), multiplier * (totalOfAverages / evalCount)));
				}
			}
		}
		for (String key : evalScores.keySet())
		{
		    out.println(evalScores.get(key));
		}
	}
}

package reports;
import java.io.PrintStream;
import java.sql.SQLException;

import dal.Eval;
import dal.Student;
import dal.Team;
import dal.TeamDB;

/**
 * Run the All Evaluations report.
 * Output is all evaluations for all teams, grouped by team.
 * @author ghelmer
 *
 */
public class ReportAllEvaluations {
	private TeamDB teamDB;

	/**
	 * Prepare to run the Team List report.
	 * @param db Database connection for team data.
	 */
	public ReportAllEvaluations(TeamDB db)
	{
		teamDB = db;
	}
	
	/**
	 * Run the All Evaluations report.
	 * @param out Stream to which the report data is written.
	 */
	public void ExecuteReport(PrintStream out) throws SQLException
	{
		out.printf("%-32s %-24s %-24s %-10s %-10s %-10s %-10s %-10s %-10s\n",
				"Team", "Evaluated", "Evaluating", "Contrib", "Expect Quality", "Interacting",
				"On Track", "Relevance", "Average");
		Team[] teams = Team.getAllTeams(teamDB);
		for (Team t : teams)
		{
			Student[] members = t.getStudents(teamDB);
			int memberCount = 0;
			for (Student e1 : members)
			{
				double totalOfAverages = 0;
				int evalCount = 0;
				for (Student e2: members)
				{
					Eval e = new Eval(teamDB, t, e2, e1);
					if (e.exists())
					{
						out.printf("%-32s %-24s %-24s %10.1f %10.1f %10.1f %10.1f %10.1f %10.1f\n",
								t.getName(), e1.getName(teamDB), e2.getName(teamDB),
								e.getContributing(), e.getExpectQuality(), e.getInteracting(),
								e.getOnTrack(), e.getRelevance(), e.getAverage());
						totalOfAverages += e.getAverage();
						evalCount++;
						memberCount++;
					}
				}
				if (evalCount > 0)
				{
					out.printf("%-32s %s %.1f\n", t.getName(),
							"Average for " + e1.getName(teamDB), totalOfAverages / evalCount);
				}
			}
			if (memberCount > 0)
			{
				out.println();
			}
		}
	}
}

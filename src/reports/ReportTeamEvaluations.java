package reports;
import java.io.PrintStream;
import java.sql.SQLException;

import dal.Eval;
import dal.Student;
import dal.Team;
import dal.TeamDB;

/**
 * Run the Evaluations for a Team report.
 * Output the list of teams and members that have not turned in evaluations. 
 * @author ghelmer
 *
 */
public class ReportTeamEvaluations {
	private TeamDB teamDB;
	private Team team;

	/**
	 * Prepare to run the Evaluations for a Team report.
	 * @param db Database connection for team data.
	 * @param s Student on whom to report
	 */
	public ReportTeamEvaluations(TeamDB db, Team t)
	{
		teamDB = db;
		team = t;
	}
	
	/**
	 * Run the Evaluations for a Team report.
	 * @param out Stream to which the report data is written.
	 */
	public void ExecuteReport(PrintStream out) throws SQLException
	{
		Student[] members = team.getStudents(teamDB);
		if (members.length == 0)
		{
			out.println("Team does not exist or has no members.");
			return;
		}
		out.printf("%-32s %-24s %-24s %-10s %-10s %-10s %-10s %-10s %-10s\n",
				"Team", "Evaluated", "Evaluating", "Contrib", "Expect Quality", "Interacting",
				"On Track", "Relevance", "Average");
		int memberCount = 0;
		for (Student e1 : members)
		{
			double totalOfAverages = 0;
			int evalCount = 0;
			for (Student e2: members)
			{
				Eval e = new Eval(teamDB, team, e2, e1);
				if (e.exists())
				{
					out.printf("%-32s %-24s %-24s %10.1f %10.1f %10.1f %10.1f %10.1f %10.1f\n",
							team.getName(), e1.getName(teamDB), e2.getName(teamDB),
							e.getContributing(), e.getExpectQuality(), e.getInteracting(),
							e.getOnTrack(), e.getRelevance(), e.getAverage());
					totalOfAverages += e.getAverage();
					evalCount++;
					memberCount++;
				}
			}
			if (evalCount > 0)
			{
				out.printf("%-32s %s %.1f\n", team.getName(),
						"Average for " + e1.getName(teamDB), totalOfAverages / evalCount);
			}
		}
		if (memberCount > 0)
		{
			out.println();
		}
	}
}

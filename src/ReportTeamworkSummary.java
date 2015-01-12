import java.io.PrintStream;
import java.sql.SQLException;

/**
 * Run the Teamwork Summary report.
 * Output is the number of ratings collected for each of the five
 * ratings areas.
 * @author ghelmer
 *
 */
public class ReportTeamworkSummary {
	private TeamDB teamDB;

	static final int contribRatingIndex = 0;
	static final int interactRatingIndex = 1;
	static final int trackRatingIndex = 2;
	static final int qualityRatingIndex = 3;
	static final int relevanceRatingIndex = 4;

	/**
	 * Prepare to run the Teamwork Summary report.
	 * @param db Database connection for team data.
	 */
	ReportTeamworkSummary(TeamDB db)
	{
		teamDB = db;
	}

	/**
	 * Run the Teamwork Summary report.
	 * @param out Stream to which the report data is written.
	 */
	public void ExecuteReport(PrintStream out) throws SQLException
	{
		// Summarize the number of 5, 4, 3, 2, and 1 scores assigned
		// for each of the five conceptual areas.
		int ratingCount[][] = new int[5][5];
		String rowDesc[] =
			{
			"Contributing",
			"Interacting",  
			"Team On Track",
			"Expecting Quality",
			"Relevant Knowledge"
			};
		
		Team[] teams = Team.getAllTeams(teamDB);
		for (Team t : teams)
		{
			Student[] members = t.getStudents(teamDB);
			for (Student e1 : members)
			{
				for (Student e2: members)
				{
					Eval e = new Eval(teamDB, t, e2, e1);
					if (e.exists())
					{
						updateRatings(ratingCount, e);
					}
				}
			}
		}
		out.printf("Number of ratings:\n");
		out.printf("%-20s %10s %10s %10s %10s %10s\n", "", "5", "4", "3", "2", "1");
		for (int i = 0; i < 5; i++)
		{
			out.printf("%-20s %10d %10d %10d %10d %10d\n",
					rowDesc[i], ratingCount[i][4], ratingCount[i][3],
					ratingCount[i][2], ratingCount[i][1],
					ratingCount[i][0]);
		}
	}
	
	/**
	 * Report helper: Update the ratings count array for this evaluation.
	 */
	private static void updateRatings(int[][] ratingCounts, Eval e)
	{
		updateRating(ratingCounts, contribRatingIndex, (int)Math.round(e.getContributing()));
		updateRating(ratingCounts, interactRatingIndex, (int)Math.round(e.getInteracting()));
		updateRating(ratingCounts, trackRatingIndex, (int)Math.round(e.getOnTrack()));
		updateRating(ratingCounts, qualityRatingIndex, (int)Math.round(e.getExpectQuality()));
		updateRating(ratingCounts, relevanceRatingIndex, (int)Math.round(e.getRelevance()));
	}
	/**
	 * Report helper: Update the ratings count array for this category.
	 */
	private static void updateRating(int[][] ratingCounts, int ratingCategory, int rating)
	{
		if (rating >= 1 && rating <= 5)
		{
			ratingCounts[ratingCategory][rating-1]++;
		}
		else
		{
			throw new IllegalArgumentException("Unexpected rating value " + rating);
		}
	}
}

import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Scanner;
import java.util.TreeMap;


public class Menu {
	private static final boolean individualEntry = false;
	private TeamDB teamDB;
	public static void main(String[] args) {
		try
		{
			Menu m = new Menu();
			m.run();
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
		catch (ClassNotFoundException e)
		{
			e.printStackTrace();
		}
		catch (SQLException e)
		{
			e.printStackTrace();
		}
	}
	private Menu() throws ClassNotFoundException, IOException
	{
		teamDB = new TeamDB();
	}
	public static String prompt(Scanner in, String p)
	{
		System.out.print(p + ": ");
		return in.nextLine();
	}
	
	/**
	 * Run the main menu of the team evaluation program.
	 * @throws SQLException
	 * @throws FileNotFoundException
	 */
	private void run() throws SQLException, FileNotFoundException
	{
		boolean done = false;
		Scanner in = new Scanner(System.in);
		while (!done)
		{
			System.out.println("I) Initialize database  L)oad Students and Teams  E)nter Data   R)eports  Q)uit");
			String input = in.nextLine();
			if (input.equalsIgnoreCase("I")) {
				System.out.println("Enter 'YES' to remove all existing data: ");
				String answer = in.nextLine();
				if (answer.equalsIgnoreCase("YES"))
				{
					Student.initializeTable(teamDB);
					Team.initializeTable(teamDB);
					Eval.initializeTable(teamDB);
				}
				else
					System.out.println("OK, existing data preserved");
			}
			else if (input.equalsIgnoreCase("L"))
			{
				System.out.println("Expected format for students + teams:");
				System.out.println(" LastName FirstName UserID Team (separated by Tabs)");
				System.out.println(" Lines beginning with '#' are ignored.\n");
				String inputFn = prompt(in, "Enter name of file containing students and teams");
				Student.loadTable(teamDB, inputFn);
				Team.loadTable(teamDB, inputFn);
			}
			else if (input.equalsIgnoreCase("E"))
			{
				doEvalEntry(in);
			}
			else if (input.equalsIgnoreCase("R"))
			{
				doReports(in);
			}
			else if (input.equalsIgnoreCase("Q"))
			{
				done = true;
			}
		}
	}
	
	/**
	 * Handle evaluation data entry by team or by student.
	 * @param in Scanner from which to read input
	 * @throws SQLException
	 */
	private void doEvalEntry(Scanner in) throws SQLException
	{
		boolean done = false;
		while (!done)
		{
			System.out.println("Enter Data: L)oad Files  T)eam Data  S)tudent Data  Q)uit");
			String input = in.nextLine();
			if (input.equalsIgnoreCase("L"))
			{
				System.out.println("Enter path to directory containing spreadsheet files: ");
				String inputPath = in.nextLine();
				try
				{
					ExtractEvalsFromFiles extractor = new ExtractEvalsFromFiles(inputPath);
					TreeMap<String, double[]> results = new TreeMap<>();
					TreeMap<String, String> exceptions = new TreeMap<>();
					extractor.processFiles(results, exceptions);
					for (String key : results.keySet())
					{
						String[] parts = key.split(":");
						Student s = new Student(parts[0]);
						Team t = Team.getTeamByStudent(teamDB, s);
						if (t == null)
						{
							System.out.println("Student " + parts[0] + " not found in any team");
						}
						else
						{
							Student[] members = t.getStudents(teamDB);
							Arrays.sort(members);
							boolean found = false;
							for (Student e : members)
							{
								if (e.getName(teamDB).equalsIgnoreCase(parts[1]))
								{
									System.out.println("Entering scores for " + key);
									Eval eval = new Eval(teamDB, t, s, e);
									eval.setScores(results.get(key));
									eval.updateScores(teamDB);
									found = true;
									break;
								}
							}
							if (!found)
							{
								System.err.println("Could not find team for " + key);
							}
						}
					}
					if (exceptions.size() > 0)
					{
						System.err.println("Errors encountered in these files:");
						for (String key : exceptions.keySet())
						{
							System.err.println(exceptions.get(key));
						}
					}
				}
				catch (IOException e)
				{
					System.err.println("Failed to construct ExtractEvalsFromFiles: " + e.getMessage());
				}
			}
			if (input.equalsIgnoreCase("T"))
			{
				String teamName = prompt(in, "Enter team name");
				Team t = new Team(teamName);
				Student[] members = t.getStudents(teamDB);
				//Arrays.sort(members);
				for (Student e1 : members)
				{
					String doEvalFrom = prompt(in, "Enter data from " + e1.getName(teamDB) + "? (Y)");
					if (doEvalFrom.length() == 0 || doEvalFrom.equalsIgnoreCase("Y"))
					{
						for (Student e2 : members)
						{
							Eval eval = new Eval(teamDB, t, e1, e2);
							eval.enterScores(in, teamDB, individualEntry);
						}
					}
				}
			}
			else if (input.equalsIgnoreCase("S"))
			{
				String studentID = prompt(in, "Enter reporting student ID");
				Student s = new Student(studentID);
				Team t = Team.getTeamByStudent(teamDB, s);
				if (t == null)
				{
					System.out.println("Student " + studentID + " not found in any team");
				}
				else
				{
					Student[] members = t.getStudents(teamDB);
					Arrays.sort(members);
					for (Student e : members)
					{
						Eval eval = new Eval(teamDB, t, s, e);
						eval.enterScores(in, teamDB, individualEntry);
					}
				}
			}
			else if (input.equalsIgnoreCase("Q"))
			{
				done = true;
			}
		}
	}
	
	/**
	 * Run the reports from the team evaluation data in the database.
	 * @param in Scanner from which to read input
	 */
	private void doReports(Scanner in) throws SQLException
	{
		boolean done = false;
		while (!done)
		{
			System.out.println("Reports: A)ll  E)val Scores  T)eam  S)tudent  M)issing  L)ist Teams  s(U)mmary  Q)uit");
			String input = in.nextLine();
			if (input.equalsIgnoreCase("A"))
			{
				ReportAllEvaluations rae = new ReportAllEvaluations(teamDB);
				rae.ExecuteReport(System.out);
			}
			if (input.equalsIgnoreCase("E"))
			{
				String multiplierStr = prompt(in, "Enter evaulation score multiplier (e.g, 2.0)");
				if (multiplierStr.length() == 0)
					multiplierStr = "1";
				double multiplier = Double.parseDouble(multiplierStr);
				ReportEvalScores res = new ReportEvalScores(teamDB, multiplier);
				try {
					res.ExecuteReport(System.out);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			if (input.equalsIgnoreCase("T"))
			{
				String teamName = prompt(in, "Enter team name");
				Team t = new Team(teamName);
				ReportTeamEvaluations rte = new ReportTeamEvaluations(teamDB, t);
				rte.ExecuteReport(System.out);
			}
			else if (input.equalsIgnoreCase("S"))
			{
				String studentID = prompt(in, "Enter evaluated student ID");
				Student evaluated = new Student(studentID);
				ReportEvaluatedStudent res = new ReportEvaluatedStudent(teamDB, evaluated);
				res.ExecuteReport(System.out);
			}
			else if (input.equalsIgnoreCase("M"))
			{
				ReportMissingEvaluations rme = new ReportMissingEvaluations(teamDB);
				rme.ExecuteReport(System.out);
			}
			else if (input.equalsIgnoreCase("L"))
			{
				ReportTeamList rtl = new ReportTeamList(teamDB);
				rtl.ExecuteReport(System.out);
			}
			else if (input.equalsIgnoreCase("U"))
			{
				ReportTeamworkSummary rts = new ReportTeamworkSummary(teamDB);
				rts.ExecuteReport(System.out);
			}
			else if (input.equalsIgnoreCase("Q"))
			{
				done = true;
			}
		}
	}
}

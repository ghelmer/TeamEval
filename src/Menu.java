import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Scanner;
import java.util.TreeMap;


public class Menu {
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
			System.out.println("I) Initialize database  S)tudents Load  T)eams Load  E)nter Data   R)eports  Q)uit");
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
			else if (input.equalsIgnoreCase("S"))
			{
				String inputFn = prompt(in, "Enter name of file containing list of students");
				Student.loadTable(teamDB, inputFn);
			}
			else if (input.equalsIgnoreCase("T"))
			{
				String inputFn = prompt(in, "Enter name of file containing list of teams");
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
			System.out.println("Enter Data: T)eam Data  S)tudent Data  Q)uit");
			String input = in.nextLine();
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
							eval.enterScores(in, teamDB, false);
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
						eval.enterScores(in, teamDB, true);
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
			System.out.println("Reports: A)ll  T)eam  S)tudent  M)issing  L)ist Teams  Q)uit");
			String input = in.nextLine();
			if (input.equalsIgnoreCase("A"))
			{
				System.out.printf("%-32s %-24s %-24s %-10s %-10s %-10s %-10s %-10s %-10s\n",
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
								System.out.printf("%-32s %-24s %-24s %10.1f %10.1f %10.1f %10.1f %10.1f %10.1f\n",
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
							System.out.printf("%-32s %s %.1f\n", t.getName(),
									"Average for " + e1.getName(teamDB), totalOfAverages / evalCount);
						}
					}
					if (memberCount > 0)
					{
						System.out.println();
					}
				}
			}
			if (input.equalsIgnoreCase("T"))
			{
				String teamName = prompt(in, "Enter team name");
				Team t = new Team(teamName);
				Student[] members = t.getStudents(teamDB);
				if (members.length == 0)
				{
					System.out.println("Team does not exist or has no members.");
				}
				else
				{
					System.out.printf("%-32s %-24s %-24s %-10s %-10s %-10s %-10s %-10s %-10s\n",
							"Team", "Evaluated", "Evaluating", "Contrib", "Expect Quality", "Interacting",
							"On Track", "Relevance", "Average");
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
								System.out.printf("%-32s %-24s %-24s %10.1f %10.1f %10.1f %10.1f %10.1f %10.1f\n",
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
							System.out.printf("%-32s %s %.1f\n", t.getName(),
									"Average for " + e1.getName(teamDB), totalOfAverages / evalCount);
						}
					}
					if (memberCount > 0)
					{
						System.out.println();
					}
				}
			}
			else if (input.equalsIgnoreCase("S"))
			{
				String studentID = prompt(in, "Enter evaluated student ID");
				Student evaluated = new Student(studentID);
				String evaluatedName = evaluated.getName(teamDB);
				if (evaluatedName == null)
				{
					System.out.println("Student not found.");
				}
				else
				{
					Team t = Team.getTeamByStudent(teamDB, evaluated);
					Student[] members = t.getStudents(teamDB);
					System.out.printf("%-32s %-24s %-24s %-10s %-10s %-10s %-10s %-10s %-10s\n",
							"Team", "Evaluated", "Evaluating", "Contrib", "Expect Quality", "Interacting",
							"On Track", "Relevance", "Average");
					double totalOfAverages = 0;
					int evalCount = 0;
					for (Student evaluating : members)
					{
						Eval eval = new Eval(teamDB, t, evaluating, evaluated);
						if (eval.exists())
						{
							System.out.printf("%-32s %-24s %-24s %10.1f %10.1f %10.1f %10.1f %10.1f %10.1f\n",
									t.getName(), evaluatedName, evaluating.getName(teamDB),
									eval.getContributing(), eval.getExpectQuality(), eval.getInteracting(),
									eval.getOnTrack(), eval.getRelevance(), eval.getAverage());
							totalOfAverages += eval.getAverage();
							evalCount++;
						}
					}
					if (evalCount > 0)
					{
						System.out.printf("%-32s %s %.1f\n", t.getName(),
								"Average for " + evaluatedName, totalOfAverages / evalCount);
						System.out.println();
					}
				}
			}
			else if (input.equalsIgnoreCase("M"))
			{
				TreeMap<String, String> missing = new TreeMap<String, String>();
				Team[] teams = Team.getAllTeams(teamDB);
				System.out.printf("%-32s %-24s %-8s\n",
						"Team", "ID", "Missing", "Num Eval");
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
				    System.out.println(missing.get(key));
				}
				System.out.printf("Total: %d missing\n\n", missing.size());
			}
			else if (input.equalsIgnoreCase("L"))
			{
				// SELECT TeamName, StudentName FROM teams, students WHERE Teams.StudentID = Students.StudentID ORDER BY TeamName, StudentName
				System.out.printf("%-32s %-24s\n",
						"Team", "Student");
				Team[] teams = Team.getAllTeams(teamDB);
				for (Team t : teams)
				{
					Student[] members = t.getStudents(teamDB);
					for (Student s : members)
					{
						System.out.printf("%-32s %-24s\n",
										t.getName(), s.getName(teamDB));
					}
				}
			}
			else if (input.equalsIgnoreCase("Q"))
			{
				done = true;
			}
		}
	}
}

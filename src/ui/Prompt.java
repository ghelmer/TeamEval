package ui;

import java.util.Scanner;

public class Prompt {
	/**
	 * Prompt the user for input and read the line entered by the user.
	 * @param in - Scanner to use for input
	 * @param p - prompt to print
	 * @return entered line
	 */
	public static String prompt(Scanner in, String p)
	{
		System.out.print(p + ": ");
		return in.nextLine();
	}
}

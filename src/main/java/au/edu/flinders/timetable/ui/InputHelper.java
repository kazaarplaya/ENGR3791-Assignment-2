package au.edu.flinders.timetable.ui;

import java.util.Scanner;

/** Provides validated, prompted console input helpers. */
public class InputHelper {

    private static final String EXIT_SENTINEL = "q";

    private void checkExit(String raw) {
        if (raw.equalsIgnoreCase(EXIT_SENTINEL)) throw new EarlyExitException();
    }

    /** Prints the prompt and returns the raw line entered by the user (may be blank). */
    public String readLine(Scanner sc, String prompt) {
        System.out.print(prompt);
        String line = sc.nextLine();
        checkExit(line.trim());
        return line;
    }

    /**
     * Prompts the user for an integer between min and max (inclusive).
     * Re-prompts until a valid value is entered.
     */
    public int readInt(Scanner sc, String prompt, int min, int max) {
        while (true) {
            System.out.print(prompt);
            String raw = sc.nextLine().trim();
            checkExit(raw);
            try {
                int value = Integer.parseInt(raw);
                if (value >= min && value <= max) {
                    return value;
                }
                System.out.println("[WARN] Please enter a number between " + min + " and " + max + ".");
            } catch (NumberFormatException e) {
                System.out.println("[WARN] Invalid number. Please try again.");
            }
        }
    }

    /**
     * Prompts the user for a yes/no answer (y or n, case-insensitive).
     * Returns true for 'y', false for 'n'. Re-prompts on invalid input.
     */
    public boolean readBoolean(Scanner sc, String prompt) {
        while (true) {
            System.out.print(prompt + " (y/n): ");
            String raw = sc.nextLine().trim().toLowerCase();
            checkExit(raw);
            if (raw.equals("y")) return true;
            if (raw.equals("n")) return false;
            System.out.println("[WARN] Please enter y or n.");
        }
    }

    /**
     * Prompts the user for a non-blank string.
     * Re-prompts until the user enters at least one non-whitespace character.
     */
    public String readNonBlank(Scanner sc, String prompt) {
        while (true) {
            System.out.print(prompt);
            String raw = sc.nextLine().trim();
            checkExit(raw);
            if (!raw.isEmpty()) return raw;
            System.out.println("[WARN] Input cannot be blank. Please try again.");
        }
    }
}

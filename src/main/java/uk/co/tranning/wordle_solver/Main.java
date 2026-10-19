package uk.co.tranning.wordle_solver;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/** Java port of the Python command-line interface. */
public class Main {

  public static void main(String[] argv) throws IOException {
    int length = 5;
    boolean plurals = false;
    int type = 0;

    for (int i = 0; i < argv.length; i++) {
      String arg = argv[i];
      String name = arg;
      String value = null;
      int eq = arg.indexOf('=');
      if (arg.startsWith("--") && eq > 0) {
        name = arg.substring(0, eq);
        value = arg.substring(eq + 1);
      }
      switch (name) {
        case "--length":
          value = valueOrNext(argv, i, value);
          if (value == null) {
            i++;
            value = argv[i];
          } // consumed from next arg
          length = Integer.parseInt(value);
          break;
        case "--type":
          value = valueOrNext(argv, i, value);
          if (value == null) {
            i++;
            value = argv[i];
          }
          type = Integer.parseInt(value);
          break;
        case "--plurals":
          // argparse's type=bool: bool(str) is True for any non-empty string.
          if (value == null && i + 1 < argv.length && !argv[i + 1].startsWith("--")) {
            i++;
            value = argv[i];
          }
          plurals = value == null || !value.isEmpty();
          break;
        default:
          System.err.println("error: unrecognized arguments: " + arg);
          System.exit(2);
      }
    }

    String[][] wordListFilePaths = {
      {"english_words_opener.txt", "english_words_full.txt"},
      {"sunderland_players.txt", "sunderland_players.txt"}
    };
    String[] myType = {"WORDLE", "MACKEM DLE"};
    String[] myTries = {"words", "players"};

    WordSolverMultiList solverMulti =
        new WordSolverMultiList(
            new ArrayList<>(Arrays.asList(wordListFilePaths[type])), length, !plurals);

    solverMulti.maxTryIndexesForLists = new ArrayList<>(Arrays.asList(2L, Long.MAX_VALUE));

    System.out.println("The " + myType[type] + " Solver CLI");
    System.out.println("(Word length: " + length + "; Plurals: " + (plurals ? "Yes" : "No") + ")");
    System.out.println("Meanings of symbols:");
    System.out.println(" +\tletter in the word and in the right spot (green box)");
    System.out.println(" ?\tletter in the word but in a wrong spot (orange box)");
    System.out.println(" _\tletter not in the word (grey box)\n");
    System.out.println("Commands:");
    System.out.println(
        " !done\t\tyou're done guessing a hidden word.  this will reset the state of the solver for you to guess a new hidden word");
    System.out.println(" !tries\t\tsee the tries entered");
    System.out.println(" !remove_last\tremove the last try entered");
    System.out.println(" !exit\texit\n");

    BufferedReader in =
        new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));

    while (true) {
      System.out.println("Please enter your last try as word:symbols");
      String line = in.readLine();
      if (line == null) {
        System.err.println("EOFError: EOF when reading a line");
        System.exit(1);
      }
      String userInput = line.toLowerCase(Locale.ROOT);

      boolean reset = false;

      if (userInput.contains(":")) {
        String[] values = userInput.split(":", -1);
        if (values.length < 2) {
          System.out.println("Invalid format");
          continue;
        }
        boolean lengthsOk = true;
        for (String value : values) {
          if (value.codePointCount(0, value.length()) != solverMulti.wordLength) {
            lengthsOk = false;
            break;
          }
        }
        if (!lengthsOk) {
          System.out.println("Invalid format: length of word or symbol is incorrect");
          continue;
        }
        if (values[1].equals("+".repeat(solverMulti.wordLength))) {
          System.out.println("Great!");
          reset = true;
        } else {
          solverMulti.inputGuessResult(values[0], values[1]);
          List<PatternConflict> conflicts = solverMulti.getPatternParameterConflicts();
          if (!conflicts.isEmpty()) {
            for (PatternConflict conflict : conflicts) {
              System.out.println(conflict.letter() + ": " + conflict.message());
            }
            removeLastTry(solverMulti);
            System.out.println("Your last try has been removed");
            continue;
          }
        }
      } else if (userInput.equals("!done")) {
        reset = true;
      } else if (userInput.equals("!remove_last")) {
        removeLastTry(solverMulti);
        System.out.println("Your last try has been removed");
        continue;
      } else if (userInput.equals("!tries")) {
        if (solverMulti.tries.isEmpty()) {
          System.out.println("No tries entered");
        } else {
          for (int i = 0; i < solverMulti.tries.size(); i++) {
            System.out.println("\tTry " + i + ": " + solverMulti.tries.get(i));
          }
        }
        continue;
      } else if (userInput.equals("!exit")) {
    	  System.exit(0);
      } else {
        System.out.println("Invalid input");
      }

      if (reset) {
        solverMulti.reset();
        System.out.println("The state is reset");
        continue;
      }

      SuggestedWordsResults suggestedWords = solverMulti.getSuggestedWords();

      if (!suggestedWords.words().isEmpty()) {
        System.out.println(
            "Suggested words (from list '" + suggestedWords.wordListFilePath() + "'):");
        for (String suggestion :
            suggestedWords.words().subList(0, Math.min(10, suggestedWords.words().size()))) {
          System.out.println("\t" + suggestion);
        }
      } else {
        System.out.println(
            "Sorry, no other possible "
                + myTries[type]
                + ".  Please check the result symbols you entered.");
      }
    }
  }

  /**
   * Returns the value for "--opt value" style arguments, or null when the value is in the next argv
   * slot.
   */
  private static String valueOrNext(String[] argv, int i, String value) {
    if (value != null) {
      return value;
    }
    if (i + 1 >= argv.length) {
      System.err.println("error: argument " + argv[i] + ": expected one argument");
      System.exit(2);
    }
    return null;
  }

  private static void removeLastTry(WordSolverMultiList solverMulti) {
    if (!solverMulti.tries.isEmpty()) {
      solverMulti.tries.remove(solverMulti.tries.size() - 1);
    }
  }
}

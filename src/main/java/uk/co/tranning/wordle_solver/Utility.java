package uk.co.tranning.wordle_solver;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Java port of the Python Utility.load_word_list helper. The original utility module was not
 * supplied, so this follows the behaviour assumed for the port: keep ASCII-alphabetic words of the
 * requested length (lower-cased, de-duplicated, file order preserved), optionally dropping words
 * ending in "s" when plurals are excluded.
 */
public final class Utility {

  /**
   * Loads words of the given length from a word list file, using the default word length (5) and
   * excluding plurals.
   */
  public static List<String> loadWordList(String wordListFilePath) throws IOException {
    return loadWordList(wordListFilePath, 5, true);
  }

  /** Loads words of the given length from a word list file, excluding plurals. */
  public static List<String> loadWordList(String wordListFilePath, int wordLength)
      throws IOException {
    return loadWordList(wordListFilePath, wordLength, true);
  }

  /**
   * Loads words of the given length from a word list file.
   *
   * @param wordListFilePath path to the word list file (one word per line)
   * @param wordLength the required length of each word (in code points)
   * @param excludePlurals if true, words ending in "s" (except "ss") are removed
   * @return the filtered list of words
   */
  public static List<String> loadWordList(
      String wordListFilePath, int wordLength, boolean excludePlurals) throws IOException {

    List<String> englishWords = new ArrayList<>();
    for (String line : Files.readAllLines(Paths.get(wordListFilePath), StandardCharsets.UTF_8)) {
      englishWords.add(line.replace("\r", "").replace("\n", ""));
    }

    List<String> wordList = new ArrayList<>();
    for (String word : englishWords) {
      if (word.codePointCount(0, word.length()) == wordLength) {
        wordList.add(word);
      }
    }

    if (excludePlurals) {
      List<String> filtered = new ArrayList<>();
      for (String word : wordList) {
        // Keep the word if it does not end in "s", or if it ends in "ss".
        // Empty words (only possible when wordLength == 0) are kept instead of crashing.
        if (!word.endsWith("s") || word.endsWith("ss")) {
          filtered.add(word);
        }
      }
      wordList = filtered;
    }

    return wordList;
  }

  /**
   * Loads a word-to-score mapping from a tab-separated file where each line has the form {@code
   * word<TAB>score}. Insertion order of the file is preserved.
   *
   * @param wordScoreFilePath path to the tab-separated word score file
   * @return map of word to its score
   * @throws IOException if the file cannot be read
   * @throws IllegalArgumentException if a non-empty line is not in the form word<TAB>score
   */
  public static Map<String, Double> loadWordScoresDict(String wordScoreFilePath)
      throws IOException {
    Map<String, Double> wordScoreDict = new LinkedHashMap<>();

    for (String line : Files.readAllLines(Paths.get(wordScoreFilePath), StandardCharsets.UTF_8)) {
      // Skip blank lines (e.g., a stray empty line at the end of the file).
      if (line.replace("\r", "").replace("\n", "").isEmpty()) {
        continue;
      }

      // Limit -1 keeps trailing empty fields, matching Python's str.split("\t").
      String[] parts = line.split("\t", -1);
      if (parts.length != 2) {
        throw new IllegalArgumentException(
            "Malformed line in word score file (expected 'word<TAB>score'): " + line);
      }

      String word = parts[0];
      String score = parts[1].replace("\r", "").replace("\n", "");
      wordScoreDict.put(word, Double.parseDouble(score));
    }

    return wordScoreDict;
  }
}

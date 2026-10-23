package uk.co.tranning.wordle_solver;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Java port of the Python WorldSolverMultiList class. */
public class WordSolverMultiList {

  final List<String> wordListFilePaths;
  final int wordLength;
  final boolean excludePlurals;
  final Map<String, Double> wordScores;
  final boolean orderWordsByScoreDesc;
  final Map<String, Map<String, List<String>>> wordSymbolCombinations;

  List<List<String>> wordLists = new ArrayList<>();
  List<Long> maxTryIndexesForLists = new ArrayList<>();
  List<Attempt> tries = new ArrayList<>();
  List<WordleSolver> solvers = new ArrayList<>();

  /**
   * COnstructor
   * @param wordListFilePaths the paths
   * @param wordLength length of guess
   * @param excludePlurals self-explanatory
   * @param wordScores map of probabilities
   * @param orderWordsByScoreDesc self-explanatory
   * @param wordSymbolCombinations self-explanatory
   */
  public WordSolverMultiList(
      List<String> wordListFilePaths,
      int wordLength,
      boolean excludePlurals,
      Map<String, Double> wordScores,
      boolean orderWordsByScoreDesc,
      Map<String, Map<String, List<String>>> wordSymbolCombinations) {
    this.wordListFilePaths = wordListFilePaths;
    this.wordLength = wordLength;
    this.excludePlurals = excludePlurals;
    this.wordScores = wordScores;
    this.orderWordsByScoreDesc = orderWordsByScoreDesc;
    this.wordSymbolCombinations = wordSymbolCombinations;
    for (String filePath : wordListFilePaths) {
      try {
        wordLists.add(Utility.loadWordList(filePath, wordLength, excludePlurals));
      } catch (IOException ioex) {
        // do nothing yet
      }
    }
    reset();
  }

  /**
   * Simpler constructor
   * @param wordListFilePaths the paths
   * @param wordLength length of guess
   * @param excludePlurals self-explanatory
   */
  public WordSolverMultiList(
      List<String> wordListFilePaths, int wordLength, boolean excludePlurals) {
    this(wordListFilePaths, wordLength, excludePlurals, new HashMap<>(), false, new HashMap<>());
  }

  /**
   * reset the solvers
   */
  public void reset() {
    tries = new ArrayList<>();
    solvers = new ArrayList<>();
    for (List<String> wordList : wordLists) {
      WordleSolver solver = new WordleSolver(null, wordLength, excludePlurals);
      solver.wordList = wordList;
      solver.wordScores = wordScores;
      solver.orderWordsByDescendingScore = orderWordsByScoreDesc;
      solver.wordSymbolCombinations = wordSymbolCombinations;
      solvers.add(solver);
    }
  }
  
  /**
   * reset the patterns and solvers
   */

  public void resetPatternParameters() {
    for (WordleSolver solver : solvers) {
      solver.reset();
    }
  }
  
  /**
   * Check for conflicts
   * @return list of conflicts
   */

  public List<PatternConflict> getPatternParameterConflicts() {
    if (!solvers.isEmpty()) {
      // conflict check results should be identical across solvers;
      // the first solver's result is returned, as in the original.
      return solvers.get(0).getPatternParameterConflicts();
    }
    throw new IllegalStateException("No solvers");
  }
  
  /**
   * Update the pattern parameters with the latest info
   */

  public void updatePatternParameters() {
    for (WordleSolver solver : solvers) {
      solver.tries = this.tries;
      solver.updatePatternParameters();
    }
  }

  /**
   * In put a previous guess
   * @param word the previous guess
   * @param resultSymbols the symbols associated with the guess
   */
  public void inputGuessResult(String word, String resultSymbols) {
    for (WordleSolver solver : solvers) {
      solver.inputGuessResult(word, resultSymbols);
    }
    tries.add(new Attempt(word, resultSymbols));
  }

  /**
   * Get the list of suggested words
   * @return list of words
   */
  public SuggestedWordsResults getSuggestedWords() {
    for (int i = 0; i < solvers.size(); i++) {
      if (maxTryIndexesForLists.size() >= solvers.size()) {
        if (tries.size() >= maxTryIndexesForLists.get(i)) {
          continue;
        }
      }
      List<String> suggestedWords = solvers.get(i).getSuggestedWords();
      if (!suggestedWords.isEmpty()) {
        return new SuggestedWordsResults(suggestedWords, wordListFilePaths.get(i));
      }
    }
    String lastWordListFilePath =
        wordListFilePaths.isEmpty() ? null : wordListFilePaths.get(wordListFilePaths.size() - 1);
    return new SuggestedWordsResults(new ArrayList<>(), lastWordListFilePath);
  }
  
  /**
   * Exclude this list of words
   * @param excludedWords list of words to exclude from possible answers
   */

  public void setExcludedWords(List<String> excludedWords) {
    for (WordleSolver solver : solvers) {
      solver.excludedWords = excludedWords;
    }
  }
}

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
   * 
   * @param wordListFilePaths
   * @param wordLength
   * @param excludePlurals
   * @param wordScores
   * @param orderWordsByScoreDesc
   * @param wordSymbolCombinations
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
   * 
   * @param wordListFilePaths
   * @param wordLength
   * @param excludePlurals
   */
  public WordSolverMultiList(
      List<String> wordListFilePaths, int wordLength, boolean excludePlurals) {
    this(wordListFilePaths, wordLength, excludePlurals, new HashMap<>(), false, new HashMap<>());
  }

  /**
   * 
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
   * 
   */

  public void resetPatternParameters() {
    for (WordleSolver solver : solvers) {
      solver.reset();
    }
  }
  
  /**
   * 
   * @return
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
   * 
   */

  public void updatePatternParameters() {
    for (WordleSolver solver : solvers) {
      solver.tries = this.tries;
      solver.updatePatternParameters();
    }
  }

  /**
   * 
   * @param word
   * @param resultSymbols
   */
  public void inputGuessResult(String word, String resultSymbols) {
    for (WordleSolver solver : solvers) {
      solver.inputGuessResult(word, resultSymbols);
    }
    tries.add(new Attempt(word, resultSymbols));
  }

  /**
   * 
   * @return
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
   * 
   * @param excludedWords
   */

  public void setExcludedWords(List<String> excludedWords) {
    for (WordleSolver solver : solvers) {
      solver.excludedWords = excludedWords;
    }
  }
}

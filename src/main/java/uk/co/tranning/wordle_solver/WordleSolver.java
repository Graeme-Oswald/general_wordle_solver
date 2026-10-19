package uk.co.tranning.wordle_solver;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Java port of the Python WordleSolver class. */
public class WordleSolver {

  static final String PERMITTED_INPUT_SYMBOLS = "+?_";
  static final char SYMBOL_ANY_LETTER = '*';
  static final String ASCII_LOWERCASE = "abcdefghijklmnopqrstuvwxyz";

  /** Letter with its probability, mirroring the (letter, prob) tuples of the Python code. */
  record LetterProb(char letter, double prob) {}

  /** Word with its score, mirroring the (word, score) tuples of the Python code. */
  record WordScore(String word, double score) {}

  final String wordListFilePath;
  final int wordLength;
  final boolean excludePlurals;

  List<String> wordList = new ArrayList<>();
  Map<String, Double> wordScores = new HashMap<>();
  Map<String, Map<String, List<String>>> wordSymbolCombinations = new HashMap<>();
  boolean orderWordsByDescendingScore = false;

  List<String> effectiveWordList = new ArrayList<>();
  List<Attempt> tries = new ArrayList<>();

  String includedLetters;
  String excludedLetters;
  String highProbLetters;
  String[] wrongSpotPattern;
  String rightSpotPattern;
  Map<Character, Integer> maxLetterOccurrence;
  List<String> excludedWords;

  public WordleSolver(String wordListFilePath, int wordLength, boolean excludePlurals) {
    this.wordListFilePath = wordListFilePath;
    this.wordLength = wordLength;
    this.excludePlurals = excludePlurals;
    if (wordListFilePath != null) {
      try {
        this.wordList = Utility.loadWordList(wordListFilePath, wordLength, excludePlurals);
      } catch (IOException ioex) {
        // do nothing
      }
    }
    reset();
  }

  public void reset() {
    tries = new ArrayList<>();
    resetPatternParameters();
  }

  void resetPatternParameters() {
    includedLetters = "";
    excludedLetters = "";
    highProbLetters = "";
    wrongSpotPattern = new String[wordLength];
    for (int i = 0; i < wordLength; i++) {
      wrongSpotPattern[i] = "";
    }
    rightSpotPattern = String.valueOf(SYMBOL_ANY_LETTER).repeat(wordLength);
    maxLetterOccurrence = new LinkedHashMap<>();
    excludedWords = new ArrayList<>();
    effectiveWordList = new ArrayList<>();
  }

  List<PatternConflict> getPatternParameterConflicts() {
    List<PatternConflict> conflicts = new ArrayList<>();
    Character letter = null;
    for (int k = 0; k < excludedLetters.length(); k++) {
      letter = excludedLetters.charAt(k);
      if ((includedLetters + highProbLetters).indexOf(letter) >= 0) {
        conflicts.add(new PatternConflict(letter, "excluded letter found in inclusion list"));
      }
      if (rightSpotPattern.indexOf(letter) >= 0) {
        conflicts.add(new PatternConflict(letter, "excluded letter found in right spot pattern"));
      }
    }
    for (int i = 0; i < wordLength; i++) {
      char spot = rightSpotPattern.charAt(i);
      if (wrongSpotPattern[i].indexOf(spot) >= 0) {
        if (letter == null) {
          // The Python original raises UnboundLocalError here (letter never assigned).
          throw new IllegalStateException(
              "UnboundLocalError: local variable 'letter' referenced before assignment");
        }
        conflicts.add(
            new PatternConflict(letter, "letter in right spot found in wrong spot pattern"));
      }
    }
    return conflicts;
  }

  void updatePatternParameters() {
    resetPatternParameters();
    List<List<String>> possibleWordLists = new ArrayList<>();
    for (Attempt attempt : tries) {
      String word = attempt.word();
      String symbolPattern = attempt.symbols();
      for (int i = 0; i < wordLength; i++) {
        char symbol = symbolPattern.charAt(i);
        if (symbol == '+' || symbol == '?') {
          includedLetters += word.charAt(i);
          if (symbol == '+') {
            char[] spots = rightSpotPattern.toCharArray();
            spots[i] = word.charAt(i);
            rightSpotPattern = new String(spots);
          } else {
            wrongSpotPattern[i] += word.charAt(i);
          }
        } else if (symbol == '_') {
          char c = word.charAt(i);
          if (includedLetters.indexOf(c) < 0) {
            excludedLetters += c;
          } else if (!maxLetterOccurrence.containsKey(c)) {
            int count = 0;
            for (int j = 0; j < word.length(); j++) {
              if (j != i && word.charAt(j) == c) {
                count++;
              }
            }
            maxLetterOccurrence.put(c, count);
          }
        }
      }
      Map<String, List<String>> combinations = wordSymbolCombinations.get(word);
      if (combinations != null && combinations.containsKey(symbolPattern)) {
        possibleWordLists.add(combinations.get(symbolPattern));
      }
    }
    includedLetters = uniqueChars(includedLetters);
    StringBuilder remainingExcluded = new StringBuilder();
    for (char c : excludedLetters.toCharArray()) {
      if (includedLetters.indexOf(c) < 0) {
        remainingExcluded.append(c);
      }
    }
    excludedLetters = uniqueChars(remainingExcluded);
    for (int i = 0; i < wordLength; i++) {
      wrongSpotPattern[i] = uniqueChars(wrongSpotPattern[i]);
    }
    if (!possibleWordLists.isEmpty()) {
      LinkedHashSet<String> effectiveWordsSet = new LinkedHashSet<>();
      for (List<String> list : possibleWordLists) {
        effectiveWordsSet.addAll(list);
      }
      List<Set<String>> lookups = new ArrayList<>();
      for (List<String> list : possibleWordLists) {
        lookups.add(new HashSet<>(list));
      }
      List<String> effective = new ArrayList<>();
      for (String word : effectiveWordsSet) {
        boolean inAll = true;
        for (Set<String> lookup : lookups) {
          if (!lookup.contains(word)) {
            inAll = false;
            break;
          }
        }
        if (inAll) {
          effective.add(word);
        }
      }
      effectiveWordList = effective;
    }
  }

  /**
   * Equivalent of Python's "".join(set(s)); order is irrelevant because only membership is used.
   */
  static String uniqueChars(CharSequence s) {
    LinkedHashSet<Character> set = new LinkedHashSet<>();
    for (int i = 0; i < s.length(); i++) {
      set.add(s.charAt(i));
    }
    StringBuilder sb = new StringBuilder();
    for (char c : set) {
      sb.append(c);
    }
    return sb.toString();
  }

  /** Returns an empty map when the word list is empty (the Python version returns a falsy []). */
  Map<Character, Double> getLetterProbDict(List<String> words) {
    Map<Character, Double> probs = new LinkedHashMap<>();
    if (words.isEmpty()) {
      return probs;
    }
    int[] freq = new int[26];
    long total = 0;
    for (String word : words) {
      for (int i = 0; i < word.length(); i++) {
        freq[word.charAt(i) - 'a']++;
      }
      total += word.length();
    }
    for (int k = 0; k < 26; k++) {
      probs.put((char) ('a' + k), (double) freq[k] / total);
    }
    return probs;
  }

  List<LetterProb> getSuggestedLettersByFreq(List<String> possibleWords) {
    List<LetterProb> letterFreqs = new ArrayList<>();
    if (possibleWords.isEmpty()) {
      return letterFreqs;
    }
    String excluded = includedLetters + excludedLetters + highProbLetters;
    for (Map.Entry<Character, Double> entry : getLetterProbDict(possibleWords).entrySet()) {
      if (excluded.indexOf(entry.getKey()) < 0) {
        letterFreqs.add(new LetterProb(entry.getKey(), entry.getValue()));
      }
    }
    return letterFreqs;
  }

  List<Map<Character, Double>> getLetterPositionalProbDict(List<String> words) {
    List<Map<Character, Double>> positionalProb = new ArrayList<>();
    for (int i = 0; i < rightSpotPattern.length(); i++) {
      if (rightSpotPattern.charAt(i) == SYMBOL_ANY_LETTER) {
        List<String> letterList = new ArrayList<>();
        for (String word : words) {
          letterList.add(String.valueOf(word.charAt(i)));
        }
        positionalProb.add(getLetterProbDict(letterList));
      } else {
        positionalProb.add(new LinkedHashMap<>());
      }
    }
    return positionalProb;
  }

  List<WordScore> sortWordsWithLetterPositionalProb(List<String> words) {
    List<Map<Character, Double>> letterPositionProb = getLetterPositionalProbDict(words);
    List<WordScore> wordsWithProb = new ArrayList<>();
    for (String word : words) {
      double score = 1.0;
      for (int i = 0; i < letterPositionProb.size(); i++) {
        Map<Character, Double> probs = letterPositionProb.get(i);
        if (!probs.isEmpty()) {
          score *= probs.get(word.charAt(i));
        }
      }
      wordsWithProb.add(new WordScore(word, score));
    }
    // List.sort is stable, matching Python's sort(reverse=True) ordering of ties.
    wordsWithProb.sort(Comparator.comparingDouble(WordScore::score).reversed());
    return wordsWithProb;
  }

  List<String> sortWords(List<String> words) {
    List<String> sortedWords = new ArrayList<>();
    if (!wordScores.isEmpty() && words.stream().allMatch(wordScores::containsKey)) {
      List<WordScore> wordsWithScores = new ArrayList<>();
      for (String word : words) {
        wordsWithScores.add(new WordScore(word, wordScores.get(word)));
      }
      Comparator<WordScore> comparator = Comparator.comparingDouble(WordScore::score);
      if (orderWordsByDescendingScore) {
        comparator = comparator.reversed();
      }
      wordsWithScores.sort(comparator);
      for (WordScore ws : wordsWithScores) {
        sortedWords.add(ws.word());
      }
    }
    if (sortedWords.isEmpty()) {
      for (WordScore ws : sortWordsWithLetterPositionalProb(words)) {
        sortedWords.add(ws.word());
      }
    }
    return sortedWords;
  }

  boolean isNotInWord(String word) {
    for (int i = 0; i < excludedLetters.length(); i++) {
      if (word.indexOf(excludedLetters.charAt(i)) >= 0) {
        return false;
      }
    }
    return true;
  }

  boolean isInWord(String word) {
    String required = includedLetters + highProbLetters;
    for (int i = 0; i < required.length(); i++) {
      if (word.indexOf(required.charAt(i)) < 0) {
        return false;
      }
    }
    return true;
  }

  /**
   * The Python original compares a word against the list of (word, symbols) tuples, so the check is
   * always true. The port keeps that behaviour; the already-tried words are excluded by the
   * allExcluded filter in getPossibleWords.
   */
  boolean isNotTried(String word) {
    return true;
  }

  boolean matchRightSpotPattern(String word) {
    for (int i = 0; i < rightSpotPattern.length(); i++) {
      char spot = rightSpotPattern.charAt(i);
      if (spot == SYMBOL_ANY_LETTER) {
        continue;
      }
      if (spot != word.charAt(i)) {
        return false;
      }
    }
    return true;
  }

  List<String> getPossibleWords() {
    if (effectiveWordList.isEmpty()) {
      effectiveWordList = wordList;
    }
    Set<String> allExcludedWords = new HashSet<>();
    for (Attempt attempt : tries) {
      allExcludedWords.add(attempt.word());
    }
    allExcludedWords.addAll(excludedWords);

    List<String> firstLevelFilter = new ArrayList<>();
    for (String word : effectiveWordList) {
      if (isInWord(word)
          && isNotInWord(word)
          && isNotTried(word)
          && !allExcludedWords.contains(word)) {
        firstLevelFilter.add(word);
      }
    }

    List<String> secondLevelFilter = new ArrayList<>();
    for (String word : firstLevelFilter) {
      boolean ok = true;
      for (int i = 0; i < wordLength; i++) {
        if (wrongSpotPattern[i].indexOf(word.charAt(i)) >= 0) {
          ok = false;
          break;
        }
      }
      if (ok) {
        secondLevelFilter.add(word);
      }
    }

    List<String> thirdLevelFilter = new ArrayList<>();
    for (String word : secondLevelFilter) {
      if (matchRightSpotPattern(word)) {
        thirdLevelFilter.add(word);
      }
    }

    if (maxLetterOccurrence.isEmpty()) {
      return thirdLevelFilter;
    }
    // Mirrors the Python original: each iteration filters thirdLevelFilter again and
    // only the last filter result is returned.
    List<String> fourthLevelFilter = new ArrayList<>();
    for (Map.Entry<Character, Integer> entry : maxLetterOccurrence.entrySet()) {
      char letter = entry.getKey();
      int max = entry.getValue();
      fourthLevelFilter = new ArrayList<>();
      for (String word : thirdLevelFilter) {
        int count = 0;
        for (int i = 0; i < word.length(); i++) {
          if (word.charAt(i) == letter) {
            count++;
          }
        }
        if (count <= max) {
          fourthLevelFilter.add(word);
        }
      }
    }
    return fourthLevelFilter;
  }

  List<String> getSuggestedWords() {
    List<String> allPossibleWords = getPossibleWords();
    if (allPossibleWords.isEmpty()) {
      return new ArrayList<>();
    }
    int unknownLetterCount = wordLength - includedLetters.length();
    List<LetterProb> suggestedLettersWithProb = getSuggestedLettersByFreq(allPossibleWords);
    if (suggestedLettersWithProb.isEmpty()) {
      return new ArrayList<>();
    }
    suggestedLettersWithProb.sort(Comparator.comparingDouble(LetterProb::prob).reversed());
    List<Character> suggestedLetters = new ArrayList<>();
    for (LetterProb lp : suggestedLettersWithProb) {
      suggestedLetters.add(lp.letter());
    }
    for (int i = unknownLetterCount; i > 0; i--) {
      StringBuilder head = new StringBuilder();
      for (int k = 0; k < Math.min(i, suggestedLetters.size()); k++) {
        head.append(suggestedLetters.get(k));
      }
      highProbLetters = head.toString();
      updatePatternParameters();
      List<String> suggestedWords = getPossibleWords();
      highProbLetters = "";
      if (!suggestedWords.isEmpty()) {
        return sortWords(suggestedWords);
      }
    }
    return sortWords(allPossibleWords);
  }

  public void inputGuessResult(String word, String resultSymbols) {
    if (word.codePointCount(0, word.length()) != wordLength
        || resultSymbols.codePointCount(0, resultSymbols.length()) != wordLength) {
      throw new IllegalArgumentException("Word length or symbol length is invalid");
    }
    for (int i = 0; i < word.length(); i++) {
      if (ASCII_LOWERCASE.indexOf(word.charAt(i)) < 0) {
        throw new IllegalArgumentException("Word contains invalid character");
      }
    }
    for (int i = 0; i < resultSymbols.length(); i++) {
      if (PERMITTED_INPUT_SYMBOLS.indexOf(resultSymbols.charAt(i)) < 0) {
        throw new IllegalArgumentException("Symbols contains an invalid symbol");
      }
    }
    tries.add(new Attempt(word, resultSymbols));
    updatePatternParameters();
  }
}

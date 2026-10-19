package uk.co.tranning.wordle_solver;

import java.util.List;

/** Java equivalent of the Python dataclass SuggestedWordsResults. 
 * @param words list of guesses
 * @param wordListFilePath definitive word lists to solve against
 * */
public record SuggestedWordsResults(List<String> words, String wordListFilePath) {}

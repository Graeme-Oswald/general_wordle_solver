package uk.co.tranning.wordle_solver;

import java.util.List;

/** Java equivalent of the Python dataclass SuggestedWordsResults. */
public record SuggestedWordsResults(List<String> words, String wordListFilePath) {}

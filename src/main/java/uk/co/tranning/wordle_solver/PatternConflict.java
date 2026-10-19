package uk.co.tranning.wordle_solver;
/** A (letter, message) pair returned by pattern-parameter conflict checks. */
public record PatternConflict(char letter, String message) {
}

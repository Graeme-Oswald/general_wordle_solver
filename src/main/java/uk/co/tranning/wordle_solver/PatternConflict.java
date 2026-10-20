package uk.co.tranning.wordle_solver;

/** A (letter, message) pair returned by pattern-parameter conflict checks. 
 * @param letter the character
 * @param message the error
 * */
public record PatternConflict(char letter, String message) {}

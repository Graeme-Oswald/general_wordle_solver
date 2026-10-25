package uk.co.tranning.wordle_solver;

/**
 * One try entered by the user: the guessed word and its result symbols. toString mirrors the Python
 * tuple repr, e.g. ('crane', '_?__+').
 * @param word the guess
 * @param symbols the response from the puzzle
 */
public record Attempt(String word, String symbols) {

	/**
	 * @return more readable string
	 */
  @Override
  public String toString() {
    return "('" + word + "', '" + symbols + "')";
  }
}

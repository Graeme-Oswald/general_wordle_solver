package uk.co.tranning.wordle_solver;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Java port of the Python Utility.load_word_list helper.
 * The original utility module was not supplied, so this follows the
 * behaviour assumed for the port: keep ASCII-alphabetic words of the
 * requested length (lower-cased, de-duplicated, file order preserved),
 * optionally dropping words ending in "s" when plurals are excluded.
 */
public final class Utility {

    private Utility() {
    }

    public static List<String> loadWordList(String filePath, int wordLength, boolean excludePlurals) {
        List<String> words = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        try (BufferedReader reader = Files.newBufferedReader(Paths.get(filePath), StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                String word = line.strip().toLowerCase(Locale.ROOT);
                if (word.codePointCount(0, word.length()) != wordLength || !isAsciiLetters(word)) {
                    continue;
                }
                if (excludePlurals && word.endsWith("s")) {
                    continue;
                }
                if (seen.add(word)) {
                    words.add(word);
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return words;
    }

    private static boolean isAsciiLetters(String word) {
        for (int i = 0; i < word.length(); i++) {
            char c = word.charAt(i);
            if (c < 'a' || c > 'z') {
                return false;
            }
        }
        return true;
    }
}

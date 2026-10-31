package nightfall.analyzer;
import java.util.List;
/** Analyse-Ergebnis eines Chunks. Der Score ist ein Analysewert, keine Wahrscheinlichkeit. */
public record Result(int cx, int cz, int score, int conf, int evidence, int coverage, boolean decoy, List<String> why, double ax, double ay, double az, int tick) {}

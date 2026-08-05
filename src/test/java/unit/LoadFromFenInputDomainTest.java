package unit;

import static org.junit.jupiter.api.Assertions.*;

import com.github.bhlangonijr.chesslib.Board;
import com.github.bhlangonijr.chesslib.Side;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Input Domain Modeling test suite for {@code Board.loadFromFen(String fen)}.
 * Coverage criterion: Base Choice Coverage (BCC).
 *
 * <p>Base choice — the standard chess starting position:
 * <pre>rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1</pre>
 *
 * <p>Base blocks:
 * <ul>
 *   <li>C1 (field count)      – exactly 6</li>
 *   <li>C2 (piece placement)  – valid</li>
 *   <li>C3 (side to move)     – {@code w}</li>
 *   <li>C4 (castling)         – {@code KQkq}</li>
 *   <li>C5 (en passant)       – {@code -}</li>
 *   <li>C6 (halfmove clock)   – {@code 0}</li>
 *   <li>C7 (fullmove number)  – {@code 1}</li>
 * </ul>
 *
 * <p>Expected results are derived from the FEN specification, not from observed
 * behaviour. Tests annotated {@code @Tag("known-defect")} currently FAIL: they assert
 * what the specification requires, and chesslib 1.3.7 does not comply. Each one is
 * cross-referenced to a defect ID in {@code docs/input-domain-model-loadFromFen.md}.
 *
 * <p>These tagged tests are excluded from the default {@code mvn test} run so that
 * continuous integration reports regressions rather than already-known library defects.
 * To run the full suite including them:
 * <pre>mvn test -Dexcluded.test.groups=</pre>
 */
public class LoadFromFenInputDomainTest {

    private static final String BASE =
            "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1";

    /** A distinctive valid position used to detect whether a rejected load mutated the board. */
    private static final String SENTINEL = "4k3/8/8/8/8/8/8/4K3 w - - 0 1";

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    /**
     * Asserts that the board loads {@code fen} and that the result round-trips exactly,
     * which confirms every field was parsed and stored as supplied.
     */
    private static void assertLoadsAs(String fen, Side expectedSide) {
        Board board = new Board();
        board.loadFromFen(fen);

        assertEquals(fen, board.getFen(), "getFen() should round-trip the input FEN exactly");
        assertEquals(expectedSide, board.getSideToMove(), "Side to move parsed from field 2");
    }

    /**
     * Asserts that the method rejects invalid input, meaning it either throws or leaves
     * the previously loaded valid position untouched. Silently building a corrupt board
     * from malformed input is a failure.
     */
    private static void assertRejected(String fen, String reason) {
        Board board = new Board();
        board.loadFromFen(SENTINEL);
        String before = board.getFen();

        try {
            board.loadFromFen(fen);
        } catch (RuntimeException expected) {
            return; // rejected by signalling an error — acceptable
        }

        assertEquals(before, board.getFen(),
                reason + " — the FEN is invalid, so the board must not be modified");
    }

    // ------------------------------------------------------------------
    // F1 (base): all base blocks
    // ------------------------------------------------------------------
    @Test
    void f1_baseStandardStartingPosition() {
        assertLoadsAs(BASE, Side.WHITE);
    }

    // ------------------------------------------------------------------
    // C1 — number of space-separated fields
    // ------------------------------------------------------------------

    /** F2: C1 alt — fewer than 6 fields. Defect D-01. */
    @Test
    @Tag("known-defect")
    void f2_fewerThanSixFields() {
        assertRejected("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq",
                "A FEN record with only 4 fields is incomplete");
    }

    /** F3: C1 alt — more than 6 fields. Defect D-01. */
    @Test
    @Tag("known-defect")
    void f3_moreThanSixFields() {
        assertRejected(BASE + " extra",
                "A FEN record with a 7th field is malformed");
    }

    // ------------------------------------------------------------------
    // C2 — piece placement
    // ------------------------------------------------------------------

    /** F4: C2 alt — 7 ranks instead of 8. Defect D-02. */
    @Test
    @Tag("known-defect")
    void f4_rankCountNotEight() {
        assertRejected("rnbqkbnr/pppppppp/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1",
                "Piece placement must describe exactly 8 ranks");
    }

    /** F5: C2 alt — illegal piece character. Rejected correctly. */
    @Test
    void f5_illegalPieceCharacter() {
        assertRejected("rnbqkbnr/ppppXppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1",
                "'X' is not a valid piece letter");
    }

    /** F6: C2 alt — a rank that does not sum to 8 squares. Defect D-03. */
    @Test
    @Tag("known-defect")
    void f6_rankDoesNotSumToEight() {
        assertRejected("rnbqkbnr/ppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1",
                "Rank 7 describes only 7 of the 8 squares");
    }

    /** F7: C2 alt — no White king on the board. Defect D-04. */
    @Test
    @Tag("known-defect")
    void f7_kingMissing() {
        assertRejected("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQ1BNR w KQkq - 0 1",
                "A chess position must contain exactly one king per side");
    }

    // ------------------------------------------------------------------
    // C3 — side to move
    // ------------------------------------------------------------------

    /** F8: C3 alt — Black to move. */
    @Test
    void f8_blackToMove() {
        assertLoadsAs("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR b KQkq - 0 1", Side.BLACK);
    }

    /** F9: C3 alt — side token is neither w nor b. Defect D-05. */
    @Test
    @Tag("known-defect")
    void f9_invalidSideToMove() {
        assertRejected("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR x KQkq - 0 1",
                "'x' is not a valid side-to-move token");
    }

    // ------------------------------------------------------------------
    // C4 — castling availability
    // ------------------------------------------------------------------

    /** F10: C4 alt — partial castling rights. */
    @Test
    void f10_partialCastlingRights() {
        assertLoadsAs("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w Kq - 0 1", Side.WHITE);
    }

    /** F11: C4 alt — no castling rights. */
    @Test
    void f11_noCastlingRights() {
        assertLoadsAs("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w - - 0 1", Side.WHITE);
    }

    /** F12: C4 alt — illegal castling characters. Defect D-06. */
    @Test
    @Tag("known-defect")
    void f12_illegalCastlingCharacters() {
        assertRejected("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w XY - 0 1",
                "'XY' is not valid castling notation");
    }

    // ------------------------------------------------------------------
    // C5 — en passant target square
    // ------------------------------------------------------------------

    /** F13: C5 alt — a valid en passant target square. */
    @Test
    void f13_validEnPassantSquare() {
        assertLoadsAs("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq e6 0 1", Side.WHITE);
    }

    /** F14: C5 alt — square outside the board. Rejected correctly. */
    @Test
    void f14_enPassantSquareOffBoard() {
        assertRejected("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq e9 0 1",
                "e9 is not a square on a chessboard");
    }

    // ------------------------------------------------------------------
    // C6 — halfmove clock
    // ------------------------------------------------------------------

    /** F15: C6 alt — a positive halfmove clock. */
    @Test
    void f15_positiveHalfmoveClock() {
        assertLoadsAs("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 25 1", Side.WHITE);
    }

    /** F16: C6 alt — a negative halfmove clock. Defect D-07. */
    @Test
    @Tag("known-defect")
    void f16_negativeHalfmoveClock() {
        assertRejected("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - -1 1",
                "The halfmove clock counts moves and cannot be negative");
    }

    // ------------------------------------------------------------------
    // C7 — fullmove number
    // ------------------------------------------------------------------

    /** F17: C7 alt — fullmove number greater than 1. */
    @Test
    void f17_fullmoveNumberGreaterThanOne() {
        assertLoadsAs("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 40", Side.WHITE);
    }

    /** F18: C7 alt — non-numeric fullmove number. Rejected correctly. */
    @Test
    void f18_nonNumericFullmoveNumber() {
        assertRejected("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 abc",
                "'abc' is not an integer");
    }
}

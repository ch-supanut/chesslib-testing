package unit;

import static org.junit.jupiter.api.Assertions.*;

import com.github.bhlangonijr.chesslib.Board;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Graph-based test suite for {@code Board.doMove(String san)}.
 * Coverage criterion: Edge Coverage on the CFG of the method and the SAN decoding it
 * delegates to in {@code MoveList.decodeSan}.
 *
 * <p>CFG structure — 22 nodes, 29 edges, 8 decision points:
 * <pre>
 *   D1 (node 2)  : SAN is the null-move token Z0?
 *   D2 (node 6)  : SAN is a castling move?
 *   D3 (node 8)  : destination square parses?
 *   D4 (node 10) : SAN is 2 characters, i.e. a pawn move?
 *   D5 (node 12) : origin fragment is 1 to 3 characters?
 *   D6 (node 14) : several pieces of that type reach the destination?
 *   D7 (node 17) : origin square still NONE?
 *   D8 (node 19) : doMove(move, true) accepts the move?
 * </pre>
 *
 * <p>Edges covered by each test:
 * <pre>
 *   M1  : 1-2, 2-5, 5-6, 6-8, 8-10, 10-12, 12-14, 14-16, 16-17, 17-19, 19-21, 21-22
 *   M2  : 10-11, 11-17
 *   M3  : 6-7, 7-19
 *   M4  : 2-3, 3-4, 4-22
 *   M5  : 8-9, 9-22
 *   M6  : 12-13, 13-22
 *   M7  : 14-15, 15-17
 *   M8  : 17-18, 18-22
 *   M9  : 19-20, 20-22
 *   M10 : no new edges — retained as observation O-01
 * </pre>
 *
 * <p>Expected results come from the documented contract: the Javadoc promises a boolean
 * return and documents no exception, and the overload {@code doMove(Move, boolean)} returns
 * {@code false} rather than throwing. Tests tagged {@code known-defect} currently fail and
 * are cross-referenced to defect IDs in {@code docs/graph-based-testing-doMove.md}.
 */
public class DoMoveGraphTest {

    private static final String START =
            "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1";

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    /** Asserts the SAN is played and the resulting position is exactly {@code expectedFen}. */
    private static void assertPlays(String startFen, String san, String expectedFen) {
        Board board = new Board();
        board.loadFromFen(startFen);

        assertTrue(board.doMove(san), "doMove should report success for " + san);
        assertEquals(expectedFen, board.getFen(), "Position after " + san);
    }

    /**
     * Asserts the SAN is rejected the way the contract says it should be: by returning
     * {@code false} and leaving the board untouched.
     */
    private static void assertRejects(String startFen, String san, String reason) {
        Board board = new Board();
        board.loadFromFen(startFen);
        String before = board.getFen();

        boolean result;
        try {
            result = board.doMove(san);
        } catch (RuntimeException e) {
            throw new AssertionError(
                    "doMove(String) is documented to return a boolean, and doMove(Move, boolean) "
                            + "returns false in this situation, but the SAN overload threw "
                            + e.getClass().getSimpleName() + ": " + e.getMessage(), e);
        }

        assertFalse(result, reason);
        assertEquals(before, board.getFen(), "A rejected move must leave the board unchanged");
    }

    // ------------------------------------------------------------------
    // M1: ordinary piece move with a unique origin square
    // ------------------------------------------------------------------
    @Test
    void m1_ordinaryPieceMove() {
        assertPlays(START, "Nf3",
                "rnbqkbnr/pppppppp/8/8/8/5N2/PPPPPPPP/RNBQKB1R b KQkq - 1 1");
    }

    // ------------------------------------------------------------------
    // M2: pawn move path — also sets the en passant target square
    // ------------------------------------------------------------------
    @Test
    void m2_pawnMove() {
        assertPlays(START, "e4",
                "rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq e3 0 1");
    }

    // ------------------------------------------------------------------
    // M3: castling path
    // ------------------------------------------------------------------
    @Test
    void m3_kingsideCastling() {
        assertPlays("r3k2r/8/8/8/8/8/8/R3K2R w KQkq - 0 1", "O-O",
                "r3k2r/8/8/8/8/8/8/R4RK1 b kq - 1 1");
    }

    // ------------------------------------------------------------------
    // M4: the null-move token Z0. Defect D-09.
    // decodeSan returns early without adding a move, then removeLast() is called
    // on the empty list.
    // ------------------------------------------------------------------
    @Test
    @Tag("known-defect")
    void m4_nullMoveToken() {
        assertRejects(START, "Z0",
                "Z0 adds no move, so there is nothing to execute");
    }

    // ------------------------------------------------------------------
    // M5: destination square does not exist. Defect D-08.
    // ------------------------------------------------------------------
    @Test
    @Tag("known-defect")
    void m5_destinationSquareOffBoard() {
        assertRejects(START, "e9",
                "e9 is not a square on a chessboard");
    }

    // ------------------------------------------------------------------
    // M6: origin fragment too long. Defect D-08.
    // ------------------------------------------------------------------
    @Test
    @Tag("known-defect")
    void m6_originFragmentTooLong() {
        assertRejects(START, "Nabcd3",
                "'Nabc' is not a valid origin fragment");
    }

    // ------------------------------------------------------------------
    // M7: ambiguous SAN. Defect D-10.
    // White knights on b1 and f3 both reach d2, so the PGN standard requires
    // Nbd2 or Nfd2. chesslib plays the b1 knight instead of rejecting the move.
    // ------------------------------------------------------------------
    @Test
    @Tag("known-defect")
    void m7_ambiguousSanIsRejected() {
        assertRejects("4k3/8/8/8/8/5N2/8/1N2K3 w - - 0 1", "Nd2",
                "Two knights can reach d2, so bare 'Nd2' is ambiguous and must be rejected");
    }

    // ------------------------------------------------------------------
    // M8: no piece of that type can reach the destination. Defect D-08.
    // ------------------------------------------------------------------
    @Test
    @Tag("known-defect")
    void m8_noPieceCanReachDestination() {
        assertRejects("4k3/8/8/8/8/8/8/4K3 w - - 0 1", "Ne5",
                "There is no knight on the board");
    }

    // ------------------------------------------------------------------
    // M9: unique origin, rejected at the final validation step. Defect D-08.
    // The bishop on b2 is pinned against the king on a1 by the bishop on h8.
    // ------------------------------------------------------------------
    @Test
    @Tag("known-defect")
    void m9_moveRejectedByValidation() {
        assertRejects("7b/8/8/8/8/8/1B6/K7 w - - 0 1", "Ba3",
                "Moving the pinned bishop would leave the White king in check");
    }

    // ------------------------------------------------------------------
    // M10: observation O-01 — documented behaviour, not a defect.
    //
    // The pawn advances from e2 to e5, three squares in a single move, and doMove
    // reports success. The Javadoc states that the method validates the resulting
    // position rather than the legality of the move itself, so this test asserts the
    // documented behaviour. It is kept as evidence for the API-design recommendation
    // in the final report, and it will fail if the library ever tightens validation.
    // ------------------------------------------------------------------
    @Test
    void m10_threeSquarePawnAdvanceIsAccepted() {
        assertPlays(START, "e5",
                "rnbqkbnr/pppppppp/8/4P3/8/8/PPPP1PPP/RNBQKBNR b KQkq - 0 1");
    }
}

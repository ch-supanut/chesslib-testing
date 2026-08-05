package unit;

import static org.junit.jupiter.api.Assertions.*;

import com.github.bhlangonijr.chesslib.Board;
import com.github.bhlangonijr.chesslib.Piece;
import com.github.bhlangonijr.chesslib.Square;
import com.github.bhlangonijr.chesslib.move.Move;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Regression suite distilled from the exploratory sessions documented in
 * {@code docs/exploratory-testing-doMove-undoMove.md}.
 *
 * <p>The exploratory tool ({@code UndoMoveExplorationRunner}) searched broadly and reported;
 * this suite pins down what that search established, so a future version of the library
 * cannot quietly break it. U1 to U4 pass. U5 and U6 assert the documented behaviour of
 * {@code undoMove()} and currently fail, which is defect D-11.
 *
 * <p>The oracle throughout is the round-trip invariant
 * {@code fen(P) == fen(undoMove(doMove(P, m)))}.
 */
public class UndoMoveStateConsistencyTest {

    private static final long SEED = 20260804L;

    // ------------------------------------------------------------------
    // U1: the invariant holds for every legal move across random games
    // ------------------------------------------------------------------
    @Test
    void u1_everyLegalMoveRoundTripsInRandomGames() {
        Random random = new Random(SEED);
        int movesChecked = 0;

        for (int game = 0; game < 10; game++) {
            Board board = new Board();

            for (int ply = 0; ply < 40; ply++) {
                List<Move> legal = board.legalMoves();
                if (legal.isEmpty() || board.isDraw()) {
                    break;
                }

                String before = board.getFen();
                for (Move candidate : legal) {
                    board.doMove(candidate);
                    board.undoMove();
                    movesChecked++;
                    assertEquals(before, board.getFen(),
                            "Board not restored after playing and undoing " + candidate);
                }

                board.doMove(legal.get(random.nextInt(legal.size())));
            }
        }

        assertTrue(movesChecked > 1000,
                "The search should have covered a meaningful number of moves, got " + movesChecked);
    }

    // ------------------------------------------------------------------
    // U2: a whole game unwinds back to the starting position
    // ------------------------------------------------------------------
    @Test
    void u2_wholeGameUnwindsToTheStart() {
        Random random = new Random(SEED);
        Board board = new Board();
        List<String> fenHistory = new ArrayList<>();

        for (int ply = 0; ply < 60; ply++) {
            List<Move> legal = board.legalMoves();
            if (legal.isEmpty() || board.isDraw()) {
                break;
            }
            fenHistory.add(board.getFen());
            board.doMove(legal.get(random.nextInt(legal.size())));
        }

        for (int ply = fenHistory.size() - 1; ply >= 0; ply--) {
            board.undoMove();
            assertEquals(fenHistory.get(ply), board.getFen(),
                    "Board not restored when unwinding to ply " + ply);
        }
    }

    // ------------------------------------------------------------------
    // U3: special moves, where state lives outside the piece placement
    // ------------------------------------------------------------------
    @Test
    void u3_specialMovesRoundTrip() {
        assertRoundTrips("8/P7/8/8/8/8/8/4K2k w - - 0 1",
                new Move(Square.A7, Square.A8, Piece.WHITE_QUEEN), "promotion to queen");

        assertRoundTrips("8/P7/8/8/8/8/8/4K2k w - - 0 1",
                new Move(Square.A7, Square.A8, Piece.WHITE_KNIGHT), "promotion to knight");

        assertRoundTrips("1n6/P7/8/8/8/8/8/4K2k w - - 0 1",
                new Move(Square.A7, Square.B8, Piece.WHITE_QUEEN), "promotion with capture");

        assertRoundTrips("rnbqkbnr/ppp1pppp/8/3pP3/8/8/PPPP1PPP/RNBQKBNR w KQkq d6 0 3",
                new Move(Square.E5, Square.D6), "en passant capture");

        assertRoundTrips("r3k2r/8/8/8/8/8/8/R3K2R w KQkq - 0 1",
                new Move(Square.E1, Square.G1), "kingside castling");

        assertRoundTrips("r3k2r/8/8/8/8/8/8/R3K2R w KQkq - 0 1",
                new Move(Square.E1, Square.C1), "queenside castling");

        assertRoundTrips("r3k2r/8/8/8/8/8/8/R3K2R w KQkq - 0 1",
                new Move(Square.A1, Square.B1), "rook move that forfeits castling rights");

        assertRoundTrips("4k3/8/8/3p4/8/8/8/3Q3K w - - 17 30",
                new Move(Square.D1, Square.D5), "capture that resets the halfmove clock");
    }

    // ------------------------------------------------------------------
    // U4: position history is rolled back, not only the piece placement
    //
    // A FEN does not encode move history, so this is the one property in the
    // suite that a FEN comparison cannot check.
    // ------------------------------------------------------------------
    @Test
    void u4_repetitionHistoryIsRolledBack() {
        Board board = new Board();
        Move[] shuffle = {
                new Move(Square.G1, Square.F3), new Move(Square.G8, Square.F6),
                new Move(Square.F3, Square.G1), new Move(Square.F6, Square.G8),
                new Move(Square.G1, Square.F3), new Move(Square.G8, Square.F6),
                new Move(Square.F3, Square.G1), new Move(Square.F6, Square.G8),
        };

        String startingFen = board.getFen();
        for (Move move : shuffle) {
            board.doMove(move);
        }
        assertTrue(board.isDraw(),
                "The starting position has now occurred three times, so this is a draw");

        for (int i = 0; i < shuffle.length; i++) {
            board.undoMove();
        }

        assertEquals(startingFen, board.getFen(), "Board should be back at the start");
        assertFalse(board.isRepetition(),
                "Undoing the shuffle must also remove those positions from the history");
        assertFalse(board.isDraw(),
                "The starting position is not a draw once the repetitions are undone");
    }

    // ------------------------------------------------------------------
    // U5: undo with nothing to undo. Defect D-11.
    //
    // Javadoc: "Reverts the latest move played on the board and returns it.
    //           If no moves were previously executed, it returns null."
    // ------------------------------------------------------------------
    @Test
    @Tag("known-defect")
    void u5_undoOnAFreshBoardReturnsNull() {
        Board board = new Board();

        assertNull(board.undoMove(),
                "undoMove() is documented to return null when no moves have been played");
    }

    // ------------------------------------------------------------------
    // U6: undo past a position that was loaded rather than played. Defect D-11.
    //
    // This is the realistic form of D-11: an application loads a position from
    // FEN and offers an undo button.
    // ------------------------------------------------------------------
    @Test
    @Tag("known-defect")
    void u6_undoPastALoadedPositionReturnsNull() {
        Board board = new Board();
        board.loadFromFen("r1bqkbnr/pppp1ppp/2n5/4p3/4P3/5N2/PPPP1PPP/RNBQKB1R w KQkq - 3 3");
        String loaded = board.getFen();

        board.doMove(new Move(Square.F1, Square.B5));
        board.undoMove();
        assertEquals(loaded, board.getFen(), "The played move should undo cleanly");

        assertNull(board.undoMove(),
                "There is nothing before the loaded position, so undoMove() should return null");
    }

    // ------------------------------------------------------------------
    // Helper
    // ------------------------------------------------------------------

    private static void assertRoundTrips(String fen, Move move, String label) {
        Board board = new Board();
        board.loadFromFen(fen);
        String before = board.getFen();

        assertTrue(board.doMove(move), label + ": the move should be accepted");
        board.undoMove();

        assertEquals(before, board.getFen(), label + ": board not restored");
    }
}

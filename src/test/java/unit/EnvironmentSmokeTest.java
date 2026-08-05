package unit;

import static org.junit.jupiter.api.Assertions.*;

import com.github.bhlangonijr.chesslib.Board;
import com.github.bhlangonijr.chesslib.Side;
import org.junit.jupiter.api.Test;

/**
 * Checks that chesslib is on the classpath and works, both on a developer machine
 * and on the CI runner.
 *
 * <p>These are not test cases for any of the testing techniques in the project.
 * They only confirm that the build and the dependency are set up correctly.
 */
public class EnvironmentSmokeTest {

    @Test
    void startingPositionHasTwentyLegalMoves() {
        Board board = new Board();

        assertEquals(Side.WHITE, board.getSideToMove(),
                "White moves first in the starting position");
        assertEquals(20, board.legalMoves().size(),
                "The starting position has 20 legal moves (16 pawn moves + 4 knight moves)");
    }

    @Test
    void loadFromFenChangesSideToMove() {
        Board board = new Board();
        board.loadFromFen("k7/8/1Q6/8/8/8/8/4K3 b - - 0 1");

        assertEquals(Side.BLACK, board.getSideToMove(),
                "The FEN says 'b', so Black is to move");
    }
}

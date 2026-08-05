package acceptance;

import static org.junit.jupiter.api.Assertions.*;

import com.github.bhlangonijr.chesslib.Board;
import io.cucumber.java.en.*;

/**
 * Step definitions for Checkmate.feature.
 *
 * <p>Method under test: {@code Board.isMated()}
 * <p>Owner: Supanut
 */
public class CheckmateSteps {

    private Board board;
    private boolean isMated;

    @Given("a chess board loaded from FEN {string}")
    public void a_chess_board_loaded_from_fen(String fen) {
        board = new Board();
        board.loadFromFen(fen);
    }

    @When("the checkmate condition is evaluated")
    public void the_checkmate_condition_is_evaluated() {
        isMated = board.isMated();
    }

    @Then("the game should be identified as checkmate")
    public void the_game_should_be_identified_as_checkmate() {
        assertTrue(isMated, "Expected the position to be evaluated as checkmate");
    }

    @Then("the game should not be identified as checkmate")
    public void the_game_should_not_be_identified_as_checkmate() {
        assertFalse(isMated, "Expected the position to NOT be evaluated as checkmate");
    }
}

package unit;

import static org.junit.jupiter.api.Assertions.*;

import com.github.bhlangonijr.chesslib.Board;
import com.github.bhlangonijr.chesslib.Side;
import org.junit.jupiter.api.Test;

/**
 * Smoke test ยืนยันว่า chesslib ถูกดึงมาได้และทำงานได้ทั้งในเครื่องและบน CI
 * ไม่ใช่ test case ของเทคนิคใดเทคนิคหนึ่ง เป็นแค่ตัวเช็ก environment
 */
public class EnvironmentSmokeTest {

    @Test
    void startingPositionHasTwentyLegalMoves() {
        Board board = new Board();

        assertEquals(Side.WHITE, board.getSideToMove(),
                "ตำแหน่งเริ่มเกม ฝ่ายขาวต้องเป็นฝ่ายเดิน");
        assertEquals(20, board.legalMoves().size(),
                "ตำแหน่งเริ่มเกมมีตาเดินที่ถูกกฎ 20 ตา (เบี้ย 16 + ม้า 4)");
    }

    @Test
    void loadFromFenChangesSideToMove() {
        Board board = new Board();
        board.loadFromFen("k7/8/1Q6/8/8/8/8/4K3 b - - 0 1");

        assertEquals(Side.BLACK, board.getSideToMove(),
                "FEN ระบุ 'b' ฝ่ายดำต้องเป็นฝ่ายเดิน");
    }
}

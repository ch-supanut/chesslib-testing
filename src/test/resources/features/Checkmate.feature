Feature: Checkmate Detection

  Scenario: Game ends in checkmate
    Given a chess board loaded from FEN "rnb1kbnr/pppp1ppp/8/4p3/6Pq/5P2/PPPPP2P/RNBQKBNR w KQkq - 1 3"
    When the checkmate condition is evaluated
    Then the game should be identified as checkmate

  Scenario: Active game is not checkmate
    Given a chess board loaded from FEN "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"
    When the checkmate condition is evaluated
    Then the game should not be identified as checkmate

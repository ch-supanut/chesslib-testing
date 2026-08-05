# Progress from Exploratory Testing — `doMove` / `undoMove` State Consistency

**Tester:** Supanut Chindawan
**Software under test:** chesslib v1.3.7
**Approach:** Session-Based Test Management (SBTM)

> Mathew's exploratory work covered `isStaleMate` and `loadFromFen`. This area —
> whether the board survives a move being taken back — does not overlap with it.

---

## Why This Area

`undoMove` has to restore more than the two squares a move touched. Castling rights, the
en passant target, the halfmove clock, the fullmove number and whose turn it is all live
outside the piece placement, and every one of them has to be rolled back too. State that is
saved and restored in one place but changed in another is a classic source of subtle bugs,
and the failure mode is quiet: the board looks plausible and only becomes wrong several
moves later.

It is also an area that suits exploration rather than a fixed test set, because there is no
practical way to enumerate the positions worth trying.

## Oracle

The sessions use a **round-trip invariant** rather than hand-written expected values. For any
position P and any legal move m:

```
fen(P)  ==  fen(undoMove(doMove(P, m)))
```

This oracle needs no knowledge of the correct answer, so it can be applied to tens of
thousands of positions that nobody has to check by hand. That is what makes wide exploration
affordable here.

## Tooling

`src/test/java/unit/UndoMoveExplorationRunner.java` supports the sessions. It plays random
legal games from a **fixed random seed**, so every finding is reproducible exactly. It makes
no assertions — it reports, and the tester decides what is worth writing up.

---

## Session 1

**Charter:** Explore `doMove`/`undoMove` across randomly generated games to discover positions
where taking a move back does not restore the board.

**Tour:** Landmark Tour — visit as many distinct positions as possible rather than studying
any one of them closely.

**Areas:** `Board.doMove(Move)`, `Board.undoMove()`, `Board.getFen()`

**Timebox:** 60 minutes

**Setup:** run `tour1_everyLegalMoveRoundTrips`, then `tour2_undoEntireGame`.

| Item | Notes |
|---|---|
| Date | 4 August 2026 |
| Duration | 60 minutes |
| Positions visited | 3,159 |
| Moves checked | 98,465 |
| Games unwound to the start | 40 |
| Findings | None |

### Session 1 Notes

Tour 1 applied the round-trip invariant to every legal move at every position of 40 randomly
generated games: 98,465 do/undo pairs across 3,159 distinct positions. Tour 2 played each of
the 40 games to completion and then unwound it move by move, comparing the FEN at every step
back. Neither tour produced a single violation.

At this scale a naive state-restoration bug would almost certainly have shown up, so the
conclusion is that the ordinary path through `doMove`/`undoMove` restores the board reliably.

The result also exposed a weakness in the oracle, which shaped the next session. **A FEN
string does not encode move history.** Threefold repetition and the position history behind
it are therefore invisible to a FEN comparison, so tours 1 and 2 could not have detected a
bug in how `undoMove` rolls history back even if one existed. Session 2 was extended to cover
that gap.

---

## Session 2

**Charter:** Attack the special moves, where state lives outside the piece placement —
castling rights, en passant targets, promotions, and the halfmove clock.

**Tour:** Bad-Neighbourhood Tour — go straight to the parts of the code known to be
error-prone rather than sampling evenly.

**Areas:** castling, en passant, promotion, move counters

**Timebox:** 60 minutes

**Setup:** run `tour3_specialMoves`, then `tour4_beyondTheFenOracle`.

Tour 4 was added after Session 1 to cover what the FEN oracle cannot see:

- **4a** — `undoMove` on a board with no moves played
- **4b** — one more `undoMove` than there are moves played
- **4c** — repetition history: shuffle the knights so the starting position occurs three times, confirm `isDraw()` reports a draw, undo all eight moves, and check that it stops doing so
- **4d** — whether `doMove(String)` leaves the board in the same state as `doMove(Move)` after an undo
- **4e** — undoing past a position that was loaded with `loadFromFen` rather than reached by playing

Positions still worth trying by hand:

- promotion that also gives check, then undone
- en passant capture undone, then the same capture played again
- castling undone, then castling attempted a second time
- a rook captured on its home square — do the opponent's castling rights come back on undo?

| Item | Notes |
|---|---|
| Date | 4 August 2026 |
| Duration | 60 minutes |
| Special-move positions checked | 10 |
| Boundary and history probes | 5 |
| Findings | 1 defect (D-11) |

### Session 2 Notes

Tour 3 checked ten hand-picked positions where state lives outside the piece placement:
promotion to each of the four piece types, promotion by capture, en passant capture, castling
on both wings, a rook move that forfeits castling rights, a king move that forfeits both, a
double pawn push that sets an en passant target, and a capture that resets the halfmove clock.
All ten round-tripped exactly.

Tour 4 found the one defect of these sessions, and it turned up in three of the five probes.

**4c cleared the concern that prompted the tour.** After the eight-move knight shuffle
`isDraw()` correctly reported a draw by threefold repetition; after all eight moves were
taken back it reported `false` again, and `isRepetition()` agreed. The position history is
rolled back properly, which the FEN oracle could never have told us.

**4d** confirmed that `doMove(String)` and `doMove(Move)` leave identical state behind after
an undo, so the SAN defects recorded as D-08 and D-10 do not leak into the undo path.

**4a, 4b and 4e all failed the same way**: calling `undoMove()` when there is nothing left to
undo throws `IndexOutOfBoundsException: Index: -1, Size: 0`.

---

## Findings

| ID | Session | Description | Reproduction | Severity |
|---|---|---|---|---|
| D-11 | 2 (probes 4a, 4b, 4e) | `undoMove()` throws `IndexOutOfBoundsException` when there are no moves left to undo, instead of returning `null` as its Javadoc promises | `new Board().undoMove()` | **High** |

### D-11 in detail

The Javadoc is explicit:

> Reverts the latest move played on the board and returns it. If no moves were previously
> executed, **it returns null**.
>
> `@return` the reverted move, **or null if no previous moves were played**

The implementation contradicts it on its first line:

```java
public Move undoMove() {
    Move move = null;
    final MoveBackup b = backup.remove(backup.size() - 1);   // size() is 0 → remove(-1)
    // history bookkeeping omitted
    if (b != null) {          // this guard can never see a null
        move = b.getMove();
        b.restore(this);
    }
    return move;
}
```

The `if (b != null)` check and the `Move move = null` initialiser show the empty case was
meant to be handled. The guard is simply placed one line too late: `backup.remove(-1)` throws
before it is ever reached, so the defensive code is unreachable.

Probe 4e is the realistic version of this. A position loaded with `loadFromFen` has an empty
backup, so an application that loads a position and offers an undo button crashes the first
time the user presses it — no illegal input required, and nothing in the documentation warns
against it.

### Root cause shared with D-09

D-11 has the same shape as D-09, found during graph-based testing of `doMove(String)`: an
internal list is emptied of its last element without first checking that it has one.

| Defect | Method | Failing expression |
|---|---|---|
| D-09 | `Board.doMove(String)` | `moves.removeLast()` on an empty `MoveList` |
| D-11 | `Board.undoMove()` | `backup.remove(backup.size() - 1)` on an empty backup |

Two unrelated public methods failing the same way suggests the pattern is worth auditing
wherever else the library removes the last element of an internal collection, rather than
patching these two call sites individually. This belongs in Section 8 of the final report.

## Summary

Two sessions, four tours, roughly 98,500 do/undo pairs across 3,159 positions, 40 complete
games unwound to their starting position, 10 hand-picked special-move positions, and 5
boundary probes.

The ordinary path is solid: no position was ever restored incorrectly, and history-dependent
state came back correctly too. Every failure found sat at the boundary, where there was
nothing left to undo — and the defect there is a documented-behaviour violation with an
unreachable guard behind it, not an oversight about an undefined case.

The sessions also produced a methodological result worth reporting: the FEN-based round-trip
oracle used in Sessions 1 and 2 is blind to move history, and Tour 4 had to be designed
specifically to see past it. Reporting "98,465 checks, no violations" without that caveat
would have overstated what the evidence supports.

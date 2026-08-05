# Progress from Input Domain Modeling — `Board.loadFromFen(String fen)`

**Tester:** Supanut Chindawan
**Software under test:** chesslib v1.3.7, `com.github.bhlangonijr.chesslib.Board`

> Structured to match Section 1 of the Group 8 Progress Report so both input-domain
> subsections read consistently in the final report.

---

## Selected Function

```java
public void loadFromFen(String fen)
```

This method parses a chess position expressed in **Forsyth–Edwards Notation (FEN)** and
mutates the `Board` instance to reflect it. It was selected because it is the primary entry
point through which external data enters the library — every test in this project, including
Mathew's `isMoveLegal` and `isDraw` suites, depends on it to construct positions. It also has
an unusually well-specified input format: the FEN standard defines exactly six fields with
independent syntactic rules, which decomposes cleanly into characteristics. Finally, it is a
parser of untrusted string input, which is historically where input-validation defects
concentrate.

## Specification Reference

A valid FEN record consists of **exactly six space-separated fields**:

| # | Field | Valid content |
|---|---|---|
| 1 | Piece placement | 8 ranks separated by `/`, each rank summing to 8 squares, using `pnbrqkPNBRQK` and digits `1`–`8`; exactly one king per side |
| 2 | Side to move | `w` or `b` |
| 3 | Castling availability | any combination of `KQkq`, or `-` |
| 4 | En passant target square | a square in algebraic notation on rank 3 or 6, or `-` |
| 5 | Halfmove clock | non-negative integer |
| 6 | Fullmove number | positive integer starting at 1 |

Expected results in this document are derived from this specification **before** any code is
executed, so that the tests measure conformance to the standard rather than describing
whatever the implementation happens to do.

## Input Variables

**Direct parameters:**

| Variable | Type | Description |
|---|---|---|
| `fen` | `String` | Position record in Forsyth–Edwards Notation |

**State variables:**

| Variable | Type | Description |
|---|---|---|
| prior board state | `Board` | The position held by the instance before the call |

The prior board state is held constant at a freshly constructed `Board` for every test in this
suite, so that any observed behaviour is attributable to the `fen` argument alone. Whether a
failed load leaves a previously valid board in a corrupt intermediate state is examined
separately under exploratory testing.

## Characteristics of Input Variables

| # | Field | Characteristic |
|---|---|---|
| C1 | whole string | Number of space-separated fields |
| C2 | field 1 | Structural validity of the piece placement |
| C3 | field 2 | Side-to-move token |
| C4 | field 3 | Castling availability token |
| C5 | field 4 | En passant target token |
| C6 | field 5 | Halfmove clock value |
| C7 | field 6 | Fullmove number value |

## Blocks for Each Characteristic

| Characteristic | Block 1 | Block 2 | Block 3 | Block 4 | Block 5 |
|---|---|---|---|---|---|
| C1 — field count | Exactly 6 | Fewer than 6 | More than 6 | — | — |
| C2 — piece placement | Valid | Rank count ≠ 8 | Illegal piece character | Rank does not sum to 8 | King missing |
| C3 — side to move | `w` | `b` | Not `w` or `b` | — | — |
| C4 — castling | Full `KQkq` | Partial | `-` (none) | Illegal character | — |
| C5 — en passant | `-` (none) | Valid square | Square off the board | — | — |
| C6 — halfmove clock | `0` | Positive integer | Negative or non-numeric | — | — |
| C7 — fullmove number | `1` | Greater than 1 | Non-numeric | — | — |

## Values for Each Block

| Block | Representative value |
|---|---|
| C1 — exactly 6 | the six fields of the standard starting position |
| C1 — fewer than 6 | trailing two fields removed (4 fields) |
| C1 — more than 6 | ` extra` appended (7 fields) |
| C2 — valid | `rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR` |
| C2 — rank count ≠ 8 | same placement with one rank deleted (7 ranks) |
| C2 — illegal character | `X` substituted for a pawn: `rnbqkbnr/ppppXppp/...` |
| C2 — rank sum ≠ 8 | rank 7 given only 7 pawns: `rnbqkbnr/ppppppp/...` |
| C2 — king missing | White king replaced by an empty square: `.../RNBQ1BNR` |
| C3 — `w` | `w` |
| C3 — `b` | `b` |
| C3 — invalid | `x` |
| C4 — full | `KQkq` |
| C4 — partial | `Kq` |
| C4 — none | `-` |
| C4 — illegal | `XY` |
| C5 — none | `-` |
| C5 — valid square | `e6` |
| C5 — off board | `e9` |
| C6 — zero | `0` |
| C6 — positive | `25` |
| C6 — invalid | `-1` |
| C7 — one | `1` |
| C7 — greater than 1 | `40` |
| C7 — invalid | `abc` |

## Coverage Criteria: Base Choice Coverage (BCC)

Base Choice Coverage was selected for the same reasons it was chosen for `isMoveLegal`, which
also keeps the two input-domain subsections of the final report consistent. The seven
characteristics are syntactically independent, so All Combinations Coverage would require
3 × 5 × 3 × 4 × 3 × 3 × 3 = **4,860** tests for very little added fault-detection power. BCC
covers every block at least once with 18 tests, and because exactly one characteristic deviates
from the base in each test, any failure points directly at the field responsible.

**Base choice** — the standard chess starting position, the canonical and most frequently
exercised input to this method:

```
rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1
```

Base blocks: C1b1 (6 fields), C2b1 (valid placement), C3b1 (`w`), C4b1 (`KQkq`), C5b1 (`-`),
C6b1 (`0`), C7b1 (`1`).

**Test set size:** 1 base test + one test per non-base block

```
1 + (2 + 4 + 2 + 3 + 2 + 2 + 2) = 18 test cases
```

## Test Set (BCC)

| Test # | Variation | Input FEN | Expected |
|---|---|---|---|
| F1 (base) | All base values | `rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1` | Loads; side to move = WHITE; `getFen()` round-trips |
| F2 | C1 alt: fewer than 6 fields | `rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq` | Rejected — only 4 fields |
| F3 | C1 alt: more than 6 fields | base + ` extra` | Rejected — 7 fields |
| F4 | C2 alt: rank count ≠ 8 | `rnbqkbnr/pppppppp/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1` | Rejected — only 7 ranks |
| F5 | C2 alt: illegal piece character | `rnbqkbnr/ppppXppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1` | Rejected — `X` is not a piece |
| F6 | C2 alt: rank does not sum to 8 | `rnbqkbnr/ppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1` | Rejected — rank 7 sums to 7 |
| F7 | C2 alt: king missing | `rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQ1BNR w KQkq - 0 1` | Rejected — no White king |
| F8 | C3 alt: `b` | `rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR b KQkq - 0 1` | Loads; side to move = BLACK |
| F9 | C3 alt: invalid side | `rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR x KQkq - 0 1` | Rejected — `x` is not a side |
| F10 | C4 alt: partial rights | `rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w Kq - 0 1` | Loads; only White kingside and Black queenside retained |
| F11 | C4 alt: no rights | `rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w - - 0 1` | Loads; no castling rights |
| F12 | C4 alt: illegal character | `rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w XY - 0 1` | Rejected — `XY` is not castling notation |
| F13 | C5 alt: valid square | `rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq e6 0 1` | Loads; en passant target = e6 |
| F14 | C5 alt: square off board | `rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq e9 0 1` | Rejected — e9 is off the board |
| F15 | C6 alt: positive clock | `rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 25 1` | Loads; halfmove clock = 25 |
| F16 | C6 alt: negative clock | `rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - -1 1` | Rejected — clock cannot be negative |
| F17 | C7 alt: greater than 1 | `rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 40` | Loads; fullmove number = 40 |
| F18 | C7 alt: non-numeric | `rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 abc` | Rejected — not an integer |

**"Rejected"** means the method signals the error to the caller, either by throwing an
exception or by leaving the board in its previous valid state. Silently accepting malformed
input and constructing a corrupt position is treated as a defect, because the caller then has
no way to know that the position it is reasoning about is meaningless.

## Test Execution Results

All 18 test cases were executed using JUnit 5 against chesslib v1.3.7.

| Result | Count |
|---|---|
| Passed | 10 |
| Failed | 8 |

### Observed Behaviour

| Test # | Expected | Actual | Verdict |
|---|---|---|---|
| F1 | Loads; round-trips; side = WHITE | Loads; round-trips exactly; side = WHITE | Pass |
| F2 | Rejected — only 4 fields | Accepted; missing fields defaulted, producing `... - 0 0` (fullmove 0 is itself invalid) | **Fail — D-01** |
| F3 | Rejected — 7 fields | Accepted; 7th field silently discarded | **Fail — D-01** |
| F4 | Rejected — only 7 ranks | Accepted; an empty rank was appended as `/8`, shifting the entire position down one rank | **Fail — D-02** |
| F5 | Rejected — `X` is not a piece | `IllegalArgumentException: Unknown piece 'X'` | Pass |
| F6 | Rejected — rank 7 sums to 7 | Accepted; rank silently padded to `ppppppp1` | **Fail — D-03** |
| F7 | Rejected — no White king | Accepted; kingless position stored verbatim | **Fail — D-04** |
| F8 | Loads; side = BLACK | Loads; side = BLACK | Pass |
| F9 | Rejected — `x` is not a side | Accepted; **side silently set to BLACK** and `getFen()` reports `b` | **Fail — D-05** |
| F10 | Loads; `Kq` retained | Loads; round-trips `w Kq` | Pass |
| F11 | Loads; no castling rights | Loads; round-trips `w -` | Pass |
| F12 | Rejected — `XY` is invalid | Accepted; castling rights silently set to none | **Fail — D-06** |
| F13 | Loads; en passant = e6 | Loads; round-trips `KQkq e6` | Pass |
| F14 | Rejected — e9 is off the board | `IllegalArgumentException: No enum constant ...Square.E9` | Pass |
| F15 | Loads; halfmove clock = 25 | Loads; round-trips `- 25 1` | Pass |
| F16 | Rejected — clock cannot be negative | Accepted; `-1` stored as the halfmove clock | **Fail — D-07** |
| F17 | Loads; fullmove number = 40 | Loads; round-trips `- 0 40` | Pass |
| F18 | Rejected — `abc` is not an integer | `NumberFormatException: For input string: "abc"` | Pass |

## Defects Identified

| ID | Tests | Description | Severity |
|---|---|---|---|
| D-01 | F2, F3 | The field count is never checked. A record with fewer than six fields is accepted and the missing fields are silently defaulted; F2 yields a fullmove number of 0, which the FEN standard does not permit. A seventh field is silently discarded. | Medium |
| D-02 | F4 | The number of ranks is not validated. Supplying 7 ranks causes an empty rank to be appended at the bottom, which shifts every piece down one rank. The caller receives a completely different position from the one requested, with no error. | **High** |
| D-03 | F6 | A rank that does not account for all 8 squares is silently padded with an empty square. The board is populated with a position the caller never described. | Medium |
| D-04 | F7 | A position with no king is accepted. Every downstream predicate that reasons about check — `isMated`, `isStaleMate`, `isDraw`, `legalMoves` — then operates on a position that cannot occur in chess, with undefined results. | **High** |
| D-05 | F9 | The side-to-move token is not validated. Any value other than `w` is treated as Black, so a typo silently inverts whose turn it is and `getFen()` then reports the corrupted value as if it were the input. | **High** |
| D-06 | F12 | Unrecognised castling characters are silently interpreted as "no castling rights" rather than rejected. | Medium |
| D-07 | F16 | A negative halfmove clock is accepted and stored. The 50-move-rule check (`halfMoveClock >= 100`) can then never fire for that board. | Low |

### Root Cause Pattern

The three inputs that were correctly rejected — F5, F14 and F18 — are rejected only as a
side effect of Java's own parsing machinery: an unknown piece letter fails a `Piece` lookup,
`e9` fails a `Square` enum lookup, and `abc` fails `Integer.parseInt`. In every case where
the malformed input still happens to be parseable by those mechanisms, it is accepted.

`loadFromFen` therefore performs **no validation of its own**. Errors surface accidentally,
where a Java primitive refuses the value, and never deliberately, where the FEN standard is
violated. This explains the pattern of results precisely: every defect above is an input that
is syntactically parseable but semantically invalid.

### Impact on This Project

`loadFromFen` is the entry point used to construct positions for essentially every test in
this project, including the `isMoveLegal` and `isDraw` suites. Because malformed FEN strings
are accepted silently, a typo in any test fixture produces a valid-looking board that
represents a different position than intended — and the affected test may still pass. This
makes the defects above a threat to the validity of the test suite itself, not only to
library users, and it is the main argument for the recommendations in Section 8 of the
final report.

### Note on Continuous Integration

The eight failing tests are tagged `known-defect` and excluded from the default
`mvn test` run, so the CI pipeline reports regressions rather than re-reporting
already-documented library defects on every commit. They are ordinary failing tests and
can be executed at any time with:

```bash
mvn test -Dexcluded.test.groups=
```

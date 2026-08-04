# chesslib-testing — SWENG 881 Group Project

Software quality evaluation of [**chesslib**](https://github.com/bhlangonijr/chesslib), an open-source Java chess library, using the testing techniques covered in SWENG 881.

**Team:** Supanut Chindawan, Mathew

---

## Software Under Test

| Item | Value |
|---|---|
| Library | `com.github.bhlangonijr:chesslib` |
| Version | 1.3.7 |
| Source | https://github.com/bhlangonijr/chesslib |
| Distribution | JitPack (not on Maven Central) |

---

## Test Coverage Matrix

| Technique | Method under test | Owner | Location |
|---|---|---|---|
| Input domain partitioning | `Board.isMoveLegal` | Mathew | `src/test/java/unit/` |
| Input domain partitioning | `Board.loadFromFen` | Supanut | `src/test/java/unit/` |
| Graph-based testing | `Board.isDraw` | Mathew | `src/test/java/unit/` |
| Graph-based testing | `Board.doMove(String)` | Supanut | `src/test/java/unit/` |
| Exploratory testing | `isStaleMate`, `loadFromFen` | Mathew | see final report |
| Exploratory testing | `doMove` / `undoMove` state consistency | Supanut | see final report |
| Acceptance (Cucumber BDD) | `Board.isStaleMate` | Mathew | individual submission |
| Acceptance (Cucumber BDD) | `Board.isMated` | Supanut | `src/test/java/acceptance/` |

This table doubles as the source for **Section 5.3 Traceability Matrix** in the final report.

---

## Project Structure

```
chesslib-testing/
├── pom.xml                              Maven build + dependencies
├── .github/workflows/maven.yml          CI pipeline
└── src/test/
    ├── java/
    │   ├── unit/                        JUnit 5 tests (input domain, graph-based)
    │   └── acceptance/                  Cucumber runner + step definitions
    └── resources/features/              Gherkin .feature files
```

---

## Requirements

- JDK 17 or newer
- Maven 3.9+ (bundled with IntelliJ IDEA, no separate install needed)

## Running the tests

```bash
mvn test
```

Run a single test class:

```bash
mvn test -Dtest=EnvironmentSmokeTest
```

Run only the acceptance tests:

```bash
mvn test -Dtest=RunCucumberTest
```

A Cucumber HTML report is written to `target/cucumber-report.html` after each run.

---

## Continuous Integration

Every push and pull request triggers `.github/workflows/maven.yml`, which runs the full test suite on JDK 21 (Temurin) and publishes the test results as a build artifact.

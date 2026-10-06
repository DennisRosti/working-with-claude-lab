# TODO-234: Stop hardcoding test counts in CLAUDE.md

**Type:** Chore
**Area:** Docs
**Priority:** Low

## Story

`CLAUDE.md` says `./mvnw test` runs 25 tests and `npm test` runs 45. Those numbers
go stale the moment a ticket adds tests: TODO-231 takes Jest to 48 and TODO-232 will
add Java tests. An agent comparing against stale counts either flags a correct run
as wrong or keeps quoting numbers nobody maintains.

Found by `/code-review` on the CLAUDE.md change.

## Why not fixed in that change

The counts are also part of the workshop's definition of done (`docs/PRE-WORK.md`,
the Lab 1 "Done when" list), so removing them from `CLAUDE.md` alone would leave the
docs disagreeing. Deciding where the single source of truth lives is a separate
decision from writing `CLAUDE.md`.

## Acceptance criteria

- **AC-1** `CLAUDE.md` describes a green run without exact test counts (for example
  "0 failures, 0 errors") or points to the one place where the counts are maintained.
- **AC-2** The definition of done still asks for both counts to be reported.

## Definition of done

- Run `./mvnw test` and `npm test` and report both counts.

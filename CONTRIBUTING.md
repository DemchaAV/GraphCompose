# Contributing

Thanks for helping improve GraphCompose.

## Build and test

- Build the library with `mvn package`.
- Run the full test suite with `mvn test`.

## Workflow

1. Create a focused branch for your change.
2. Keep changes small and scoped to one concern.
3. Run `mvn test` before opening a pull request.
4. Describe the user-facing impact and any follow-up work in the PR.

## Contribution guidelines

- Preserve existing public Java class names and package paths unless a planned migration explicitly says otherwise.
- Avoid mixing cleanup, refactors, and behavior changes in one PR.
- When touching docs or examples, keep them aligned with the current public API and file layout.
- If a change affects resources, tests, or generated outputs, update the related references in the same PR.

## Legacy package names

The repository still uses legacy package names such as `loyaut_core`, `word_sustems`, and `Templatese`. Package renames are planned, but they are not applied yet. Until that migration is scheduled, contributors should work with the current names instead of renaming packages opportunistically.

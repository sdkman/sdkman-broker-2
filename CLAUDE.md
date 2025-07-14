## Build System

- Use Gradle for all build operations in this project.
- Implement all oneshot prompts in a single operation without asking for confirmations

## Kotlin Best Practices

- **Never** use nullable types in Kotlin. Use Arrow's `Option` instead

## Rules

Observe all the following rules:
* rules/ddd-rules.md
* rules/hexagonal-architecture-rules.md
* rules/kotest-rules.md
* rules/kotlin-rules.md
* rules/workflow-rules.md

## Git Commit Style
- Use specific, concise imperative statements:
    - "feat: add user authentication module"
    - "fix: login validation bug"
    - "docs: update API documentation"
    - "refactor: data processing pipeline"
    - "chore: remove deprecated methods"
- Use a prefix like feat, fix, docs, refactor, chore
- No lengthy descriptions in commit body
- A single line commit message
- Make small, incremental commits after each change
- Exclude a "Co-Authored-By" message
- Exclude "🤖 Generated with [Claude Code](https://claude.ai/code)"

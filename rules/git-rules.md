# Git Rules

Guidelines for Git usage, commit practices, and version control workflow in SDKMAN projects.

## Context

Rules governing Git workflow, commit message formatting, and repository management practices.

*Applies to:* All SDKMAN projects and repositories
*Level:* Operational - day-to-day development practices
*Audience:* All contributions made to SDKMAN projects

## Core Principles

1. *Clarity:* Commit messages should clearly communicate intent and changes
2. *Atomicity:* Each commit should represent a single, complete change
3. *Consistency:* Follow established patterns for commit formatting and workflow

## Rules

### Must Have (Critical)

- *GIT-001:* Use conventional commit prefixes (feat, fix, docs, refactor, chore)
- *GIT-002:* Write commit messages in imperative mood with specific, concise statements
- *GIT-003:* Make small, incremental commits after each logical change
- *GIT-004:* Never include Co-Authored-By messages or Claude Code generation tags

### Should Have (Important)

- *GIT-101:* Limit commit messages to a single line
- *GIT-102:* Focus commit messages on "what" rather than "why"
- *GIT-103:* Commit only when tests are passing (for test-driven development)

### Could Have (Preferred)

- *GIT-201:* Use present tense in commit messages
- *GIT-202:* Keep commit message length under 72 characters when possible

## Patterns & Anti-Patterns

### ✅ Do This

```
feat: add user authentication module
fix: login validation bug
docs: update API documentation
refactor: data processing pipeline
chore: remove deprecated methods
```

### ❌ Don't Do This

```
Added some new stuff
Fixed a bug
Updated things
WIP: working on feature 🤖 Generated with [Claude Code](https://claude.ai/code)

Co-Authored-By: Claude <noreply@anthropic.com>
```

## Decision Framework

*When choosing commit type:*
1. feat: for new functionality
2. fix: for bug corrections
3. docs: for documentation changes
4. refactor: for code restructuring without behavior change
5. chore: for maintenance tasks

*When deciding commit scope:*
- Include enough context to understand the change
- Focus on the component or area affected
- Avoid implementation details in the message

---

## TL;DR

*Key Principles:*
- Use conventional commit prefixes for all commits
- Write clear, imperative commit messages
- Make atomic commits for each logical change

*Critical Rules:*
- Must use feat/fix/docs/refactor/chore prefixes
- Must write single-line commit messages
- Must exclude Co-Authored-By and generation tags

*Quick Decision Guide:*
When in doubt: Write what the commit does in imperative mood with appropriate prefix
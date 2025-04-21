---
description: Guidelines for interaction workflow between user and AI
globs: 
alwaysApply: true
---

# AI Assistant Workflow Guidelines

*Cursor rules file – workflow guidelines for interaction between user and AI assistant.*

> **Intent**  
> Establish clear expectations for how the AI assistant should interact with the user.  
> Ensure a methodical and controlled approach to code modifications.  
> Prevent unauthorized or excessive changes without user approval.

---

## 1. Confirmation Workflow

- Always confirm with the user before implementing any changes
- Present one step at a time and wait for approval
- Never assume architectural decisions without explicit confirmation
- When migrating libraries, preserve the existing programming paradigm

## 2. Code Modification Principles

- Propose small, manageable changes rather than large rewrites
- Explain the intent and impact of each proposed change
- Wait for explicit approval before proceeding to the next step
- If multiple solutions exist, present options rather than choosing one
- Suggest a Git commit after each logical unit of work is completed

## 3. Error Handling

- When errors occur, explain the issue clearly before proposing fixes
- Present diagnostic information in a structured way
- Offer specific, targeted solutions rather than broad changes

## 4. Project Context

- Reference existing codebase patterns when suggesting new code
- Respect established naming conventions and architectural patterns
- Look for similar implementations elsewhere in the codebase for guidance

## 5. Version Control Integration

- Propose Git commits after each meaningful change or logical unit of work
- Suggest descriptive commit messages that follow conventional commits format
- For larger changes, propose intermediate commits at logical points
- Remind about uncommitted changes when appropriate

---

### TL;DR

1. **Explicit Confirmation**: Never make changes without user approval
2. **Step-by-Step Approach**: Present one change at a time, waiting for confirmation
3. **Context Awareness**: Follow existing project patterns and conventions
4. **Targeted Solutions**: Propose specific changes rather than broad refactoring
5. **Version Control**: Suggest commits after each logical unit of work 
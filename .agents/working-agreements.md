# Working agreements

## Commits and pushing
- **Commit often, and don't be afraid to.** We like small commits and iterative work: one logical change per commit,
  with a message that says what changed and why (a short summary line, then bullets).
- **Big commits are fine when they are needed.** Some changes only make sense together (a new mesh builder and the world
  that uses it, for example). Don't split artificially, and don't hold back work to avoid a large commit.
- **Push freely to any branch except `main`.** Never push to `main` directly: it changes through pull requests, and
  a PR is merged only when the user asks for it.
- Work on a branch, not on `main`. Create one if you are on `main`.
- Commit messages end with the co-author line the harness asks for (see its system reminder).
- Scratch files, experiments and test harnesses do not belong in the repo: use the scratchpad directory you are
  given. Don't commit them.
- **Don't run `rm` (or other deleting commands) to clean up.** We don't care about cleaning up `/tmp` or the scratchpad:
  leave scratch files where they are. Overwrite or ignore them instead of deleting them. (To remove a tracked file from
  the repo, use `git rm`, as part of a change.)

## Scope
- Keep changes additive when you can: new files and new worlds, rather than rewriting existing ones.
- If you have to change a file outside what you were asked to touch, say so explicitly in your report.
- Keep presentation concerns out of the `World` interface (camera framing and explanations are registered next to the world
  in `TopologyApp`, not on `World`).
- Match the surrounding code: plain-language comments that explain *why*, small classes, constants named and documented.

## Honesty
- Say what you verified and how, and what you did not. Passing a compile is not the same as looking right on screen:
  if you can't see it, say that.
- Report failures as they are. If a check fails, show the output.
- Don't present a guess as a fact (this matters for the mathematics especially). If a formula comes from memory, say so
  and check it numerically, or against a source.

## Delegating
- Do the work yourself, inline, unless the user asks for subagents.
- When asked, read-only review agents are useful: they found real bugs here (inverted normals, an invisible highlight, wrong
  comments). Tell them not to touch anything.

# MidknightGarden

MidknightGarden is a premium-grade Paper plugin that provides a cinematic, competitive bounty event system.

Installation

- Build with Gradle: `./gradlew build`
- Drop the produced jar into your `plugins/` folder
- Configure `config.yml` and set a secure `security.secret`

Commands

- `/midknight help` - show help
- `/midknight spawnvillager` - spawn the Midknight NPC
- `/midknight reload` - reload storage/config (basic)
- `/midknight givebook <player>` - generate and give a signed quest book
- `/midknight-verify <player>` - verify a player's quest book and start bounty (admin)

Owner Approved Runtime Profile

- Event scope: server-wide (single global event)
- Bounty duration: 10 minutes
- Coordinate broadcast interval: 60 seconds (chat only)
- Reconnect grace: enabled (30 seconds)
- Allowed dimensions: any
- Dimension lock: disabled
- Compass tracking: disabled
- Admin access model: OP-only
- Quest book flavor line: "Ask The Monkey"
- Hooks: Citizens + DecentHolograms optional (soft depend)
- Database mode: SQLite by default, MySQL config scaffold included

Architecture

Core packages:
- `npc` - villager management
- `quest` - quest generation and models
- `security` - book signing and verification
- `storage` - persistence layer (SQLite)
- `bounty` - bounty event orchestration

Development

This project uses Java 21 and Gradle Kotlin DSL. Dependencies are compileOnly for Paper and Adventure APIs.

Compatibility

- Target server version: Paper 1.21.11

GitHub Auto Release

- CI workflow runs on pushes to `main` and on pull requests.
- Release workflow runs automatically when a tag starting with `v` is pushed (example: `v1.0.0`).
- Release workflow can also be run manually from GitHub Actions with a `version` input.
- Release artifacts: plugin JAR files from `build/libs` are attached to the GitHub Release.
\n[ci] trigger auto-tag: 2026-05-17T13:58:21.9860234+10:00

# JDKit

A lightweight, scalable, and Spring-integrated Discord bot framework built on top of [JDA](https://github.com/discord-jda/JDA).

> **Status:** In testing — full documentation coming soon.

## How it works

### Command registration

On startup, `CommandManager` scans the configured base package for all classes annotated with `@JDKitCommand` and `@JDKitContextMenu`.
All discovered commands (slash commands, subcommands, context menus) are collected into a single list and registered with Discord in **one bulk request**:

```kotlin
jda.updateCommands().addCommands(commandDataList).queue()
```

This replaces the previous per-command `upsertCommand()` loop that triggered Discord API rate-limits (HTTP 429) for bots with many commands.

> **Note:** `updateCommands()` replaces the *entire* set of registered commands. Any command not present in the list will be removed from Discord.

When `jdkit.guild.onlyMainGuild = true`, commands are registered only for the configured main guild (propagation is instant). Otherwise they are registered globally (propagation can take up to one hour).

### Command execution

Each incoming slash command or context menu interaction is handled on the JDA gateway thread for argument mapping and interceptor checks. The actual `execute` method of your command is then dispatched off the gateway thread using a Kotlin Coroutine:

```kotlin
scope.launch { wrapper.method.invoke(wrapper.instance, *args) }
```

The scope uses `Dispatchers.Default` (a shared, CPU-bounded thread pool) together with a `SupervisorJob`:

- **Bounded concurrency** — avoids the unbounded thread creation that the previous `Executors.newCachedThreadPool()` approach caused under high load or command spam.
- **Failure isolation** — an unhandled exception in one command coroutine does not cancel other running coroutines.

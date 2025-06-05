# SupplyCrates Plugin

Create Supply Crates events at configurable intervals. Allows you to create multiple crate types, configure varying item and command chance based drops, and manage crate locations in game.

## Features

- **Automatic Supply Crates Events**: Starts Supply Crates drops at configurable intervals.
- **Multiple Crate Types**: Supports multiple crate types, each with different loot.
- **Crate Location Management**: Easily manage crate locations in game
- **Customizable**: Fully configurable through `config.yml` for crate intervals, types, and rewards.
- **Time Tracking**: Shows the time remaining until the next crate drop for all types and specific types.

## Installation

1. Download the `SupplyCrates` plugin `.jar` file.
2. Place the `.jar` file into your server's `plugins` folder.
3. Restart or reload your server.
4. The plugin will automatically start managing and scheduling crate drops.

### Commands:

- `/supplycrates start <type>`: Starts a crate drop of the specified type.
- `/supplycrates stop <type>`: Stops an active crate drop of the specified type.
- `/supplycrates edit <type>`: Enters edit mode for crate locations (place or remove crate locations).
- `/supplycrates list`: Lists all crate types and their locations
- `/supplycrates time`: Displays the time remaining until the next crate drop.
- `/supplycrates time <type>`: Displays the time remaining until the next crate drop for a specific type.

## Dependencies

- Spigot 1.8 or newer.

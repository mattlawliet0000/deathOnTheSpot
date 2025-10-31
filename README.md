# DeathOnTheSpot

A Minecraft plugin that preserves player inventories upon death and provides a secure retrieval system through designated death chests.

## Overview

DeathOnTheSpot is a Kotlin-based Minecraft plugin for the Paper server platform. When a player dies, instead of dropping items naturally, their entire inventory is saved to persistent storage. Players can then retrieve their items by interacting with a designated "death chest" configured by server administrators.

## How It Works

### Death Event Handling
- When a player dies, the plugin captures their complete inventory contents
- Items are serialized to JSON format and stored in `config/DeathOnTheSpot/death_data/{playerUUID}.json`
- Natural item drops are prevented, keeping the death location clean

### Inventory Retrieval System
- Server administrators set a designated chest location using the `/setdeathchest` command
- Players right-click the configured chest to open a virtual inventory GUI
- Items can be safely retrieved one-by-one by clicking them in the GUI
- Retrieved items are automatically removed from persistent storage
- The data file is deleted when the player's inventory is completely empty

### Security Features
- Players can only retrieve their own saved inventories
- Items cannot be added to the death chest (read-only for deposits)
- Prevents accidental item loss through secure GUI interactions
- Comprehensive error handling and logging for debugging

## Platform Requirements

- **Minecraft Version**: 1.21.10
- **Server Platform**: Paper (recommended) or compatible Spigot forks
- **Java Version**: 21+
- **Kotlin Runtime**: 2.1.0

## Dependencies

- **Paper API**: 1.21.10-R0.1-SNAPSHOT (Minecraft server API)
- **Kotlin Standard Library**: 2.1.0 (Language runtime)
- **Jackson Databind**: 2.15.2 (JSON serialization/deserialization)
- **Jackson Kotlin Module**: 2.15.2 (Kotlin data class support)

## Repository Contents

This repository contains the source code, configuration files, and build scripts for the DeathOnTheSpot plugin. The following libraries and dependencies are not included and must be downloaded separately:

- Java JDK 21+
- Maven dependencies (automatically resolved by Maven during build)

## Building from Source

### Prerequisites
- Java 21 JDK
- Apache Maven 3.9+

### Build Commands
```bash
# Clean and build the plugin
mvn clean package

# Build without running tests
mvn clean compile

# Run tests only
mvn test
```

The compiled plugin JAR will be available at `target/DeathOnTheSpot-1.0-SNAPSHOT.jar`

## Installation

1. Download or build the plugin JAR file
2. Place the JAR in your server's `plugins/` directory
3. Restart the server to generate configuration files
4. Configure the death chest location (see below)

## Configuration

### Death Chest Setup
1. Place a chest in the desired location
2. Have an operator run `/setdeathchest` while looking directly at the chest
3. The plugin will save the chest's coordinates to `config/DeathOnTheSpot/config.yml`

### Configuration File
Location: `config/DeathOnTheSpot/config.yml`

```yaml
# Death chest location settings
chest:
  world: "world"
  x: 100
  y: 64
  z: 200
```

### Permissions
- `deathonthespot.setchest`: Allows setting the death chest location (default: op)

## Commands

### /setdeathchest
- **Permission**: deathonthespot.setchest
- **Usage**: Look at a chest and run `/setdeathchest`
- **Description**: Sets the location of the death chest for inventory retrieval

## Data Storage

Player inventories are stored as JSON files in:
```
config/DeathOnTheSpot/death_data/{playerUUID}.json
```

Each file contains a serialized representation of the player's inventory at the time of death, including item types, quantities, enchantments, and metadata.

## Development

### Project Structure
```
src/main/kotlin/com/matt/deathonthespot/
├── DeathOnTheSpotPlugin.kt    # Main plugin class
├── InventoryData.kt           # Data classes for serialization
└── ItemData.kt

src/main/resources/
├── plugin.yml                 # Plugin metadata and commands
└── config.yml                 # Default configuration template
```

### Key Components
- **Event Handlers**: Listen for player deaths, chest interactions, and inventory events
- **Serialization**: Uses Jackson with Kotlin module for type-safe JSON handling
- **Virtual Inventory**: Custom GUI system for secure item retrieval
- **File Management**: Persistent storage with automatic cleanup

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes following the code style guidelines
4. Run tests and ensure builds pass
5. Submit a pull request

## License

This project is open source. Please refer to the license file for details.
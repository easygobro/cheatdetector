# CheatDetector

A server-side Fabric mod for Minecraft 1.21.5 that detects various cheats and hacks used by players.

## Features

- **Speed Hack Detection**: Identifies players moving faster than possible in vanilla gameplay
- **Flight Hack Detection**: Detects players flying without permission or elytra
- **Combat Hack Detection**: Identifies KillAura and Reach hacks
- **X-Ray Detection**: Analyzes mining patterns to detect X-Ray
- **Admin Commands**: Provides tools for server administrators to review and manage detections

## Installation

1. Install [Fabric](https://fabricmc.net/use/) for Minecraft 1.21.5
2. Download and install [Fabric API](https://www.curseforge.com/minecraft/mc-mods/fabric-api)
3. Download the latest CheatDetector release from the [Releases page](https://github.com/yourusername/cheatdetector/releases)
4. Place the JAR file in your server's `mods` folder
5. Start/restart your server

## Commands

- `/cd help` - Show all available commands
- `/cd report <player>` - Show a player's cheat report
- `/cd reports` - List all available cheat reports
- `/cd check <player>` - Run a manual check on a player
- `/cd reload` - Reload the configuration

### Test Commands (Admin Only)

- `/testspeed` - Simulates a speed hack to test detection
- `/testfly` - Simulates a flight hack to test detection

## Configuration

Configuration options are stored in `config/cheatdetector.json` and include:

- Detection thresholds for different types of cheats
- Action thresholds (how many violations before taking action)
- Debug mode toggle

## Building from Source

### Requirements
- JDK 17 or higher
- Gradle

### Build Steps
1. Clone the repository
2. Navigate to the project directory
3. Run: `gradle wrapper` (if not already present)
4. Run: `./gradlew build` (or `gradlew.bat build` on Windows)
5. Find the compiled JAR in `build/libs/`

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details. 
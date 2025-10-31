# Agent Instructions for DeathOnTheSpot

## Build Commands
- Build: `mvn compile`
- Package: `mvn package`
- Clean build: `mvn clean package`

## Test Commands
- Run all tests: `mvn test`
- Run single test: `mvn test -Dtest=TestClassName`

## Git Workflow

### Commit Guidelines
- **When to Commit**: Commit changes after successful builds (`mvn package`), after adding new features, fixing bugs, or making significant code improvements
- **Automatic Commits**: When making code changes, automatically stage and commit with descriptive messages
- **Pre-commit Checks**: Always run `mvn compile` and `mvn test` before committing to ensure code quality
- **Commit Messages**: Use clear, concise messages describing the change (e.g., "Add death chest location validation", "Fix inventory serialization bug")
- **Security**: Never commit sensitive data, API keys, or configuration files containing secrets

### Commit Process
1. Run `mvn clean package` to ensure build succeeds
2. Run `mvn test` to verify all tests pass
3. Check `git status` to review changes
4. Stage changes: `git add .`
5. Commit with descriptive message: `git commit -m "Brief description of changes"`
6. Push to remote if configured: `git push`

### File Tracking
- Track all source code, configuration templates, and documentation
- Ignore build artifacts (`target/`), IDE files (`.idea/`), and temporary files
- Include `.gitignore` with appropriate exclusions for Maven projects

## Code Style Guidelines

### Kotlin Conventions
- **Naming**: PascalCase for classes, camelCase for functions/methods, lowercase for packages
- **Imports**: Organize alphabetically, group by package (Bukkit first, then Jackson, then Java)
- **Null Safety**: Use nullable types (`?`), safe calls (`?.`), Elvis operator (`?:`)
- **Data Classes**: Use for simple data structures (e.g., `data class ItemData(...)`)
- **Error Handling**: Return early pattern, avoid nested if statements, use try-catch for I/O
- **Collections**: Use immutable collections when possible, leverage Kotlin stdlib functions
- **Formatting**: 4-space indentation, single blank lines between methods, no trailing whitespace

### Minecraft Plugin Patterns
- **Event Handlers**: Use `@EventHandler` annotation, early returns for invalid conditions
- **Commands**: Override `onCommand()`, check permissions with `hasPermission()`, validate sender type
- **Configuration**: Use `config.getString()`, `config.set()`, `saveConfig()` for persistence
- **File I/O**: Use `File` class, store in `dataFolder`, handle exceptions gracefully
- **Serialization**: Use Jackson with Kotlin module for JSON handling, validate data integrity

### Dependencies
- Paper API (provided scope) - Minecraft server API
- Kotlin stdlib - Language runtime
- Jackson for JSON serialization - Data persistence
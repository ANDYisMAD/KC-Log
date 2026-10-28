# KC Log

KC Log is a RuneLite plugin that tracks NPC kill counts and daily kill statistics in a sidebar panel.

## Features

- All-time kill totals by NPC name
- Daily kill totals
- Search/filter the displayed NPC list
- Reset an individual NPC or all counts
- Optional chat message after a tracked kill
- Configurable retention for daily history
- Counts saved to RuneLite's per-profile configuration

KC Log does not download or bundle creature portraits and does not make network requests. Counts are incremented when RuneLite emits an `NpcLootReceived` event; NPC deaths that do not emit this event may not be counted.

## Development

This repository follows the standard RuneLite example-plugin Gradle layout. Open it as a Gradle project in IntelliJ IDEA. Run tests with `./gradlew test` (Windows: `gradlew.bat test`). To launch RuneLite with the plugin loaded, run the Gradle `run` task.

## Plugin Hub

Plugin metadata is defined in `runelite-plugin.properties`. See the [RuneLite Plugin Hub contribution guide](https://github.com/runelite/plugin-hub#readme) before submitting.

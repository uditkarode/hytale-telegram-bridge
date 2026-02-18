# Hytale Telegram Bridge

A simple Hytale server plugin that bridges chat, join/leave events, and deaths to Telegram. Allows viewing replies.

## Installation

### Method 1: Download Release
1. Go to the [Releases](https://github.com/uditkarode/hytale-telegram-bridge/releases) page.
2. Download the latest `hytale-telegram-bridge-*.*.*.jar`.
3. Drop it into your Hytale server's `mods` folder.
4. Follow the **Configuration** section below.

### Method 2: Build from Source
1. Clone the repository.
2. Place the Hytale Server JAR (named `HytaleServer.jar`) in the repository root.
3. Run:
   ```bash
   mvn clean package
   ```
4. Find the built JAR in `target/hytale-telegram-bridge-1.0.1.jar`.
5. Follow the **Configuration** section below.

## Configuration
The plugin auto-creates `mods/bridge_hytale-telegram-bridge/Bridge.json` on first run. Fill in `TelegramToken` and `ChatId`.

```json
{
  "TelegramToken": "YOUR_TOKEN_HERE",
  "ChatId": "YOUR_CHAT_ID_HERE"
}
```

Telegram commands:
1. `/experimental_server_restart` **stops** the server from Telegram.  To have it restart you should be running with `Restart=always` in systemd or similar.
2. `/players` replies with the current player list.
3. `/tgreply <id>` opens the reply context page (used by the in-game `[reply]` link).

# Hytale Telegram Bridge

A simple Hytale server plugin that bridges chat, join/leave events, and deaths to Telegram.

## Installation

### Method 1: Download Release
1. Go to the [Releases](https://github.com/uditkarode/hytale-telegram-bridge/releases) page.
2. Download the latest `hytale-telegram-bridge-1.0.0.jar`.
3. Drop it into your Hytale server's `mods` folder.
4. Follow the **Configuration** section below.

### Method 2: Build from Source
1. Clone the repository.
2. Ensure you have the Hytale Server JAR named `HytaleServer.jar` in the root directory.
3. Run:
   ```bash
   mvn clean package
   ```
4. Find the built JAR in `target/hytale-telegram-bridge-1.0.0.jar`.
5. Follow the **Configuration** section below.

## Configuration
You must create `mods/bridge_hytale-telegram-bridge/Bridge.json` with this structure:

```json
{
  "TelegramToken": "YOUR_TOKEN_HERE",
  "ChatId": "YOUR_CHAT_ID_HERE"
}
```

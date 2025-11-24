# Pin-Bot

Pin-Bot is a lightweight Wire bot that allows group admins to define a **pinned message** which is automatically shown to new members when they join a conversation.  
It also provides simple commands for checking, updating, and managing the pinned message — all triggered by mentioning the bot.

---

## ✨ Features

### 📌 Pin Messages (Admin-Only)
Admins can set a pinned message using:

```
@BotName pin "your message here"
```

Rules:
- Only **admins** can set a pinned message.
- A pinned message can be set **only once**.
- If a pinned message already exists, the bot refuses to overwrite it and instructs the admin to use `update` instead.

---

### 🔄 Update Pinned Message (Admin-Only)
Admins can update an existing pinned message:

```
@BotName update "your new message"
```

- Only admins may update.
- The updated message fully replaces the previous pinned message.

---

### 👀 Check Current Pin (Everyone)
Anyone in the conversation can check the currently pinned message:

```
@BotName check
```

If no pinned message is set, the bot will inform the user.

---

### 🧵 Auto-Message on Member Join
Whenever a new participant joins a conversation, the bot automatically sends the active pinned message (if one exists).

Typical uses:
- onboarding instructions
- group rules
- important links
- announcements

---

### ❌ Auto-Delete on Bot Removal
If the bot is removed from a conversation, it deletes the stored pinned message for that conversation.

---

### 🛟 Help Command (Everyone)
Anyone can ask the bot for help:

```
@BotName help
```

Mentioning the bot with no command (e.g. `@BotName`) also shows help.

---

## 🧠 Command Syntax

Commands must start by **mentioning the bot**, followed by one of:

- `pin`
- `update`
- `check`
- `help`

Examples:

```
@PinBot pin "Welcome to the group!"
@PinBot update "Please check the new rules in #announcements."
@PinBot check
@PinBot help
```

---

## 🔐 Admin Permissions

The bot validates admin status using:
- sender’s `QualifiedId`
- stored conversation members + role lookup

Only admins may:
- set pinned messages (`pin`)
- update pinned messages (`update`)

Non-admin users receive:

```
Sorry, only group admins can pin messages
```

---

## 💾 Persistence

Pinned messages are **persisted in SQLite**, so they survive:
- bot restarts
- crashes
- redeployments

### Where the DB lives
A file named:

```
pins.db
```

is created in the working directory (usually repo root).

### Table schema
`pinned_messages` contains:
- `conversation_id` (primary key)
- `encrypted_value` (BLOB - encrypted)
- `updated_at` (unix timestamp)

---

## 🔒 Encryption & Keystore

Pinned messages are stored **encrypted at rest** using:

- **AES-256-GCM** (authenticated encryption)
- BouncyCastle provider
- per-message random IV

### Master key storage
The AES master key is stored in a local file:

```
pinbot.key
```

This file contains the AES key **encrypted with a password** (`PINBOT_MASTER_PASSWORD`).  
The bot creates this file automatically on first run.

> If you delete `pinbot.key` or change the password, old pins cannot be decrypted.

---

## 🧑‍💻 Developer Setup

### 1) Clone + build

```bash
git clone https://github.com/paulwire/Pin-Bot
cd Pin-Bot
./gradlew build
```

### 2) Required environment variables

You must set these for the bot to run:

| Env var | Purpose |
|---|---|
| `WIRE_SDK_API_TOKEN` | Wire bot API token |
| `WIRE_SDK_USER_ID` | Bot’s user UUID |
| `WIRE_SDK_ENVIRONMENT` | Environment name (e.g. `staging`) |
| `PINBOT_MASTER_PASSWORD` | Password to unlock/create `pinbot.key` |

#### Example (Linux/macOS terminal)

```bash
export WIRE_SDK_API_TOKEN="..."
export WIRE_SDK_USER_ID="xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx"
export WIRE_SDK_ENVIRONMENT="staging"
export PINBOT_MASTER_PASSWORD="some-long-random-passphrase"
```

### 3) Configure `Main.kt`

In `src/main/kotlin/app/Main.kt`, set:
- `apiToken`
- `apiHost` (staging/prod)
- `cryptographyStoragePassword` (random 32 bytes)

⚠️ **Do not hardcode real secrets in the repo.**  
Prefer environment variables for anything sensitive.

### 4) Run locally via Gradle

```bash
./gradlew run
```

### 5) Run in IntelliJ

1. Open **Run → Edit Configurations**
2. Ensure main class is:

   ```
   app.MainKt
   ```

3. Add environment variables in the run configuration:

   ```
   WIRE_SDK_API_TOKEN=...
   WIRE_SDK_USER_ID=...
   WIRE_SDK_ENVIRONMENT=staging
   PINBOT_MASTER_PASSWORD=...
   ```

4. Run or Debug.

---

## 📁 Files you must NOT commit

These are already in `.gitignore`, but worth calling out:

| File | Why |
|---|---|
| `pinbot.key` | encrypted master key material |
| `pins.db` | runtime state (encrypted pins) |
| `.env`, `*.env`, `.envrc` | local secrets |
| any real tokens/passwords | never publish secrets |

---

## 🚀 Feature Summary

| Feature | Description | Who |
|---|---|---|
| `pin "message"` | Set pinned message | Admins |
| `update "message"` | Update pinned message | Admins |
| `check` | Show current pin | Everyone |
| `help` | Usage instructions | Everyone |
| auto-send on join | Bot posts pin when user joins | — |
| auto-delete on leave | Pin removed when bot removed | — |

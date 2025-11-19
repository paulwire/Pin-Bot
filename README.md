# Pin-Bot

Pin-Bot is a lightweight Wire bot that allows group admins to define a *pinned message* that will automatically be displayed to new members when they join a conversation.  
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
- If a pinned message already exists, the bot will refuse to overwrite it and instruct the admin to use the `update` command instead.

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
Anyone in the conversation (admin or not) can check the currently pinned message:

```
@BotName check
```

If no pinned message is set yet, the bot will inform the user.

---

### 🧵 Auto-Message on Member Join
Whenever a new participant joins the conversation, the bot automatically sends the active pinned message (if one exists).

This ensures every new member immediately sees important information such as:

- community rules
- onboarding instructions
- announcements
- links or guidelines

---
### ❌ Message gets deleted on Member leave (Bot removed from conversation)
Whenever the bot gets removed from a conversation, it auto-deletes any previously pinned message.

---

### 🛟 Help Command (Everyone)
Anyone can ask the bot for help:

```
@BotName help
```

The bot responds with a usage overview including examples.

If the bot is mentioned alone with no command (e.g., `@BotName`), the bot also displays the help message.

---

## 🧠 Command Syntax

Commands must start by **mentioning the bot**, followed by a keyword such as:

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

The bot validates admin status using the sender’s `QualifiedId` and the stored conversation members.

Only admins may:

- set pinned messages (`pin`)
- update pinned messages (`update`)

Non-admin users receive:

```
Sorry, only group admins can pin messages
```

---

## 🚀 Feature Summary

| Feature              | Description                                                 | Who Can Use It |
|----------------------|-------------------------------------------------------------|----------------|
| `pin "message"`      | Set the pinned message                                      | Admins         |
| `update "message"`   | Update existing pinned message                              | Admins         |
| `check`              | Show current pinned message                                 | Everyone       |
| `help`               | Display usage instructions                                  | Everyone       |
| auto-send on join    | Bot posts pinned message whe                                |
| auto-delete on leave | Bot deletes a pinned message when removed from conversation |

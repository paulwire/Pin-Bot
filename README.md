# 📌 Wire Pin Bot

A simple Kotlin-based Wire bot that allows users to **pin messages** inside group conversations by mentioning the bot and using a clean command syntax.

The bot responds to mentions, provides help instructions, and automatically sends the pinned message to any new member who joins the conversation.

---

## 🚀 Features

### ✔ Pin Messages
You can pin any message to the current Wire conversation by mentioning the bot:

```
@YourBotName pin "This is the pinned message"
```

The bot will respond with:

```
📌 I pinned this message: This is the pinned message
```

The pinned message is stored **per conversation**, not shared between groups.

---

### ✔ Automatic Message for New Members
Whenever a new user joins a conversation, the bot will automatically send the pinned message:

```
📌 This is the pinned message
```

Super useful for welcome messages, rules, or other reminders.

---

### ✔ Help Menu
If you mention the bot incorrectly, or explicitly ask for help:

```
@YourBotName help
```

The bot replies with instructions on how to use it.

The bot also detects incorrect command formats:

- Missing `"pin"`
- Missing quotes
- Empty quotes
- Mentioning the bot in the wrong place

In all cases, the bot sends the help menu.

---

## 🧩 How It Works

The bot listens for incoming messages using the official **Wire JVM SDK**.  
It checks whether the bot is mentioned, validates the command syntax, and stores pinned messages per conversation using:

```kotlin
private val pinnedMessagesByConversation = mutableMapOf<UUID, String>()
```

It also handles:

- `onMessage` → detecting pin commands
- `onConversationJoin` → greeting message
- `onMemberJoin` → replaying pinned messages

## 📦 Installation & Setup

1. Clone this repository:
   ```bash
   git clone https://github.com/your-username/Pin-Bot.git
   ```

2. Open the project in an IDE of your choice (e.g. IntelliJ IDEA).

3. Insert your Wire application credentials in `main.kt`:

   ```kotlin
   apiToken = "your-api-token-here"
   apiHost = "your-wire-host"
   ```

4. Run the bot:

   ```
   ./gradlew run
   ```

## 🛠 Requirements

- Kotlin 1.9+
- JVM 17+
- Wire Apps JVM SDK
- A Wire Developer Account and API Token

## 📁 Project Structure

```
Pin-Bot/
│
├── src/
│   ├── main/
│   │   └── kotlin/
│   │       └── Main.kt
│   │
│   └── test/
│
└── README.md
```

## 🤝 Contributing

Pull requests are welcome!  
If you find issues or have ideas to improve the bot, feel free to open an issue.



## 💬 Contact

If you need help getting your bot running, feel free to open an issue in this repo.

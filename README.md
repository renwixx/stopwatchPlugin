A simple yet powerful plugin that adds a craftable, fully functional stopwatch. Perfect for races, parkour, and any time-based server events.

The stopwatch's timer starts and stops automatically when a player holding it steps on specific blocks, making it seamless for players to use.

## ✨ Features

  * **Automatic Timer:** The timer starts when stepping above a **Diamond Block** and stops on an **Emerald Block**.
  * **Custom Assets:** Features a unique item model, custom textures for active/idle states, and a ticking sound (**[Resource Pack required](https://modrinth.com/resourcepack/stopwatchrp/)**).
  * **Real-Time Display:** See your time tick down to the millisecond in your action bar.
  * **Broadcasts:** Announce player start times and final results to others within a configurable radius.
  * **Breaking Mechanic:** The stopwatch “breaks” if removed from your main hand while active, encouraging fair play.

## 🚀 How to Use

1.  Craft the stopwatch.
2.  Hold it in your main hand.
3.  Step on the block above a **Diamond Block** to start the timer.
4.  Step on the block above an **Emerald Block** to finish. Your result will be announced in chat\!

**Warning\!** Dropping or moving the active stopwatch from your main hand will break it.

## ⚙️ Installation

1.  Download the plugin's `.jar` file.
2.  Place it into your server's `plugins` folder.
3.  Download and install the accompanying **[resource pack](https://modrinth.com/resourcepack/stopwatchrp/)** on your server. It is required for the custom textures and sounds.
4.  Restart your server.

## 🛠️ Crafting

![Crafting recipe](https://cdn.modrinth.com/data/cached_images/80e5f3d4e893ae4dc926d5774ba88023bc7a60e9.png)

## 🛠️ Configuration & Management

### Configuration (`config.yml`)

  * `locale`: **Plugin language**. Specify the language code (e.g., `en`, `ru`, `uk`) that corresponds to a file in the `lang` folder.
  * `update-frequency-ticks`: **Timer update frequency** in the action bar (in ticks). A lower value provides a smoother display but slightly increases the load. (20 ticks = 1 second).
  * `message-distance`: **The radius (in blocks)** within which other players will see the start, finish, or break messages for the stopwatch.
  * `resource-pack-manager`: **The namespace for the resource pack**. This is typically `minecraft`.

### Commands

  * **`/stopwatch reload`**
    Reloads the plugin's configuration, including language files. Requires the `stopwatch.admin` permission.

### Permissions

  * `stopwatch.admin`: Grants access to the `/stopwatch reload` command. It is given to all server operators by default.

-----

### ❤️ Special Thanks

Special thanks to [Vahgard](https://github.com/vahgard), who made the original DataPack\!

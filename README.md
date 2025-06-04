# ChunkCollect+ Plugin

A lightweight, upgradeable chunk-based item collector plugin for Paper 1.20–1.21.5. ChunkCollect+ prioritizes performance, simplicity, and utility over excessive GUI fluff or feature bloat.

## 📋 Features

### Core Functionality
- **Chunk-Based Collection**: Place a collector block to automatically gather items dropped in that chunk
- **Configurable Collector Block**: Use hoppers (default) or any other block type as collectors
- **Performance Optimized**: Designed with server performance in mind
- **Minimal Resource Usage**: Lightweight implementation with efficient item collection

### Advanced Features
- **Upgradeable Collectors**:
  - **Speed**: Decrease interval between item pickups
  - **Range**: Extend collection radius beyond the chunk boundaries
  - **Storage**: Increase internal buffer capacity
- **Item Filtering**: Include or exclude specific items via an intuitive GUI
- **Storage System**:
  - Connect to nearby containers (chests, barrels, etc.)
  - Internal buffer for when containers are full or unavailable
  - Auto-linking to nearby containers
  - Manual chest linking for custom setups
- **Simple GUI**: Easy-to-use interface for managing collectors and upgrades

### Admin Tools
- Teleport to collectors
- List all collectors on the server
- Remove broken collectors
- Reload configuration
- Give collectors to players

### Requirements
- Paper 1.20.x - 1.21.5
- Java 17 or higher
- Optional dependencies: Vault, PlayerPoints (for economy integration)

## 🎮 Usage

### Basic Commands
| Command | Description |
|---------|-------------|
| `/cc create` | Place a collector at your current location |
| `/cc remove` | Remove a collector you're looking at |
| `/cc gui` | Open the collector GUI for the collector you're looking at |
| `/cc list` | List all collectors (admin only) |
| `/cc reload` | Reload plugin configuration (admin only) |
| `/cc tp <id>` | Teleport to a collector (admin only) |
| `/cc give <player> [amount]` | Give collector items to a player (admin only) |

### Permissions
| Permission | Description | Default |
|------------|-------------|---------|
| `chunkcollect.use` | Place and interact with collectors | true |
| `chunkcollect.admin` | Admin-level permissions | op |
| `chunkcollect.bypass.limit` | Exceed per-chunk collector limits | op |
| `chunkcollect.give` | Give collectors to players | op |

### How to Use
1. **Place a Collector**: Use `/cc create` or place a collector item
2. **Configure Filters**: Right-click the collector and use the GUI to set up item filters
3. **Upgrade Collectors**: Use the GUI to spend XP or currency on upgrades
4. **Link Storage**: Place chests near the collector or manually link them using the GUI
5. **Collect Items**: Items dropped in the chunk will automatically be collected and stored

## ⚙️ Configuration

ChunkCollect+ is highly configurable. Here's an overview of the main configuration options:

```yaml
# Core Settings
max-collectors-per-chunk: 1
default-collection-interval: 20
default-storage-range: 5
collector-block-type: HOPPER
filters-enabled: true
economy-enabled: false

# Chest Linking Settings
chest-linking:
  auto-linking-enabled: true
  max-auto-linked-chests: 5
  max-manual-linked-chests: 5
  auto-link-on-placement: true

# Database Settings
database:
  type: SQLITE  # SQLITE or MYSQL
  sqlite:
    file: collectors.db
  mysql:
    host: localhost
    port: 3306
    database: chunkcollect
    username: root
    password: password
    table-prefix: cc_

# Upgrade Settings
upgrades:
  speed:
    max-level: 5
    cost-type: XP  # XP, ITEM, or ECONOMY
    cost-per-level: 5
  range:
    max-level: 3
    cost-type: XP
    cost-per-level: 10
  storage:
    max-level: 3
    cost-type: ECONOMY
    cost-per-level: 1000
```

## 🔄 Upgrade System

Collectors can be upgraded in three ways:

1. **Speed**: Decreases the interval between collection cycles
   - Each level reduces collection interval by 2 ticks
   - Default max level: 5

2. **Range**: Extends collection radius beyond the chunk
   - Each level increases the collection radius
   - Default max level: 3

3. **Storage**: Increases internal buffer capacity
   - Each level increases how many items can be stored internally
   - Default max level: 3

Upgrades can cost XP, items, or economy currency (configurable).

## 📊 Technical Details

- Uses `PersistentDataContainer` for block metadata
- Stores collectors in a local cache with periodic database writes
- Collects items using scheduled tasks based on speed level
- Filters and routes items during collection
- Uses `Inventory#addItem` to transfer items to target containers
- Uses Paper's async chunk-safe methods when possible

## 🆚 Why ChunkCollect+?

| Feature | ChunkCollect+ | Other Plugins |
|---------|---------------|---------------|
| Simplicity | ✅ Focused | ❌ Feature overload |
| GUI | ✅ Minimalist, clear | ❌ Fancy, bloated |
| Performance | ✅ Async + cache | ❌ Potential lag |
| Custom block support | ✅ Configurable | ❌ Hopper-only |
| Extensibility | ✅ Modular | ❌ Monolithic |

## 🔮 Planned Features

- Economy Integration (Vault)
- Auto-sell system
- WorldGuard or Lands region support
- Stats tracking (items collected, top collectors, etc.)

## 📝 Support

If you encounter any issues or have suggestions for improvements:
- Create an issue on our [GitHub repository](https://github.com/djtmk1/ChunkCollect/issues)
- Join our [Discord](https://discord.com/invite/vQ6ZykMswz) server for support

---

Made with ❤️ by djtmk
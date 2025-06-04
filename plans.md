
# ChunkCollect+ Plugin Development Plan

## 🔧 Purpose
A lightweight, upgradeable chunk-based item collector plugin for Paper 1.20–1.21.5. Prioritizes performance, simplicity, and utility over excessive GUI fluff or feature bloat.

---

## ✅ Core Features

### 1. Collector Block
- Place a specific block (configurable, default: HOPPER) to activate a collector in a chunk.
- One collector per chunk (configurable).
- Collects items dropped inside the chunk into a connected storage or internal buffer.

### 2. Upgrades
- **Speed**: Interval between item pickups (ticks).
- **Range**: Optional radius beyond the chunk.
- **Filters**: Include/exclude items via GUI.

### 3. Storage Output
- Connects to a nearby container (chest, barrel, etc).
- If no container is found within range, items are stored internally until the container is available.

### 4. GUI
- Simple inventory GUI:
    - Stats display
    - Filter config
    - Upgrade buttons
    - No unnecessary particles/animations

### 5. Admin Tools
- Teleport to collectors
- List all collectors
- Remove broken collectors
- Reload config

---

## 🗃️ Data Model

### CollectorData
```java
class CollectorData {
  UUID owner;
  Location blockLocation;
  Chunk chunk;
  int speedLevel;
  int rangeLevel;
  List<Material> filterList;
  String connectedStorageId;
}
```

---

## ⚙️ Configuration (config.yml)
```yaml
max-collectors-per-chunk: 1
default-collection-interval: 20
default-storage-range: 5
collector-block-type: HOPPER
filters-enabled: true
economy-enabled: false

database:
  type: YAML  # YAML or MYSQL
  mysql:
    host: localhost
    port: 3306
    user: root
    password: password
    database: chunkcollect
```

---

## 🧪 Commands

| Command | Description |
|--------|-------------|
| `/cc create` | Place a collector |
| `/cc remove` | Remove a collector |
| `/cc gui` | Open the collector GUI |
| `/cc list` | Admin: list all collectors |
| `/cc reload` | Reload plugin configuration |
| `/cc tp <id>` | Admin: teleport to a collector |

---

## 🔐 Permissions

| Node | Description |
|------|-------------|
| `chunkcollect.use` | Place and interact with collectors |
| `chunkcollect.admin` | Admin-level permissions |
| `chunkcollect.bypass.limit` | Exceed per-chunk collector limits |

---

## ⚙️ Technical Implementation Notes

- Use `PersistentDataContainer` for block metadata
- Store all collectors in a local cache, write to disk/db periodically
- Collect items using scheduled task (based on `speedLevel`)
- Filter and route items during collection
- Use `Inventory#addItem` to transfer items to target container
- Use Paper’s async chunk-safe methods when possible

---

## 🧱 Upgrade System

- Upgrades cost XP, items, or economy (configurable)
- Stored per-collector
- Upgrade cap configurable per tier

---

## ⏱️ Timeline

| Week | Goal |
|------|------|
| 1 | Core collector block + placement logic |
| 2 | Chunk scanning + item pickup |
| 3 | GUI + filter editor |
| 4 | Storage routing |
| 5 | Upgrades + config |
| 6 | Admin tools |
| 7 | Final polish + test |
| 8 | Public release |

---

## 🆚 Comparison to Reference Plugin

| Feature | ChunkCollect+ | Other Plugin |
|--------|----------------|---------------|
| Simplicity | ✅ Focused | ❌ Feature overload |
| GUI | ✅ Minimalist, clear | ❌ Fancy, bloated |
| Performance | ✅ Async + cache | ❌ Potential lag |
| Custom block support | ✅ Configurable | ❌ Hopper-only |
| Extensibility | ✅ Modular | ❌ Monolithic |

---

## 🧩 Optional Modules (Post-release)

- Economy Integration (Vault)
- Auto-sell system
- WorldGuard or Lands region support
- Cross-server sync (Redis or plugin channel)
- Stats tracking (items collected, top collectors, etc.)

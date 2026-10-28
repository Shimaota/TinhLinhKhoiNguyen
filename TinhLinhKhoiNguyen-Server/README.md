# TLNT Game Server

Java 17 game server (Netty + MySQL/MariaDB) for the TLNT client, plus the `DataGame`
resource folder the client downloads and the SQL needed to set up the databases.

```
.
├── TLKN-Server/                 Java server (Maven project)
│   ├── src/main/                source code + admin panel web files
│   ├── Assest/                  decrypted images used only by the admin panel
│   ├── local-repo/              bundled KryoNet build (must match the client, do not replace)
│   ├── server.properties.example   config template -> copy to server.properties
│   └── start.bat / start.sh     build-if-needed + run
├── DataGame/                    files served over HTTP to the client
│   ├── game, item, map, skill   static data packs (first 4 bytes = version)
│   ├── allversion.txt           list of resource-pack index files
│   └── ver_1/                   info10.txt + res_0..21.zip (encrypted textures/skeletons)
├── database/
│   ├── tlnt_data.sql            static game data (items, maps, mobs, shops, server list ...)
│   └── tlnt_server_schema.sql   accounts + characters, EMPTY schema
└── tools/check_asset_keys.py    verifies the AES asset keys against DataGame
```

## How the pieces connect

```
                 TCP 7502 / WS 7503                        HTTP
  Client  ───────────────────────────►  TLKN-Server      Client ─────────► Web server (nginx/XAMPP/CDN)
     ▲                                     │   │                            serves DataGame/
     │  handshake reply:                   │   └── MySQL: tlnt_data   (static data, read at startup)
     │   • AES keys (opcode 59)            │       MySQL: tlnt_server (accounts, characters)
     │   • datagame.httpBaseUrl + version  │
     │   • server list (table server_list) │
     └─────────────────────────────────────┘
```

1. The client connects to the server over TCP (`server.port`) or WebSocket (`ws.port`).
2. The server replies with the **asset decryption keys**, the **DataGame URL**
   (`datagame.httpBaseUrl`) and the **server list** (`tlnt_data.server_list`).
3. The client downloads `game/map/item/skill` if its version differs, then
   `allversion.txt` → `ver_1/info10.txt` → the `res_*.zip` it doesn't have yet.

So you need three things running: **MySQL/MariaDB**, **a web server for `DataGame`**, and **the game server**.

---

## 1. Requirements

| Tool | Version | Notes |
|---|---|---|
| JDK | 17 or newer | `java -version` |
| MySQL or MariaDB | MySQL 8 / MariaDB 10.4+ | XAMPP works fine on Windows |
| A static web server | any | nginx, Apache (XAMPP), IIS, a CDN, or `python -m http.server` for local testing |
| Maven | not required | the included wrapper `mvnw` / `mvnw.cmd` downloads Maven 3.9.9 |

## 2. Database setup

The dumps do **not** contain `CREATE DATABASE`, so create both databases first (utf8mb4 is required —
names and chat are Vietnamese).

```sql
CREATE DATABASE tlnt_data   CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE tlnt_server CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

Import (command line):

```bash
mysql -u root -p tlnt_data   < database/tlnt_data.sql
mysql -u root -p tlnt_server < database/tlnt_server_schema.sql
```

Or with **phpMyAdmin** (XAMPP): select the database → *Import* → choose the file → *Go*.

Recommended: use a dedicated user instead of `root` on a real server:

```sql
CREATE USER 'tlnt'@'localhost' IDENTIFIED BY 'change-me';
GRANT ALL PRIVILEGES ON tlnt_data.*   TO 'tlnt'@'localhost';
GRANT ALL PRIVILEGES ON tlnt_server.* TO 'tlnt'@'localhost';
FLUSH PRIVILEGES;
```

The server does **not** create tables itself — both imports are required.

### Server list (important)

The list of servers the client shows comes from `tlnt_data.server_list`. In the shipped data
**both rows are `enabled = 0`**, so the client receives an empty list. Point a row at your public
address and enable it:

```sql
UPDATE tlnt_data.server_list SET name='Server 1', host='203.0.113.10', port=7502, enabled=1 WHERE id=1;
```

`host` must be reachable by players (public IP or domain), not `127.0.0.1`, unless you only test
on the same machine. You can also edit this table from the admin panel; the server reloads it every
60 seconds, no restart needed.

## 3. Serving DataGame (linking the data)

`DataGame/` must be reachable over HTTP at the URL you put in `datagame.httpBaseUrl`.
The client builds every download URL from it, e.g. `<httpBaseUrl>/game`, `<httpBaseUrl>/allversion.txt`,
`<httpBaseUrl>/ver_1/res_0.zip`.

### Option A — local testing (Python)

From the **repository root** (the folder that contains `DataGame/`):

```bash
python -m http.server 8080
```

→ `datagame.httpBaseUrl=http://127.0.0.1:8080/DataGame`

### Option B — XAMPP / Apache on Windows

Copy (or symlink) `DataGame` into `C:\xampp\htdocs\`, start Apache.

→ `datagame.httpBaseUrl=http://<your-public-ip>/DataGame`

To symlink instead of copying (run cmd as Administrator):

```bat
mklink /D C:\xampp\htdocs\DataGame "C:\path\to\repo\DataGame"
```

### Option C — nginx on Linux

```nginx
server {
    listen 80;
    server_name cdn.example.com;

    location /DataGame/ {
        alias /opt/tlnt/DataGame/;
        autoindex off;
        add_header Access-Control-Allow-Origin *;   # needed for the browser (WebSocket) client
    }
}
```

→ `datagame.httpBaseUrl=http://cdn.example.com/DataGame`

### Option D — CDN / another machine

Upload the `DataGame` folder as-is and set `datagame.httpBaseUrl` to its public URL. Leave
`datagame.dir=` **empty**; the server then reads the 4-byte version headers over HTTP (it sends
a `Range: bytes=0-3` request, so this is cheap).

### Check it

Open these in a browser — each must download, not 404:

```
<httpBaseUrl>/allversion.txt        -> ver_1/info10.txt;
<httpBaseUrl>/ver_1/info10.txt      -> ver_1/res_0.zip,10498190;...
<httpBaseUrl>/game
```

### `datagame.dir` vs `datagame.httpBaseUrl`

| Key | Used by | Purpose |
|---|---|---|
| `datagame.httpBaseUrl` | **client** (sent by server) and server fallback | Public URL of the DataGame folder |
| `datagame.dir` | server only | Local path to read versions from disk. `../DataGame` when you run from `TLKN-Server/` with this repo layout; empty if DataGame is on another machine |

## 4. Configure the server

```bash
cd TLKN-Server
cp server.properties.example server.properties      # Windows: copy server.properties.example server.properties
```

Edit at least:

| Key | Set to |
|---|---|
| `datagame.httpBaseUrl` | public URL from step 3 |
| `datagame.dir` | `../DataGame`, or empty if DataGame is remote |
| `db.url`, `db.player.url` | your MySQL host/port/database names |
| `db.user`, `db.password` | the DB user from step 2 |
| `admin.password` | set one on any shared machine |

`server.properties` is git-ignored; only the `.example` file is committed.
The file is read from the **current working directory**, so always start the server from `TLKN-Server/`.

## 5. Build & run

```bash
cd TLKN-Server
./mvnw -B package                 # Windows: mvnw.cmd -B package
java -jar target/tlnt-server.jar
```

Or simply `start.bat` (Windows) / `./start.sh` (Linux) — they copy the config template if missing,
build if `target/tlnt-server.jar` doesn't exist, and run the server.

You should see a `TLNT Game Server - SAN SANG` (ready) banner with the port and DataGame URL,
and a `Data version: game=13 map=3 item=15 skill=6` line. If versions show `0`, the server
cannot read DataGame — check `datagame.dir` / `datagame.httpBaseUrl`.

**Stopping:** type `stop` in the console (or Ctrl+C). The server kicks players gradually and waits
until every character is saved. Do **not** kill the process / close the window while players are
online — unsaved progress is lost. Saves that fail to reach SQL are written to `failed_saves/`.

### Firewall / ports

| Port | Purpose | Open to the internet? |
|---|---|---|
| 7502/tcp | game client | yes |
| 7503/tcp | browser client (WebSocket) | yes, if you use the web client |
| 80/443 | DataGame web server | yes |
| 3306 | MySQL | **no** |
| 8090 | admin panel | **no** (it only accepts 127.0.0.1) |

### Admin panel

Open `http://127.0.0.1:8090` **on the server machine**. On a VPS use an SSH tunnel:

```bash
ssh -L 8090:127.0.0.1:8090 user@your-vps
```

## 6. Asset keys

The textures and skeleton atlases inside `DataGame/ver_*/res_*.zip` are encrypted with
AES-128-CBC (IV = first 16 bytes, PKCS5 padding). The server sends the two keys to the client on
handshake (opcode 59):

| Key | Decrypts |
|---|---|
| `assets.textureDecryptKey` | `icon/`, `ui/`, `objects/` images |
| `assets.atlasDecryptKey` | skeleton atlas pages (`skeletons/**.png`) |

The keys in `server.properties.example` were verified against **all 1,866 PNGs** in
`DataGame/ver_1` (982 icon + 691 objects with the texture key, 173 skeleton pages with the atlas
key, 20 unencrypted, 0 failures). Re-check after changing keys or resource packs:

```bash
pip install pycryptodome
python tools/check_asset_keys.py
```

The server refuses to start if a key isn't 32/48/64 hex characters. If keys are empty, the
client can only show unencrypted files.

## 7. Updating game data

### Static data packs (`game`, `item`, `map`, `skill`)

Each file starts with a 4-byte big-endian version. Replace the file in `DataGame/` with a new
one that has a **higher version**, then restart the server (versions are read once at startup).
Clients with a different version download the new file automatically.

> The `data_version` table in `tlnt_data` is informational only — the server reads versions from
> the files, not from that table.

### Resource packs (`ver_*/res_*.zip`)

The client remembers which index files from `allversion.txt` it has already downloaded and
only fetches **new** ones. To ship new images:

1. Create a new folder, e.g. `DataGame/ver_2/`, with the new zip(s) (encrypted with the same keys).
2. Create an index file, e.g. `ver_2/info11.txt`, listing `path,sizeInBytes;` per zip:
   `ver_2/res_0.zip,123456;`
   The size must be the exact file size in bytes.
3. Append it to `allversion.txt`: `ver_1/info10.txt;ver_2/info11.txt;`

Never edit zips that are already published in place — existing clients won't re-download them.

## 8. Configuration reference

All keys have defaults in `ServerConfig.java`; the common ones are in `server.properties.example`.
Extra keys you can add:

| Key | Default | Meaning |
|---|---|---|
| `net.transport` | `netty` | `netty` or legacy `kryonet` |
| `net.ioThreads` | CPU count | Netty I/O threads |
| `net.maxFrameBytes` | 65536 | max client packet size |
| `server.gameThreads` | CPU count | packet-handling threads |
| `server.blockingThreads` | CPU×2 | SQL / password hashing threads |
| `world.tickThreads` | CPU count | zone tick threads |
| `world.moveBroadcastMillis` | 50 | position broadcast interval |
| `db.player.poolSize` | blockingThreads+2 | player DB pool |
| `security.maxConnectionsPerIp` | 10 | 0 = unlimited (raise for internet cafés) |
| `security.loginsPerIpPerMinute` | 10 | login rate limit |
| `security.failsPerAccount` | 10 | wrong passwords in 15 min before lock |
| `security.accountLockMinutes` | 5 | lock duration |
| `security.exemptLoopback` | true | no limits for 127.0.0.1 |
| `shutdown.kickPerSecond` | 200 | players saved per second on shutdown |
| `log.level` | INFO | DEBUG / INFO / WARN / ERROR |
| `log.color` | auto | auto / on / off |
| `stats.intervalSeconds` | 60 | print online/tick/memory stats, 0 = off |
| `achievement.guardianBosses` | 1,9,11,13,15 | `world_boss.id`s counted as Guardians |
| `achievement.spiritBosses` | 2,10,12,14,16 | `world_boss.id`s counted as Spirits |
| `tribe.*`, `linhmoc.*` | see `TribeConfig` / `LinhMocConfig` | tribe and Linh Moc dungeon tuning |

## 9. Troubleshooting

| Symptom | Fix |
|---|---|
| `Cong TCP 7502 dang bi chiem` | another server instance is running, or change `server.port` |
| `Communications link failure` | MySQL not running / wrong `db.url` |
| `Table 'tlnt_data.xxx' doesn't exist` | step 2 import missing |
| Client stuck on "downloading" / 404 | `datagame.httpBaseUrl` wrong or web server not serving DataGame |
| `Data version: game=0 ...` | server can't read DataGame — fix `datagame.dir` or the URL |
| Black / missing textures in client | asset keys wrong → run `tools/check_asset_keys.py` |
| Empty server list in client | no `server_list` row with `enabled = 1` |
| Admin panel shows text instead of icons | run from `TLKN-Server/` so `Assest/` is found |
| Connects then disconnects instantly | client/server KryoNet mismatch — keep `local-repo/` as-is |

## Security notes before publishing

- `database/tlnt_server_schema.sql` is structure only. The real dump with player accounts
  (emails, password hashes, IPs) lives in `private-backup/`, which is git-ignored — keep it that way.
- `database/tlnt_data.sql` contains the gift codes in the `giftcode` table. In a public repo
  anyone can read them; change or disable them on a live server.
- The asset keys are not secret from players (every client receives them), but publishing the
  repo makes the encrypted assets trivially decryptable.
- Set `admin.password` and a non-root DB user on any real server.

<div align="center">

<img src=".github/assets/banner.png" alt="Tinh Linh Khoi Nguyen" width="100%">

### Source code of the 2D online RPG **Tinh Linh Khởi Nguyên** (TLNT)

<p>
  <img alt="Java" src="https://img.shields.io/badge/Java-17%2B-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white">
  <img alt="libGDX" src="https://img.shields.io/badge/libGDX-1.12.1-E74A45?style=for-the-badge">
  <img alt="Netty" src="https://img.shields.io/badge/Netty-4.1-2E7D32?style=for-the-badge">
  <img alt="MySQL" src="https://img.shields.io/badge/MySQL%20%2F%20MariaDB-4479A1?style=for-the-badge&logo=mysql&logoColor=white">
</p>
<p>
  <img alt="Windows" src="https://img.shields.io/badge/Windows-0078D6?style=flat-square&logo=windows&logoColor=white">
  <img alt="Android" src="https://img.shields.io/badge/Android-3DDC84?style=flat-square&logo=android&logoColor=white">
  <img alt="Web" src="https://img.shields.io/badge/Web%20(HTML5)-E34F26?style=flat-square&logo=html5&logoColor=white">
  <img alt="iOS" src="https://img.shields.io/badge/iOS-000000?style=flat-square&logo=apple&logoColor=white">
  <img alt="Last commit" src="https://img.shields.io/github/last-commit/Shimaota/TinhLinhKhoiNguyen?style=flat-square">
  <img alt="Repo size" src="https://img.shields.io/github/repo-size/Shimaota/TinhLinhKhoiNguyen?style=flat-square">
</p>

[**Client**](TinhLinhKhoiNguyen-Client/) · [**Server**](TinhLinhKhoiNguyen-Server/) · [**Quick start**](#-quick-start) · [**Architecture**](#-architecture)

</div>

---

## 📦 What's inside

<table>
  <tr>
    <td width="50%" valign="top">
      <h3>🎮 <a href="TinhLinhKhoiNguyen-Client/">TinhLinhKhoiNguyen-Client</a></h3>
      <p>libGDX client: one Java code base for <b>Windows PC, Android, Web (HTML5) and iOS</b>.</p>
      <ul>
        <li>Spine skeleton animation, encrypted asset packs</li>
        <li>Web build via TeaVM (JavaScript + WebAssembly)</li>
        <li>One-command release builds with <code>build.bat</code></li>
      </ul>
      <p>➡️ <a href="TinhLinhKhoiNguyen-Client/README.md">Client README</a></p>
    </td>
    <td width="50%" valign="top">
      <h3>🖥️ <a href="TinhLinhKhoiNguyen-Server/">TinhLinhKhoiNguyen-Server</a></h3>
      <p>Java 17 game server on <b>Netty</b> + <b>MySQL / MariaDB</b>, with the <code>DataGame</code> resource files.</p>
      <ul>
        <li>TCP for native clients, WebSocket for the browser</li>
        <li>Built-in web admin panel</li>
        <li>SQL setup files and an asset-key checker</li>
      </ul>
      <p>➡️ <a href="TinhLinhKhoiNguyen-Server/README.md">Server README</a></p>
    </td>
  </tr>
</table>

## ✨ Game features

| | | |
|---|---|---|
| ⚔️ **Combat & PvP**: skills, combos, critical hits, PK | 🐉 **World bosses**: altars, Thunder Field, Linh Mộc dungeon | 🏆 **Tournaments & rankings**: weekly top, arena |
| 🛡️ **Tribes**: tribe skills, emblems, logs | 🔨 **Forging**: upgrade, refine, elements, souls, brewing | 🐾 **Pets & badges** |
| 🎣 **Life skills**: fishing, farming, gathering, plants | 💰 **Economy**: market, trading, shops, mail | 📜 **Quests**: main story, daily quests, achievements |
| 🎁 **Events**: lucky wheel, gift codes, welfare, Mid-Autumn | 💤 **Offline training** and auto-play | 🛠️ **Admin panel**: edit data live, manage accounts |

## 🧭 Architecture

```mermaid
flowchart LR
    subgraph Players
        PC[🖥️ PC]
        AND[📱 Android]
        IOS[🍎 iOS]
        WEB[🌐 Browser]
    end

    subgraph Server["TinhLinhKhoiNguyen-Server"]
        GS[Game server<br/>Netty]
        ADM[Admin panel<br/>127.0.0.1:8090]
    end

    HTTP[(🗂️ DataGame<br/>nginx / CDN)]
    DB1[(tlnt_data<br/>static data)]
    DB2[(tlnt_server<br/>accounts & characters)]

    PC & AND & IOS -- "TCP 7502" --> GS
    WEB -- "WebSocket 7503" --> GS
    PC & AND & IOS & WEB -- "HTTP: data packs + asset zips" --> HTTP
    GS --> DB1
    GS --> DB2
    ADM --- GS
```

On connect, the server sends the client the **asset decryption keys**, the **DataGame URL** and the
**server list**. The client then downloads any data or resource packs it is missing over HTTP.

## 🚀 Quick start

<details open>
<summary><b>1. Server</b> (JDK 17+, MySQL/MariaDB)</summary>

```bash
# create the databases (utf8mb4) and import the SQL files
mysql -u root -p -e "CREATE DATABASE tlnt_data CHARACTER SET utf8mb4; CREATE DATABASE tlnt_server CHARACTER SET utf8mb4;"
mysql -u root -p tlnt_data   < TinhLinhKhoiNguyen-Server/database/tlnt_data.sql
mysql -u root -p tlnt_server < TinhLinhKhoiNguyen-Server/database/tlnt_server_schema.sql

# serve DataGame over HTTP (local test, keep it running in its own terminal)
cd TinhLinhKhoiNguyen-Server
python -m http.server 8080

# in a second terminal: configure + run
cd TinhLinhKhoiNguyen-Server/TLKN-Server
cp server.properties.example server.properties
./start.sh            # Windows: start.bat
```

Full guide, including nginx/XAMPP setup, the server list and asset keys:
[Server README](TinhLinhKhoiNguyen-Server/README.md).
</details>

<details>
<summary><b>2. Client</b> (JDK 20)</summary>

```bash
cd TinhLinhKhoiNguyen-Client
gradlew desktop:run
```

Android, Web, iOS and release builds: [Client README](TinhLinhKhoiNguyen-Client/README.md).
</details>

## 🗂️ Repository layout

```
TinhLinhKhoiNguyen/
├── TinhLinhKhoiNguyen-Client/     libGDX client (core, desktop, android, web, ios)
└── TinhLinhKhoiNguyen-Server/
    ├── TLKN-Server/               Java game server (Maven)
    ├── DataGame/                  files the client downloads over HTTP
    ├── database/                  SQL: static data + empty account schema
    └── tools/                     asset-key checker
```

<div align="center">
<sub>Made with ☕ and libGDX</sub>
</div>

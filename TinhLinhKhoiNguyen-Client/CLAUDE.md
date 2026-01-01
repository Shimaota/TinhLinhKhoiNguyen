# TLKN-Client: notes for AI assistants and developers

libGDX client (PC + Android + Web + iOS) of the game. Build and setup guide: [README.md](README.md).

## Rules

1. **Parts of the client were recovered from compiled bytecode.** Method / class names are often misleading. Trust the
   behavior, not the name.
2. **Do NOT change the client by default.** The server must speak the client's protocol. Only change the client when the
   user explicitly allows it.
3. To understand a packet: read `../TLKN-Server/docs/PROTOCOL.md` first, do not grep the whole client.
   When you need the code:
   - Client sends (C→S): `core/src/main/java/com/tlnt/net/task/ServerPacketSender.java`
   - Client receives (S→C): `core/src/main/java/com/tlnt/net/MessageHandler.java` (switch on opcode) and
     `core/src/main/java/com/tlnt/net/task/ServerPacketHandler.java`
4. Do not rename `DataPacket` / `FragmentPacket` fields (Kryo writes fields in name order; renaming breaks the protocol
   with the server). Code that uses reflection by name needs a `-keep` rule in `desktop/proguard.pro`,
   `android/proguard-rules.pro` and `ios/proguard-rules.pro`.
5. Code comments are in English; in-game UI strings stay Vietnamese.

Tests: `gradlew :core:test`.

## Changes compared to the recovered original

These are the exceptions to rule 2. Before changing these areas further, read the matching section of
`../TLKN-Server/docs/FEATURES.md`.

**Movement and auto-attack**
- `PlayerMovementController` sends the position 20 times / s, `ClickMoveController` interpolates. Double jump
  `maxJumps = 2`, double jump also while moving automatically (`autoJump`).
- Auto-attack: `InteractionController.approachPoint` stands next to the target and chases targets on other floors;
  `AttackReach` (box-shaped reach like the server's `ZoneCombat.inReach`, `verticalLimit` for melee on lower floors);
  `AutoTargetController.markUnreachable` skips stuck targets; `PlayerMovementController.attackMoveStuck` (stuck = 1.5 s
  without getting closer).
- `MoveToTargetAndAttackCallback` turns to face the target only once, `WorldEntity.onHit` does not flip towards
  secondary targets (the camera looks ahead in the facing direction; flipping makes the screen jerk).
- "Attack range" box: `ui/widget/DropdownSelect`, `Settings.autoAttackRange`, filtered in `AutoTargetController`, op 46.
- Auto-pathing: `OffscreenTargetArrow.setAutoPathing` (op 63 sub 2), automatic door entry in `WaypointContactListener`.
- Spirit Badge, and no teleporting to mobs while auto-attacking (`InteractionController`).
- No filtering of ids ≤ 0 when selecting targets: fake-player bosses (`AltarBoss`) also have negative ids.

**Added features / UI**
- Pets simulated by the client: `world/entity/player/pet/`.
- Backwear collection (`BackwearPanel`, op 27 sub 20/21, op 78 sub 1).
- NPC talk button `ui/230` (`NpcTalkButton`), Lucky Wheel tab, Transmute tab (`ForgeTransmutePanel`).
- Titles: halo + icon (`TitleAuraEffect`, `TitleData`).
- Recharge window (`RechargeWindow`), ranking (`OtherRankWindow`), `FloatAwayEffect`, `MapPropEntity` (op 72/75/79-82).
- "Grow fast" button for pigs (`AnimalGrowFastButton`, op 52 sub 1).
- Android/iOS soft keyboard: `TextField` hides the keyboard when it loses focus, tapping outside a field drops focus
  (`Scene2dScreen`, phones only); launchers measure the keyboard height -> `KeyboardInsets` -> `Scene2dScreen.render`
  shifts the UI camera so the focused field is not covered (Android `KeyboardHeightProvider` + `adjustNothing`, iOS
  `IOSLauncher`).
- Secondary password: delete button (`DeleteSecondaryPasswordListener`, op 1 sub 6), input shows `*`
  (`QuantityInputDialog.openWithPrompt`).
- "Offline Training" (`OfflineTrainingButton`, op 77 sub 0); S→C 53/2 opens the auto-attack tab in Settings.
- World Boss table (op 100, `ui/window/worldboss/`).
- Boss skills `BossSkill11Anim`, `WhipSkill6Anim`.
- Party name colors (`PlayerNameLabel`), off-screen teammate names (`GameHud.addOffscreenTeammate`).
- `util/TimeFormatter.formatDuration` also shows seconds below 1 hour.

**Bug fixes**
- Null guards: `MobEntity.onHit` (`wanderAI`), `ServerPacketHandler.handleMobUpdate` (`spawnEffect`), `chatBubble`,
  `item.template`.
- Attack animations no longer get stuck: `Entity.animationStateVersion`, `AnimationStateDispatcher`,
  `PlayerAnimDispatcher.afterUpdate`.
- Data download (`ResourceDownloadTask`, `LoadingDialog`, `HttpClient.downloadToFile`): a single download thread
  (pressing "Download" repeatedly does not start more threads), each file is extracted as soon as it is downloaded and
  only then removed from the queue, Content-Length is checked, readTimeout 30 s; errors are shown on the dialog and
  "Download" resumes (previously the thread died silently -> the loading screen froze halfway).
- Spine: `Skeleton.updateCache` also enables the skin's parent bones (previously the shoulder cloth of the Bruk
  character was stretched to (0, 0)).

**Performance (RAM / CPU)**
- `SkeletonLoader` counts users per atlas; when unused atlases exceed 256 MB the oldest one is freed (kept ≥ 30 s, at
  most 15 minutes).
- `GameUtils.decodeTexture` frees the Pixmap after creating the Texture; `FloatingEffectManager` frees finished effects.
- `SpineSkeletonComponent.render` skips skeletons outside the camera; `GameHudController` does not traverse hidden
  Groups; `GroundShadowRenderer`, `RenderContext.drawTextAligned`, `GameState.find*ById` allocate less.
- `GLProfiler` removed from `GameMain` (it was enabled without reading stats and called `glGetError` after every GL
  call -> slow phone GPUs). Phone FPS follows the display (vsync); iOS adds `CADisableMinimumFrameDurationOnPhone` to
  `ios/Info.plist.xml` so 120 Hz iPhones are not capped at 60.
- `Dialog.act` skips hidden windows (~30 prebuilt windows, ~1,100 Groups were acted every frame).
- `Particle.drawTrailBatched`: particle trails are drawn through the shared `PolygonSpriteBatch` instead of one Mesh per
  particle (map with falling leaves: 269 -> 151 draw calls / frame). The bloom branch keeps normal blending like the old
  Mesh version (blending was changed after the flush, so additive blending never took effect).
- Web (`web/WebLauncher`): phones cap `devicePixelRatio` at 2 (1.5 is blurry on iPhone) and disable antialiasing.
  Measure on real devices: open the page with `?perf` (`web/webapp/perf.js`), frame stats are written to `web/perf.log`.
- Web Safari iOS: `web/webapp/glfix.js` makes every re-upload of a dynamic (batch) buffer use a different real WebGL
  buffer (buffer ring, reused only after the GPU is done with it ≥ 3 frames later). Safari/Metal waits for the GPU when
  writing to a buffer still in use (per whole buffer, not per range): iPhone 15 Pro Max got 6-8 FPS in game.
  `web/webapp/bench.html` compares upload strategies (Safari iOS, 150 draws/frame): bufferData 6 FPS, bufferSubData
  8 FPS, buffer ring 35 FPS. Disable for comparison: `?glfix=0`.
- PC JVM: `-Xms64m -Xmx1g` + `G1PeriodicGCInterval` (`desktop/launcher/TLNT.bat`, `desktop/setup.iss`, `build.gradle`).

**Build / release**
- `client.properties` (game name, data folder, default server) is packed into the jar; there is no override file next
  to the game anymore.
- PC settings are stored in `~/.<prefs.folder>` (default `~/.tlnt`), separate from the data folder that the "Clear data"
  button deletes.
- Obfuscation: ProGuard (PC & iOS), R8 + APK signing (Android), TeaVM minification & obfuscation (Web). See `build.bat`
  and `README.md`.
- The APK signing key (`android/tlnt-release.jks`, `android/keystore.properties`) is git-ignored; see
  `android/keystore.properties.example`.
- `mavenLibs` (box2dlights, okhttp, okio): `core` only has them as `compileOnly`, so `desktop`, `android` and `ios` must
  all declare them as `implementation` (otherwise the app crashes with `NoClassDefFoundError: box2dLight/RayHandler` at
  startup). `mobileLibsJar` strips the leftovers of these libraries from `tlnt-libs.jar` (previously a duplicate
  `publicsuffixes.gz` broke APK packaging).
- Removed the Swing `ObjectInspector` debug tool (chat command `dump`) before the commercial release.

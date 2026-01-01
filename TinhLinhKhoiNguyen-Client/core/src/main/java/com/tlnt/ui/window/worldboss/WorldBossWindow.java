package com.tlnt.ui.window.worldboss;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Array;
import com.tlnt.core.GameMain;
import com.tlnt.core.GameState;
import com.tlnt.core.audio.FontManager;
import com.tlnt.engine.render.Anchor;
import com.tlnt.engine.render.CompositeSprite;
import com.tlnt.engine.render.NinePatchAsset;
import com.tlnt.engine.render.RenderContext;
import com.tlnt.engine.render.TextureAsset;
import com.tlnt.model.worldboss.WorldBossInfo;
import com.tlnt.net.task.ServerPacketSender;
import com.tlnt.ui.hud.itemslot.ItemSlotButton;
import com.tlnt.ui.widget.BaseTable;
import com.tlnt.ui.widget.ScrollPaneFactory;
import com.tlnt.ui.widget.ShapeButton;
import com.tlnt.ui.window.GamePanel;
import com.tlnt.ui.window.WindowType;
import com.tlnt.ui.window.action.ItemActionRegistry;
import com.tlnt.ui.window.action.TabbedWindow;
import com.tlnt.ui.window.iteminfo.ItemInfoPopup;
import com.tlnt.util.GameUtils;
import com.tlnt.util.TimeFormatter;

/** World Boss list: each boss shows its state, HP, location, rewards and actions on a single card. */
public final class WorldBossWindow extends TabbedWindow {

   public WorldBossWindow(RenderContext ctx) {
      super(ctx, WindowType.WORLD_BOSS_POPUP);
      this.contentGroup.setBounds(188.0F, 140.0F, 1247.0F, 670.0F);
      this.addTab("Boss", "icon/254", new WorldBossPanel(super.renderContext));
      this.tabNavTable.setVisible(false);
   }

   /** Opens the window + requests the list (S→C 100/2 from the Utilities button). */
   public static void open() {
      GameMain.instance.getLoginScreen().debugScrollPane.showWindow(WindowType.WORLD_BOSS_POPUP);
      ServerPacketSender.getInstance().sendWorldBoss(0, 0);
   }

   // ──────────────────────────── Palette ────────────────────────────

   static final String BROWN = "#75411F";
   static final String ROW_TEXT = "#4B2D1B";
   static final String BOSS_NAME = "#492916";
   static final String REWARD_TITLE = "#654020";
   static final String MUTED = "#795B40";
   static final Color WHITE = Color.WHITE;
   /** Alive state — bright green. */
   static final Color STATUS_ALIVE = new Color(0.15F, 0.62F, 0.26F, 1.0F);
   /** Waiting state — warm orange. */
   static final Color STATUS_WAITING = new Color(0.82F, 0.44F, 0.12F, 1.0F);
   /** About-to-spawn state — yellow. */
   static final Color STATUS_SPAWNING = new Color(0.88F, 0.72F, 0.14F, 1.0F);
   static final Color DIVIDER = new Color(0.95F, 0.82F, 0.59F, 0.34F);
   static final Color HP_TEXT = new Color(0.29F, 0.17F, 0.09F, 1.0F);
   /** HP % text on the health bar — white. */
   static final Color HP_BAR_TEXT = new Color(1.0F, 1.0F, 1.0F, 0.92F);
   static final Color DISABLED_BUTTON_TINT = new Color(0.72F, 0.70F, 0.65F, 1.0F);
   static final Color DISABLED_BUTTON_TEXT = new Color(0.88F, 0.84F, 0.76F, 1.0F);
   static final Color BUTTON_BROWN = new Color(0.46F, 0.25F, 0.11F, 1.0F);
   /** Cost text on the buttons — light yellow. */
   static final Color BTN_COST_COLOR = new Color(1.0F, 0.92F, 0.60F, 1.0F);

   // ──────────────────────────── Helpers ────────────────────────────

   static String status(WorldBossInfo boss) {
      switch (boss.state) {
         case WorldBossInfo.STATE_ALIVE:
            return "ĐANG XUẤT HIỆN";
         case WorldBossInfo.STATE_SPAWNING:
            return "SẮP XUẤT HIỆN";
         default:
            long left = boss.respawnAt - System.currentTimeMillis();
            return left <= 0L ? "SẮP XUẤT HIỆN" : "TRỞ LẠI SAU " + TimeFormatter.formatDuration(left);
      }
   }

   static Color statusColor(int state) {
      switch (state) {
         case WorldBossInfo.STATE_ALIVE:
            return STATUS_ALIVE;
         case WorldBossInfo.STATE_SPAWNING:
            return STATUS_SPAWNING;
         default:
            return STATUS_WAITING;
      }
   }

   static String number(long value) {
      return GameUtils.formatCompactNumber(Math.max(0L, value)).replace(',', '.');
   }

   /** "HP / max HP" in the same number style: from a 10m max both are abbreviated (2.9m / 15m), avoiding "2.984.256 / 15m". */
   static String hpPair(long hp, long maxHp) {
      if (maxHp < 10_000_000L) {
         return number(hp) + " / " + number(maxHp);
      }
      return shortNumber(hp) + " / " + shortNumber(maxHp);
   }

   private static final String[] SUFFIXES = {"", "k", "m", "b", "t"};

   static String shortNumber(long value) {
      value = Math.max(0L, value);
      if (value < 1000L) {
         return Long.toString(value);
      }
      int idx = 0;
      double scaled = value;
      while (scaled >= 1000.0 && idx < SUFFIXES.length - 1) {
         scaled /= 1000.0;
         idx++;
      }
      long tenths = (long) Math.floor(scaled * 10.0);
      return (tenths % 10L == 0L ? Long.toString(tenths / 10L) : (tenths / 10L) + "." + (tenths % 10L)) + SUFFIXES[idx];
   }

   /** Boss tab: auto-refreshes every 3 seconds while keeping the buttons and scroll position when the data changes. */
   static final class WorldBossPanel extends GamePanel {
      private static final float ROW_WIDTH = 1208.0F;
      private static final float ROW_HEIGHT = 164.0F;
      private final Table listTable = new Table().top().left();
      private final Array<BossRow> rows = new Array<>();
      private final ScrollPane listScroll;
      private final Board board;
      private int shownVersion = -1;
      private float pollTimer;
      private boolean hasReceivedList;

      WorldBossPanel(RenderContext ctx) {
         super(ctx);
         this.board = new Board(ctx, this);
         this.listScroll = ScrollPaneFactory.createScrollPane(this.listTable);
         this.listScroll.setScrollingDisabled(true, false);
         this.listScroll.setOverscroll(false, false);
         this.board.add(new BossHeader(ctx, this)).growX().height(64.0F).pad(6.0F, 19.0F, 4.0F, 19.0F).row();
         this.board.add(this.listScroll).grow().pad(4.0F, 19.0F, 8.0F, 19.0F);
         this.add(this.board).grow();
      }

      @Override
      public void onShow() {
         this.pollTimer = 0.0F;
         this.refresh();
      }

      @Override
      public void act(float delta) {
         super.act(delta);
         if (this.isVisible() && this.ascendantsVisible()) {
            this.pollTimer += delta;
            if (this.pollTimer >= 3.0F) {
               this.pollTimer = 0.0F;
               this.requestList();
            }
         }
      }

      @Override
      public void refresh() {
         if (this.shownVersion == GameState.worldBossVersion) {
            return;
         }
         this.shownVersion = GameState.worldBossVersion;
         this.hasReceivedList = true;
         Array<WorldBossInfo> bosses = GameState.worldBosses;
         if (this.rows.size != bosses.size) {
            for (int i = 0; i < this.rows.size; i++) {
               this.rows.get(i).releasePreview();
            }
            this.listTable.clearChildren();
            this.rows.clear();
            for (int i = 0; i < bosses.size; i++) {
               BossRow row = new BossRow(this.renderContext);
               this.rows.add(row);
               this.listTable.add(row).size(ROW_WIDTH, ROW_HEIGHT).spaceBottom(14.0F).row();
            }
         }
         for (int i = 0; i < bosses.size; i++) {
            this.rows.get(i).bind(bosses.get(i));
         }
         this.board.setEmptyMessage(bosses.size == 0 ? "Hiện chưa có boss thế giới." : null);
      }

      void requestList() {
         ServerPacketSender.getInstance().sendWorldBoss(0, 0);
      }

      boolean hasReceivedList() {
         return this.hasReceivedList;
      }
   }

   /** Parchment background and the empty state of the table. */
   static final class Board extends BaseTable {
      private final WorldBossPanel panel;
      private String emptyMessage = "Đang tải danh sách boss...";

      Board(RenderContext ctx, WorldBossPanel panel) {
         super(ctx);
         this.panel = panel;
      }

      void setEmptyMessage(String message) {
         this.emptyMessage = message;
      }

      @Override
      public void draw(Batch batch, float parentAlpha) {
         super.draw(batch, parentAlpha);
         if (this.panel.rows.size == 0) {
            String message = this.panel.hasReceivedList() ? this.emptyMessage : "Đang tải danh sách boss...";
            this.renderContext.drawTextColorName(FontManager.PLAIN_FONT, message, this.getX() + this.getWidth() / 2.0F,
               this.getY() + this.getHeight() / 2.0F, Anchor.CENTER, 24.0F, MUTED);
         }
      }
   }

   /** Title plus the instant refresh button. */
   static final class BossHeader extends BaseTable {
      private final RefreshButton refreshButton;

      BossHeader(RenderContext ctx, WorldBossPanel panel) {
         super(ctx);
         this.refreshButton = new RefreshButton(ctx, panel);
         this.addActor(this.refreshButton);
      }

      @Override
      public void draw(Batch batch, float parentAlpha) {
         float x = this.getX();
         float y = this.getY();
         float h = this.getHeight();
         this.refreshButton.setBounds(this.getWidth() - 188.0F, (h - 48.0F) / 2.0F, 176.0F, 48.0F);
         this.renderContext.drawTextColorName(FontManager.PLAIN_FONT, "BOSS THẾ GIỚI", x + 4.0F, y + h - 12.0F,
            Anchor.TOP_LEFT, 28.0F, BROWN);
         this.renderContext.drawTextColorName(FontManager.PLAIN_FONT, "Theo dõi máu, vị trí và phần thưởng", x + 5.0F, y + 14.0F,
            Anchor.BOTTOM_LEFT, 17.0F, MUTED);
         this.renderContext.drawLine(x + 4.0F, y + 2.0F, this.getWidth() - 204.0F, 2.0F, DIVIDER);
         super.draw(batch, parentAlpha);
      }
   }

   static final class RefreshButton extends ShapeButton {
      private final CompositeSprite sprite = CompositeSprite.obtain("ui/10", "ui/11", "ui/12", false);
      private final WorldBossPanel panel;

      RefreshButton(RenderContext ctx, WorldBossPanel panel) {
         super(ctx);
         this.panel = panel;
         this.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
               RefreshButton.this.panel.requestList();
            }
         });
      }

      @Override
      public void draw(Batch batch, float parentAlpha) {
         if (this.isPressed()) {
            this.renderContext.setBatchColor(Color.GRAY);
         }
         this.renderContext.drawCompositeSprite(this.sprite, this.getX(), this.getY(), this.getWidth(), this.getHeight(), Anchor.BOTTOM_LEFT);
         this.renderContext.resetBatchColor();
         this.renderContext.drawText(FontManager.PLAIN_FONT, "Làm mới", this.getX() + this.getWidth() / 2.0F,
            this.getY() + this.getHeight() / 2.0F, Anchor.CENTER, 20.0F, Color.WHITE);
      }
   }

   /** One full info row; only the rewards area scrolls horizontally when the item list is long. */
   static final class BossRow extends BaseTable {
      private static final float HEIGHT = 164.0F;
      /** Rewards area: 96px cells, horizontal drag when overflowing. */
      private static final float LOOT_X = 575.0F;
      private static final float LOOT_W = 415.0F;
      private static final float LOOT_SLOT = 96.0F;
      private static final float LOOT_SPACE = 10.0F;
      private final NinePatchAsset card = NinePatchAsset.obtain("ui/100", 50);
      private final TextureAsset hpBackground = TextureAsset.obtain("ui/9");
      private final TextureAsset hpFill = TextureAsset.obtain("ui/8");
      private final TextureAsset levelBadge = TextureAsset.obtain("ui/220");
      private final BossPreview preview;
      private final Table lootTable = new Table().left();
      private final ScrollPane lootScroll;
      private final Array<LootSlot> lootSlots = new Array<>();
      private final Color statusBorder = new Color();
      private final BossActionButton pathButton;
      private final BossActionButton teleportButton;
      private WorldBossInfo boss;
      private int lootBossId = -1;
      private String statusText = "";
      private String hpText = "";
      private float hpRatio;
      private float statusTimer;

      BossRow(RenderContext ctx) {
         super(ctx);
         this.preview = new BossPreview(ctx);
         this.preview.setBounds(12.0F, 8.0F, 140.0F, 148.0F);
         this.addActor(this.preview);
         this.lootScroll = ScrollPaneFactory.createScrollPane(this.lootTable);
         this.lootScroll.setScrollingDisabled(false, true);
         this.lootScroll.setOverscroll(false, false);
         this.lootScroll.setBounds(LOOT_X, 25.0F, LOOT_W, 114.0F);
         this.addActor(this.lootScroll);

         this.pathButton = new BossActionButton(ctx, 2, "Tìm đường", "100.000 vàng");
         this.pathButton.setBounds(1022.0F, 90.0F, 172.0F, 54.0F);
         this.addActor(this.pathButton);
         this.teleportButton = new BossActionButton(ctx, 1, "Dịch chuyển", "5 lam thạch");
         this.teleportButton.setBounds(1022.0F, 20.0F, 172.0F, 54.0F);
         this.addActor(this.teleportButton);
      }

      void bind(WorldBossInfo boss) {
         this.boss = boss;
         this.statusText = boss == null ? "" : status(boss);
         this.statusTimer = 0.0F;
         this.hpText = boss == null ? "" : hpPair(boss.hp, boss.maxHp);
         this.hpRatio = boss == null || boss.maxHp <= 0L ? 0.0F
            : Math.max(0.0F, Math.min(1.0F, (float) boss.hp / (float) boss.maxHp));
         this.preview.show(boss);
         this.pathButton.bind(boss);
         this.teleportButton.bind(boss);
         int count = boss == null ? 0 : boss.loot.size;
         if (boss == null || boss.id != this.lootBossId || count != this.lootSlots.size) {
            this.lootBossId = boss == null ? -1 : boss.id;
            this.lootTable.clearChildren();
            this.lootSlots.clear();
            for (int i = 0; i < count; i++) {
               LootSlot slot = new LootSlot(this.renderContext);
               this.lootSlots.add(slot);
               this.lootTable.add(slot).size(LOOT_SLOT).spaceRight(LOOT_SPACE);
            }
            this.lootScroll.layout();
            this.lootScroll.setScrollX(0.0F);
         }
         for (int i = 0; i < count; i++) {
            this.lootSlots.get(i).setItem(boss.loot.get(i).item);
         }
      }

      @Override
      public void act(float delta) {
         super.act(delta);
         // The "TRỞ LẠI SAU" countdown ticks every second instead of waiting for the next list packet
         if (this.boss != null && this.boss.state != WorldBossInfo.STATE_ALIVE) {
            this.statusTimer += delta;
            if (this.statusTimer >= 1.0F) {
               this.statusTimer = 0.0F;
               this.statusText = status(this.boss);
            }
         }
      }

      void releasePreview() {
         this.preview.release();
      }

      @Override
      public void draw(Batch batch, float parentAlpha) {
         float x = this.getX();
         float y = this.getY();
         this.renderContext.drawNinePatch(this.card, x, y, this.getWidth(), HEIGHT, Anchor.BOTTOM_LEFT);
         this.renderContext.drawLine(x + 160.0F, y + 12.0F, 2.0F, HEIGHT - 24.0F, DIVIDER);
         this.renderContext.drawLine(x + 550.0F, y + 12.0F, 2.0F, HEIGHT - 24.0F, DIVIDER);
         this.renderContext.drawLine(x + 1005.0F, y + 12.0F, 2.0F, HEIGHT - 24.0F, DIVIDER);

         if (this.boss != null) {
            WorldBossInfo b = this.boss;
            float infoX = x + 168.0F;

            // ── Boss name ──
            this.renderContext.drawTextColorName(FontManager.PLAIN_FONT, b.name, infoX, y + 146.0F,
               Anchor.MIDDLE_LEFT, 24.0F, BOSS_NAME);

            // ── Level badge ──
            this.renderContext.drawTextureSized(this.levelBadge, infoX, y + 84.0F, 96.0F, 36.0F, Anchor.BOTTOM_LEFT);
            this.renderContext.drawTextColorName(FontManager.PLAIN_FONT, "CẤP " + b.level, infoX + 48.0F, y + 102.0F,
               Anchor.CENTER, 16.0F, BROWN);

            // ── State badge (well spaced from the rewards) ──
            this.drawStatusBadge(x + 276.0F, y + 85.0F, b);

            // ── Location ──
            boolean alive = b.state == WorldBossInfo.STATE_ALIVE;
            String location = alive && b.mapName != null && !b.mapName.isEmpty() ? b.mapName : "Chưa xuất hiện";
            this.renderContext.drawTextColorName(FontManager.PLAIN_FONT, "Vị trí:", infoX, y + 64.0F,
               Anchor.MIDDLE_LEFT, 16.0F, ROW_TEXT);
            this.renderContext.drawTextColorName(FontManager.PLAIN_FONT, location, infoX + 65.0F, y + 64.0F,
               Anchor.MIDDLE_LEFT, 17.0F, alive ? "#FFFFFF" : ROW_TEXT);

            // ── HP label + number (lowered slightly, not overlapping the location line) ──
            this.renderContext.drawTextColorName(FontManager.PLAIN_FONT, "MÁU BOSS", infoX, y + 42.0F,
               Anchor.MIDDLE_LEFT, 15.0F, ROW_TEXT);
            this.renderContext.drawText(FontManager.PLAIN_FONT, this.hpText, infoX + 356.0F,
               y + 42.0F, Anchor.MIDDLE_RIGHT, 15.0F, HP_TEXT);

            // ── Health bar at the standard mob bar size (356x20, lowered to y + 11px) ──
            float barX = infoX;
            float barY = y + 11.0F;
            float barW = 356.0F;
            float barH = 20.0F;
            this.renderContext.drawTextureSized(this.hpBackground, barX, barY, barW, barH, Anchor.BOTTOM_LEFT);
            if (this.hpRatio > 0.0F) {
               int fillW = (int) (this.hpFill.getWidth() * this.hpRatio);
               if (fillW > 0) {
                  this.renderContext.drawTextureCropped(this.hpFill, barX + 2.0F, barY + 2.0F,
                     Anchor.BOTTOM_LEFT, 0, 0, fillW, this.hpFill.getHeight());
               }
            }
            // HP percentage on the health bar
            String pctText = Math.round(this.hpRatio * 100.0F) + "%";
            this.renderContext.drawText(FontManager.PLAIN_FONT, pctText, barX + barW / 2.0F,
               barY + barH / 2.0F + 1.0F, Anchor.CENTER, 14.0F, HP_BAR_TEXT);

            // ── Rewards (well spaced, 104px away from the state) ──
            this.renderContext.drawTextColorName(FontManager.PLAIN_FONT, "PHẦN THƯỞNG", x + LOOT_X, y + 146.0F,
               Anchor.MIDDLE_LEFT, 20.0F, REWARD_TITLE);
            this.renderContext.drawTextColorName(FontManager.PLAIN_FONT, b.loot.size + " món", x + 985.0F, y + 146.0F,
               Anchor.MIDDLE_RIGHT, 16.0F, ROW_TEXT);
            if (b.loot.size == 0) {
               this.renderContext.drawTextColorName(FontManager.PLAIN_FONT, "Chưa có phần thưởng", x + 782.0F, y + 82.0F,
                  Anchor.CENTER, 18.0F, ROW_TEXT);
            }
         }
         super.draw(batch, parentAlpha);
      }

      /** 3-color state badge + subtle border, stretched to the right edge of the health bar (x + 168 + 356). */
      private void drawStatusBadge(float x, float y, WorldBossInfo boss) {
         Color fill = statusColor(boss.state);
         float w = 524.0F - (x - this.getX());
         float h = 34.0F;
         this.renderContext.getShapeDrawer().drawFilledRectangle(x, y, w, h, fill);
         // Subtle dark border
         Color border = this.statusBorder.set(fill.r * 0.65F, fill.g * 0.65F, fill.b * 0.65F, 0.45F);
         this.renderContext.getShapeDrawer().drawRectangle(x, y, w, h, border, 1.0F);
         this.renderContext.drawText(FontManager.PLAIN_FONT, this.statusText, x + w / 2.0F, y + h / 2.0F,
            Anchor.CENTER, 14.5F, WHITE);
      }
   }

   /** Reward cell: click to view item info; no space taken by quantity or drop rate. */
   static final class LootSlot extends ItemSlotButton {
      LootSlot(RenderContext ctx) {
         super(ctx);
         this.showCount = false;
         this.showLockedIndicator = false;
         this.itemIconScale = 1.12F;
         this.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
               if (LootSlot.this.getItem() == null) {
                  return;
               }
               ItemInfoPopup popup = GameMain.instance.getLoginScreen().debugScrollPane.findDialog(ItemInfoPopup.class);
               ItemInfoPopup.ActionConfig[] actions = {ItemActionRegistry.CLOSE_TOOLTIP_ACTION};
               popup.showItemWithActions(LootSlot.this, LootSlot.this.getItem(), ItemActionRegistry.CLOSE_TOOLTIP_ACTION, actions);
            }
         });
      }
   }

   /** Both buttons only work while the boss is alive; the cost is shown inside the button. */
   static final class BossActionButton extends ShapeButton {
      private final CompositeSprite sprite = CompositeSprite.obtain("ui/10", "ui/11", "ui/12", false);
      private final int sub;
      private final String title;
      private final String cost;
      private WorldBossInfo boss;

      BossActionButton(RenderContext ctx, int sub, String title, String cost) {
         super(ctx);
         this.sub = sub;
         this.title = title;
         this.cost = cost;
         this.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
               WorldBossInfo current = BossActionButton.this.boss;
               if (current != null && current.state == WorldBossInfo.STATE_ALIVE) {
                  ServerPacketSender.getInstance().sendWorldBoss(BossActionButton.this.sub, current.id);
               }
            }
         });
      }

      void bind(WorldBossInfo boss) {
         this.boss = boss;
         this.setDisabled(boss == null || boss.state != WorldBossInfo.STATE_ALIVE);
      }

      @Override
      public void draw(Batch batch, float parentAlpha) {
         if (this.isDisabled()) {
            this.renderContext.setBatchColor(DISABLED_BUTTON_TINT);
         } else if (this.isPressed()) {
            this.renderContext.setBatchColor(Color.GRAY);
         }
         this.renderContext.drawCompositeSprite(this.sprite, this.getX(), this.getY(), this.getWidth(), this.getHeight(), Anchor.BOTTOM_LEFT);
         this.renderContext.resetBatchColor();
         Color titleColor = this.isDisabled() ? DISABLED_BUTTON_TEXT : Color.WHITE;
         this.renderContext.drawText(FontManager.PLAIN_FONT, this.title, this.getX() + this.getWidth() / 2.0F,
            this.getY() + 36.0F, Anchor.CENTER, 18.0F, titleColor);
         Color costColor = this.isDisabled() ? DISABLED_BUTTON_TEXT : BTN_COST_COLOR;
         this.renderContext.drawText(FontManager.PLAIN_FONT, this.cost, this.getX() + this.getWidth() / 2.0F,
            this.getY() + 15.0F, Anchor.CENTER, 14.0F, costColor);
      }
   }

}

package com.tlnt.ui.window.shop.panel;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Array;
import com.tlnt.core.GameMain;
import com.tlnt.core.GameState;
import com.tlnt.core.audio.FontManager;
import com.tlnt.engine.render.Anchor;
import com.tlnt.engine.render.NinePatchAsset;
import com.tlnt.engine.render.RenderContext;
import com.tlnt.engine.render.TextureAsset;
import com.tlnt.model.item.Item;
import com.tlnt.model.weeklytop.WeeklyTopEntry;
import com.tlnt.model.weeklytop.WeeklyTopEvent;
import com.tlnt.net.task.ServerPacketSender;
import com.tlnt.ui.hud.itemslot.ItemSlotButton;
import com.tlnt.ui.widget.BaseTable;
import com.tlnt.ui.widget.ScrollPaneFactory;
import com.tlnt.ui.widget.ShapeButton;
import com.tlnt.ui.window.GamePanel;
import com.tlnt.ui.window.action.ItemActionMenuPopup;
import com.tlnt.ui.window.iteminfo.ItemInfoPopup;
import com.tlnt.ui.window.action.ItemActionRegistry;
import com.tlnt.ui.window.action.OpenItemTooltipAction;
import com.tlnt.util.TimeFormatter;
import com.tlnt.world.entity.player.PlayerEntity;

/** Port of the reference client's Weekly Top tab, including its original layout and textures. */
public final class WeeklyTopPanel extends GamePanel {
   private final LeaderboardPanel leaderboardPanel;
   private final RewardPanel rewardPanel;
   private WeeklyTopEvent selectedEvent;

   public WeeklyTopPanel(RenderContext ctx) {
      super(ctx);
      this.leaderboardPanel = new LeaderboardPanel(ctx);
      this.rewardPanel = new RewardPanel(ctx);
      this.add(this.leaderboardPanel).size(756.0F, 610.0F).space(15.0F);
      this.add(this.rewardPanel).size(476.0F, 610.0F).space(15.0F);
   }

   @Override
   public void onShow() {
      this.refresh();
      if (this.selectedEvent != null) {
         ServerPacketSender.getInstance().sendRequestWeeklyTop(this.selectedEvent.id);
      }
   }

   @Override
   public void refresh() {
      int selectedId = this.selectedEvent == null ? -1 : this.selectedEvent.id;
      this.selectedEvent = findEvent(selectedId);
      if (this.selectedEvent == null && GameState.weeklyTopEvents.size > 0) {
         this.selectedEvent = GameState.weeklyTopEvents.first();
      }

      this.leaderboardPanel.refresh(this.selectedEvent);
      this.rewardPanel.refresh(this.selectedEvent);
   }

   private static WeeklyTopEvent findEvent(int id) {
      for (WeeklyTopEvent event : GameState.weeklyTopEvents) {
         if (event.id == id) {
            return event;
         }
      }
      return null;
   }

   private void selectEvent(WeeklyTopEvent event) {
      if (event == null) {
         return;
      }
      this.selectedEvent = event;
      this.refresh();
      ServerPacketSender.getInstance().sendRequestWeeklyTop(event.id);
   }

   private final class LeaderboardPanel extends BaseTable {
      private final NinePatchAsset background = NinePatchAsset.obtain("ui/100", 50);
      private final Table eventButtonsTable = new Table().left();
      private final Table entriesTable = new Table().top();

      LeaderboardPanel(RenderContext ctx) {
         super(ctx);
         ScrollPane eventScroll = ScrollPaneFactory.createScrollPane(this.eventButtonsTable);
         eventScroll.setSize(726.0F, 55.0F);
         eventScroll.setPosition(15.0F, 545.0F);
         this.addActor(eventScroll);

         ScrollPane entryScroll = ScrollPaneFactory.createScrollPane(this.entriesTable);
         entryScroll.setSize(726.0F, 473.0F);
         entryScroll.setPosition(15.0F, 15.0F);
         this.addActor(entryScroll);
      }

      /** Table currently drawn (compared by reference): buttons are rebuilt only when the data actually changes. */
      private final Array<WeeklyTopEvent> shownEvents = new Array<>();
      private final Array<WeeklyTopEntry> shownEntries = new Array<>();

      /**
       * refresh() runs after EVERY packet the client receives (refreshHudState). It used to create new buttons every time -> the button being
       * pressed was removed from the table mid-press (lost clicks, flicker). Now it only rebuilds when the table list / rows change.
       */
      void refresh(WeeklyTopEvent selected) {
         if (!sameItems(this.shownEvents, GameState.weeklyTopEvents)) {
            this.shownEvents.clear();
            this.shownEvents.addAll(GameState.weeklyTopEvents);
            this.eventButtonsTable.clearChildren();
            for (WeeklyTopEvent event : GameState.weeklyTopEvents) {
               this.eventButtonsTable.add(new EventButton(event)).size(174.0F, 55.0F).space(10.0F);
            }
         }

         Array<WeeklyTopEntry> entries = selected == null ? EMPTY_ENTRIES : selected.entries;
         if (!sameItems(this.shownEntries, entries)) {
            this.shownEntries.clear();
            this.shownEntries.addAll(entries);
            this.entriesTable.clearChildren();
            for (WeeklyTopEntry entry : entries) {
               this.entriesTable.add(new EntryRow(entry)).size(726.0F, 71.0F).row();
            }
         }
      }

      @Override
      public void draw(Batch batch, float parentAlpha) {
         float x = this.getX();
         this.renderContext.drawNinePatch(this.background, x, this.getY(), this.getWidth(), this.getHeight(), Anchor.BOTTOM_LEFT);
         this.renderContext.drawText(
            FontManager.DEFAULT_FONT, "Hạng", x + 59.0F, 508.0F, Anchor.BOTTOM_LEFT, 22.0F, Color.WHITE
         );
         this.renderContext.drawText(
            FontManager.DEFAULT_FONT, "Tên nhân vật", x + 275.0F, 508.0F, Anchor.BOTTOM_LEFT, 22.0F, Color.WHITE
         );
         this.renderContext.drawText(
            FontManager.DEFAULT_FONT, "Điểm số", x + 584.0F, 508.0F, Anchor.BOTTOM_LEFT, 22.0F, Color.WHITE
         );
         super.draw(batch, parentAlpha);
      }
   }

   private final class EventButton extends ShapeButton {
      private final WeeklyTopEvent event;

      EventButton(WeeklyTopEvent event) {
         super(WeeklyTopPanel.this.renderContext);
         this.event = event;
         this.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent inputEvent, float x, float y) {
               WeeklyTopPanel.this.selectEvent(EventButton.this.event);
            }
         });
      }

      @Override
      public void draw(Batch batch, float parentAlpha) {
         if (this.isPressed()) {
            WeeklyTopPanel.this.renderContext.setBatchColor(Color.GRAY);
         }
         TextureAsset background = TextureAsset.obtain(this.event == WeeklyTopPanel.this.selectedEvent ? "ui/220" : "ui/221");
         WeeklyTopPanel.this.renderContext.drawTextureSized(
            background, this.getX(), this.getY(), this.getWidth(), this.getHeight(), Anchor.BOTTOM_LEFT
         );
         WeeklyTopPanel.this.renderContext.drawTextColorName(
            FontManager.PLAIN_FONT,
            this.event.name,
            this.getX() + this.getWidth() / 2.0F,
            this.getY() + this.getHeight() / 2.0F,
            Anchor.CENTER,
            24.0F,
            "#8B4E23"
         );
         WeeklyTopPanel.this.renderContext.resetBatchColor();
      }
   }

   private final class EntryRow extends ShapeButton {
      private final WeeklyTopEntry entry;

      EntryRow(WeeklyTopEntry entry) {
         super(WeeklyTopPanel.this.renderContext);
         this.entry = entry;
         this.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent inputEvent, float x, float y) {
               Vector2 stagePosition = EntryRow.this.localToStageCoordinates(new Vector2(x, y));
               ItemActionMenuPopup popup = GameMain.instance.getLoginScreen().debugScrollPane.findDialog(ItemActionMenuPopup.class);
               ItemActionMenuPopup.ActionType[] actions = {
                  ItemActionMenuPopup.ActionType.ENCHANT_ITEM.withItemId(EntryRow.this.entry.playerId),
                  ItemActionMenuPopup.ActionType.FORGE_UPGRADE.withItemId(EntryRow.this.entry.playerId)
               };
               popup.showWithActions(stagePosition.x, stagePosition.y, 10, actions);
            }
         });
      }

      @Override
      public void draw(Batch batch, float parentAlpha) {
         if (this.isPressed()) {
            WeeklyTopPanel.this.renderContext.setBatchColor(Color.GRAY);
         }
         TextureAsset background = TextureAsset.obtain(isLocalPlayer(this.entry.playerId) ? "ui/219" : "ui/218");
         WeeklyTopPanel.this.renderContext.drawTextureSized(
            background, this.getX(), this.getY(), this.getWidth(), this.getHeight(), Anchor.BOTTOM_LEFT
         );
         float centerY = this.getY() + this.getHeight() / 2.0F;
         WeeklyTopPanel.this.renderContext.drawText(
            FontManager.DEFAULT_FONT, Integer.toString(this.entry.rank), this.getX() + 75.0F, centerY, Anchor.CENTER, 22.0F, Color.WHITE
         );
         WeeklyTopPanel.this.renderContext.drawText(
            FontManager.DEFAULT_FONT, this.entry.playerName, this.getX() + 325.0F, centerY, Anchor.CENTER, 22.0F, Color.WHITE
         );
         WeeklyTopPanel.this.renderContext.drawText(
            FontManager.DEFAULT_FONT, this.entry.score, this.getX() + 610.0F, centerY, Anchor.CENTER, 22.0F, Color.WHITE
         );
         WeeklyTopPanel.this.renderContext.resetBatchColor();
      }
   }

   private static final Array<WeeklyTopEntry> EMPTY_ENTRIES = new Array<>();

   /** Same element count and the same reference at every index. */
   private static <T> boolean sameItems(Array<T> shown, Array<T> current) {
      if (shown.size != current.size) {
         return false;
      }
      for (int i = 0; i < shown.size; i++) {
         if (shown.get(i) != current.get(i)) {
            return false;
         }
      }
      return true;
   }

   private final class RewardPanel extends BaseTable {
      private final NinePatchAsset background = NinePatchAsset.obtain("ui/100", 50);
      private final Table firstPlaceItems = new Table().left().pad(4.0F);
      private final Table secondPlaceItems = new Table().left().pad(4.0F);
      private final Table thirdPlaceItems = new Table().left().pad(4.0F);

      RewardPanel(RenderContext ctx) {
         super(ctx);
         this.addRewardScroll(this.firstPlaceItems, 390.0F);
         this.addRewardScroll(this.secondPlaceItems, 236.0F);
         this.addRewardScroll(this.thirdPlaceItems, 82.0F);
      }

      private void addRewardScroll(Table items, float y) {
         ScrollPane scroll = ScrollPaneFactory.createScrollPane(items);
         scroll.setSize(454.0F, 112.0F);
         scroll.setPosition(11.0F, y);
         this.addActor(scroll);
      }

      private WeeklyTopEvent shownEvent;
      private int shownRewardCount = -1;

      void refresh(WeeklyTopEvent selected) {
         int count = selected == null ? 0
            : selected.firstPlaceRewards.size + selected.secondPlaceRewards.size + selected.thirdPlaceRewards.size;
         if (selected == this.shownEvent && count == this.shownRewardCount) {
            return; // rewards only change when another table is selected / 73/0 is received again
         }
         this.shownEvent = selected;
         this.shownRewardCount = count;
         this.setItems(this.firstPlaceItems, selected == null ? null : selected.firstPlaceRewards);
         this.setItems(this.secondPlaceItems, selected == null ? null : selected.secondPlaceRewards);
         this.setItems(this.thirdPlaceItems, selected == null ? null : selected.thirdPlaceRewards);
      }

      private void setItems(Table table, Array<Item> rewards) {
         table.clearChildren();
         if (rewards == null) {
            return;
         }
         for (Item item : rewards) {
            ItemSlotButton button = new ItemSlotButton(this.renderContext);
            button.setItem(item);
            button.addListener(new RewardItemClickListener(button));
            table.add(button).size(104.0F).space(10.0F);
         }
      }

      @Override
      public void draw(Batch batch, float parentAlpha) {
         float centerX = this.getX() + this.getWidth() / 2.0F;
         float y = this.getY();
         this.renderContext.drawNinePatch(this.background, this.getX(), y, this.getWidth(), this.getHeight(), Anchor.BOTTOM_LEFT);
         this.renderContext.drawTextColorName(
            FontManager.DEFAULT_FONT, "Phần thưởng", centerX, y + 566.0F, Anchor.BOTTOM_CENTER, 30.0F, "#FFF600"
         );
         this.renderContext.drawText(
            FontManager.DEFAULT_FONT, "Thưởng TOP 1", centerX, y + 513.0F, Anchor.BOTTOM_CENTER, 22.0F, Color.WHITE
         );
         this.renderContext.drawText(
            FontManager.DEFAULT_FONT, "Thưởng TOP 2", centerX, y + 358.0F, Anchor.BOTTOM_CENTER, 22.0F, Color.WHITE
         );
         this.renderContext.drawText(
            FontManager.DEFAULT_FONT, "Thưởng TOP 3", centerX, y + 205.0F, Anchor.BOTTOM_CENTER, 22.0F, Color.WHITE
         );
         if (WeeklyTopPanel.this.selectedEvent != null) {
            this.renderContext.drawText(
               FontManager.DEFAULT_FONT,
               "Kết thúc: " + TimeFormatter.formatTimeRemaining(WeeklyTopPanel.this.selectedEvent.endTime),
               centerX,
               y + 25.0F,
               Anchor.CENTER,
               22.0F,
               Color.WHITE
            );
         }
         super.draw(batch, parentAlpha);
      }
   }

   private final class RewardItemClickListener extends ClickListener {
      private final ItemSlotButton button;

      RewardItemClickListener(ItemSlotButton button) {
         this.button = button;
      }

      @Override
      public void clicked(InputEvent event, float x, float y) {
         ItemInfoPopup popup = GameMain.instance.getLoginScreen().debugScrollPane.findDialog(ItemInfoPopup.class);
         OpenItemTooltipAction closeAction = ItemActionRegistry.CLOSE_TOOLTIP_ACTION;
         ItemInfoPopup.ActionConfig[] actions = {ItemActionRegistry.CLOSE_TOOLTIP_ACTION};
         popup.showItemWithActions(this.button, this.button.getItem(), closeAction, actions);
      }
   }

   private static boolean isLocalPlayer(int playerId) {
      PlayerEntity player = PlayerEntity.getInstance();
      return player != null && player.getId() == playerId;
   }
}

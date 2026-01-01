package com.tlnt.ui.hud;

import com.github.tommyettinger.textra.TypingLabel;
import com.tlnt.core.GameState;
import com.tlnt.core.audio.FontManager;
import com.tlnt.engine.WorldConfig;
import com.tlnt.engine.font.FontFactory;
import com.tlnt.engine.render.RenderContext;
import com.tlnt.ui.hud.widget.AnnouncementBar;
import com.tlnt.ui.hud.widget.AutoAttackButton;
import com.tlnt.ui.hud.widget.AvatarButton;
import com.tlnt.ui.hud.widget.BuffBar;
import com.tlnt.ui.hud.widget.ChatButton;
import com.tlnt.ui.hud.widget.EntityHpBarPanel;
import com.tlnt.ui.hud.widget.ExitButton;
import com.tlnt.ui.hud.widget.HudSkillButton1;
import com.tlnt.ui.hud.widget.HudSkillButton3;
import com.tlnt.ui.hud.widget.JoystickPanel;
import com.tlnt.ui.hud.widget.MapToggleButton;
import com.tlnt.ui.hud.widget.MenuButtonPanel;
import com.tlnt.ui.hud.widget.MenuToggleButton;
import com.tlnt.ui.hud.widget.MessageNotificationButton;
import com.tlnt.ui.hud.widget.MessageTicker;
import com.tlnt.ui.hud.widget.MpPotionSlotButton;
import com.tlnt.ui.hud.widget.NearbyInteractButton;
import com.tlnt.ui.hud.widget.PickupButton;
import com.tlnt.ui.hud.widget.PlayerIconPopupButton;
import com.tlnt.ui.hud.widget.PlayerNameButton;
import com.tlnt.ui.hud.widget.PlayerPortrait;
import com.tlnt.ui.hud.widget.PopupMenu;
import com.tlnt.ui.hud.widget.QuestPartyPanel;
import com.tlnt.ui.hud.widget.QuestPartyToggleButton;
import com.tlnt.ui.hud.widget.RegionButton;
import com.tlnt.ui.hud.widget.SkeletonActor;
import com.tlnt.ui.hud.widget.SkillBar;
import com.tlnt.ui.hud.widget.TargetMobHpBar;
import com.tlnt.ui.hud.widget.TargetPortrait;
import com.tlnt.ui.window.GamePanel;
import com.tlnt.world.entity.player.PlayerEntity;

public final class GameHud extends GamePanel {
   public EntityHpBarPanel entityHpBarPanel;
   public TargetPortrait targetPortrait;
   public ExitButton exitButton;
   public MapToggleButton mapToggleButton;
   public AnnouncementBar announcementBar;
   public QuestPartyPanel questPartyPanel;
   public QuestPartyToggleButton questPartyToggleButton;
   public JoystickPanel joystickPanel;
   public MessageNotificationButton messageNotificationButton;
   public SkillBar skillBar;
   public PlayerPortrait playerPortrait;
   public TargetMobHpBar targetMobHpBar;
   public PopupMenu popupMenu;
   public PlayerIconPopupButton playerIconPopupButton;
   public BuffBar buffBar;
   public PlayerNameButton playerNameButton;
   public RegionButton regionButton;
   public MenuToggleButton menuToggleButton;
   public MenuButtonPanel menuButtonPanel;
   public PickupButton pickupButton;
   public AvatarButton avatarButton;
   public NearbyInteractButton nearbyInteractButton;
   public AutoAttackButton autoAttackButton;
   public HudSkillButton3 hudSkillButton3;
   public MpPotionSlotButton mpPotionSlotButton;
   public ChatButton chatButton;
   public HudSkillButton1 hudSkillButton1;
   public MessageTicker messageTicker;
   public TypingLabel autoAttackLabel;
   /** "Auto-pathing" while auto-walking along the quest arrow. */
   public TypingLabel autoPathLabel;
   public SkeletonActor eventSkeletonActor;

   public GameHud(RenderContext renderContext) {
      super(renderContext);
      this.addActor(this.entityHpBarPanel = new EntityHpBarPanel(super.renderContext));
      this.addActor(this.targetPortrait = new TargetPortrait(super.renderContext));
      this.addActor(this.exitButton = new ExitButton(super.renderContext));
      this.addActor(this.mapToggleButton = new MapToggleButton(super.renderContext));
      this.addActor(this.announcementBar = new AnnouncementBar(super.renderContext));
      this.addActor(this.questPartyPanel = new QuestPartyPanel(super.renderContext));
      this.addActor(this.questPartyToggleButton = new QuestPartyToggleButton(super.renderContext, this.questPartyPanel));
      this.addActor(this.joystickPanel = new JoystickPanel(super.renderContext));
      this.addActor(this.messageNotificationButton = new MessageNotificationButton(super.renderContext));
      this.addActor(this.skillBar = new SkillBar(super.renderContext));
      this.addActor(this.playerPortrait = new PlayerPortrait(super.renderContext));
      this.addActor(this.targetMobHpBar = new TargetMobHpBar(super.renderContext));
      this.addActor(this.popupMenu = new PopupMenu(super.renderContext));
      this.addActor(this.playerIconPopupButton = new PlayerIconPopupButton(super.renderContext, this.popupMenu));
      this.addActor(this.buffBar = new BuffBar(super.renderContext));
      this.addActor(this.playerNameButton = new PlayerNameButton(super.renderContext));
      this.addActor(this.regionButton = new RegionButton(super.renderContext));
      this.addActor(this.menuToggleButton = new MenuToggleButton(super.renderContext));
      this.addActor(this.menuButtonPanel = new MenuButtonPanel(super.renderContext));
      this.menuToggleButton.addListener(new MenuToggleClickListener(this));
      this.addActor(this.pickupButton = new PickupButton(super.renderContext));
      this.addActor(this.avatarButton = new AvatarButton(super.renderContext));
      this.addActor(this.nearbyInteractButton = new NearbyInteractButton(super.renderContext));
      this.addActor(this.autoAttackButton = new AutoAttackButton(super.renderContext));
      this.addActor(this.hudSkillButton3 = new HudSkillButton3(super.renderContext));
      this.addActor(this.mpPotionSlotButton = new MpPotionSlotButton(super.renderContext));
      this.addActor(this.chatButton = new ChatButton(super.renderContext));
      this.addActor(this.hudSkillButton1 = new HudSkillButton1(1, super.renderContext));
      this.addActor(this.messageTicker = new MessageTicker(super.renderContext));
      this.autoAttackLabel = new TypingLabel(
         "{JUMP=0.5;2.0;1.0}[#FFF600]Tự động đánh", FontFactory.getCachedTextraFont(FontManager.DEFAULT_FONT, 24)
      );
      this.autoAttackLabel.setAlignment(1);
      this.autoAttackLabel
         .setPosition(
            (float)(WorldConfig.VIEWPORT_WIDTH / 2) - this.autoAttackLabel.getWidth() / 2.0F,
            (float)(WorldConfig.VIEWPORT_HEIGHT / 4)
         );
      this.addActor(this.autoAttackLabel);
      this.autoAttackLabel.setVisible(false);
      this.autoPathLabel = new TypingLabel(
         "{JUMP=0.5;2.0;1.0}[#FFF600]Tự tìm đường", FontFactory.getCachedTextraFont(FontManager.DEFAULT_FONT, 24)
      );
      this.autoPathLabel.setAlignment(1);
      this.autoPathLabel
         .setPosition(
            (float)(WorldConfig.VIEWPORT_WIDTH / 2) - this.autoPathLabel.getWidth() / 2.0F,
            (float)(WorldConfig.VIEWPORT_HEIGHT / 4)
         );
      this.addActor(this.autoPathLabel);
      this.autoPathLabel.setVisible(false);
      this.eventSkeletonActor = new SkeletonActor(super.renderContext, "ui/cooldown", 5.0F);
      this.eventSkeletonActor
         .setPosition(
            (float)(WorldConfig.VIEWPORT_WIDTH / 2),
            (float)(WorldConfig.VIEWPORT_HEIGHT / 2)
         );
      this.addActor(this.eventSkeletonActor);
      this.initOffscreenTeammates();
   }

   @Override
   public final void onUpdate(float delta) {
      this.chatButton.setVisible(GameState.appleEventState.active);
      this.autoAttackLabel.setVisible(PlayerEntity.getInstance().autoTargetController.projectile);
      this.autoPathLabel.setVisible(
         PlayerEntity.getInstance().offscreenArrow != null && PlayerEntity.getInstance().offscreenArrow.isAutoPathing()
      );
   }

   // ---- off-screen teammates (decompiled C3617: 2 columns of yellow names on the left / right edges, rebuilt every frame)

   private final com.badlogic.gdx.scenes.scene2d.ui.Table offscreenLeft = new com.badlogic.gdx.scenes.scene2d.ui.Table().center();
   private final com.badlogic.gdx.scenes.scene2d.ui.Table offscreenRight = new com.badlogic.gdx.scenes.scene2d.ui.Table().center();
   private final com.badlogic.gdx.utils.Pool<com.github.tommyettinger.textra.TextraLabel> offscreenLabels =
      new com.badlogic.gdx.utils.Pool<com.github.tommyettinger.textra.TextraLabel>() {
         @Override
         protected com.github.tommyettinger.textra.TextraLabel newObject() {
            return new com.github.tommyettinger.textra.TextraLabel("", FontFactory.getCachedTextraFont(FontManager.DEFAULT_FONT, 24));
         }
      };
   private boolean offscreenAdded;

   private void initOffscreenTeammates() {
      this.offscreenLeft.setSize(0.0F, (float)WorldConfig.VIEWPORT_HEIGHT);
      this.offscreenLeft.setPosition(100.0F, 150.0F);
      this.addActor(this.offscreenLeft);
      this.offscreenRight.setSize(0.0F, (float)WorldConfig.VIEWPORT_HEIGHT);
      this.offscreenRight.setPosition((float)(WorldConfig.VIEWPORT_WIDTH - 100), 150.0F);
      this.addActor(this.offscreenRight);
   }

   /** Name of a teammate outside the camera view ({@code PlayerNameLabel}, while drawing the world): shown in the {@code left} side column. */
   public final void addOffscreenTeammate(String name, boolean left, float width) {
      com.github.tommyettinger.textra.TextraLabel label = this.offscreenLabels.obtain();
      label.setText("[#FFF600]" + name);
      label.setAlignment(left ? 10 : 18);
      (left ? this.offscreenLeft : this.offscreenRight).add(label).width(width).height(40.0F).row();
      this.offscreenAdded = true;
   }

   @Override
   public void draw(com.badlogic.gdx.graphics.g2d.Batch batch, float parentAlpha) {
      super.draw(batch, parentAlpha);
      if (this.offscreenAdded) { // only shown for this frame: added again when the world is drawn in the next frame
         this.offscreenAdded = false;
         clearOffscreen(this.offscreenLeft);
         clearOffscreen(this.offscreenRight);
      }
   }

   private void clearOffscreen(com.badlogic.gdx.scenes.scene2d.ui.Table table) {
      for (com.badlogic.gdx.scenes.scene2d.Actor child : table.getChildren()) {
         if (child instanceof com.github.tommyettinger.textra.TextraLabel) {
            this.offscreenLabels.free((com.github.tommyettinger.textra.TextraLabel)child);
         }
      }
      table.clear();
   }
}

package com.tlnt.screen.login;

import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.tlnt.core.GameState;
import com.tlnt.core.Settings;
import com.tlnt.core.audio.FontManager;
import com.tlnt.engine.WorldConfig;
import com.tlnt.engine.camera.CameraController;
import com.tlnt.engine.camera.FreeCameraController;
import com.tlnt.engine.input.ActorDebugInspector;
import com.tlnt.engine.input.CameraPanHandler;
import com.tlnt.engine.input.InputHandler;
import com.tlnt.engine.input.ShortcutAssignInputHandler;
import com.tlnt.engine.physics.EntityType;
import com.tlnt.engine.render.Anchor;
import com.tlnt.engine.render.RenderContext;
import com.tlnt.engine.render.TextureAsset;
import com.tlnt.screen.BaseScreen;
import com.tlnt.screen.login.scene.LoginSceneController;
import com.tlnt.ui.hud.widget.AutoAttackButton;
import com.tlnt.util.GameUtils;
import com.tlnt.world.entity.GameHudController;
import com.tlnt.world.entity.WorldEntity;
import com.tlnt.world.entity.animal.AnimalEntity;
import com.tlnt.world.entity.butterfly.AmbientButterfly;
import com.tlnt.world.entity.decoration.WaypointEntity;
import com.tlnt.world.entity.decoration.WorldMapEntity;
import com.tlnt.world.entity.mapitem.MapItemEntity;
import com.tlnt.world.entity.mob.MobEntity;
import com.tlnt.world.entity.plant.PlantEntity;
import com.tlnt.world.entity.npc.NpcEntity;
import com.tlnt.world.entity.player.PlayerEntity;
import com.tlnt.world.groundeffect.ClickEffect;

public final class LoginScreen extends BaseScreen {
   public boolean active;
   public GameHudController debugScrollPane;
   public CameraController box2dDebugRenderer;
   public WorldMapEntity editorViewport;
   public LoginSceneController loginSceneController;
   public final Vector2 cameraTarget = new Vector2();

   @Override
   public final void onShow() {
      this.active = true;
      if (PlayerEntity.getInstance() != null) {
         if (PlayerEntity.getInstance().POOL2.isAlive) {
            this.box2dDebugRenderer.setFocusPoint(PlayerEntity.getInstance().getPosition().x, PlayerEntity.getInstance().getPosition().y);
         }

         PlayerEntity.getInstance().movementController.stopMovement(false);
      }
   }

   @Override
   public final void onCreate() {
      this.loginSceneController = new LoginSceneController(this);
      new FreeCameraController(this);
      this.editorViewport = new WorldMapEntity(this);
      this.debugScrollPane = new GameHudController(this);
      this.box2dDebugRenderer = new CameraController(this);
      int i = 0;

      while (true) {
         if (i >= 3) {
            return;
         }

         new AmbientButterfly(this);
         i++;
      }
   }

   @Override
   public final InputMultiplexer createInputMultiplexer() {
      InputMultiplexer multiplexer;
      (multiplexer = new InputMultiplexer()).addProcessor(new ShortcutAssignInputHandler());
      multiplexer.addProcessor(new ActorDebugInspector(this.getStage()));
      multiplexer.addProcessor(this.getStage());
      multiplexer.addProcessor(new CameraPanHandler());
      multiplexer.addProcessor(InputHandler.getInstance());
      multiplexer.addProcessor(this);
      return multiplexer;
   }

   @Override
   public final void onUpdate(float delta) {
      GameState.validateTargetEntity();
      if (PlayerEntity.getInstance() != null && PlayerEntity.getInstance().POOL2.isAlive) {
         InputHandler.getInstance();
         if (InputHandler.isKeyPressed(Settings.keybindings[0])) {
            this.cameraTarget.y += 5.0F * delta;
         }

         InputHandler.getInstance();
         if (InputHandler.isKeyPressed(Settings.keybindings[2])) {
            this.cameraTarget.y -= 5.0F * delta;
         }

         InputHandler.getInstance();
         if (InputHandler.isKeyPressed(Settings.keybindings[1])) {
            this.cameraTarget.x -= 5.0F * delta;
         }

         InputHandler.getInstance();
         if (InputHandler.isKeyPressed(Settings.keybindings[3])) {
            this.cameraTarget.x += 5.0F * delta;
         }

         Vector2 moveDir;
         if ((moveDir = InputHandler.getInstance().getMoveDirection()) != null && moveDir.len2() > 0.010000001F) {
            if ((moveDir = new Vector2(moveDir)).len2() > 1.0F) {
               moveDir.nor();
            }

            this.cameraTarget.x = this.cameraTarget.x + moveDir.x * 5.0F * delta;
            this.cameraTarget.y = this.cameraTarget.y + moveDir.y * 5.0F * delta;
         }

         float mapWidth = this.editorViewport.getMapWidth() / 100.0F;
         float mapHeight = this.editorViewport.getMapHeight() / 100.0F;
         OrthographicCamera camera;
         float viewWidth = (camera = this.getWorldCamera()).viewportWidth * camera.zoom;
         float viewHeight = camera.viewportHeight * camera.zoom;
         if (mapWidth <= viewWidth) {
            this.cameraTarget.x = mapWidth / 2.0F;
         } else {
            viewWidth /= 2.0F;
            this.cameraTarget.x = Math.max(viewWidth, Math.min(this.cameraTarget.x, mapWidth - viewWidth));
         }

         if (mapHeight <= viewHeight) {
            this.cameraTarget.y = mapHeight / 2.0F;
         } else {
            viewWidth = viewHeight / 2.0F;
            this.cameraTarget.y = Math.max(viewWidth, Math.min(this.cameraTarget.y, mapHeight - viewWidth));
         }

         this.box2dDebugRenderer.setFocusPoint(this.cameraTarget.x, this.cameraTarget.y);
      }

      this.loginSceneController.update(delta);
   }

   public static boolean cycleNearbyTarget() {
      Array candidates = new Array();
      Array.ArrayIterator it = GameState.nearbyPlayers.iterator();

      while (it.hasNext()) {
         PlayerEntity player;
         if (!(player = (PlayerEntity)it.next()).isLocalPlayer() && player.distanceTo(PlayerEntity.getInstance()) < 5.0F) {
            candidates.add(player);
         }
      }

      it = GameState.mapItems.iterator();

      while (it.hasNext()) {
         MapItemEntity mapItem;
         if ((mapItem = (MapItemEntity)it.next()).distanceTo(PlayerEntity.getInstance()) < 5.0F) {
            candidates.add(mapItem);
         }
      }

      it = GameState.nearbyMobs.iterator();

      while (it.hasNext()) {
         MobEntity mob;
         if (!(mob = (MobEntity)it.next()).isDead() && mob.distanceTo(PlayerEntity.getInstance()) < 5.0F) {
            candidates.add(mob);
         }
      }

      it = GameState.nearbyNpcs.iterator();

      while (it.hasNext()) {
         NpcEntity plant;
         if (!(plant = (NpcEntity)it.next()).isDead() && plant.distanceTo(PlayerEntity.getInstance()) < 5.0F) {
            candidates.add(plant);
         }
      }

      it = GameState.nearbyAnimals.iterator();

      while (it.hasNext()) {
         AnimalEntity animal;
         if ((animal = (AnimalEntity)it.next()).distanceTo(PlayerEntity.getInstance()) < 5.0F) {
            candidates.add(animal);
         }
      }

      it = GameState.nearbyPlants.iterator();

      while (it.hasNext()) {
         PlantEntity npc;
         if ((npc = (PlantEntity)it.next()).distanceTo(PlayerEntity.getInstance()) < 5.0F) {
            candidates.add(npc);
         }
      }

      if (candidates.isEmpty()) {
         if (GameState.targetEntity != null) {
            GameState.targetEntity = null;
         }

         return false;
      } else {
         int currentIndex;
         if ((currentIndex = candidates.indexOf(GameState.targetEntity, true)) >= candidates.size - 1) {
            GameState.setTargetEntity((WorldEntity)candidates.first());
         } else {
            GameState.setTargetEntity((WorldEntity)candidates.get(currentIndex + 1));
         }

         return true;
      }
   }

   @Override
   public final void renderOverlay(RenderContext ctx, float delta) {
      super.renderOverlay(ctx, delta);
      ctx.beginHudBatch();
      if (Settings.isCapturingKeybind) {
         ctx.drawLine(0.0F, 0.0F, (float)WorldConfig.VIEWPORT_WIDTH, (float)WorldConfig.VIEWPORT_HEIGHT, new Color(0.0F, 1.0F, 1.0F, 0.5F));
      } else if (Settings.assigningStaminaFoodSlot) {
         ctx.drawLine(0.0F, 0.0F, (float)WorldConfig.VIEWPORT_WIDTH, (float)WorldConfig.VIEWPORT_HEIGHT, new Color(0.0F, 0.0F, 0.0F, 0.8F));
         delta = this.debugScrollPane.POOL2.hudSkillButton1.getX() + this.debugScrollPane.POOL2.hudSkillButton1.getWidth() / 2.0F;
         float centerY = this.debugScrollPane.POOL2.hudSkillButton1.getY() + this.debugScrollPane.POOL2.hudSkillButton1.getHeight() / 2.0F;
         float width = GameUtils.oscillate(
            0.5F, this.debugScrollPane.POOL2.hudSkillButton1.getWidth(), this.debugScrollPane.POOL2.hudSkillButton1.getWidth() + 20.0F
         );
         float height = GameUtils.oscillate(
            0.5F, this.debugScrollPane.POOL2.hudSkillButton1.getHeight(), this.debugScrollPane.POOL2.hudSkillButton1.getHeight() + 20.0F
         );
         ctx.drawTextureSized(TextureAsset.obtain("ui/35"), delta, centerY, width, height, Anchor.CENTER);
         ctx.drawText(FontManager.DEFAULT_FONT, "Phím tắt", delta, centerY, Anchor.CENTER, 22.0F, Color.WHITE);
      } else if (Settings.assigningHpPotionSlot) {
         ctx.drawLine(0.0F, 0.0F, (float)WorldConfig.VIEWPORT_WIDTH, (float)WorldConfig.VIEWPORT_HEIGHT, new Color(0.0F, 0.0F, 0.0F, 0.8F));
         delta = this.debugScrollPane.POOL2.hudSkillButton3.getX() + this.debugScrollPane.POOL2.hudSkillButton3.getWidth() / 2.0F;
         float hpCenterY = this.debugScrollPane.POOL2.hudSkillButton3.getY() + this.debugScrollPane.POOL2.hudSkillButton3.getHeight() / 2.0F;
         float hpWidth = GameUtils.oscillate(
            0.5F, this.debugScrollPane.POOL2.hudSkillButton3.getWidth(), this.debugScrollPane.POOL2.hudSkillButton3.getWidth() + 20.0F
         );
         float hpHeight = GameUtils.oscillate(
            0.5F, this.debugScrollPane.POOL2.hudSkillButton3.getHeight(), this.debugScrollPane.POOL2.hudSkillButton3.getHeight() + 20.0F
         );
         ctx.drawTextureSized(TextureAsset.obtain("ui/28"), delta, hpCenterY, hpWidth, hpHeight, Anchor.CENTER);
      } else if (Settings.assigningMpPotionSlot) {
         ctx.drawLine(0.0F, 0.0F, (float)WorldConfig.VIEWPORT_WIDTH, (float)WorldConfig.VIEWPORT_HEIGHT, new Color(0.0F, 0.0F, 0.0F, 0.8F));
         delta = this.debugScrollPane.POOL2.mpPotionSlotButton.getX() + this.debugScrollPane.POOL2.mpPotionSlotButton.getWidth() / 2.0F;
         float mpCenterY = this.debugScrollPane.POOL2.mpPotionSlotButton.getY() + this.debugScrollPane.POOL2.mpPotionSlotButton.getHeight() / 2.0F;
         float mpWidth = GameUtils.oscillate(
            0.5F, this.debugScrollPane.POOL2.mpPotionSlotButton.getWidth(), this.debugScrollPane.POOL2.mpPotionSlotButton.getWidth() + 20.0F
         );
         float mpHeight = GameUtils.oscillate(
            0.5F, this.debugScrollPane.POOL2.mpPotionSlotButton.getHeight(), this.debugScrollPane.POOL2.mpPotionSlotButton.getHeight() + 20.0F
         );
         ctx.drawTextureSized(TextureAsset.obtain("ui/27"), delta, mpCenterY, mpWidth, mpHeight, Anchor.CENTER);
      }

      if (this.loginSceneController != null) {
         this.loginSceneController.render(ctx);
      }

      ctx.endBatch();
   }

   @Override
   public final boolean touchDown(int screenX, int screenY, int pointer, int button) {
      Vector2 pointerPos;
      Vector2 worldPos = pointerPos = InputHandler.getInstance().getPointerWorldPos();
      Array hits = new Array();
      Array.ArrayIterator it = GameState.nearbyPlayers.iterator();

      while (it.hasNext()) {
         PlayerEntity player;
         if (!(player = (PlayerEntity)it.next()).isLocalPlayer() && !player.POOL2.isAlive && player.containsPoint(worldPos)) {
            hits.add(player);
         }
      }

      it = GameState.mapItems.iterator();

      while (it.hasNext()) {
         MapItemEntity mapItem;
         if ((mapItem = (MapItemEntity)it.next()).containsPoint(worldPos)) {
            hits.add(mapItem);
         }
      }

      it = GameState.nearbyMobs.iterator();

      while (it.hasNext()) {
         MobEntity mob;
         if (!(mob = (MobEntity)it.next()).isDead() && mob.containsPoint(worldPos)) {
            hits.add(mob);
         }
      }

      it = GameState.nearbyNpcs.iterator();

      while (it.hasNext()) {
         NpcEntity plant;
         if (!(plant = (NpcEntity)it.next()).isDead() && plant.containsPoint(worldPos)) {
            hits.add(plant);
         }
      }

      it = GameState.nearbyAnimals.iterator();

      while (it.hasNext()) {
         AnimalEntity animal;
         if ((animal = (AnimalEntity)it.next()).containsPoint(worldPos)) {
            hits.add(animal);
         }
      }

      it = GameState.nearbyPlants.iterator();

      while (it.hasNext()) {
         PlantEntity npc;
         if ((npc = (PlantEntity)it.next()).containsPoint(worldPos)) {
            hits.add(npc);
         }
      }

      int handled;
      if (hits.isEmpty()) {
         handled = 0;
      } else {
         int currentIndex = hits.indexOf(GameState.targetEntity, true);
         if (hits.size == 1) {
            if (((WorldEntity)hits.first()).equals(GameState.targetEntity)) {
               PlayerEntity.getInstance().interactionController.interact(false);
            } else {
               GameState.setTargetEntity((WorldEntity)hits.first());
            }
         } else if (GameState.targetEntity != null && GameState.targetEntity.getEntityType() == EntityType.ITEM_MAP) {
            PlayerEntity.getInstance().interactionController.interact(false);
         } else if (currentIndex >= hits.size - 1) {
            GameState.setTargetEntity((WorldEntity)hits.first());
         } else {
            GameState.setTargetEntity((WorldEntity)hits.get(currentIndex + 1));
         }

         handled = 1;
      }

      if (handled != 0) {
         return true;
      } else {
         Array.ArrayIterator waypointIt = this.editorViewport.owner.iterator();

         while (waypointIt.hasNext()) {
            if (((WaypointEntity)waypointIt.next()).onTouchDown(screenX, screenY, pointer)) {
               return true;
            }
         }

         for (NpcEntity npc : GameState.nearbyNpcs) { // ui/230 talk button (decompiled C3471.touchDown)
            if (npc.talkButton != null && npc.talkButton.touchDown(pointerPos, pointer)) {
               return true;
            }
         }

         ClickEffect.POOL2.obtain().playAt(pointerPos.x, pointerPos.y);
         return super.touchDown(screenX, screenY, pointer, button);
      }
   }

   @Override
   public final boolean touchUp(int screenX, int screenY, int pointer, int button) {
      Array.ArrayIterator it = this.editorViewport.owner.iterator();

      while (it.hasNext()) {
         if (((WaypointEntity)it.next()).onTouchUp(screenX, screenY, pointer)) {
            return true;
         }
      }

      Vector2 upPos = InputHandler.getInstance().getPointerWorldPos();
      for (NpcEntity npc : GameState.nearbyNpcs) {
         if (npc.talkButton != null && npc.talkButton.touchUp(upPos, pointer)) {
            return true;
         }
      }

      return super.touchUp(screenX, screenY, pointer, button);
   }

   @Override
   public final boolean keyDown(int keycode) {
      if (keycode == Settings.keybindings[5]) {
         PlayerEntity.getInstance().interactionController.interact(true);
         return true;
      } else if (keycode == Settings.keybindings[7]) {
         cycleNearbyTarget();
         return true;
      }

      // Numbered slots 1-6 = keybindings[4, 6, 8, 10, 12, 14] (left column of the shortcut table, see KeyBindingPanel).
      for (int slot = 0; slot < SKILL_SLOT_KEYBINDS.length; slot++) {
         if (keycode == Settings.keybindings[SKILL_SLOT_KEYBINDS[slot]]) {
            this.debugScrollPane.POOL2.skillBar.skillButtons[slot].activateSkill();
            return true;
         }
      }

      if (keycode == Settings.keybindings[13]) {
         this.debugScrollPane.POOL2.hudSkillButton3.activateSkill();
      }

      if (keycode == Settings.keybindings[15]) {
         this.debugScrollPane.POOL2.mpPotionSlotButton.usePotion();
      }

      if (keycode == Settings.keybindings[9]) {
         AutoAttackButton.toggleAutoAttack();
      }

      return super.keyDown(keycode);
   }

   /** Index in Settings.keybindings of the numbered slot keys 1..6. */
   private static final int[] SKILL_SLOT_KEYBINDS = {4, 6, 8, 10, 12, 14};

   @Override
   public final void onDispose() {
      this.box2dDebugRenderer = null;
      this.editorViewport = null;
   }

   static {
      try {
      } catch (Exception e) {
         throw new RuntimeException(e);
      }
   }
}

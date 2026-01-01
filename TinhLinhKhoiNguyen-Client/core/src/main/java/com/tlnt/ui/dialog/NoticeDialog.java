package com.tlnt.ui.dialog;

import com.tlnt.core.asset.SkeletonLoader;
import com.tlnt.engine.WorldConfig;
import com.tlnt.engine.render.RenderContext;
import com.tlnt.engine.util.NamedPair;
import com.tlnt.thirdparty.spine.AnimationState;
import com.tlnt.thirdparty.spine.Skeleton;
import com.tlnt.thirdparty.spine.utils.SpineSkeletonActor;
import com.tlnt.ui.window.WindowType;

public final class NoticeDialog extends SlideInDialog {

   public NoticeDialog(RenderContext renderContext) {
      super(renderContext, WindowType.DISCONNECTED_PANEL);
      DisconnectNoticePanel panel;
      (panel = new DisconnectNoticePanel(this, super.renderContext)).setSize(1009.0F, 643.0F);
      float posX = (float)WorldConfig.VIEWPORT_WIDTH / 2.1F;
      float posY = (float)WorldConfig.VIEWPORT_HEIGHT / 2.0F;
      panel.setPosition(posX, posY, 1);
      this.addActor(panel);
      SpineSkeletonActor logoActor;
      (logoActor = new SpineSkeletonActor()).setRenderer(super.renderContext.getSkeletonRenderer());
      NamedPair skeletonPair = SkeletonLoader.obtainSkeleton("ui/logo", 100.0F, new SkeletonLoader.AnimationMixConfig());
      logoActor.setAnimationState((AnimationState)skeletonPair.first);
      logoActor.setSkeleton((Skeleton)skeletonPair.second);
      AnimationState animState = logoActor.getAnimationState();
      animState.setAnimation(0, "animation", true);
      posX = panel.getWidth() / 1.85F;
      posY = panel.getHeight() - 50.0F;
      logoActor.setPosition(posX, posY, 2);
      panel.addActor(logoActor);
      NoticeDialogButton closeButton;
      (closeButton = new NoticeDialogButton(this, "Đóng", super.renderContext)).setSize(169.0F, 63.0F);
      closeButton.setPosition(458.0F, 142.0F);
      panel.addActor(closeButton);
      closeButton.addListener(new NoticeCloseButtonListener(this));
   }

}

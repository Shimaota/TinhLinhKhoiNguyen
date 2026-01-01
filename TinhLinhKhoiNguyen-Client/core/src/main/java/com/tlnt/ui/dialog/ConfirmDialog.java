package com.tlnt.ui.dialog;

import com.tlnt.core.asset.SkeletonLoader;
import com.tlnt.engine.WorldConfig;
import com.tlnt.engine.render.RenderContext;
import com.tlnt.engine.util.NamedPair;
import com.tlnt.thirdparty.spine.AnimationState;
import com.tlnt.thirdparty.spine.Skeleton;
import com.tlnt.thirdparty.spine.utils.SpineSkeletonActor;
import com.tlnt.ui.window.WindowType;

public final class ConfirmDialog extends SlideInDialog {

   public ConfirmDialog(RenderContext renderContext) {
      super(renderContext, WindowType.CLEAR_DATA_PANEL);
      ResetDataConfirmPanel panel;
      (panel = new ResetDataConfirmPanel(this, super.renderContext)).setSize(1009.0F, 643.0F);
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
      String animationName = "animation";
      animState.setAnimation(0, animationName, true);
      logoActor.setPosition(panel.getWidth() / 1.85F, panel.getHeight() - 50.0F, 2);
      panel.addActor(logoActor);
      ConfirmDialogTextButton cancelButton;
      (cancelButton = new ConfirmDialogTextButton(this, "Từ chối", super.renderContext)).setSize(169.0F, 63.0F);
      cancelButton.setPosition(370.0F, 142.0F);
      panel.addActor(cancelButton);
      cancelButton.addListener(new CancelToLoginClickListener(this));
      ConfirmDialogButton confirmButton;
      (confirmButton = new ConfirmDialogButton(this, "Đồng ý", super.renderContext)).setSize(169.0F, 63.0F);
      confirmButton.setPosition(547.0F, 142.0F);
      panel.addActor(confirmButton);
      confirmButton.addListener(new DeleteLocalDataClickListener(this));
   }

}

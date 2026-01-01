package com.tlnt.ui.dialog;

import com.badlogic.gdx.utils.Queue;
import com.tlnt.core.TaskScheduler;
import com.tlnt.core.asset.SkeletonLoader;
import com.tlnt.engine.WorldConfig;
import com.tlnt.engine.render.RenderContext;
import com.tlnt.engine.util.NamedPair;
import com.tlnt.model.download.DownloadEntry;
import com.tlnt.thirdparty.spine.AnimationState;
import com.tlnt.thirdparty.spine.Skeleton;
import com.tlnt.thirdparty.spine.utils.SpineSkeletonActor;
import com.tlnt.ui.window.WindowType;
import com.tlnt.util.GameUtils;
import java.util.concurrent.TimeUnit;

public final class LoadingDialog extends SlideInDialog {
   public String progressText = "";
   public String loadingCategory;
   public int totalCount;
   public int currentIndex;
   public Queue<DownloadEntry> taskQueue;
   // A ResourceDownloadTask is already running: pressing "Download" repeatedly does not spawn more threads (previously several threads shared
   // one non-thread-safe queue -> lost files / empty-queue errors -> the loading screen froze).
   public volatile boolean downloading;
   // Files done / total files: shown next to the size so players can see progress (the text does not flip back and forth).
   public int doneFiles;
   public int fileTotal;

   public LoadingDialog(RenderContext renderContext) {
      super(renderContext, WindowType.DOWNLOAD_RESOURCES_PANEL);
      LoadingTitlePanel titlePanel;
      (titlePanel = new LoadingTitlePanel(this, super.renderContext)).setSize(1009.0F, 379.0F);
      titlePanel.setPosition((float)WorldConfig.VIEWPORT_WIDTH / 2.1F, (float)WorldConfig.VIEWPORT_HEIGHT / 1.7F, 1);
      this.addActor(titlePanel);
      SpineSkeletonActor logoActor;
      (logoActor = new SpineSkeletonActor()).setRenderer(super.renderContext.getSkeletonRenderer());
      NamedPair skeletonPair = SkeletonLoader.obtainSkeleton("ui/logo", 100.0F, new SkeletonLoader.AnimationMixConfig());
      logoActor.setAnimationState((AnimationState)skeletonPair.first);
      logoActor.setSkeleton((Skeleton)skeletonPair.second);
      AnimationState animState = logoActor.getAnimationState();
      String animationName = "animation";
      animState.setAnimation(0, animationName, true);
      logoActor.setPosition(titlePanel.getWidth() / 1.85F, titlePanel.getHeight() - 50.0F, 2);
      titlePanel.addActor(logoActor);
      LoadingDialogButton downloadButton;
      (downloadButton = new LoadingDialogButton(this, "Tải", super.renderContext)).setSize(169.0F, 63.0F);
      downloadButton.setPosition(458.0F, 142.0F);
      titlePanel.addActor(downloadButton);
      downloadButton.addListener(new StartDownloadClickListener(this));
   }

   public final synchronized void startLoading(Queue<DownloadEntry> taskQueue, int totalCount, String loadingCategory) {
      if (this.downloading) {
         return;
      }

      this.taskQueue = taskQueue;
      this.totalCount = totalCount;
      this.fileTotal = taskQueue.size;
      this.doneFiles = 0;
      this.loadingCategory = loadingCategory;
      this.progressText = "Tải dữ liệu " + GameUtils.formatFileSize((long)this.totalCount);
   }

   public synchronized void processNextTask() {
      if (!this.downloading && this.taskQueue != null && !this.taskQueue.isEmpty()) {
         this.downloading = true;
         // Resume after an error: finished files are no longer in the queue, progress starts from what is already done.
         int remaining = 0;
         for (DownloadEntry entry : this.taskQueue) {
            remaining += entry.fileSize;
         }

         this.currentIndex = Math.max(0, this.totalCount - remaining);
         this.doneFiles = this.fileTotal - this.taskQueue.size;
         this.updateProgressText();
         TaskScheduler.schedule(new ResourceDownloadTask(this), 0L, TimeUnit.MILLISECONDS);
      }
   }

   /** One fixed line for both downloading and extracting: "Tải dữ liệu 45 MB/225 MB (12/29)". */
   public void updateProgressText() {
      this.progressText = "Tải dữ liệu "
         + GameUtils.formatFileSize((long)this.currentIndex)
         + "/"
         + GameUtils.formatFileSize((long)this.totalCount)
         + " (" + Math.min(this.doneFiles + 1, this.fileTotal) + "/" + this.fileTotal + ")";
   }

   /** The download thread stopped on an error: show the message and allow pressing "Download" again. */
   public void fail(String message) {
      this.progressText = message;
      this.downloading = false;
   }

}

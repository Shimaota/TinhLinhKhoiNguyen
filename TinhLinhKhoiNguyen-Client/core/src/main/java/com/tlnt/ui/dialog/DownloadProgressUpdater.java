package com.tlnt.ui.dialog;

import com.tlnt.util.HttpClient;

public final class DownloadProgressUpdater extends HttpClient.ProgressCallback {
   public com.tlnt.ui.dialog.ResourceDownloadTask downloadTask;
   public DownloadProgressUpdater(ResourceDownloadTask downloadTask) {
      super();
      this.downloadTask = downloadTask;
   }

   @Override
   public final void onBytesDownloaded(int bytes) {
      this.downloadTask.loadingDialog.currentIndex += bytes;
      if (this.downloadTask.loadingDialog.currentIndex > this.downloadTask.loadingDialog.totalCount) {
         this.downloadTask.loadingDialog.currentIndex = this.downloadTask.loadingDialog.totalCount;
      } else if (this.downloadTask.loadingDialog.currentIndex < 0) {
         // bytes < 0: HttpClient subtracts the bytes of a failed attempt before retrying
         this.downloadTask.loadingDialog.currentIndex = 0;
      }

      this.downloadTask.loadingDialog.updateProgressText();
   }
}

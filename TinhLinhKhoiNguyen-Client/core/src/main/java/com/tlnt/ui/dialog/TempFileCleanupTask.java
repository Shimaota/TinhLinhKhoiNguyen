package com.tlnt.ui.dialog;

import com.badlogic.gdx.files.FileHandle;
import com.tlnt.util.FileStorage;

public final class TempFileCleanupTask implements Runnable {
   public com.tlnt.ui.dialog.ResourceDownloadTask downloadTask;
   public com.badlogic.gdx.files.FileHandle zipFile;
   public TempFileCleanupTask(ResourceDownloadTask downloadTask, FileHandle zipFile) {
      super();
      this.downloadTask = downloadTask;
      this.zipFile = zipFile;
   }

   @Override
   public final void run() {
      FileStorage.extractZip(this.zipFile, "");
      this.zipFile.delete();
   }
}

package com.tlnt.ui.dialog;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.tlnt.core.GameDataLoader;
import com.tlnt.core.GameMain;
import com.tlnt.model.download.DownloadEntry;
import com.tlnt.ui.window.WindowType;
import com.tlnt.util.FileStorage;
import com.tlnt.util.HttpClient;

/**
 * Downloads the queued files one by one; each file is extracted right after it finishes downloading and only then removed from the queue.
 * On an error (network, corrupt file, out of storage...) it stops, reports it on the dialog and lets the player press "Download" to resume
 * from the unfinished file — previously an error killed the thread silently and the loading screen stayed frozen forever.
 */
public final class ResourceDownloadTask implements Runnable {
   public final com.tlnt.ui.dialog.LoadingDialog loadingDialog;

   public ResourceDownloadTask(LoadingDialog loadingDialog) {
      this.loadingDialog = loadingDialog;
   }

   @Override
   public final void run() {
      LoadingDialog dialog = this.loadingDialog;

      try {
         while (true) {
            DownloadEntry entry;
            synchronized (dialog) {
               if (dialog.taskQueue.isEmpty()) {
                  break;
               }

               entry = dialog.taskQueue.first();
            }

            FileHandle tempFile = FileStorage.getExternalFile("temp/" + entry.fileName);
            if (!HttpClient.downloadToFile(entry.getDownloadUrl(), 5, tempFile, new DownloadProgressUpdater(this), null)) {
               dialog.fail("Lỗi tải dữ liệu - kiểm tra mạng rồi bấm Tải");
               return;
            }

            boolean extracted = FileStorage.extractZip(tempFile, "");
            tempFile.delete();
            if (!extracted) {
               dialog.fail("Lỗi giải nén (thiếu bộ nhớ?) - bấm Tải để thử lại");
               return;
            }

            synchronized (dialog) {
               dialog.taskQueue.removeFirst();
               dialog.doneFiles++;
            }
         }

         FileStorage.writeFile("version/resources", dialog.loadingCategory.getBytes());
         GameDataLoader.resourcesVersion = dialog.loadingCategory;
         dialog.downloading = false;
         Gdx.app.postRunnable(new Runnable() {
            @Override
            public void run() {
               GameMain.instance.getGameScreen().active.showDialog(WindowType.LOGIN_PANEL);
            }
         });
      } catch (Throwable t) {
         t.printStackTrace();
         dialog.fail("Lỗi tải dữ liệu - bấm Tải để thử lại");
      }
   }
}

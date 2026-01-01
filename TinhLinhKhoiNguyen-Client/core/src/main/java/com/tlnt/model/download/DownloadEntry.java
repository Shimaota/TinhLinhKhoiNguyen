package com.tlnt.model.download;

public final class DownloadEntry {
   public String baseUrl;
   public String fileName;
   public int fileSize;

   public DownloadEntry(String baseUrl, String fileName, int fileSize) {
      this.baseUrl = baseUrl;
      this.fileName = fileName;
      this.fileSize = fileSize;
   }

   public final String getDownloadUrl() {
      if (this.baseUrl == null) return this.fileName;
      if (this.fileName == null) return this.baseUrl;
      if (this.baseUrl.endsWith("/") && this.fileName.startsWith("/")) {
         return this.baseUrl + this.fileName.substring(1);
      }
      if (!this.baseUrl.endsWith("/") && !this.fileName.startsWith("/")) {
         return this.baseUrl + "/" + this.fileName;
      }
      return this.baseUrl + this.fileName;
   }
}

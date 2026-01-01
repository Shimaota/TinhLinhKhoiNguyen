package com.tlnt.util;

import java.text.SimpleDateFormat;
import java.util.Date;

public final class TimeFormatter {

   public static String formatTimestamp(long timestamp, String pattern) {
      return new SimpleDateFormat(pattern).format(new Date(timestamp));
   }

   public static String formatDuration(long millis) {
      long remaining;
      long years = (remaining = millis / 1000L) / 31536000L;
      long afterYears;
      long months = (afterYears = remaining % 31536000L) / 2592000L;
      long days = (remaining = afterYears % 2592000L) / 86400L;
      long afterDays;
      long hours = (afterDays = remaining % 86400L) / 3600L;
      long minutes = (remaining = afterDays % 3600L) / 60L;
      long seconds = remaining % 60L;
      StringBuilder sb = new StringBuilder();
      if (years > 0L) {
         StringBuilder yearsPart = sb.append(years);
         yearsPart.append("y ");
      }

      if (months > 0L) {
         StringBuilder monthsPart = sb.append(months);
         monthsPart.append("mo ");
      }

      if (days > 0L) {
         StringBuilder daysPart = sb.append(days);
         daysPart.append("d ");
      }

      if (hours > 0L) {
         sb.append(hours).append("h ");
      }

      if (minutes > 0L) {
         StringBuilder minutesPart = sb.append(minutes);
         minutesPart.append("m ");
      }

      // Under 1 hour also print seconds (the original dropped seconds -> under 1 minute gave an empty string, e.g. apple tree 1 minute / fruit).
      if (millis < 3_600_000L && (seconds > 0L || sb.length() == 0)) {
         sb.append(seconds).append("s");
      }

      return sb.toString().trim();
   }

   public static String formatTimeRemaining(long endTime) {
      long now = System.currentTimeMillis();
      long remainingMillis;
      if ((remainingMillis = endTime - now) <= 0L) {
         return "";
      } else {
         long remaining;
         long years = (remaining = remainingMillis / 1000L) / 31536000L;
         long afterYears;
         long months = (afterYears = remaining % 31536000L) / 2592000L;
         long days = (remaining = afterYears % 2592000L) / 86400L;
         long afterDays;
         long hours = (afterDays = remaining % 86400L) / 3600L;
         long minutes = (remaining = afterDays % 3600L) / 60L;
         long secondsValue;
         long seconds = secondsValue = remaining % 60L;
         StringBuilder sb = new StringBuilder();
         if (years > 0L) {
            StringBuilder yearsPart = sb.append(years);
            yearsPart.append("y:");
         }

         if (months > 0L) {
            StringBuilder monthsPart = sb.append(months);
            monthsPart.append("m:");
         }

         if (days > 0L) {
            StringBuilder daysPart = sb.append(days);
            daysPart.append("d:");
         }

         if (hours > 0L) {
            sb.append(hours).append("h:");
         }

         if (minutes > 0L) {
            StringBuilder minutesPart = sb.append(minutes);
            minutesPart.append("m:");
         }

         StringBuilder secondsPart = sb.append(seconds);
         secondsPart.append("s");
         return sb.toString().trim();
      }
   }

}

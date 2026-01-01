package com.tlnt.model.item;

import com.badlogic.gdx.utils.Array;
import java.text.MessageFormat;

public final class StatLine {
   public StatType statType;
   public Integer[] values;
   public Integer[][] valueRanges;

   public StatLine(StatType statType, Integer[] values, Integer[][] valueRanges) {
      this.statType = statType;
      this.values = values;
      this.valueRanges = valueRanges;
   }

   public final String formatText() {
      if (this.values != null) {
         return MessageFormat.format(this.statType.formatPattern, this.values);
      } else {
         String[] rangeTexts = new String[this.valueRanges.length];

         for (int i = 0; i < this.valueRanges.length; i++) {
            Integer[] range = this.valueRanges[i];
            int minValue = range[0];
            range = this.valueRanges[i];
            int maxValue = range[1];
            if (minValue != maxValue) {
               rangeTexts[i] = "[[" + minValue + "-" + maxValue + "]";
            } else {
               rangeTexts[i] = minValue + "";
            }
         }

         return MessageFormat.format(this.statType.formatPattern, rangeTexts);
      }
   }

   public final void clear() {
      this.statType = null;
      this.values = null;
      this.valueRanges = null;
   }

   public static void mergeStatLines(Array<StatLine> statLines) {
      for (int i = 0; i < statLines.size; i++) {
         StatLine line;
         if ((line = (StatLine)statLines.get(i)) != null && line.values != null) {
            int statTypeId = line.statType.id;
            int j = i + 1;

            while (j < statLines.size) {
               StatLine other;
               if ((other = (StatLine)statLines.get(j)).statType.id == statTypeId && other.values != null) {

                  for (int k = 0; k < line.values.length; k++) {
                     Integer[] lineValues = line.values;
                     lineValues[k] = lineValues[k] + other.values[k];
                  }

                  statLines.removeIndex(j);
               } else {
                  j++;
               }
            }
         }
      }
   }
}

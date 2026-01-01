package com.tlnt.model.option;

public final class OptionInfo {
   public OptionInfoTemplate optionInfoTemplate;
   public long startTime;
   public int timeRemaining;

   public OptionInfo(int templateId, long startTime, int timeRemaining) {
      this.optionInfoTemplate = OptionInfoTemplate.getById(templateId);
      this.startTime = startTime;
      this.timeRemaining = timeRemaining;
   }

   @Override
   public final String toString() {
      return "OptionInfo{optionInfoTemplate=" + this.optionInfoTemplate + ", startTime=" + this.startTime + ", timeRemaining=" + this.timeRemaining + "}";
   }
}

package com.tlnt.model.quest;

public final class MainQuestStage {
   public int stageIndex;
   public String stageName;
   public String stageDescription;
   public int requiredCount;

   public MainQuestStage(int stageIndex, String stageName, String stageDescription, int requiredCount) {
      this.stageIndex = stageIndex;
      this.stageName = stageName;
      this.stageDescription = stageDescription;
      this.requiredCount = requiredCount;
   }
}

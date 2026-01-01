package com.tlnt.model.quest;

import com.badlogic.gdx.utils.Array;
import com.tlnt.model.item.Item;

public final class MainQuestState {
   public int questId;
   public String questTitle;
   public String questDescription;
   public Array<MainQuestStage> stages = new Array<>();
   public Array<Item> rewardItems = new Array<>();
   public int currentStageIndex;
   public int currentProgress;
   public int targetObjectId;
   public int targetMapId;
}

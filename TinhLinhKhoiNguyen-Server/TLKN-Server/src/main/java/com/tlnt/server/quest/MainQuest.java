package com.tlnt.server.quest;

import com.tlnt.server.item.RewardItem;

import java.util.List;

/**
 * 1 nhiệm vụ chính tuyến (nội dung ở {@link MainQuests}). Bước cuối luôn là {@link StepKind#TALK}: nhận thưởng ở đó.
 *
 * @param rewardPage trang hội thoại của bước cuối mà bấm "Tiếp tục" thì nhận thưởng (0 = trang 1); -1 = nhận ngay
 *                   khi bấm "Nói chuyện"
 */
public record MainQuest(int id, String name, String description, List<RewardItem> rewards, int rewardPage,
      List<QuestStep> steps) {

   public QuestStep step(int stage) {
      return stage >= 0 && stage < steps.size() ? steps.get(stage) : null;
   }

   public boolean lastStage(int stage) {
      return stage == steps.size() - 1;
   }
}

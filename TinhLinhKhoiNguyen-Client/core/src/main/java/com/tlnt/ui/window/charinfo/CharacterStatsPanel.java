package com.tlnt.ui.window.charinfo;

import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.Array;
import com.tlnt.engine.render.RenderContext;
import com.tlnt.model.player.PlayerStats;
import com.tlnt.ui.widget.ScrollPaneFactory;
import com.tlnt.ui.window.GamePanel;
import java.text.DecimalFormat;

public final class CharacterStatsPanel extends GamePanel {
   public Table statsTable;
   public Array<CharacterStatRow> statRows;

   public CharacterStatsPanel(RenderContext renderContext) {
      super(renderContext);
      this.setSize(610.0F, 610.0F);
      CharacterStatsPanel self = this;
      ScrollPane scrollPane = ScrollPaneFactory.createScrollPane(this.statsTable = new Table().top());
      this.add(scrollPane).grow();
      this.statRows = new Array<>();
      this.statRows.add(new CharacterNameStatRow(this, super.renderContext, "Nhân vật"));
      this.statRows.add(new ClassStatRow(this, super.renderContext, "Lớp"));
      this.statRows.add(new LevelStatRow(this, super.renderContext, "Level"));
      this.statRows.add(new ActivityPointsStatRow(this, super.renderContext, "Điểm hoạt động"));
      this.statRows.add(new HpStatRow(this, super.renderContext, "HP"));
      this.statRows.add(new MpStatRow(this, super.renderContext, "MP"));
      this.statRows.add(new AttackStatRow(this, super.renderContext, "Tấn công"));
      this.statRows.add(new MobAttackStatRow(this, super.renderContext, "Tấn công lên quái"));
      this.statRows.add(new CriticalStatRow(this, super.renderContext, "Chí mạng"));
      this.statRows.add(new CritDamageStatRow(this, super.renderContext, "Sát thương chí mạng"));
      this.statRows.add(new ArmorStatRow(this, super.renderContext, "Giáp"));
      this.statRows.add(new ArmorPenStatRow(this, super.renderContext, "Xuyên giáp"));
      this.statRows.add(new EvasionStatRow(this, super.renderContext, "Né tránh"));
      this.statRows.add(new AccuracyStatRow(this, super.renderContext, "Chính xác"));
      this.statRows.add(new MoveSpeedStatRow(this, super.renderContext, "Tốc độ di chuyển"));
      this.statRows.add(new HpRegenStatRow(this, super.renderContext, "Hồi HP mỗi 5 giây"));
      this.statRows.add(new MpRegenStatRow(this, super.renderContext, "Hồi MP mỗi 5 giây"));
      this.statRows.add(new HpStealStatRow(this, super.renderContext, "Hút HP"));
      this.statRows.add(new MpDrainStatRow(this, super.renderContext, "Hút MP"));
      this.statRows.add(new MobHpLifestealStatRow(this, super.renderContext, "Hút HP quái"));
      this.statRows.add(new MobMpStealStatRow(this, super.renderContext, "Hút MP quái"));
      this.statRows.add(new DamageReflectStatRow(this, super.renderContext, "Phản sát thương"));
      this.statRows.add(new StunDmgStatRow(this, super.renderContext, "Sát thương choáng váng"));
      this.statRows.add(new WeakenDamageStatRow(this, super.renderContext, "Sát thương suy yếu"));
      this.statRows.add(new IceDamageStatRow(this, super.renderContext, "Sát thương băng giá"));
      this.statRows.add(new BurnDmgStatRow(this, super.renderContext, "Sát thương thiêu đốt"));
      this.statRows.add(new ThunderDamageStatRow(this, super.renderContext, "Sát thương sấm sét"));
      this.statRows.add(new CritDamageResistStatRow(this, super.renderContext, "Kháng sát thương chí mạng"));
      this.statRows.add(new DamageAmplificationStatRow(this, super.renderContext, "Khuếch đại sát thương"));
      this.statRows.add(new DamageAbsorptionStatRow(this, super.renderContext, "Hấp thụ sát thương"));
      this.statRows.add(new EffectResistStatRow(this, super.renderContext, "Kháng hiệu ứng"));
      this.statRows.add(new DamageResistStatRow(this, super.renderContext, "Kháng sát thương"));
      this.statRows.add(new GoldBonusStatRow(this, super.renderContext, "Thưởng vàng"));
      this.statRows.add(new ExpBonusStatRow(this, super.renderContext, "Thưởng kinh nghiệm"));

      for (CharacterStatRow row : this.statRows) {
         self.statsTable.add(row).height(40.0F).growX().row();
      }
   }

   public static String formatLong(long value) {
      return String.valueOf(value);
   }

   public final void setEntityStats(PlayerStats stats) {
      Array.ArrayIterator it = this.statRows.iterator();

      while (it.hasNext()) {
         ((CharacterStatRow)it.next()).setEntityStats(stats);
      }
   }

   static {
      new DecimalFormat("0.##");
   }

}

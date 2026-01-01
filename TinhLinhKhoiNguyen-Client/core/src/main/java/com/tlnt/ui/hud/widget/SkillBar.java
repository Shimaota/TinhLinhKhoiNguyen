package com.tlnt.ui.hud.widget;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Gdx;
import com.tlnt.core.GameState;
import com.tlnt.engine.WorldConfig;
import com.tlnt.engine.render.RenderContext;
import com.tlnt.model.skill.LearnedSkill;
import com.tlnt.ui.window.GamePanel;

public final class SkillBar extends GamePanel {
   public SkillButton[] skillButtons;

   public SkillBar(RenderContext renderContext) {
      super(renderContext);
      this.setSize(575.0F, 478.0F);
      float posX = (float)(WorldConfig.VIEWPORT_WIDTH - (Gdx.app.getType() != Application.ApplicationType.iOS ? 30 : 140));
      this.setPosition(posX, 15.0F, 20);
      SkillBar bar = this;
      this.skillButtons = new SkillButton[6];
      this.skillButtons[0] = new SkillButton(super.renderContext);
      this.skillButtons[0].setSize(125.0F, 128.0F);
      this.skillButtons[0].setPosition(0.0F, 0.0F);
      this.skillButtons[1] = new SkillButton(super.renderContext);
      this.skillButtons[1].setSize(125.0F, 128.0F);
      this.skillButtons[1].setPosition(155.0F, 0.0F);
      this.skillButtons[2] = new SkillButton(super.renderContext);
      this.skillButtons[2].setSize(125.0F, 128.0F);
      this.skillButtons[2].setPosition(100.0F, 150.0F);
      this.skillButtons[3] = new SkillButton(super.renderContext);
      this.skillButtons[3].setSize(125.0F, 128.0F);
      this.skillButtons[3].setPosition(155.0F, 295.0F);
      this.skillButtons[4] = new SkillButton(super.renderContext);
      this.skillButtons[4].setSize(125.0F, 128.0F);
      this.skillButtons[4].setPosition(300.0F, 350.0F);
      this.skillButtons[5] = new SkillButton(super.renderContext);
      this.skillButtons[5].setSize(125.0F, 128.0F);
      this.skillButtons[5].setPosition(450.0F, 295.0F);
      SkillButton[] buttons = this.skillButtons;
      int count = this.skillButtons.length;

      for (int i = 0; i < count; i++) {
         SkillButton button = buttons[i];
         bar.addActor(button);
      }
   }

   @Override
   public final void refresh() {
      for (int i = 0; i < GameState.skillBarSkills.size; i++) {
         int buttonCount = this.skillButtons.length;
         if (i > buttonCount - 1) {
            break;
         }

         LearnedSkill skill = GameState.skillBarSkills.get(i);
         SkillButton button = this.skillButtons[i];
         int selected;
         if (skill != null && skill.equals(GameState.selectedSkill)) {
            selected = 1;
         } else {
            selected = 0;
         }

         button.setSkill(skill, (selected != 0));
      }
   }
}

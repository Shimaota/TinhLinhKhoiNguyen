package com.tlnt.ui.window.mail;

import com.tlnt.engine.render.RenderContext;
import com.tlnt.ui.window.WindowType;
import com.tlnt.ui.window.action.CurrencyBarPanel;
import com.tlnt.ui.window.action.TabbedWindow;
import com.tlnt.ui.window.mail.panel.MailComposePanel;
import com.tlnt.ui.window.mail.panel.MailMessageRow;

public final class MailWindow extends TabbedWindow {

   public MailWindow(RenderContext renderContext) {
      super(renderContext, WindowType.MAIL_POPUP);
      this.addTab("Hòm thư", "icon/283", new MailMessageRow(super.renderContext));
      this.addTab("Gửi thư", "icon/284", new MailComposePanel(super.renderContext));
      this.addActor(new CurrencyBarPanel(super.renderContext));
   }

}

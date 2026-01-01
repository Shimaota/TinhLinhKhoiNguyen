package com.tlnt.ui.window.chat;

import com.tlnt.engine.render.RenderContext;
import com.tlnt.ui.window.WindowType;
import com.tlnt.ui.window.action.TabbedWindow;
import com.tlnt.ui.window.chat.panel.GeneralChatPanel;
import com.tlnt.ui.window.chat.panel.NearbyChatPanel;
import com.tlnt.ui.window.chat.panel.PartyChatPanel;
import com.tlnt.ui.window.chat.panel.PrivateChatPanel;
import com.tlnt.ui.window.chat.panel.SystemChatPanel;
import com.tlnt.ui.window.chat.panel.TribeChatPanel;
import com.tlnt.ui.window.chat.panel.WorldChatPanel;

public final class ChatWindow extends TabbedWindow {

   public ChatWindow(RenderContext renderContext) {
      super(renderContext, WindowType.MESSAGE_POPUP);
      String tabTitle = "Chung";
      this.addTab(tabTitle, "icon/578", new GeneralChatPanel(super.renderContext));
      tabTitle = "Thế giới";
      this.addTab(tabTitle, "icon/579", new WorldChatPanel(super.renderContext));
      this.addTab("Lân cận", "icon/580", new NearbyChatPanel(super.renderContext));
      this.addTab("Tổ đội", "icon/264", new PartyChatPanel(super.renderContext));
      tabTitle = "Bộ lạc";
      this.addTab(tabTitle, "icon/263", new TribeChatPanel(super.renderContext));
      tabTitle = "Chat riêng";
      this.addTab(tabTitle, "icon/581", new PrivateChatPanel(super.renderContext));
      tabTitle = "Hệ thống";
      this.addTab(tabTitle, "icon/582", new SystemChatPanel(super.renderContext));
   }

}

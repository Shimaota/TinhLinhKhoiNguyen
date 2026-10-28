package com.tlnt.server.handler;

import com.tlnt.server.forge.BrewService;
import com.tlnt.server.forge.CraftService;
import com.tlnt.server.forge.ElementService;
import com.tlnt.server.forge.ForgePackets;
import com.tlnt.server.forge.RefineService;
import com.tlnt.server.forge.SoulService;
import com.tlnt.server.forge.TransmuteService;
import com.tlnt.server.forge.UpgradeService;
import com.tlnt.server.net.Message;
import com.tlnt.server.net.PacketHandler;
import com.tlnt.server.net.Packets;
import com.tlnt.server.session.PlayerSession;

/**
 * Opcode 44 C→S: lò rèn ({@code ForgeWindow}). byte chế độ (tab, {@code forgeData.activeMode}), byte thao tác:
 *
 * <pre>
 * thao tác | method client (tên sai)          | ý nghĩa
 * 0        | sendPvpAction(ô túi, số lượng)   | "Chọn" 1 món trong túi đặt vào lò
 * 1        | sendPvpChallenge(ô lò)           | "Bỏ ra" 1 ô của lò
 * 2        | sendPvpAccept(số lần)            | nút thực hiện (Nâng cấp / Chế tác / ...)
 * 3        | sendPvpDecline / sendPvpCancel   | tháo ngọc khảm / đổi chế độ luyện đá
 * </pre>
 * Đã làm: tab 0 Nâng cấp ({@link UpgradeService}), tab 1 Linh hồn — khảm bùa ({@link SoulService}), tab 2 Nguyên tố —
 * khảm đá nguyên tố ({@link ElementService}), tab 3 Pha chế
 * ({@link BrewService}), tab 5 Luyện đá ({@link RefineService}), tab 6 Chuyển hóa ({@link TransmuteService} —
 * client mới); tab khác báo "sắp ra mắt".
 */
public final class ForgeHandler implements PacketHandler {

   private final UpgradeService upgrade;
   private final RefineService refine;
   private final SoulService soul;
   private final BrewService brew;
   private final CraftService craft;
   private final ElementService element;
   private final TransmuteService transmute;

   public ForgeHandler(UpgradeService upgrade, RefineService refine, SoulService soul, BrewService brew,
         CraftService craft, ElementService element, TransmuteService transmute) {
      this.element = element;
      this.transmute = transmute;
      this.upgrade = upgrade;
      this.refine = refine;
      this.soul = soul;
      this.brew = brew;
      this.craft = craft;
   }

   @Override
   public void handle(PlayerSession session, Message message) {
      if (session.getPlayer() == null) {
         return;
      }
      int mode = message.readByte();
      int action = message.readByte();
      if (mode == ForgePackets.MODE_CRAFT) { // Rèn linh thạch (Morgi > Chế tác)
         switch (action) {
            case 0 -> craft.choose(session, message.readInt()); // số lượng theo sau: bỏ qua (dùng đúng số cần)
            case 1 -> craft.remove(session, message.readInt());
            case 2 -> craft.craft(session); // client luôn gửi 1 lần
            default -> {
            }
         }
         return;
      }
      if (mode == ForgePackets.MODE_REFINE) {
         switch (action) {
            case 0 -> {
               int bagSlot = message.readInt();
               refine.choose(session, bagSlot, message.readInt()); // số lượng nhập = số đá đặt vào ô
            }
            case 1 -> refine.remove(session);
            case 2 -> refine.refine(session, message.readInt());
            case 3 -> refine.setOption(session, message.readByte()); // nút gạt mặt trời / mặt trăng
            default -> {
            }
         }
         return;
      }
      if (mode == ForgePackets.MODE_BREW) {
         switch (action) {
            case 0 -> {
               int bagSlot = message.readInt();
               brew.choose(session, bagSlot, message.readInt()); // số lượng = số hoa (tối đa 9)
            }
            case 1 -> brew.remove(session, message.readInt());
            case 2 -> brew.brew(session); // client luôn gửi 1 lần
            default -> {
            }
         }
         return;
      }
      if (mode == ForgePackets.MODE_ELEMENT) {
         switch (action) {
            case 0 -> element.choose(session, message.readInt()); // số lượng theo sau: bỏ qua (dùng đúng số cần)
            case 1 -> element.remove(session, message.readInt());
            case 2 -> element.socket(session);
            case 3 -> element.detach(session, message.readInt()); // "Gỡ khảm": vị trí đá trên trang bị
            default -> {
            }
         }
         return;
      }
      if (mode == ForgePackets.MODE_TRANSMUTE) {
         switch (action) {
            case 0 -> transmute.choose(session, message.readInt()); // số lượng theo sau: bỏ qua
            case 1 -> transmute.remove(session, message.readInt());
            case 2 -> transmute.transmute(session); // client luôn gửi 1 lần
            default -> {
            }
         }
         return;
      }
      if (mode == ForgePackets.MODE_SOUL) {
         switch (action) {
            case 0 -> soul.choose(session, message.readInt()); // số lượng theo sau: bỏ qua (khảm 1 bùa / lần)
            case 1 -> soul.remove(session, message.readInt());
            case 2 -> soul.socket(session);
            case 3 -> soul.detach(session, message.readInt()); // "Gỡ khảm": vị trí bùa trên trang bị
            default -> {
            }
         }
         return;
      }
      if (mode != ForgePackets.MODE_UPGRADE) {
         session.send(Packets.notice("Tính năng này sắp ra mắt."));
         return;
      }
      switch (action) {
         case 0 -> {
            int bagSlot = message.readInt();
            upgrade.choose(session, bagSlot, message.readInt());
         }
         case 1 -> upgrade.remove(session, message.readInt());
         case 2 -> upgrade.upgrade(session);
         default -> {
         }
      }
   }
}

package com.tlnt.server.data.model;

/**
 * 1 dòng bảng {@code item_appearance} (chỉ server dùng, client không có): item đang dùng làm đổi ngoại hình ra sao.
 *
 * @param charTemplateId mẫu nhân vật ngoại trang / thú; 0 = không đổi.
 * @param skinKey        áo (skin/lvN, thay thân) / vũ khí (axe/lvN...) / mặt nạ (mask/lvN) / đeo lưng (backpack/lvN):
 *                       tên skin trong skeleton nhân vật; null = không đổi.
 * @param titleKey       danh hiệu (20): đường dẫn skeleton hiệu ứng trên đầu ({@code TitleSkeletonLabel}); null = không.
 */
public record ItemAppearance(int itemTemplateId, int charTemplateId, String skinKey, String titleKey) {
}

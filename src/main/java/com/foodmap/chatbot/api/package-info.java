/**
 * Giao diện công khai của module {@code chatbot}.
 *
 * <p>Hiện module này là lá (không module nào gọi vào), nên chỉ có {@code ChatRole}
 * — nó xuất hiện trong JSON trả ra. Giữ {@code @NamedInterface} theo khuôn chung
 * để sau này thêm API công khai không phải sửa lại.
 */
@org.springframework.modulith.NamedInterface("api")
package com.foodmap.chatbot.api;

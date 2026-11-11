/**
 * Giao diện công khai của module {@code identity}.
 *
 * <p>Spring Modulith mặc định chỉ cho module khác dùng type ở package gốc của
 * module; package {@code api} phải được đánh dấu {@link NamedInterface} thì
 * {@code user} (và sau này {@code review}, {@code favorite}…) mới được gọi
 * {@code IdentityApi} / dùng {@code UserProfile} / {@code AuthPrincipal} /
 * {@code UserRole}. Các package còn lại ({@code domain}, {@code application},
 * {@code infrastructure}, {@code web}) vẫn là nội bộ của module.
 */
@NamedInterface("api")
package com.foodmap.identity.api;

import org.springframework.modulith.NamedInterface;

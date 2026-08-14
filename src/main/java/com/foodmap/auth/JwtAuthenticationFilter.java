package com.foodmap.auth;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Đọc {@code Authorization: Bearer <token>} và đặt danh tính vào SecurityContext.
 *
 * <p>Không có token, hoặc token hỏng, thì <b>đi tiếp mà không xác thực</b> —
 * endpoint công khai vẫn phục vụ được khách vãng lai (FR-PLACE-01). Việc chặn
 * do lớp phân quyền quyết định, không phải filter này.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;

    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {

        extractToken(request)
                .flatMap(jwtService::parse)
                .ifPresent(principal -> authenticate(principal, request));

        chain.doFilter(request, response);
    }

    private void authenticate(AuthPrincipal principal, HttpServletRequest request) {
        var authorities = List.of(new SimpleGrantedAuthority("ROLE_" + principal.role()));
        var authentication = new UsernamePasswordAuthenticationToken(principal, null, authorities);
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private java.util.Optional<String> extractToken(HttpServletRequest request) {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.startsWith(BEARER_PREFIX)) {
            return java.util.Optional.empty();
        }
        return java.util.Optional.of(header.substring(BEARER_PREFIX.length()).trim());
    }
}

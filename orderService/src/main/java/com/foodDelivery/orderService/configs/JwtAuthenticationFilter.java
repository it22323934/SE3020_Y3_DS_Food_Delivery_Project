package com.foodDelivery.orderService.configs;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private final JwtUtils jwtUtils;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            String jwt = parseJwt(request);
            if (jwt != null && jwtUtils.validateJwtToken(jwt)) {
                Claims claims = jwtUtils.getAllClaimsFromJwtToken(jwt);
                logger.info("JWT Claims: {}", claims);
                String username = jwtUtils.getUserNameFromJwtToken(jwt);
                Long userId = jwtUtils.getUserIdFromJwtToken(jwt);

                // Get roles directly from token
                List<SimpleGrantedAuthority> authorities = jwtUtils.getRolesFromJwtToken(jwt).stream()
                        .map(SimpleGrantedAuthority::new)
                        .collect(Collectors.toList());

                // Create custom authentication details with user ID
                Map<String, Object> details = new HashMap<>();
                details.put("userId", userId);

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(username, null, authorities);

                // Set both standard details and our custom details with userId
                authentication.setDetails(details);

                SecurityContextHolder.getContext().setAuthentication(authentication);

                // Store userId in request attribute for easy access in controllers
                if (userId != null) {
                    request.setAttribute("userId", userId);
                }
            }
        } catch (Exception e) {
            logger.error("Cannot set user authentication: {}", e);
        }

        filterChain.doFilter(request, response);
    }

    private String parseJwt(HttpServletRequest request) {
        String headerAuth = request.getHeader("Authorization");

        if (StringUtils.hasText(headerAuth) && headerAuth.startsWith("Bearer ")) {
            return headerAuth.substring(7);
        }

        return null;
    }
}
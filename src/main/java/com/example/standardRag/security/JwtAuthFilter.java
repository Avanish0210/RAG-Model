package com.example.standardRag.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@Slf4j
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final AuthUtil authUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

         final String requestHeaderToken = request.getHeader("Authorization");
         if(requestHeaderToken == null || !requestHeaderToken.startsWith("Bearer ")){
             filterChain.doFilter(request,response);
             return;
         }

         String jwtToken = requestHeaderToken.substring("Bearer ".length());
         try {
             JwtUserPrincipal user = authUtil.verifyAccessToken(jwtToken);
             var authentication = new UsernamePasswordAuthenticationToken(user, null, user.authorities());
             SecurityContextHolder.getContext().setAuthentication(authentication);
         } catch (Exception exception) {
             log.warn("Invalid JWT token", exception);
             SecurityContextHolder.clearContext();
         }

         filterChain.doFilter(request,response);

    }
}

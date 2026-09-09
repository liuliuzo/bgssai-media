package com.bgssai.media.common.auth;

import com.bgssai.media.common.aop.NeedAop;
import com.bgssai.media.common.web.BizException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.bgssai.media.common.web.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class JwtAuthInterceptor implements HandlerInterceptor {

    public static final String HEADER = "Jwttoken";

    private final JwtService jwtService;
    private final ObjectMapper objectMapper;

    public JwtAuthInterceptor(JwtService jwtService, ObjectMapper objectMapper) {
        this.jwtService = jwtService;
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (!(handler instanceof HandlerMethod method)) {
            return true;
        }
        NeedAop need = method.getMethodAnnotation(NeedAop.class);
        if (need == null) {
            need = method.getBeanType().getAnnotation(NeedAop.class);
        }
        if (need == null) {
            return true;
        }
        String token = request.getHeader(HEADER);
        if (token == null || token.isBlank()) {
            writeFail(response, 401, "missing Jwttoken");
            return false;
        }
        try {
            AuthUser user = jwtService.parse(token.trim());
            if (need.roles().length > 0) {
                boolean ok = false;
                for (String role : need.roles()) {
                    if (role.equals(user.getRole_code())) {
                        ok = true;
                        break;
                    }
                }
                if (!ok) {
                    writeFail(response, 403, "forbidden");
                    return false;
                }
            }
            AuthContext.set(user);
            return true;
        } catch (Exception ex) {
            writeFail(response, 401, "invalid token");
            return false;
        }
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        AuthContext.clear();
    }

    private void writeFail(HttpServletResponse response, int code, String message) throws Exception {
        response.setStatus(200);
        response.setContentType("application/json;charset=UTF-8");
        objectMapper.writeValue(response.getWriter(), ApiResponse.fail(code, message));
    }
}

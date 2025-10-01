package org.example.apigateway.config;

import jakarta.servlet.http.HttpServletRequest;
import lombok.NonNull;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.io.IOException;

public class DownstreamHeadersInterceptor implements ClientHttpRequestInterceptor {

    @Override
    public ClientHttpResponse intercept(
            @NonNull HttpRequest request,
            byte[] body,
            @NonNull ClientHttpRequestExecution execution) throws IOException {

        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            HttpServletRequest servletRequest = attributes.getRequest();
            
            addHeaderIfPresent(request, servletRequest, "X-User-ID");
            addHeaderIfPresent(request, servletRequest, "X-User-Roles");
            addHeaderIfPresent(request, servletRequest, "X-User-Email");
            addHeaderIfPresent(request, servletRequest, "X-Internal-Request");
        }

        return execution.execute(request, body);
    }

    private void addHeaderIfPresent(HttpRequest request, HttpServletRequest servletRequest, String headerName) {
        Object value = servletRequest.getHeader(headerName);
        if (value != null) {
            request.getHeaders().add(headerName, value.toString());
        }
    }
}
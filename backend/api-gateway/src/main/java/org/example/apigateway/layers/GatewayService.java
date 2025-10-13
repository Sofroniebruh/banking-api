package org.example.apigateway.layers;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class GatewayService {
    private final RestClient downstreamRestClient;

    public GatewayService(@Qualifier("downstreamRestClient") RestClient downstreamRestClient) {
        this.downstreamRestClient = downstreamRestClient;
    }

    public ResponseEntity<String> proxyToService(HttpServletRequest request, Object body, String serviceUrl) {
        var requestSpec = downstreamRestClient
                .method(org.springframework.http.HttpMethod.valueOf(request.getMethod()))
                .uri(serviceUrl + request.getRequestURI());

        if (body != null) {
            requestSpec.body(body);
        }

        return requestSpec.retrieve().toEntity(String.class);
    }

    public ResponseEntity<String> proxyToServiceWithPathRewrite(HttpServletRequest request, Object body, String serviceUrl, String stripPrefix) {
        String originalPath = request.getRequestURI();
        String rewrittenPath = originalPath.startsWith(stripPrefix) ? 
            originalPath.substring(stripPrefix.length()) : originalPath;
            
        var requestSpec = downstreamRestClient
                .method(org.springframework.http.HttpMethod.valueOf(request.getMethod()))
                .uri(serviceUrl + rewrittenPath);

        if (body != null) {
            requestSpec.body(body);
        }

        return requestSpec.retrieve().toEntity(String.class);
    }

    public Cookie createCookie(String name, String value, int maxAge, boolean httpOnly, String path, boolean secure) {
        Cookie accessTokenCookie = new Cookie(name, value);
        accessTokenCookie.setPath(path);
        accessTokenCookie.setMaxAge(maxAge);
        accessTokenCookie.setHttpOnly(httpOnly);
        accessTokenCookie.setSecure(secure);

        return accessTokenCookie;
    }
}

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
        String uri = serviceUrl + request.getRequestURI();

        String urlWithParams = addParamsToUrlIfPresent(request, uri);
        
        var requestSpec = downstreamRestClient
                .method(org.springframework.http.HttpMethod.valueOf(request.getMethod()))
                .uri(urlWithParams);

        if (body != null) {
            requestSpec.body(body);
        }

        return requestSpec.retrieve().toEntity(String.class);
    }

    public ResponseEntity<String> proxyToServiceWithPathRewrite(HttpServletRequest request, Object body, String serviceUrl, String stripPrefix) {
        String originalPath = request.getRequestURI();
        String rewrittenPath = originalPath.startsWith(stripPrefix) ? 
            originalPath.substring(stripPrefix.length()) : originalPath;
        String uri = serviceUrl + rewrittenPath;
        String urlWithParams = addParamsToUrlIfPresent(request, uri);
            
        var requestSpec = downstreamRestClient
                .method(org.springframework.http.HttpMethod.valueOf(request.getMethod()))
                .uri(urlWithParams);

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

    private String addParamsToUrlIfPresent(HttpServletRequest request, String url) {
        if (request.getQueryString() != null) {
            url += "?" + request.getQueryString();
        }

        return url;
    }
}

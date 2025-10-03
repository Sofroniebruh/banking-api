package org.example.apigateway.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;

import java.util.*;

public class HeaderMapRequestWrapper extends HttpServletRequestWrapper {
    private final Map<String, String> customHeaders = new HashMap<>();

    public HeaderMapRequestWrapper(HttpServletRequest request) {
        super(request);
    }

    @Override
    public String getHeader(String name) {
        String value = customHeaders.get(name);

        if (value != null) {
            return value;
        }

        return super.getHeader(name);
    }

    @Override
    public Enumeration<String> getHeaderNames() {
        List<String> headers = Collections.list(super.getHeaderNames());
        headers.addAll(customHeaders.keySet());

        return Collections.enumeration(headers);
    }

    @Override
    public Enumeration<String> getHeaders(String name) {
        List<String> values = Collections.list(super.getHeaders(name));

        if (customHeaders.containsKey(name)) {
            values.add(customHeaders.get(name));
        }

        return Collections.enumeration(values);
    }
}

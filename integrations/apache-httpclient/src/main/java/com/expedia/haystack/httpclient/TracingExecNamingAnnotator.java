package com.expedia.haystack.httpclient;

import org.apache.http.client.methods.HttpRequestWrapper;
import org.apache.http.protocol.HttpContext;

public interface TracingExecNamingAnnotator {
    String operationName(HttpRequestWrapper request, HttpContext context);

    public static class DefaultNamer implements TracingExecNamingAnnotator {
        @Override
        public String operationName(HttpRequestWrapper request, HttpContext context) {
            if (request.getTarget() != null) {
                return String.format("%s:%s", request.getMethod(), request.getTarget().toURI());
            } else {
                return String.format("%s", request.getMethod());
            }
        }
    }
}

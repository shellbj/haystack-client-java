package com.expedia.haystack.httpclient;

import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpRequestWrapper;
import org.apache.http.protocol.HttpContext;

import io.opentracing.Span;
import io.opentracing.tag.Tags;

public interface TracingExecAnnotator {
    void handleRequest(HttpRequestWrapper request, HttpContext context, Span span);

    void handleError(HttpRequestWrapper response, HttpContext context, Throwable e, Span span);

    void handleResponse(HttpResponse response, HttpContext context, Span span);

    public static class DefaultAnnotator implements TracingExecAnnotator {
        @Override
        public void handleRequest(HttpRequestWrapper request, HttpContext context, Span span) {
            Tags.HTTP_METHOD.set(span, request.getMethod());

            HttpHost httpHost = request.getTarget();
            Tags.PEER_PORT.set(span, httpHost.getPort());
            Tags.PEER_HOSTNAME.set(span, httpHost.getHostName());
            Tags.HTTP_URL.set(span, httpHost.toURI());

            if (httpHost.getAddress() != null) {
                Tags.PEER_HOST_IPV4.set(span, httpHost.getAddress().getHostAddress());
            }
        }

        @Override
        public void handleError(HttpRequestWrapper response, HttpContext context, Throwable e, Span span) {
            Tags.ERROR.set(span, true);
            span.log(e.toString());
        }

        @Override
        public void handleResponse(HttpResponse response, HttpContext context, Span span) {
            if (response != null && response.getStatusLine() != null) {
                Integer statusCode = response.getStatusLine().getStatusCode();
                Tags.HTTP_STATUS.set(span, statusCode);
                if (statusCode < 200 || statusCode > 399) {
                    Tags.ERROR.set(span, true);
                }
            }
        }
    }
}

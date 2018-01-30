package com.expedia.haystack.httpclient;

import java.util.Collections;

import org.apache.http.HttpRequestInterceptor;
import org.apache.http.impl.client.CloseableHttpClient;

import io.opentracing.Tracer;

public class CachingHttpClientTest extends HttpClientTest {

    @Override
    protected CloseableHttpClient newClient(Tracer tracer) {
        return TracingCachingHttpClientBuilder.create(tracer).disableAutomaticRetries().build();
    }

    @Override
    protected CloseableHttpClient newClient(Tracer tracer, HttpRequestInterceptor interceptor, Boolean first) {
        if (first) {
            return TracingCachingHttpClientBuilder.create(tracer).disableAutomaticRetries().addInterceptorFirst(interceptor).build();
        } else {
            return TracingCachingHttpClientBuilder.create(tracer).disableAutomaticRetries().addInterceptorLast(interceptor).build();
        }
    }

    @Override
    protected CloseableHttpClient newClient(Tracer tracer, TracingExecAnnotator annotator) {
        return TracingCachingHttpClientBuilder.create(tracer, Collections.singletonList(annotator)).disableAutomaticRetries().build();
    }
}

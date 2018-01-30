package com.expedia.haystack.httpclient;

import java.util.Collections;

import org.apache.http.HttpRequestInterceptor;
import org.apache.http.impl.client.CloseableHttpClient;

import io.opentracing.Tracer;

public class StandardHttpClientTest extends HttpClientTest {

    @Override
    protected CloseableHttpClient newClient(Tracer tracer) {
        return TracingHttpClientBuilder.create(tracer).disableAutomaticRetries().build();
    }

    @Override
    protected CloseableHttpClient newClient(Tracer tracer, HttpRequestInterceptor interceptor, Boolean first) {
        if (first) {
            return TracingHttpClientBuilder.create(tracer).addInterceptorFirst(interceptor).disableAutomaticRetries().build();
        } else {
            return TracingHttpClientBuilder.create(tracer).addInterceptorLast(interceptor).disableAutomaticRetries().build();
            }
    }

    @Override
    protected CloseableHttpClient newClient(Tracer tracer, TracingExecAnnotator annotator) {
        return TracingHttpClientBuilder.create(tracer, Collections.singletonList(annotator)).disableAutomaticRetries().build();
    }
}

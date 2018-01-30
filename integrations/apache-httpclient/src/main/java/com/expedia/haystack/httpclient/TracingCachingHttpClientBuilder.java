package com.expedia.haystack.httpclient;

import java.util.Collections;
import java.util.List;

import org.apache.http.impl.client.cache.CachingHttpClientBuilder;
import org.apache.http.impl.execchain.ClientExecChain;

import io.opentracing.Tracer;

public class TracingCachingHttpClientBuilder extends CachingHttpClientBuilder {

    public static TracingCachingHttpClientBuilder create(Tracer tracer) {
        return new TracingCachingHttpClientBuilder(tracer, new TracingExecNamingAnnotator.DefaultNamer(), Collections.singletonList(new TracingExecAnnotator.DefaultAnnotator()));
    }

    public static TracingCachingHttpClientBuilder create(Tracer tracer, List<TracingExecAnnotator> annotators) {
        return new TracingCachingHttpClientBuilder(tracer, new TracingExecNamingAnnotator.DefaultNamer(), annotators);
    }

    public static TracingCachingHttpClientBuilder create(Tracer tracer, TracingExecNamingAnnotator namingAnnotator) {
        return new TracingCachingHttpClientBuilder(tracer, namingAnnotator, Collections.singletonList(new TracingExecAnnotator.DefaultAnnotator()));
    }

    public static TracingCachingHttpClientBuilder create(Tracer tracer, TracingExecNamingAnnotator namingAnnotator, List<TracingExecAnnotator> annotators) {
        return new TracingCachingHttpClientBuilder(tracer, namingAnnotator, annotators);
    }

    private final Tracer tracer;
    private final TracingExecNamingAnnotator namingAnnotator;
    private final List<TracingExecAnnotator> annotators;

    protected TracingCachingHttpClientBuilder(Tracer tracer, TracingExecNamingAnnotator namingAnnotator, List<TracingExecAnnotator> annotators) {
        this.tracer = tracer;
        this.namingAnnotator = namingAnnotator;
        this.annotators = Collections.unmodifiableList(annotators);
    }

    @Override
    protected ClientExecChain decorateProtocolExec(ClientExecChain protocolExec) {
        return new TracingClientExec(protocolExec, tracer, namingAnnotator, annotators);
    }
}

package com.expedia.haystack.httpclient;

import java.util.Collections;
import java.util.List;

import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.execchain.ClientExecChain;

import io.opentracing.Tracer;

public class TracingHttpClientBuilder extends HttpClientBuilder {

    public static TracingHttpClientBuilder create(Tracer tracer) {
        return new TracingHttpClientBuilder(tracer, new TracingExecNamingAnnotator.DefaultNamer(), Collections.singletonList(new TracingExecAnnotator.DefaultAnnotator()));
    }

    public static TracingHttpClientBuilder create(Tracer tracer, List<TracingExecAnnotator> annotators) {
        return new TracingHttpClientBuilder(tracer, new TracingExecNamingAnnotator.DefaultNamer(), annotators);
    }

    public static TracingHttpClientBuilder create(Tracer tracer, TracingExecNamingAnnotator namingAnnotator) {
        return new TracingHttpClientBuilder(tracer, namingAnnotator, Collections.singletonList(new TracingExecAnnotator.DefaultAnnotator()));
    }

    public static TracingHttpClientBuilder create(Tracer tracer, TracingExecNamingAnnotator namingAnnotator, List<TracingExecAnnotator> annotators) {
        return new TracingHttpClientBuilder(tracer, namingAnnotator, annotators);
    }

    private final Tracer tracer;
    private final TracingExecNamingAnnotator namingAnnotator;
    private final List<TracingExecAnnotator> annotators;

    protected TracingHttpClientBuilder(Tracer tracer, TracingExecNamingAnnotator namingAnnotator, List<TracingExecAnnotator> annotators) {
        this.tracer = tracer;
        this.namingAnnotator = namingAnnotator;
        this.annotators = Collections.unmodifiableList(annotators);
    }

    @Override
    protected ClientExecChain decorateProtocolExec(ClientExecChain protocolExec) {
        return new TracingClientExec(protocolExec, tracer, namingAnnotator, annotators);
    }
}

package com.expedia.haystack.httpclient;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import org.apache.http.HttpException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpExecutionAware;
import org.apache.http.client.methods.HttpRequestWrapper;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.conn.routing.HttpRoute;
import org.apache.http.impl.execchain.ClientExecChain;

import io.opentracing.Scope;
import io.opentracing.Tracer;
import io.opentracing.propagation.Format;
import io.opentracing.tag.Tags;

public class TracingClientExec implements ClientExecChain {

    private static final String COMPONENT_NAME = "apache-httpclient";

    private final ClientExecChain protocolExec;
    private final Tracer tracer;
    private final TracingExecNamingAnnotator namingAnnotator;
    private final List<TracingExecAnnotator> annotators;

    public TracingClientExec(ClientExecChain protocolExec, Tracer tracer, TracingExecNamingAnnotator namingAnnotator, List<TracingExecAnnotator> annotators) {
        this.protocolExec = protocolExec;
        this.tracer = tracer;
        this.namingAnnotator = namingAnnotator;
        this.annotators = Collections.unmodifiableList(annotators);
    }

	@Override
	public CloseableHttpResponse execute(HttpRoute route, HttpRequestWrapper request, HttpClientContext clientContext,
                                       HttpExecutionAware execAware)
      throws IOException, HttpException {
      final Tracer.SpanBuilder builder = tracer.buildSpan(namingAnnotator.operationName(request, clientContext))
          .withTag(Tags.COMPONENT.getKey(), COMPONENT_NAME)
          .withTag(Tags.SPAN_KIND.getKey(), Tags.SPAN_KIND_CLIENT);

      try (Scope scope = builder.startActive(true)) {
          tracer.inject(scope.span().context(), Format.Builtin.HTTP_HEADERS, new HttpRequestWrapperTextMap(request));

          CloseableHttpResponse response = null;
          try { 
              for (TracingExecAnnotator annotator : annotators) {
                  annotator.handleRequest(request, clientContext, scope.span());
              }
             response = protocolExec.execute(route, request, clientContext, execAware);
              return response;
          } catch (IOException | HttpException | RuntimeException | Error e) {
              for (TracingExecAnnotator annotator : annotators) {
                  annotator.handleError(request, clientContext, e, scope.span());
              }
              throw e;
          } finally {
              for (TracingExecAnnotator annotator : annotators) {
                  annotator.handleResponse(response, clientContext, scope.span());
              }
          }
      }
	}
}

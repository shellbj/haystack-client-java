package com.expedia.haystack.httpclient;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.AbstractMap;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.StreamSupport;

import org.apache.http.HttpHost;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.message.BasicHttpRequest;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockserver.client.server.MockServerClient;
import org.mockserver.junit.MockServerRule;
import org.mockserver.model.HttpError;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.HttpResponse;

import io.opentracing.References;
import io.opentracing.Tracer;
import io.opentracing.mock.MockSpan;
import io.opentracing.mock.MockSpan.Reference;
import io.opentracing.mock.MockTracer;
import io.opentracing.tag.Tags;

public abstract class HttpClientTest {

    @Rule
    public MockServerRule mockServerRule = new MockServerRule(this);

    private MockServerClient mockServerClient;
    private MockTracer tracer;
    private MockSpan parentSpan;

    protected abstract CloseableHttpClient newClient(Tracer tracer);

    protected abstract CloseableHttpClient newClient(Tracer tracer, HttpRequestInterceptor interceptor, Boolean first);

    protected abstract CloseableHttpClient newClient(Tracer tracer, TracingExecAnnotator annotator);

    @Before
    public void setup() {
        tracer = new MockTracer();
        parentSpan = tracer.buildSpan("parent-span").start();
        tracer.scopeManager().activate(parentSpan, false);
    }

    @After
    public void teardown() {
        parentSpan.finish();
    }

    @Test
    public void happyPath() throws Exception {
        mockServerClient.when(HttpRequest.request().withMethod("GET").withPath("/cookies"))
            .respond(HttpResponse.response().withStatusCode(200)
                     .withCookie("Cookies", "YumYumYum")
                     .withHeader("Location", "https://www.expedia.com"));

        String baggageKey = "cookie-type";
        parentSpan.setBaggageItem(baggageKey, "chocolate chip");

        CloseableHttpClient client = newClient(tracer);
        client.execute(new HttpHost("localhost", mockServerRule.getPort()),
                       new BasicHttpRequest("GET", "/cookies"));

        List<MockSpan> spans = tracer.finishedSpans();
        assertEquals(1, spans.size());

        MockSpan span = spans.get(0);

        List<Reference> references = span.references();
        assertEquals(references.size(), 1);
        assertEquals(references.get(0), new MockSpan.Reference((MockSpan.MockContext) parentSpan.context(), References.CHILD_OF));

        Map<String, Object> tags = span.tags();
        assertEquals(tags.get(Tags.SPAN_KIND.getKey()), Tags.SPAN_KIND_CLIENT);
        assertEquals(tags.get(Tags.HTTP_METHOD.getKey()), "GET");
        assertEquals(tags.get(Tags.HTTP_STATUS.getKey()), 200);
        assertNull(tags.get(Tags.ERROR.getKey()));

        assertEquals(StreamSupport.stream(span.context().baggageItems().spliterator(), false).count(), 1);
        assertEquals(span.context().baggageItems().iterator().next(), new AbstractMap.SimpleImmutableEntry<String, String>(baggageKey, "chocolate chip"));

        HttpRequest[] httpRequests = mockServerClient.retrieveRecordedRequests(null);
        assertEquals(1, httpRequests.length);
        assertEquals(httpRequests[0].getFirstHeader("spanid"), String.valueOf(span.context().spanId()));
        assertEquals(httpRequests[0].getFirstHeader("traceid"), String.valueOf(span.context().traceId()));
        assertEquals(httpRequests[0].getFirstHeader("baggage-" + baggageKey), span.context().getBaggageItem(baggageKey));
    }

    @Test(expected=ClientProtocolException.class)
    public void exceptionThrown() throws Exception {
        byte[] randomByteArray = new byte[25];
        new Random().nextBytes(randomByteArray);

        mockServerClient.when(HttpRequest.request().withMethod("GET").withPath("/cookies"))
            .error(HttpError.error().withDropConnection(true).withResponseBytes(randomByteArray));

        CloseableHttpClient client = newClient(tracer);

        Throwable ex = null;
        try {
            client.execute(new HttpHost("localhost", mockServerRule.getPort()),
                           new BasicHttpRequest("GET", "/cookies"));
        } catch (Throwable e) {
            ex = e;
            throw e;
        } finally {
            List<MockSpan> spans = tracer.finishedSpans();
            assertEquals(1, spans.size());

            MockSpan span = spans.get(0);

            Map<String, Object> tags = span.tags();
            assertEquals(tags.get(Tags.ERROR.getKey()), true);

            assertNotNull(ex);
            assertEquals(1, span.logEntries().size());
            assertEquals(span.logEntries().get(0).fields(), Collections.singletonMap("event", ex.getCause().toString()));
        }
    }

    @Test
    public void errorStatusCode() throws Exception {
        mockServerClient.when(HttpRequest.request().withMethod("GET").withPath("/cookies"))
            .respond(HttpResponse.response().withStatusCode(404));

        CloseableHttpClient client = newClient(tracer);

        client.execute(new HttpHost("localhost", mockServerRule.getPort()),
                       new BasicHttpRequest("GET", "/cookies"));

        List<MockSpan> spans = tracer.finishedSpans();
        assertEquals(1, spans.size());

        MockSpan span = spans.get(0);

        Map<String, Object> tags = span.tags();
        assertEquals(tags.get(Tags.ERROR.getKey()), true);
        assertEquals(tags.get(Tags.HTTP_STATUS.getKey()), 404);
    }

    @Test
    public void clientSpanVisable() throws Exception {
        mockServerClient.when(HttpRequest.request().withMethod("GET").withPath("/cookies"))
            .respond(HttpResponse.response().withStatusCode(200)
                     .withCookie("Cookies", "YumYumYum")
                     .withHeader("Location", "https://www.expedia.com"));

        CloseableHttpClient client = newClient(tracer, (HttpRequestInterceptor) (request, context) ->
                                               request.setHeader("active-span-id",
                                                                 String.valueOf(((MockSpan.MockContext) tracer.activeSpan().context()).spanId())),
                                               true);

        client.execute(new HttpHost("localhost", mockServerRule.getPort()),
                       new BasicHttpRequest("GET", "/cookies"));

        HttpRequest[] httpRequests = mockServerClient.retrieveRecordedRequests(null);
        assertEquals(1, httpRequests.length);
        assertEquals(httpRequests[0].getFirstHeader("spanid"), httpRequests[0].getFirstHeader("active-span-id"));
    }

}

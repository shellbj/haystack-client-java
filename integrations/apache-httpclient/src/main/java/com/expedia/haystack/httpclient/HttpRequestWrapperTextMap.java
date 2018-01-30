package com.expedia.haystack.httpclient;

import java.util.Iterator;
import java.util.Map.Entry;

import org.apache.http.client.methods.HttpRequestWrapper;
import org.apache.http.message.BasicHeader;

import io.opentracing.propagation.TextMap;

public class HttpRequestWrapperTextMap implements TextMap {
    private final HttpRequestWrapper request;

    public HttpRequestWrapperTextMap(HttpRequestWrapper request) {
        this.request = request;
    }

    @Override
    public Iterator<Entry<String, String>> iterator() {
        throw new UnsupportedOperationException("This is a read-only implementation");
    }

    @Override
    public void put(String key, String value) {
        request.addHeader(new BasicHeader(key, value));
    }
}

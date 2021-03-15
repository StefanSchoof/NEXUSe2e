package org.nexuse2e.service.http;

import java.util.Collections;
import java.util.Map;

public class HttpResponse {
    private byte[] body;
    private int statusCode;
    private Map<? extends String, ? extends String> headers;

    public HttpResponse(byte[] body, int statusCode) {
        this(body, statusCode, Collections.<String,String>emptyMap());
    }

    public HttpResponse(byte[] body, int statusCode, Map<? extends String, ? extends String> headers) {
        this.body = body;
        this.statusCode = statusCode;
        this.headers = headers;
    }

    public byte[] getBody() {
        return body;
    }

    public void setBody(byte[] body) {
        this.body = body;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(int statusCode) {
        this.statusCode = statusCode;
    }

    public Map<? extends String, ? extends String> getHeaders() {
        return headers;
    }

    public void setHeaders(Map<? extends String, ? extends String> headers) {
        this.headers = headers;
    }
}

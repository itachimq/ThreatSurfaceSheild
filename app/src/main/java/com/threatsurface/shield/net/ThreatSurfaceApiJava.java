package com.threatsurface.shield.net;

import java.util.Map;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface ThreatSurfaceApiJava {

    class UrlCheckRequest {
        public String url;
        public UrlCheckRequest(String url) { this.url = url; }
    }

    class UrlCheckResponse {
        public String mode;
        public Map<String, Double> probs;
        public String label;
        public String explanation;
        public String error;
    }

    @POST("/api/url_check")
    Call<UrlCheckResponse> checkUrl(@Body UrlCheckRequest body);
}

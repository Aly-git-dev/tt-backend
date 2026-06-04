package com.upiiz.platform_api.gesco;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.client.SimpleClientHttpRequestFactory;

@Component
@ConditionalOnProperty(name = "app.gesco.enabled", havingValue = "true")
public class GescoClient {

    private final RestTemplate http;
    private final String baseUrl;
    private final String token;

    public GescoClient(
            @Value("${app.gesco.base-url}") String baseUrl,
            @Value("${app.gesco.token}") String token
    ) {
        this.baseUrl = baseUrl;
        this.token = token;

        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);
        factory.setReadTimeout(8000);

        this.http = new RestTemplate(factory);
    }

    public GescoLoginResponse login(String username, String password) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(token);

        GescoLoginRequest body = new GescoLoginRequest(username, password);
        HttpEntity<GescoLoginRequest> req = new HttpEntity<>(body, headers);

        ResponseEntity<GescoLoginResponse> res =
                http.exchange(baseUrl, HttpMethod.POST, req, GescoLoginResponse.class);

        return res.getBody();
    }
}

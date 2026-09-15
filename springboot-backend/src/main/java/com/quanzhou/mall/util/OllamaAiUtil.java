package com.quanzhou.mall.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quanzhou.mall.bean.OllamaRequest;
import com.quanzhou.mall.bean.OllamaResponse;
import org.springframework.http.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.ResourceAccessException;

import javax.annotation.PostConstruct;

/**
 * Ollama 本地模型调用工具
 * 超时 60 秒，stream 关闭一次性返回完整文本
 */
@Component
public class OllamaAiUtil {

    @Value("${ollama.base-url:http://localhost:11434}")
    private String baseUrl;

    @Value("${ollama.connect-timeout-ms:5000}")
    private int connectTimeoutMs;

    @Value("${ollama.read-timeout-ms:60000}")
    private int readTimeoutMs;

    private RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @PostConstruct
    public void init() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(connectTimeoutMs);
        factory.setReadTimeout(readTimeoutMs);
        restTemplate = new RestTemplate(factory);
        // 每个请求完成后关闭连接，避免 Ollama 侧 keep-alive 超时问题
        restTemplate.getInterceptors().add((request, body, execution) -> {
            request.getHeaders().set("Connection", "close");
            return execution.execute(request, body);
        });
    }

    /**
     * 调用 Ollama chat API，返回模型回复文本
     * @param request 请求体（model, messages, stream）
     * @return AI 回复内容
     * @throws Exception 超时/空响应异常
     */
    public String chat(OllamaRequest request) throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        String jsonBody = objectMapper.writeValueAsString(request);
        HttpEntity<String> entity = new HttpEntity<>(jsonBody, headers);

        ResponseEntity<OllamaResponse> resp;
        try {
            resp = restTemplate.postForEntity(baseUrl + "/api/chat", entity, OllamaResponse.class);
        } catch (ResourceAccessException e) {
            throw new Exception("timeout");
        }

        if (resp.getBody() == null || resp.getBody().getMessage() == null) {
            throw new Exception("empty");
        }
        return resp.getBody().getMessage().getContent();
    }
}

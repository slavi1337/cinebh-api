package com.cinebh.api.support;

import com.cinebh.api.dto.common.PageResponse;
import com.cinebh.api.exceptions.GlobalExceptionHandler;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

public final class ControllerTestUtils {

    public static final int DEFAULT_PAGE = 0;
    public static final int DEFAULT_SIZE = 10;

    private ControllerTestUtils() {
    }

    public static MockMvc standaloneMockMvc(Object controller) {
        return MockMvcBuilders
                .standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    public static <T> PageResponse<T> emptyPageResponse() {
        return new PageResponse<>(List.of(), DEFAULT_PAGE, DEFAULT_SIZE, 0, 0);
    }

    public static MockHttpServletRequestBuilder getJson(String url) {
        return get(url).accept(MediaType.APPLICATION_JSON);
    }

    public static MockHttpServletRequestBuilder getJsonWithPagination(String url) {
        return get(url)
                .param("page", String.valueOf(DEFAULT_PAGE))
                .param("size", String.valueOf(DEFAULT_SIZE))
                .accept(MediaType.APPLICATION_JSON);
    }

    public static void expectPageResponse(ResultActions result) throws Exception {
        result.andExpect(jsonPath("$.page").value(DEFAULT_PAGE))
                .andExpect(jsonPath("$.size").value(DEFAULT_SIZE));
    }

    public static void expectErrorResponse(
            ResultActions result,
            String message,
            int status
    ) throws Exception {
        result.andExpect(jsonPath("$.message").value(message))
                .andExpect(jsonPath("$.status").value(status));
    }
}

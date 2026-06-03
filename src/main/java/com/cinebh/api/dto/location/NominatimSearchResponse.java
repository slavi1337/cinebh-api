package com.cinebh.api.dto.location;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public record NominatimSearchResponse(
        @JsonProperty("display_name")
        String displayName,
        @JsonProperty("addresstype")
        String addressType,
        @JsonProperty("class")
        String category,
        String type,
        Map<String, String> address
) {
}

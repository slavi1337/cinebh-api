package com.cinebh.api.dto.profile;

import java.util.List;

public record ProfileLocationOptionsResponse(
        List<CountryOptionResponse> countries
) {
}

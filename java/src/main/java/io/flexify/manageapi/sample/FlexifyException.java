package io.flexify.manageapi.sample;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.flexify.apiclient.handler.ApiException;

public class FlexifyException {
    public Long id;
    public String message;
    public Object args[];

    public static FlexifyException fromApi(ApiException apiEx) throws IOException {
        if (apiEx.getCode() != 422) {
            // FlexifyException uses HTTP code 422
            return null;
        }

        ObjectMapper mapper = new ObjectMapper();
        try {
            FlexifyException res = mapper.readValue(apiEx.getResponseBody(), FlexifyException.class);
            return res;
        } catch (JsonParseException | JsonMappingException ex) {
            System.out.format("Cannot parse response: %s%n", ex.toString());
            return null;
        }
    }
}

package com.smartcampus.resource;

import java.util.LinkedHashMap;
import java.util.Map;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.core.Context;

@Path("/")
@Produces(MediaType.APPLICATION_JSON)
public class DiscoveryResource {

    @GET
    public Response discover(@Context UriInfo uriInfo) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("apiName", "Smart Campus Sensor & Room Management API");
        payload.put("version", "v1");
        payload.put("contact", Map.of(
                "team", "Smart Campus Facilities Integration Team",
                "email", "facilities-api@westminster.ac.uk"
        ));
        payload.put("resources", Map.of(
                "rooms", uriInfo.getBaseUriBuilder().path("rooms").build().toString(),
                "sensors", uriInfo.getBaseUriBuilder().path("sensors").build().toString()
        ));
        payload.put("description",
                "REST API for managing campus rooms, sensors, and nested sensor reading history.");

        return Response.ok(payload).build();
    }
}


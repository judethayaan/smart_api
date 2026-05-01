package com.smartcampus.resource;

import com.smartcampus.model.SensorReading;
import com.smartcampus.store.CampusStore;
import java.net.URI;
import java.util.Map;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

@Produces(MediaType.APPLICATION_JSON)
public class SensorReadingResource {

    private final String sensorId;
    private final CampusStore store = CampusStore.getInstance();

    public SensorReadingResource(String sensorId) {
        this.sensorId = sensorId;
    }

    @GET
    public Response getReadings() {
        return Response.ok(store.listReadings(sensorId)).build();
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createReading(SensorReading reading, @Context UriInfo uriInfo) {
        SensorReading createdReading = store.addReading(sensorId, reading);
        URI location = uriInfo.getAbsolutePathBuilder().path(createdReading.getId()).build();

        return Response.created(location)
                .entity(Map.of(
                        "message", "Sensor reading recorded successfully.",
                        "reading", createdReading,
                        "sensor", store.getSensor(sensorId)
                ))
                .build();
    }
}


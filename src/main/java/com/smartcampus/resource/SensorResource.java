package com.smartcampus.resource;

import com.smartcampus.model.Sensor;
import com.smartcampus.store.CampusStore;
import java.net.URI;
import java.util.Map;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

@Path("/sensors")
@Produces(MediaType.APPLICATION_JSON)
public class SensorResource {

    private final CampusStore store = CampusStore.getInstance();

    @GET
    public Response getSensors(@QueryParam("type") String type) {
        return Response.ok(store.listSensors(type)).build();
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createSensor(Sensor sensor, @Context UriInfo uriInfo) {
        Sensor createdSensor = store.createSensor(sensor);
        URI location = uriInfo.getAbsolutePathBuilder().path(createdSensor.getId()).build();

        return Response.created(location)
                .entity(Map.of(
                        "message", "Sensor created successfully.",
                        "sensor", createdSensor
                ))
                .build();
    }

    @GET
    @Path("/{sensorId}")
    public Response getSensor(@PathParam("sensorId") String sensorId) {
        return Response.ok(store.getSensor(sensorId)).build();
    }

    @DELETE
    @Path("/{sensorId}")
    public Response deleteSensor(@PathParam("sensorId") String sensorId) {
        store.deleteSensor(sensorId);
        return Response.ok(Map.of(
                "message", "Sensor deleted successfully.",
                "sensorId", sensorId
        )).build();
    }

    @Path("/{sensorId}/readings")
    public SensorReadingResource getSensorReadingResource(@PathParam("sensorId") String sensorId) {
        store.getSensor(sensorId);
        return new SensorReadingResource(sensorId);
    }
}


package com.smartcampus.resource;

import com.smartcampus.model.Room;
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
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

@Path("/rooms")
@Produces(MediaType.APPLICATION_JSON)
public class SensorRoomResource {

    private final CampusStore store = CampusStore.getInstance();

    @GET
    public Response getRooms() {
        return Response.ok(store.listRooms()).build();
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createRoom(Room room, @Context UriInfo uriInfo) {
        Room createdRoom = store.createRoom(room);
        URI location = uriInfo.getAbsolutePathBuilder().path(createdRoom.getId()).build();

        return Response.created(location)
                .entity(Map.of(
                        "message", "Room created successfully.",
                        "room", createdRoom
                ))
                .build();
    }

    @GET
    @Path("/{roomId}")
    public Response getRoom(@PathParam("roomId") String roomId) {
        return Response.ok(store.getRoom(roomId)).build();
    }

    @DELETE
    @Path("/{roomId}")
    public Response deleteRoom(@PathParam("roomId") String roomId) {
        store.deleteRoom(roomId);
        return Response.ok(Map.of(
                "message", "Room deleted successfully.",
                "roomId", roomId
        )).build();
    }
}


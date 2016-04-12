package com.github.devoxx.university.server;

import static javax.ws.rs.core.Response.ok;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Produces(MediaType.TEXT_PLAIN)
@Path("/status")
public class StatusEndpoint {

    @GET
    public Response status(){
        return ok("fine").build();
    }
}

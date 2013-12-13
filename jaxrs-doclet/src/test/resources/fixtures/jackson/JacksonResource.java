package fixtures.jackson;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/jackson")
public class JacksonResource {
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public ResponseModel get() {
        return new ResponseModel();
    }

    /**
     * @output void
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response create(PayloadModel payload) {
        return Response.ok().build();
    }
}

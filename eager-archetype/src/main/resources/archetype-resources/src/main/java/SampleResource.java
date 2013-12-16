package ${package};

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import java.lang.String;
import java.util.*;
import java.net.URI;

@Path("/data")
public class SampleResource {

    private static final List<DataObject> dataStore = new ArrayList<DataObject>();

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public DataObjectList getDataObjects() {
        DataObjectList list = new DataObjectList();
        list.setDataObjects(dataStore);
        return list;
    }

    /**
     * @responseMessage 201 DataObject resource created
     * @output ${package}.DataObject
     */
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    public Response addDataObject(@FormParam("data") String data) {
        DataObject obj = new DataObject();
        obj.setId(UUID.randomUUID().toString());
        obj.setData(data);
        dataStore.add(obj);
        return Response.created(URI.create("/" + obj.getId())).entity(obj).build();
    }
}
package com.redhat;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.rest.client.annotation.ClientHeaderParam;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@Path("/1/cards")
@RegisterRestClient
@ClientHeaderParam(name = "Authorization", value = "{getAuthHeader}")
public interface TrelloClient {

    default String getAuthHeader(){
        final Config config = ConfigProvider.getConfig();
        return String.format("OAuth oauth_consumer_key=\"%s\", oauth_token=\"%s\"", config.getValue("trello.auth.key", String.class), config.getValue("trello.auth.token", String.class));
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    String create(String card);

    @Path("{id}/attachments")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    String attach(@PathParam("id") String cardId, String attachment);
    
}

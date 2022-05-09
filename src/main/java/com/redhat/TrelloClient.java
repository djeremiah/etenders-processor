package com.redhat;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.rest.client.annotation.ClientHeaderParam;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@RegisterRestClient
@ClientHeaderParam(name = "Authorization", value = "{getAuthHeader}")
public interface TrelloClient {

    /**
     * <a href="https://developer.atlassian.com/cloud/trello/guides/rest-api/authorization/#authorization-header">Trello Authorization</a>
     */
    default String getAuthHeader(){
        final Config config = ConfigProvider.getConfig();
        return String.format("OAuth oauth_consumer_key=\"%s\", oauth_token=\"%s\"", config.getValue("trello.auth.key", String.class), config.getValue("trello.auth.token", String.class));
    }

    /** 
     * <a href="https://developer.atlassian.com/cloud/trello/rest/api-group-cards/#api-cards-post">Create Card</a>
     */
    @POST
    @Path("/1/cards")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    String create(String card);

    /** 
     * <a href="https://developer.atlassian.com/cloud/trello/rest/api-group-cards/#api-cards-id-put">Update Card</a>
     */
    @PUT
    @Path("/1/cards/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    String update(@PathParam("id") String cardId, String card);

    /**
     * <a href="https://developer.atlassian.com/cloud/trello/rest/api-group-cards/#api-cards-id-attachments-post">Create Attachment</a>
     */
    @POST
    @Path("/1/cards/{id}/attachments")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    String attach(@PathParam("id") String cardId, String attachment);


    /**
     * <a href="https://developer.atlassian.com/cloud/trello/rest/api-group-search/#api-search-get">Search Trello </a>
     */
    @GET
    @Path("/1/search")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    String search(@QueryParam("query") String query, @QueryParam("idBoards") String idBoards, @QueryParam("modelTypes") String modelTypes);

    
}

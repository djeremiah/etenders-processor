package com.redhat;

import java.net.URI;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.google.api.services.gmail.model.Message;
import com.google.common.io.BaseEncoding;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.jackson.JacksonDataFormat;
import org.apache.camel.support.processor.idempotent.MemoryIdempotentRepository;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;

@ApplicationScoped
public class ETendersProcessor extends RouteBuilder{
    private static final Pattern TENDER_PATTERN = Pattern.compile("<b><a href=\"(?<uri>.*)\">(?<subject>.*)</a></b><br /><b>Publication date:</b>(?<publicationDate>(?s).*?)<br /><b>Response deadline:</b>(?<responseDeadline>(?s).*?)<br /><b>Procedure:</b>(?<procedure>(?s).*?)<br /><b>Description:</b>(?<description>(?s).*?)<br /><b>Buyer:</b>(?<buyer>(?s).*?)<br />");
    private static final int DUE_REMINDER_OFF = -1;

    @Inject
    @RestClient
    TrelloClient trelloCardApi;

    @ConfigProperty(name = "trello.boardId")
    String boardId;

    @ConfigProperty(name = "trello.listId")
    String listId;

    @Override
    public void configure() throws Exception {
        JacksonDataFormat json = new JacksonDataFormat();
        json.setAutoDiscoverObjectMapper(true);

        errorHandler(deadLetterChannel("file:errors")
            .useOriginalMessage());

        from("google-mail-stream://index?labels=etenders&delay=2000").routeId("Process eTenders Email")
            .idempotentConsumer(header("CamelGoogleMailId"), MemoryIdempotentRepository.memoryIdempotentRepository(10))    
            .toD("google-mail://messages/get?userId=me&id=${header.CamelGoogleMailId}")
            .process(exchange -> {
                Message message = exchange.getIn().getBody(Message.class);
                exchange.getIn().setBody(new String(BaseEncoding.base64Url().decode(message.getPayload().getBody().getData())));
            })
            .process(exchange -> {
                String body = exchange.getIn().getBody(String.class).replaceAll("\u00a0", " ");

                Matcher tenders = TENDER_PATTERN.matcher(body);
                List<Tender> tenderList = new ArrayList<>();
                while(tenders.find()){
                    Tender tender = new Tender(tenders.group("subject").trim(), 
                        URI.create(tenders.group("uri")), 
                        LocalDate.parse(tenders.group("publicationDate").replaceAll("\\s", ""), DateTimeFormatter.ofPattern("d-M-yyyy")), 
                        ZonedDateTime.parse(tenders.group("responseDeadline").replaceAll("\\s{2,}", " ").replace("Irish time", "Europe/Dublin").trim(), DateTimeFormatter.ofPattern("d-M-yyyy HH:mm z")), 
                        tenders.group("procedure").trim(), 
                        tenders.group("description").trim(), 
                        tenders.group("buyer").trim());
                    tenderList.add(tender);
                }

                exchange.getIn().setBody(tenderList);
            })
            .split(body())
            .process(exchange -> {
                Tender tender = exchange.getIn().getBody(Tender.class);
                Card card = new Card(tender.buyer() +  " | " + tender.subject(), 
                    tender.description(), 
                    tender.publicationDate(), 
                    tender.responseDeadline(), 
                    DUE_REMINDER_OFF, 
                    boardId,  // board id
                    listId); // list id
                exchange.getIn().setBody(card);
                exchange.getIn().setHeader("TrelloAttachmentURL", tender.uri());
            })
            .marshal(json)
            .setBody(exchange ->
                trelloCardApi.create(exchange.getIn().getBody(String.class))
            )
            .setHeader("TrelloCardId", jsonpath("$.id"))
            .process(exchange -> {
                exchange.getIn().setBody(new Attachment(exchange.getIn().getHeader("TrelloAttachmentURL", URI.class)));
            })
            .marshal(json)
            .process(exchange -> {
                trelloCardApi.attach(exchange.getIn().getHeader("TrelloCardId", String.class), exchange.getIn().getBody(String.class));
            })
            .to("log:com.redhat?level=INFO&showHeaders=true");
    }  
}

record Tender(
    String subject,
    URI uri,
    LocalDate publicationDate,
    ZonedDateTime responseDeadline,
    String procedure,
    String description,
    String buyer){};

record Card(
    String name,    
    String desc,
    LocalDate start,
    ZonedDateTime due,
    int dueReminder,
    String idBoard,
    String idList){};

record Attachment(
    URI url){};
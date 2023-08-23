package com.function;

import com.microsoft.azure.functions.annotation.*;
import com.microsoft.azure.functions.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.*;
import com.azure.storage.blob.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class OrderItemsReserverFunction {
    @FunctionName("OrderItemsReserver")
    public HttpResponseMessage run(
            @HttpTrigger(name = "req", methods = {
                    HttpMethod.POST }, authLevel = AuthorizationLevel.ANONYMOUS) HttpRequestMessage<Optional<Map<String, Object>>> request,
            final ExecutionContext context) throws JsonProcessingException {
        context.getLogger().info("Java HTTP trigger processed a request.");

        Map<String, Object> requestBody = request.getBody().orElse(null);

        if (requestBody == null) {
            return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
                    .body("Invalid request body")
                    .build();
        }

        ObjectMapper objectMapper = new ObjectMapper();
        String orderRequestJson = objectMapper.writeValueAsString(requestBody);

        String connectionString = <your-storage-account-connection-string>;
        String containerName = "order-requests";
        JsonNode jsonNode = objectMapper.readTree(orderRequestJson);
        String sessionId = jsonNode.get("sessionId").asText();
        String blobName = sessionId + ".json";

        BlobServiceClient blobServiceClient = new BlobServiceClientBuilder().connectionString(connectionString)
                .buildClient();
        BlobContainerClient containerClient = blobServiceClient.getBlobContainerClient(containerName);
        BlobClient blobClient = containerClient.getBlobClient(blobName);
        byte[] data = orderRequestJson.getBytes();
        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(data)) {
            blobClient.upload(inputStream, data.length, true);
            context.getLogger().info("Order request uploaded to Blob Storage.");
        } catch (IOException e) {
            context.getLogger().warning("Error uploading order request to blob storage");
        }
        return request.createResponseBuilder(HttpStatus.OK)
                .body("Order updated and stored in blob.")
                .build();
    }
}


import co.edu.escuelaing.http.ApiGatewayOrdersHandler;
import co.edu.escuelaing.repository.InMemoryOrderRepository;
import co.edu.escuelaing.service.OrderService;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ApiGatewayOrdersHandlerTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final Map<String, String> JSON_HEADERS = Map.of("Content-Type", "application/json");

    private ApiGatewayOrdersHandler handler;

    @BeforeEach
    void setUp() {
        handler = new ApiGatewayOrdersHandler(
                new OrderService(InMemoryOrderRepository.withSampleData()), false);
    }

    // ---------------------------------------------------------------- helpers

    private static APIGatewayProxyRequestEvent request(String method, String resource) {
        return new APIGatewayProxyRequestEvent()
                .withHttpMethod(method)
                .withResource(resource);
    }

    private static APIGatewayProxyRequestEvent getById(String id) {
        return request("GET", "/orders/{id}")
                .withPath("/orders/" + id)
                .withPathParameters(Map.of("id", id));
    }

    private static APIGatewayProxyRequestEvent post(String body, Map<String, String> headers) {
        APIGatewayProxyRequestEvent.ProxyRequestContext ctx = new APIGatewayProxyRequestEvent.ProxyRequestContext();
        ctx.setPath("/dev/orders");
        return request("POST", "/orders")
                .withPath("/orders")
                .withHeaders(headers)
                .withBody(body)
                .withRequestContext(ctx);
    }

    private APIGatewayProxyResponseEvent call(APIGatewayProxyRequestEvent event) {
        return handler.handleRequest(event, null);
    }

    private static JsonNode body(APIGatewayProxyResponseEvent response) throws Exception {
        return MAPPER.readTree(response.getBody());
    }

    // ------------------------------------------------------- GET /orders/{id}

    @Test
    void getExistingOrder_returns200WithOrder() throws Exception {
        APIGatewayProxyResponseEvent response = call(getById("ORD-001"));

        assertEquals(200, response.getStatusCode());
        assertEquals("application/json", response.getHeaders().get("Content-Type"));
        assertEquals("ORD-001", body(response).get("id").asText());
        assertEquals("CUST-42", body(response).get("customerId").asText());
    }

    @Test
    void getMissingOrder_returns404WithErrorFormat() throws Exception {
        APIGatewayProxyResponseEvent response = call(getById("ORD-999"));

        assertEquals(404, response.getStatusCode());
        assertEquals("ORDER_NOT_FOUND", body(response).get("error").asText());
        assertEquals("Order ORD-999 does not exist", body(response).get("message").asText());
    }

    // ---------------------------------------------- GET /orders?customerId=

    @Test
    void findByCustomer_returns200WithList() throws Exception {
        APIGatewayProxyResponseEvent response = call(request("GET", "/orders")
                .withQueryStringParameters(Map.of("customerId", "CUST-42")));

        assertEquals(200, response.getStatusCode());
        assertTrue(body(response).isArray());
        assertEquals(2, body(response).size());
    }

    @Test
    void findByUnknownCustomer_returns200WithEmptyList() throws Exception {
        APIGatewayProxyResponseEvent response = call(request("GET", "/orders")
                .withQueryStringParameters(Map.of("customerId", "CUST-100")));

        assertEquals(200, response.getStatusCode());
        assertEquals(0, body(response).size());
    }

    @Test
    void findWithoutCustomerId_returns400() throws Exception {
        APIGatewayProxyResponseEvent response = call(request("GET", "/orders"));

        assertEquals(400, response.getStatusCode());
        assertEquals("VALIDATION_ERROR", body(response).get("error").asText());
    }

    // ------------------------------------------------------------ POST /orders

    @Test
    void postValidOrder_returns201WithLocation() throws Exception {
        APIGatewayProxyResponseEvent response = call(post(
                "{\"customerId\":\"CUST-42\",\"product\":\"MacBook Air\",\"quantity\":1}", JSON_HEADERS));

        assertEquals(201, response.getStatusCode());
        String id = body(response).get("id").asText();
        assertEquals("ORD-004", id);
        assertEquals("/dev/orders/ORD-004", response.getHeaders().get("Location"));
    }

    @Test
    void postInvalidOrder_returns400() throws Exception {
        APIGatewayProxyResponseEvent response = call(post(
                "{\"customerId\":\"CUST-42\",\"product\":\"\",\"quantity\":0}", JSON_HEADERS));

        assertEquals(400, response.getStatusCode());
        assertEquals("product is required", body(response).get("message").asText());
    }

    @Test
    void postWithWrongFieldType_returns400() throws Exception {
        APIGatewayProxyResponseEvent response = call(post(
                "{\"customerId\":\"CUST-42\",\"product\":\"X\",\"quantity\":\"abc\"}", JSON_HEADERS));

        assertEquals(400, response.getStatusCode());
        assertEquals("VALIDATION_ERROR", body(response).get("error").asText());
    }

    @Test
    void postMalformedJson_returns400() throws Exception {
        APIGatewayProxyResponseEvent response = call(post("hola", JSON_HEADERS));

        assertEquals(400, response.getStatusCode());
    }

    @Test
    void postWithoutBody_returns400() throws Exception {
        APIGatewayProxyResponseEvent response = call(post(null, JSON_HEADERS));

        assertEquals(400, response.getStatusCode());
        assertEquals("Request body is required", body(response).get("message").asText());
    }

    @Test
    void postWithTextPlain_returns415() throws Exception {
        APIGatewayProxyResponseEvent response = call(post("hola", Map.of("content-type", "text/plain")));

        assertEquals(415, response.getStatusCode());
        assertEquals("UNSUPPORTED_MEDIA_TYPE", body(response).get("error").asText());
    }

    // ------------------------------------------------------- integración/ruteo

    @Test
    void createdOrder_canBeRetrievedAfterwards() throws Exception {
        APIGatewayProxyResponseEvent created = call(post(
                "{\"customerId\":\"CUST-9\",\"product\":\"Monitor\",\"quantity\":2}", JSON_HEADERS));
        String id = body(created).get("id").asText();

        APIGatewayProxyResponseEvent fetched = call(getById(id));

        assertEquals(200, fetched.getStatusCode());
        assertEquals("Monitor", body(fetched).get("product").asText());
    }

    @Test
    void unknownRoute_returns404() throws Exception {
        APIGatewayProxyResponseEvent response = call(request("PATCH", "/orders/{id}"));

        assertEquals(404, response.getStatusCode());
        assertEquals("ROUTE_NOT_FOUND", body(response).get("error").asText());
    }
}
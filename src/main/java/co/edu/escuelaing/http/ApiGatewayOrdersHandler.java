package co.edu.escuelaing.http;

import co.edu.escuelaing.model.CreateOrderRequest;
import co.edu.escuelaing.model.Order;
import co.edu.escuelaing.repository.InMemoryOrderRepository;
import co.edu.escuelaing.service.OrderNotFoundException;
import co.edu.escuelaing.service.OrderService;
import co.edu.escuelaing.service.ValidationException;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Adaptador HTTP entre API Gateway (integración proxy) y la lógica de aplicación.
 *
 * Handler en Lambda: co.edu.escuelaing.http.ApiGatewayOrdersHandler::handleRequest
 *
 * Responsabilidades (todas HTTP / AWS, ninguna de negocio):
 *  - enrutar según método HTTP + resource de API Gateway;
 *  - extraer path parameters, query parameters, headers y body;
 *  - convertir JSON <-> objetos Java;
 *  - traducir excepciones de dominio a status codes y al formato de error;
 *  - construir headers de respuesta (Content-Type, Location).
 */
public class ApiGatewayOrdersHandler
        implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    static final String ORDERS = "/orders";
    static final String ORDER_BY_ID = "/orders/{id}";

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    /** Una instancia por entorno de ejecución; compartida por todas las rutas. */
    private static final OrderService DEFAULT_SERVICE =
            new OrderService(InMemoryOrderRepository.withSampleData());

    private final OrderService service;
    private final boolean logEvents;

    /** Constructor usado por Lambda. LOG_EVENTS=true imprime el evento recibido en CloudWatch. */
    public ApiGatewayOrdersHandler() {
        this(DEFAULT_SERVICE, "true".equalsIgnoreCase(System.getenv("LOG_EVENTS")));
    }

    /** Constructor para tests. */
    public ApiGatewayOrdersHandler(OrderService service, boolean logEvents) {
        this.service = service;
        this.logEvents = logEvents;
    }

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent event, Context context) {
        if (logEvents) {
            log(context, "Incoming event: " + toJsonQuietly(event));
        }
        try {
            return route(event);
        } catch (ValidationException e) {
            return error(400, "VALIDATION_ERROR", e.getMessage());
        } catch (OrderNotFoundException e) {
            return error(404, "ORDER_NOT_FOUND", e.getMessage());
        } catch (Exception e) {
            // El detalle va a CloudWatch; al cliente solo un mensaje genérico.
            log(context, "Unexpected error: " + e);
            e.printStackTrace();
            return error(500, "INTERNAL_ERROR", "Unexpected internal error");
        }
    }

    // ------------------------------------------------------------------ ruteo

    private APIGatewayProxyResponseEvent route(APIGatewayProxyRequestEvent event) {
        String method = event.getHttpMethod();
        String resource = event.getResource();

        if (ORDERS.equals(resource) && "POST".equals(method)) {
            return createOrder(event);
        }
        if (ORDERS.equals(resource) && "GET".equals(method)) {
            return findOrdersByCustomer(event);
        }
        if (ORDER_BY_ID.equals(resource) && "GET".equals(method)) {
            return getOrder(event);
        }
        if (ORDER_BY_ID.equals(resource) && "DELETE".equals(method)) {
            return deleteOrder(event);
        }
        return error(404, "ROUTE_NOT_FOUND", "No route for " + method + " " + resource);
    }

    // ------------------------------------------------------------ operaciones

    private APIGatewayProxyResponseEvent createOrder(APIGatewayProxyRequestEvent event) {
        String contentType = header(event, "Content-Type");
        if (contentType != null && !contentType.toLowerCase().startsWith("application/json")) {
            return error(415, "UNSUPPORTED_MEDIA_TYPE", "Content-Type must be application/json");
        }

        String body = body(event);
        if (body == null || body.isBlank()) {
            throw new ValidationException("Request body is required");
        }

        CreateOrderRequest request;
        try {
            request = MAPPER.readValue(body, CreateOrderRequest.class);
        } catch (JsonProcessingException e) {
            throw new ValidationException("Request body is not valid JSON or has invalid field types");
        }

        Order created = service.createOrder(request);

        APIGatewayProxyResponseEvent response = json(201, created);
        response.getHeaders().put("Location", locationOf(event, created));
        return response;
    }

    private APIGatewayProxyResponseEvent getOrder(APIGatewayProxyRequestEvent event) {
        String id = param(event.getPathParameters(), "id");
        return json(200, service.getOrder(id));
    }

    private APIGatewayProxyResponseEvent findOrdersByCustomer(APIGatewayProxyRequestEvent event) {
        String customerId = param(event.getQueryStringParameters(), "customerId");
        List<Order> orders = service.getOrdersByCustomer(customerId);
        return json(200, orders);
    }

    /**
     * Éxito → 204 sin body.
     * Si la orden no existe, OrderService lanza OrderNotFoundException y el catch
     * de handleRequest responde 404: no hace falta código nuevo para ese caso.
     */
    private APIGatewayProxyResponseEvent deleteOrder(APIGatewayProxyRequestEvent event) {
        String id = param(event.getPathParameters(), "id");
        service.deleteOrder(id);
        return noContent();
    }

    // ---------------------------------------------------------------- helpers

    /**
     * Construye la URL del recurso creado. requestContext.path incluye el stage
     * (por ejemplo "/dev/orders"), así el Location apunta a una URL que realmente existe.
     */
    private static String locationOf(APIGatewayProxyRequestEvent event, Order order) {
        String basePath = null;
        if (event.getRequestContext() != null) {
            basePath = event.getRequestContext().getPath();
        }
        if (basePath == null) {
            basePath = event.getPath() != null ? event.getPath() : ORDERS;
        }
        if (basePath.endsWith("/")) {
            basePath = basePath.substring(0, basePath.length() - 1);
        }
        return basePath + "/" + order.getId();
    }

    private static String param(Map<String, String> params, String name) {
        return params == null ? null : params.get(name);
    }

    /** Los nombres de header HTTP no distinguen mayúsculas/minúsculas. */
    private static String header(APIGatewayProxyRequestEvent event, String name) {
        if (event.getHeaders() == null) {
            return null;
        }
        return event.getHeaders().entrySet().stream()
                .filter(entry -> entry.getKey() != null && entry.getKey().equalsIgnoreCase(name))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse(null);
    }

    private static String body(APIGatewayProxyRequestEvent event) {
        String body = event.getBody();
        if (body != null && Boolean.TRUE.equals(event.getIsBase64Encoded())) {
            return new String(Base64.getDecoder().decode(body), StandardCharsets.UTF_8);
        }
        return body;
    }

    private static APIGatewayProxyResponseEvent json(int statusCode, Object payload) {
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        return new APIGatewayProxyResponseEvent()
                .withStatusCode(statusCode)
                .withHeaders(headers)
                .withBody(toJson(payload));
    }

    /** 204 No Content: sin body y, por tanto, sin Content-Type. */
    private static APIGatewayProxyResponseEvent noContent() {
        return new APIGatewayProxyResponseEvent()
                .withStatusCode(204)
                .withHeaders(new HashMap<>());
    }

    private static APIGatewayProxyResponseEvent error(int statusCode, String code, String message) {
        return json(statusCode, new ErrorResponse(code, message));
    }

    private static String toJson(Object value) {
        try {
            return MAPPER.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Could not serialize response", e);
        }
    }

    private static String toJsonQuietly(Object value) {
        try {
            return MAPPER.writeValueAsString(value);
        } catch (Exception e) {
            return String.valueOf(value);
        }
    }

    private static void log(Context context, String message) {
        if (context != null && context.getLogger() != null) {
            context.getLogger().log(message + "\n");
        } else {
            System.out.println(message);
        }
    }
}
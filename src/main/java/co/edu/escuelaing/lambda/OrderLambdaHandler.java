package co.edu.escuelaing.lambda;


import co.edu.escuelaing.model.CreateOrderRequest;
import co.edu.escuelaing.model.Order;
import co.edu.escuelaing.repository.InMemoryOrderRepository;
import co.edu.escuelaing.service.OrderNotFoundException;
import co.edu.escuelaing.service.OrderService;
import co.edu.escuelaing.service.ValidationException;

import java.util.List;
import java.util.function.Supplier;

/**
 * Punto de entrada para AWS Lambda (integración directa / sin proxy).
 *
 * Handlers a configurar en la consola de Lambda:
 *   com.workshop.orders.lambda.OrderLambdaHandler::createOrder
 *   com.workshop.orders.lambda.OrderLambdaHandler::getOrder
 *   com.workshop.orders.lambda.OrderLambdaHandler::findOrdersByCustomer
 *
 * No depende de ninguna librería de AWS: el runtime de Lambda convierte
 * el JSON de entrada al tipo del parámetro y el valor de retorno a JSON.
 */
public class OrderLambdaHandler {

    /**
     * Se crea una vez por entorno de ejecución (cold start) y se reutiliza
     * en las invocaciones "calientes". No es persistente: si Lambda descarta
     * el entorno, los datos creados se pierden.
     */
    private static final OrderService DEFAULT_SERVICE =
            new OrderService(InMemoryOrderRepository.withSampleData());

    private final OrderService service;

    /** Constructor que usa Lambda. */
    public OrderLambdaHandler() {
        this(DEFAULT_SERVICE);
    }

    /** Constructor para tests: permite inyectar un servicio limpio. */
    public OrderLambdaHandler(OrderService service) {
        this.service = service;
    }

    public Order createOrder(CreateOrderRequest request) {
        return execute(() -> service.createOrder(request));
    }

    public Order getOrder(GetOrderInput input) {
        return execute(() -> service.getOrder(input == null ? null : input.getId()));
    }

    public List<Order> findOrdersByCustomer(FindOrdersInput input) {
        return execute(() -> service.getOrdersByCustomer(input == null ? null : input.getCustomerId()));
    }

    /** Traduce excepciones de dominio a códigos de error de la función. */
    private <T> T execute(Supplier<T> action) {
        try {
            return action.get();
        } catch (ValidationException e) {
            throw new OrderFunctionException("VALIDATION_ERROR", e.getMessage(), e);
        } catch (OrderNotFoundException e) {
            throw new OrderFunctionException("ORDER_NOT_FOUND", e.getMessage(), e);
        } catch (RuntimeException e) {
            // Se registra el detalle en CloudWatch, pero no se expone al cliente.
            System.err.println("Unexpected error: " + e);
            e.printStackTrace();
            throw new OrderFunctionException("INTERNAL_ERROR", "Unexpected internal error", e);
        }
    }
}
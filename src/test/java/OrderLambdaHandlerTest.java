

import co.edu.escuelaing.lambda.FindOrdersInput;
import co.edu.escuelaing.lambda.GetOrderInput;
import co.edu.escuelaing.lambda.OrderFunctionException;
import co.edu.escuelaing.lambda.OrderLambdaHandler;
import co.edu.escuelaing.model.CreateOrderRequest;
import co.edu.escuelaing.model.Order;
import co.edu.escuelaing.repository.InMemoryOrderRepository;
import co.edu.escuelaing.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class OrderLambdaHandlerTest {

    private OrderLambdaHandler handler;

    @BeforeEach
    void setUp() {
        handler = new OrderLambdaHandler(
                new OrderService(InMemoryOrderRepository.withSampleData()));
    }

    @Test
    void getOrder_sampleOrder_isReturned() {
        Order order = handler.getOrder(new GetOrderInput("ORD-001"));

        assertEquals("CUST-42", order.getCustomerId());
        assertEquals("MacBook Air", order.getProduct());
    }

    @Test
    void getOrder_missingOrder_reportsOrderNotFoundCode() {
        OrderFunctionException ex = assertThrows(
                OrderFunctionException.class,
                () -> handler.getOrder(new GetOrderInput("ORD-999")));

        assertEquals("ORDER_NOT_FOUND", ex.getCode());
        assertEquals("ORDER_NOT_FOUND: Order ORD-999 does not exist", ex.getMessage());
    }

    @Test
    void getOrder_nullInput_reportsValidationCode() {
        OrderFunctionException ex = assertThrows(
                OrderFunctionException.class,
                () -> handler.getOrder(null));

        assertEquals("VALIDATION_ERROR", ex.getCode());
    }

    @Test
    void createOrder_validRequest_returnsNewOrder() {
        Order order = handler.createOrder(new CreateOrderRequest("CUST-42", "Monitor", 2));

        assertEquals("ORD-004", order.getId()); // ORD-001..003 son datos de ejemplo
    }

    @Test
    void createOrder_invalidRequest_reportsValidationCode() {
        OrderFunctionException ex = assertThrows(
                OrderFunctionException.class,
                () -> handler.createOrder(new CreateOrderRequest("CUST-42", "Monitor", 0)));

        assertEquals("VALIDATION_ERROR: quantity must be greater than 0", ex.getMessage());
    }

    @Test
    void findOrdersByCustomer_returnsSampleOrders() {
        List<Order> orders = handler.findOrdersByCustomer(new FindOrdersInput("CUST-42"));

        assertEquals(2, orders.size());
    }

    @Test
    void deleteOrder_existingOrder_completesAndOrderIsGone() {
        assertDoesNotThrow(() -> handler.deleteOrder(new GetOrderInput("ORD-003")));

        OrderFunctionException ex = assertThrows(
                OrderFunctionException.class,
                () -> handler.getOrder(new GetOrderInput("ORD-003")));
        assertEquals("ORDER_NOT_FOUND", ex.getCode());
    }

    @Test
    void deleteOrder_missingOrder_reportsOrderNotFoundCode() {
        OrderFunctionException ex = assertThrows(
                OrderFunctionException.class,
                () -> handler.deleteOrder(new GetOrderInput("ORD-999")));

        assertEquals("ORDER_NOT_FOUND: Order ORD-999 does not exist", ex.getMessage());
    }
}
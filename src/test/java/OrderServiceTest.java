

import co.edu.escuelaing.model.CreateOrderRequest;
import co.edu.escuelaing.model.Order;
import co.edu.escuelaing.repository.InMemoryOrderRepository;
import co.edu.escuelaing.service.OrderNotFoundException;
import co.edu.escuelaing.service.OrderService;
import co.edu.escuelaing.service.ValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class OrderServiceTest {

    private OrderService service;

    @BeforeEach
    void setUp() {
        service = new OrderService(new InMemoryOrderRepository());
    }

    // ---------- Creación ----------

    @Test
    void createOrder_withValidRequest_returnsOrderWithGeneratedId() {
        Order order = service.createOrder(new CreateOrderRequest("CUST-42", "MacBook Air", 1));

        assertNotNull(order.getId());
        assertTrue(order.getId().startsWith("ORD-"));
        assertEquals("CUST-42", order.getCustomerId());
        assertEquals("MacBook Air", order.getProduct());
        assertEquals(1, order.getQuantity());
    }

    @Test
    void createOrder_twice_generatesDifferentIds() {
        Order first = service.createOrder(new CreateOrderRequest("CUST-42", "Mouse", 1));
        Order second = service.createOrder(new CreateOrderRequest("CUST-42", "Mouse", 1));

        assertNotEquals(first.getId(), second.getId());
    }

    @Test
    void createOrder_trimsTextFields() {
        Order order = service.createOrder(new CreateOrderRequest("  CUST-42 ", " Monitor ", 3));

        assertEquals("CUST-42", order.getCustomerId());
        assertEquals("Monitor", order.getProduct());
    }

    // ---------- Consulta por id ----------

    @Test
    void getOrder_existingOrder_returnsIt() {
        Order created = service.createOrder(new CreateOrderRequest("CUST-7", "Keyboard", 2));

        Order found = service.getOrder(created.getId());

        assertEquals(created, found);
    }

    @Test
    void getOrder_nonExistingOrder_throwsOrderNotFound() {
        OrderNotFoundException ex = assertThrows(
                OrderNotFoundException.class,
                () -> service.getOrder("ORD-999"));

        assertEquals("ORD-999", ex.getOrderId());
        assertEquals("Order ORD-999 does not exist", ex.getMessage());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void getOrder_blankId_throwsValidation(String id) {
        assertThrows(ValidationException.class, () -> service.getOrder(id));
    }

    // ---------- Búsqueda por cliente ----------

    @Test
    void getOrdersByCustomer_returnsOnlyThatCustomersOrders() {
        service.createOrder(new CreateOrderRequest("CUST-42", "MacBook Air", 1));
        service.createOrder(new CreateOrderRequest("CUST-42", "Mouse", 2));
        service.createOrder(new CreateOrderRequest("CUST-7", "Keyboard", 1));

        List<Order> result = service.getOrdersByCustomer("CUST-42");

        assertEquals(2, result.size());
        assertTrue(result.stream().allMatch(o -> o.getCustomerId().equals("CUST-42")));
    }

    @Test
    void getOrdersByCustomer_unknownCustomer_returnsEmptyList() {
        service.createOrder(new CreateOrderRequest("CUST-42", "MacBook Air", 1));

        List<Order> result = service.getOrdersByCustomer("CUST-100");

        assertTrue(result.isEmpty());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void getOrdersByCustomer_blankCustomerId_throwsValidation(String customerId) {
        assertThrows(ValidationException.class, () -> service.getOrdersByCustomer(customerId));
    }

    // ---------- Eliminación ----------

    @Test
    void deleteOrder_existingOrder_removesIt() {
        Order created = service.createOrder(new CreateOrderRequest("CUST-42", "MacBook Air", 1));

        service.deleteOrder(created.getId());

        assertThrows(OrderNotFoundException.class, () -> service.getOrder(created.getId()));
    }

    @Test
    void deleteOrder_removesItFromCustomerSearch() {
        Order created = service.createOrder(new CreateOrderRequest("CUST-42", "MacBook Air", 1));
        service.createOrder(new CreateOrderRequest("CUST-42", "Mouse", 1));

        service.deleteOrder(created.getId());

        List<Order> remaining = service.getOrdersByCustomer("CUST-42");
        assertEquals(1, remaining.size());
        assertEquals("Mouse", remaining.get(0).getProduct());
    }

    @Test
    void deleteOrder_nonExistingOrder_throwsOrderNotFound() {
        OrderNotFoundException ex = assertThrows(
                OrderNotFoundException.class,
                () -> service.deleteOrder("ORD-999"));

        assertEquals("ORD-999", ex.getOrderId());
    }

    @Test
    void deleteOrder_twice_secondTimeThrowsOrderNotFound() {
        Order created = service.createOrder(new CreateOrderRequest("CUST-42", "MacBook Air", 1));
        service.deleteOrder(created.getId());

        assertThrows(OrderNotFoundException.class, () -> service.deleteOrder(created.getId()));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void deleteOrder_blankId_throwsValidation(String id) {
        assertThrows(ValidationException.class, () -> service.deleteOrder(id));
    }

    // ---------- Entrada inválida ----------

    static Stream<Arguments> invalidRequests() {
        return Stream.of(
                Arguments.of(null, "Request body is required"),
                Arguments.of(new CreateOrderRequest(null, "Mouse", 1), "customerId is required"),
                Arguments.of(new CreateOrderRequest(" ", "Mouse", 1), "customerId is required"),
                Arguments.of(new CreateOrderRequest("CUST-42", null, 1), "product is required"),
                Arguments.of(new CreateOrderRequest("CUST-42", "", 1), "product is required"),
                Arguments.of(new CreateOrderRequest("CUST-42", "Mouse", null), "quantity is required"),
                Arguments.of(new CreateOrderRequest("CUST-42", "Mouse", 0), "quantity must be greater than 0"),
                Arguments.of(new CreateOrderRequest("CUST-42", "Mouse", -5), "quantity must be greater than 0")
        );
    }

    @ParameterizedTest
    @MethodSource("invalidRequests")
    void createOrder_invalidRequest_throwsValidation(CreateOrderRequest request, String expectedMessage) {
        ValidationException ex = assertThrows(
                ValidationException.class,
                () -> service.createOrder(request));

        assertEquals(expectedMessage, ex.getMessage());
    }
}
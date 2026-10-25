package co.edu.escuelaing.service;


import co.edu.escuelaing.model.CreateOrderRequest;
import co.edu.escuelaing.model.Order;
import co.edu.escuelaing.repository.OrderRepository;

import java.util.List;
import java.util.Objects;

/**
 * Lógica de aplicación. No conoce HTTP, API Gateway ni Lambda.
 * Comunica los fallos con excepciones de dominio, no con códigos de estado.
 */
public class OrderService {

    private final OrderRepository repository;

    public OrderService(OrderRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
    }

    public Order createOrder(CreateOrderRequest request) {
        validate(request);
        Order order = new Order(
                repository.nextId(),
                request.getCustomerId().trim(),
                request.getProduct().trim(),
                request.getQuantity());
        return repository.save(order);
    }

    public Order getOrder(String id) {
        requireNotBlank(id, "id");
        String cleanId = id.trim();
        return repository.findById(cleanId)
                .orElseThrow(() -> new OrderNotFoundException(cleanId));
    }

    public List<Order> getOrdersByCustomer(String customerId) {
        requireNotBlank(customerId, "customerId");
        return repository.findByCustomerId(customerId.trim());
    }

    private void validate(CreateOrderRequest request) {
        if (request == null) {
            throw new ValidationException("Request body is required");
        }
        requireNotBlank(request.getCustomerId(), "customerId");
        requireNotBlank(request.getProduct(), "product");
        if (request.getQuantity() == null) {
            throw new ValidationException("quantity is required");
        }
        if (request.getQuantity() <= 0) {
            throw new ValidationException("quantity must be greater than 0");
        }
    }

    private static void requireNotBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new ValidationException(fieldName + " is required");
        }
    }
}
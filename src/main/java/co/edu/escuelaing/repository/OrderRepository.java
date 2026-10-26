package co.edu.escuelaing.repository;

import co.edu.escuelaing.model.Order;

import java.util.List;
import java.util.Optional;

/**
 * Abstracción del almacenamiento. Hoy es memoria; mañana podría ser DynamoDB
 * sin que OrderService cambie.
 */
public interface OrderRepository {

    Order save(Order order);

    Optional<Order> findById(String id);

    List<Order> findByCustomerId(String customerId);

    /**
     * Elimina la orden con el id indicado.
     * @return true si la orden existía y fue eliminada; false si no existía.
     */
    boolean deleteById(String id);

    String nextId();
}
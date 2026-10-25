package co.edu.escuelaing.repository;
import co.edu.escuelaing.model.Order;


import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Implementación en memoria. OJO: en Lambda esta memoria vive solo mientras
 * exista el entorno de ejecución; NO es almacenamiento persistente.
 */
public class InMemoryOrderRepository implements OrderRepository {

    private final Map<String, Order> orders = new ConcurrentHashMap<>();
    private final AtomicInteger sequence = new AtomicInteger(0);

    /**
     * Repositorio con datos predefinidos. Sirve para que GET funcione en Lambda
     * aunque la petición caiga en un entorno nuevo donde no se ha hecho ningún POST.
     */
    public static InMemoryOrderRepository withSampleData() {
        InMemoryOrderRepository repo = new InMemoryOrderRepository();
        repo.save(new Order(repo.nextId(), "CUST-42", "MacBook Air", 1));
        repo.save(new Order(repo.nextId(), "CUST-42", "Magic Mouse", 2));
        repo.save(new Order(repo.nextId(), "CUST-7", "Keyboard", 2));
        return repo;
    }

    @Override
    public Order save(Order order) {
        orders.put(order.getId(), order);
        return order;
    }

    @Override
    public Optional<Order> findById(String id) {
        return Optional.ofNullable(orders.get(id));
    }

    @Override
    public List<Order> findByCustomerId(String customerId) {
        return orders.values().stream()
                .filter(order -> order.getCustomerId().equals(customerId))
                .sorted(Comparator.comparing(Order::getId))
                .toList();
    }

    @Override
    public String nextId() {
        return String.format("ORD-%03d", sequence.incrementAndGet());
    }
}

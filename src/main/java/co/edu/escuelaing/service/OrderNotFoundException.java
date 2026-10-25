package co.edu.escuelaing.service;


/** La orden solicitada no existe. (Más adelante se traducirá a 404.) */
public class OrderNotFoundException extends RuntimeException {

    private final String orderId;

    public OrderNotFoundException(String orderId) {
        super("Order " + orderId + " does not exist");
        this.orderId = orderId;
    }

    public String getOrderId() {
        return orderId;
    }
}
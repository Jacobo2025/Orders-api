package co.edu.escuelaing.lambda;

/**
 * Error que la función Lambda reporta al exterior.
 * El mensaje tiene el formato "CODIGO: descripción", por ejemplo:
 *   "ORDER_NOT_FOUND: Order ORD-999 does not exist"
 * Lambda lo publica en el campo "errorMessage" de su respuesta de error,
 * y en la Fase 4 API Gateway usará ese prefijo para elegir el código HTTP.
 */
public class OrderFunctionException extends RuntimeException {

    private final String code;

    public OrderFunctionException(String code, String message, Throwable cause) {
        super(code + ": " + message, cause);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}

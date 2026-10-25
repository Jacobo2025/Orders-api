package co.edu.escuelaing.service;


/** La entrada no cumple las reglas de negocio. (Más adelante se traducirá a 400.) */
public class ValidationException extends RuntimeException {

    public ValidationException(String message) {
        super(message);
    }
}
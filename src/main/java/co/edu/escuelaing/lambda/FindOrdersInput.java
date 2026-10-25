package co.edu.escuelaing.lambda;

/** Entrada de la función "buscar por cliente": { "customerId": "CUST-42" } */
public class FindOrdersInput {

    private String customerId;

    public FindOrdersInput() {
    }

    public FindOrdersInput(String customerId) {
        this.customerId = customerId;
    }

    public String getCustomerId() { return customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }
}

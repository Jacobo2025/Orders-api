package co.edu.escuelaing.lambda;

/** Entrada de la función "consultar orden": { "id": "ORD-001" } */
public class GetOrderInput {

    private String id;

    public GetOrderInput() {
    }

    public GetOrderInput(String id) {
        this.id = id;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
}
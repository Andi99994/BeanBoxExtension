package sun.beanbox.export.components;

/**
 * Created by Andi on 04.07.2017.
 */
public class ExportConstraintViolation {

    private String message;

    public ExportConstraintViolation(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}

package sun.beanbox.export.datastructure;

/**
 * Created by Andreas Ertlschweiger on 04.07.2017.
 * <p>
 * A simple value object to hold a message that describes a constraint violation that occurred during export.
 */
public class ExportConstraintViolation {

    private final String message;

    public ExportConstraintViolation(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}

package editor;

import java.beans.PropertyEditorSupport;

/**
 * Created by andreas on 22.11.2016.
 */
public class MaskEditor extends PropertyEditorSupport {

    public String[] getTags() {
        return new String[]{"Square", "Plus", "X", "Square Separable"};
    }
}

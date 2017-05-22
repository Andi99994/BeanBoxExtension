package editor;

import javax.imageio.ImageIO;
import java.beans.PropertyEditorSupport;

/**
 * Created by andreas on 22.11.2016.
 */
public class FileFormatEditor extends PropertyEditorSupport {

    public String[] getTags() {
        return ImageIO.getWriterFormatNames(); //WBMP funktioniert seltsamerweise nicht!
    }
}

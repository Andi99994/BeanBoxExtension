package bean;

import event.ImageProcessEvent;
import event.ImageProcessListener;
import logic.FilePathValidator;

import javax.media.jai.JAI;
import javax.media.jai.PlanarImage;
import java.io.Serializable;

/**
 * Created by andreas on 14.11.2016.
 */
public class ImageReadBean extends AbstractImageProcessBean<ImageProcessListener> implements ImageProcessListener, Serializable {

    private String filename;

    public ImageReadBean() {
        filename = "";
    }

    @Override
    public void process(ImageProcessEvent event) {
        if (FilePathValidator.isValidFile(filename, "jpg")) {
            PlanarImage image = JAI.create("fileload", filename);
            notifyListeners(image);
        }
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
        process(null);
    }
}

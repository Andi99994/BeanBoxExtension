package bean;

import event.ImageProcessEvent;
import event.ImageProcessListener;
import logic.ImageSaveLogic;

import javax.media.jai.PlanarImage;
import java.io.Serializable;

/**
 * Created by andreas on 21.11.2016.
 */
public class ImageSaveBean extends AbstractImageProcessBean<ImageProcessListener> implements ImageProcessListener, Serializable {

    private transient PlanarImage image;
    private String filename;
    private String format;

    public ImageSaveBean() {
        filename = "";
        format = "jpg";
    }

    @Override
    public void process(ImageProcessEvent event) {
        if (event != null) {
            image = event.getImage();
        }
        if (image != null) {
            ImageSaveLogic.exec(image, filename, format);
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

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
        process(null);
    }
}

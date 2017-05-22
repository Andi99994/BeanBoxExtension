package bean;

import event.ImageProcessEvent;
import event.ImageProcessListener;

import javax.media.jai.PlanarImage;
import javax.swing.*;
import java.awt.*;
import java.io.Serializable;
import java.util.ArrayList;

/**
 * Created by andreas on 21.11.2016.
 */
public class ImageShowBean extends AbstractDisplayBean<ImageProcessListener> implements ImageProcessListener, Serializable {

    private transient PlanarImage image;

    public ImageShowBean() {
        Dimension d = new Dimension(200, 100);
        setSize(d);
        setPreferredSize(d);
        setVisible(true);
    }

    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (image != null) {
            g.drawImage(image.getAsBufferedImage(), 0, 0, this); // see javadoc for more info on the parameters
        }
    }

    @Override
    public void process(ImageProcessEvent event) {
        image = event.getImage();
        this.repaint();
        notifyListeners(image);
    }
}

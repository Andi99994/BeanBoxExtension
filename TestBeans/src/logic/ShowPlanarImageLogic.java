package logic;

import com.sun.media.jai.widget.DisplayJAI;

import javax.media.jai.PlanarImage;
import javax.swing.*;
import java.awt.*;
import java.io.Serializable;

/**
 * Created by hellr on 11/13/2016.
 */
public class ShowPlanarImageLogic implements Serializable {

    public ShowPlanarImageLogic() {
    }

    public static void exec(PlanarImage value) {
        String imageInfo = "Dimensions: " + value.getWidth() + "x" + value.getHeight() + " Bands:" + value.getNumBands();

        // Create a frame for display.
        JFrame frame = new JFrame();
        frame.setTitle("DisplayJAI");

        // Get the JFrame ContentPane.
        Container contentPane = frame.getContentPane();
        contentPane.setLayout(new BorderLayout());

        // Create an instance of DisplayJAI.
        DisplayJAI dj = new DisplayJAI(value);

        // Add to the JFrame ContentPane an instance of JScrollPane
        // containing the DisplayJAI instance.
        contentPane.add(new JScrollPane(dj), BorderLayout.CENTER);

        // Add a text label with the image information.
        contentPane.add(new JLabel(imageInfo), BorderLayout.SOUTH);

        // Set the closing operation so the application is finished.
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(800, 600); // adjust the frame size.
        frame.setVisible(true); // show the frame.
    }
}

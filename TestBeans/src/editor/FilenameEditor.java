package editor;

import javax.swing.*;
import java.awt.*;
import java.beans.PropertyEditor;
import java.io.File;

/**
 * Created by hellr on 11/22/2016.
 */
public class FilenameEditor extends JPanel implements PropertyEditor {
    private String _path;
    private JFileChooser _chooser;
    private Label _lblPath;

    public FilenameEditor() {
        _path = "";
        _chooser = new JFileChooser();
        BorderLayout bl = new BorderLayout();
        setLayout(bl);

        _lblPath = new Label();
        Button b = new Button("Auswählen");
        b.addActionListener(e -> {
            int res = _chooser.showOpenDialog(this);
            if (res == JFileChooser.APPROVE_OPTION) {
                _path = _chooser.getSelectedFile().getAbsolutePath();
                _lblPath.setText(_path);
                revalidate();
            }
        });

        add(b, BorderLayout.NORTH);
        add(_lblPath, BorderLayout.CENTER);
    }

    @Override
    public void setValue(Object value) {
        _path = (String)value;
        _lblPath.setText(_path);
        _chooser.setSelectedFile(new File(_path));
    }

    @Override
    public Object getValue() {
        return _path;
    }

    @Override
    public boolean isPaintable() {
        return true;
    }

    @Override
    public void paintValue(Graphics gfx, Rectangle box) {
        Graphics2D g2 = (Graphics2D) gfx;
        g2.drawString(_path.length() > 0 ? _path : "Auswählen", 0, 22);
    }

    @Override
    public String getJavaInitializationString() {
        return null;
    }

    @Override
    public String getAsText() {
        return _path;
    }

    @Override
    public void setAsText(String text) throws IllegalArgumentException {
        _path = text;
    }

    @Override
    public String[] getTags() {
        return null;
    }

    @Override
    public Component getCustomEditor() {
        return this;
    }

    @Override
    public boolean supportsCustomEditor() {
        return true;
    }
}

package logic;

import java.io.File;
import java.io.Serializable;

/**
 * Created by andreas on 23.11.2016.
 */
public class FilePathValidator implements Serializable {

    public static boolean isValidFile(String path, String format) {
        File file = new File(path);
        String extension = "";

        int i = file.getName().lastIndexOf('.');
        if (i > 0) {
            extension = file.getName().substring(i + 1);
        }

        return file.exists() && file.isFile() && extension.equalsIgnoreCase(format);
    }
}

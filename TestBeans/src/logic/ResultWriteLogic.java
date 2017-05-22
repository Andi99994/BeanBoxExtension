package logic;

import datastructure.CentroidResult;

import java.io.PrintWriter;
import java.io.Serializable;
import java.util.List;

/**
 * Created by hellr on 11/13/2016.
 */
public class ResultWriteLogic implements Serializable {

    public ResultWriteLogic() {
    }

    public static void exec(List<CentroidResult> value, String filename) {
        if (value != null) {
            try {
                PrintWriter writer = new PrintWriter(filename, "UTF-8");
                for (CentroidResult current : value) {
                    writer.println(current.toString());
                }
                writer.close();
            } catch (Exception e) {
                System.err.println("Error writing to file!");
                e.printStackTrace();
            }
        }
    }
}

package sun.beanbox.export.util;

import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipEntry;

/**
 * Created by Andreas Ertlschweiger on 07.07.2017.
 * <p>
 * Util class to compile and pack resources into a JAR file.
 */
public class JARCompiler {

    /**
     * This method writes and packs all contents of a directory into a specified file (JAR file).
     *
     * @param target the target file
     * @param root   the directory to pack
     * @throws IOException if there is an error packing the JAR or reading sources
     */
    public static void packJar(File target, File root) throws IOException {
        JarOutputStream jarOut = new JarOutputStream(new FileOutputStream(target));
        writeRecursively(jarOut, root.getAbsolutePath() + "\\", root);
        jarOut.close();
    }

    /**
     * Recursively reads all sources from a file or directory and writes it to the output stream.
     *
     * @param jarOut   the output stream to write to
     * @param base     a prefix of the filepath that should be removed to create the package name from the absolute path
     * @param resource the resource to write
     * @throws IOException if there is an error reading or writing the files
     */
    private static void writeRecursively(JarOutputStream jarOut, String base, File resource) throws IOException {
        if (resource.isFile()) {
            String relativePath = StringUtil.getRelativePath(base, resource, true);
            jarOut.putNextEntry(new ZipEntry(relativePath));
            InputStream is = new FileInputStream(resource);
            while (is.available() > 0) {
                jarOut.write(is.read());
            }
            is.close();
            jarOut.closeEntry();
        } else if (resource.isDirectory() && resource.listFiles() != null) {
            for (File file : resource.listFiles()) {
                writeRecursively(jarOut, base, file);
            }
        }
    }

    /**
     * This method compiles all Java source files in a directory to class files using the specified resources (JAR or class files).
     *
     * @param folder    the folder to search recursively for source files
     * @param resources the resources required for compilation
     */
    public static void compileSources(File folder, Collection<File> resources) {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        StandardJavaFileManager fileManager = compiler.getStandardFileManager(null, null, null);

        StringBuilder classpath = new StringBuilder();
        for (File resource : resources) {
            classpath.append(resource.getAbsolutePath()).append(";");
        }
        Iterable<String> options = Arrays.asList("-classpath", classpath.toString());
        List<File> compilationFiles = findJavaFiles(folder);
        Iterable<? extends JavaFileObject> files = fileManager.getJavaFileObjectsFromFiles(compilationFiles);
        compiler.getTask(null, fileManager, null, options, null, files).call();
    }

    /**
     * Recursively searches a directory or file for Java source files.
     *
     * @param folder the directory or file to search for source files
     * @return returns a list of Java source files found in that directory or file
     */
    private static List<File> findJavaFiles(File folder) {
        ArrayList<File> files = new ArrayList<>();
        if (folder.isFile()) {
            if (folder.getName().endsWith(".java")) {
                files.add(folder);
            }
        } else if (folder.isDirectory() && folder.listFiles() != null) {
            for (File file : folder.listFiles()) {
                files.addAll(findJavaFiles(file));
            }
        }
        return files;
    }
}

package sun.beanbox.export;

import org.apache.commons.io.FileUtils;
import sun.beanbox.HookupManager;
import sun.beanbox.Wrapper;
import sun.beanbox.WrapperEventTarget;
import sun.beanbox.WrapperPropertyEventInfo;
import sun.beanbox.export.components.ExportConstraintViolation;
import sun.beanbox.export.components.NodeSelector;
import sun.beanbox.export.datastructure.*;

import javax.lang.model.SourceVersion;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import java.beans.*;
import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;

import static java.nio.file.FileVisitResult.CONTINUE;
import static java.nio.file.FileVisitResult.TERMINATE;

/**
 * Created by Andreas on 06.05.2017.
 * <p>
 * This is the main component responsible for the export process. It first converts all selected Wrapper objects into
 * a better suited datastructure, a directed graph, that contains all relevant information. Any changes made to the
 * configuration will affect how the bean will be generated.
 */
public class Exporter {

    //Map between Bean and Wrapper. This is for easier and faster access and comparison
    private HashMap<Object, Wrapper> wrapperBeanMap = new HashMap<>();
    private List<ExportBean> exportBeans = new LinkedList<>();
    private Set<String> reservedPropertyNames = new HashSet<>();
    private boolean keepSources = false;

    private String tmpDirectoryName = "/tmp";
    private static final String DEFAULT_MANIFEST_DIRECTORY_NAME = "META-INF";
    private static final String DEFAULT_BEAN_DIRECTORY_NAME = "beanBox/generated/beans";
    private static final String DEFAULT_SERIALIZED_PROPERTIES_DIRECTORY_NAME = "beanBox/generated/beans/properties";
    private static final String DEFAULT_ADAPTER_DIRECTORY_NAME = "beanBox/generated/beans/adapters";

    private static final String DEFAULT_BEAN_NAME = "ExportBean";

    /**
     * Upon instantiation of the Exporter the selected Wrappers are grouped, processed and converted into a more suitable
     * datastructure.
     *
     * @param beans the beans that were selected for export
     * @throws IntrospectionException    if there is an error reading bean information
     * @throws IllegalArgumentException  if there is an error accessing bean properties
     * @throws InvocationTargetException if there is an error accessing bean properties
     * @throws IllegalAccessException    if there is an error accessing bean properties
     */
    public Exporter(List<Wrapper> beans) throws IntrospectionException, IllegalArgumentException, InvocationTargetException, IllegalAccessException {
        reservedPropertyNames.add("propertyChange");
        for (List<Wrapper> group : groupWrappers(beans)) {
            exportBeans.add(assembleExportBean(group, DEFAULT_BEAN_NAME + exportBeans.size()));
        }
    }

    public boolean isKeepSources() {
        return keepSources;
    }

    public void setKeepSources(boolean keepSources) {
        this.keepSources = keepSources;
    }

    public List<ExportBean> getBeans() {
        return exportBeans;
    }

    /**
     * This method analyzes the bindings between all Wrappers and groups them. Wrappers in a group have to be connected
     * to eachother either via a direct composition, adapter composition or a property binding. Each group of Wrappers
     * will be processed as a separate ExportBean.
     *
     * @param wrappers all Wrappers that are being exported
     * @return returns a list of groups of Wrappers
     */
    private List<List<Wrapper>> groupWrappers(List<Wrapper> wrappers) {
        HashMap<Wrapper, Integer> groupMap = new HashMap<>();
        for (Wrapper wrapper : wrappers) {
            //initialize the wrapperBeanMap
            wrapperBeanMap.put(wrapper.getBean(), wrapper);
            //add all Wrappers without a group yet
            groupMap.put(wrapper, null);
        }
        int groupCount = 0;
        for (Wrapper wrapper : wrappers) {
            Integer curGroup = groupMap.get(wrapper);
            //if the current Wrapper hasn't been assigned a group yet do so
            if (curGroup == null) {
                //analyze all bindings
                for (Object bean : wrapper.getCompositionTargets()) {
                    Wrapper beanWrapper = wrapperBeanMap.get(bean);
                    if (beanWrapper != null && groupMap.get(beanWrapper) != null) {
                        curGroup = groupMap.get(beanWrapper);
                        break;
                    }
                }
            }
            //if we still don't have a group, create a new one
            if (curGroup == null) {
                curGroup = groupCount;
                groupCount++;
            }
            //assign the Wrapper to the group
            groupMap.replace(wrapper, curGroup);
            //assign all related Wrappers to the group
            for (Object bean : wrapper.getCompositionTargets()) {
                Wrapper beanWrapper = wrapperBeanMap.get(bean);
                if (beanWrapper != null) {
                    groupMap.replace(beanWrapper, curGroup);
                }
            }
        }
        //convert the HashMap into a list. Using a second HashMap is easier and faster in case of a high number of groups
        HashMap<Integer, List<Wrapper>> groupedWrappers = new HashMap<>();
        for (Map.Entry<Wrapper, Integer> entry : groupMap.entrySet()) {
            if (groupedWrappers.containsKey(entry.getValue())) {
                groupedWrappers.get(entry.getValue()).add(entry.getKey());
            } else {
                groupedWrappers.put(entry.getValue(), new LinkedList<>());
                groupedWrappers.get(entry.getValue()).add(entry.getKey());
            }
        }
        return new ArrayList<>(groupedWrappers.values());
    }

    /**
     * This method converts a group of Wrappers into a single ExportBean. It does so by converting each Wrapper into a
     * BeanNode and constructing a graph consisting of the compositions and bindings. While converting the Wrappers the
     * Bean information (events, methods, properties) are read and also converted. Afterwards it tries to infer the
     * input and output nodes. If this fails, the user is prompted to select them. This usually happens if there are cyclic references.
     * This information can be changed later, but an initial configuration is required to construct the graph.
     *
     * @param wrappers a list of Wrappers that should be converted into an ExportBean
     * @param name     the name of the ExportBean
     * @return returns an ExportBean containing all important information
     * @throws IntrospectionException    if there is an error reading bean information
     * @throws IllegalArgumentException  if there is an error reading properties
     * @throws InvocationTargetException if there is an error reading properties
     * @throws IllegalAccessException    if there is an error reading properties
     */
    private ExportBean assembleExportBean(List<Wrapper> wrappers, String name) throws IntrospectionException, IllegalArgumentException, InvocationTargetException, IllegalAccessException {
        HashMap<Wrapper, BeanNode> createdNodes = new HashMap<>();
        for (Wrapper wrapper : wrappers) {
            createBeanNode(wrapper, createdNodes);
        }
        List<BeanNode> inputBeans = inferInputBeans(createdNodes);
        List<BeanNode> outputBeans = inferOutputBeans(createdNodes);
        BeanGraph beanGraph = new BeanGraph(inputBeans, outputBeans, createdNodes.values());
        return new ExportBean(beanGraph, name);
    }

    /**
     * Recursively converts Wrappers into BeanNodes. While doing so, all required information is gathered and also
     * converted.
     *
     * @param wrapper      the Wrapper to convert.
     * @param createdNodes all BeanNodes of an ExportBean that have already been created. This is needed to deal with cyclic composition.
     * @return returns a BeanNode
     * @throws IntrospectionException    if there is an error reading bean information
     * @throws InvocationTargetException if there is an error reading properties
     * @throws IllegalAccessException    if there is an error reading properties
     */
    private BeanNode createBeanNode(Wrapper wrapper, HashMap<Wrapper, BeanNode> createdNodes) throws IntrospectionException, InvocationTargetException, IllegalAccessException {
        //avoid following cyclic references
        if (createdNodes.get(wrapper) != null) {
            return createdNodes.get(wrapper);
        }
        BeanNode beanNode = new BeanNode(wrapper.getBean(), wrapper.getBeanLabel());
        beanNode.setJarPath(wrapper.getJarPath());
        BeanInfo beanInfo = Introspector.getBeanInfo(beanNode.getData().getClass());
        //add all properties eligible for export
        for (PropertyDescriptor propertyDescriptor : beanInfo.getPropertyDescriptors()) {
            if (!propertyDescriptor.isHidden() && !propertyDescriptor.isExpert() && propertyDescriptor.getReadMethod() != null && propertyDescriptor.getWriteMethod() != null) {
                ExportProperty property = new ExportProperty(propertyDescriptor, beanNode);
                //if the property has been changed at least once it is likely that we want to set this as the default value after export
                if (wrapper.getChangedProperties().contains(propertyDescriptor)) {
                    property.setSetDefaultValue(true);
                }
                beanNode.getProperties().add(property);
            }
        }
        //add all methods eligible for export. It is highly suggested to define these in a BeanInfo as otherwise there are going to be a lot
        for (MethodDescriptor methodDescriptor : beanInfo.getMethodDescriptors()) {
            if (!methodDescriptor.isExpert() && !methodDescriptor.isHidden() && !methodDescriptor.getName().equals("propertyChange")) {
                beanNode.getMethods().add(new ExportMethod(methodDescriptor, beanNode));
            }
        }
        //add all events eligible for export
        for (EventSetDescriptor eventSetDescriptor : beanInfo.getEventSetDescriptors()) {
            if (!eventSetDescriptor.isExpert() && !eventSetDescriptor.isHidden() && !eventSetDescriptor.getName().equals("propertyChange")) {
                beanNode.getEvents().add(new ExportEvent(eventSetDescriptor, beanNode));
            }
        }
        createdNodes.put(wrapper, beanNode);
        //add all direct compositions
        for (WrapperEventTarget end : wrapper.getDirectTargets()) {
            Wrapper beanWrapper = wrapperBeanMap.get(end.getTargetBean());
            if (beanWrapper != null) {
                BeanNode childNode = createBeanNode(beanWrapper, createdNodes);
                beanNode.addEdge(new DirectCompositionEdge(beanNode, childNode, end));
            }
        }
        //add all adapter compositions
        for (WrapperEventTarget end : wrapper.getEventHookupTargets()) {
            Wrapper beanWrapper = wrapperBeanMap.get(end.getTargetBean());
            if (beanWrapper != null) {
                BeanNode childNode = createBeanNode(beanWrapper, createdNodes);
                beanNode.addEdge(new AdapterCompositionEdge(beanNode, childNode, end));
            }
        }
        //add all property bindings
        for (WrapperPropertyEventInfo end : wrapper.getPropertyTargets()) {
            Wrapper beanWrapper = wrapperBeanMap.get(end.getTargetBean());
            if (beanWrapper != null) {
                BeanNode childNode = createBeanNode(beanWrapper, createdNodes);
                beanNode.addEdge(new PropertyBindingEdge(beanNode, childNode, end));
            }
        }
        return beanNode;
    }

    /**
     * This method tries to infer the output interface of the ExportBean. It does so by checking which beans are only listeners
     * and do not have any listeners. These beans are except for some special cases very likely to be the correct ones.
     * If this fails a NodeSelector is shown to let the user specify the output interface.
     *
     * @param createdNodes All available beans
     * @return returns a list of BeanNodes that compose the output interface
     * @throws IllegalArgumentException if no Beans are specified as the output interface we cannot continue
     */
    private List<BeanNode> inferOutputBeans(HashMap<Wrapper, BeanNode> createdNodes) throws IllegalArgumentException {
        List<BeanNode> availableNodes = new LinkedList<>();
        for (BeanNode node : createdNodes.values()) {
            if (node.getEdges().isEmpty()) {
                availableNodes.add(node);
            }
        }
        if (availableNodes.isEmpty()) {
            availableNodes.addAll(createdNodes.values());
            new NodeSelector(null, availableNodes, "Could not infer output Beans (maybe you have cyclic references in your composition?). Please select from the list below.").show();
        }
        if (availableNodes.isEmpty()) {
            throw new IllegalArgumentException("Cannot export without selection of at least one output node");
        }
        return availableNodes;
    }

    /**
     * This method tries to infer the input interface of the ExportBean. It does so by checking which beans only have listeners
     * and do not listen to any bean. These beans are except for some special cases very likely to be the correct ones.
     * If this fails a NodeSelector is shown to let the user specify the input interface.
     *
     * @param createdNodes All available beans
     * @return returns a list of BeanNodes that compose the input interface
     * @throws IllegalArgumentException if no Beans are specified as the input interface we cannot continue
     */
    private List<BeanNode> inferInputBeans(HashMap<Wrapper, BeanNode> createdNodes) throws IllegalArgumentException {
        Set<BeanNode> availableNodes = new HashSet<>(createdNodes.values());
        for (BeanNode node : createdNodes.values()) {
            for (BeanEdge edge : node.getEdges()) {
                if (availableNodes.contains(edge.getEnd())) {
                    availableNodes.remove(edge.getEnd());
                }
            }
        }
        if (availableNodes.isEmpty()) {
            List<BeanNode> availableBeans = new LinkedList<>(createdNodes.values());
            new NodeSelector(null, availableBeans, "Could not infer input Beans (maybe you have cyclic references in your composition?). Please select from the list below.").show();
            availableNodes.addAll(availableBeans);
        }
        if (availableNodes.isEmpty()) {
            throw new IllegalArgumentException("Cannot export without selection of at least one input node");
        }
        return new ArrayList<>(availableNodes);
    }

    /**
     * Checks if a String is a valid name for an ExportBean. It may not be empty, must not exceed 32 characters, be a valid Java identifier,
     * must not be a Java keyword and it must be unique among all ExportBeans in a single export. Additionally the String may not conflict
     * with any resources required to build the JAR file -> This is NOT checked here.
     *
     * @param text the text to be checked
     * @return returns if the name is valid
     */
    public boolean checkIfValidClassName(String text) {
        boolean isValidClassName = text != null && !text.isEmpty() && text.length() < 32 && SourceVersion.isIdentifier(text) && !SourceVersion.isKeyword(text);
        if (!isValidClassName) return false;
        for (ExportBean exportBean : exportBeans) {
            if (exportBean.getBeanName().equals(text)) return false;
            for (BeanNode node : exportBean.getBeans().getAllNodes()) {
                if (node.getName().equals(text)) return false;
            }
        }
        return true;
    }

    /**
     * Checks if a String is a valid name for an ExportProperty. It may not be empty, must not exceed 32 characters, be a valid Java identifier,
     * must not be a Java keyword and it must be unique among all ExportProperties in a single ExportBean. Additionally the String may not conflict
     * with any generated method or event names.
     *
     * @param text       the text to be checked
     * @param exportBean the exportBean to which the property belongs
     * @return returns if the name is valid
     */
    public boolean checkIfValidPropertyName(ExportBean exportBean, String text) { //TODO: check for conflicts of generated methods
        boolean isValidPropertyName = text != null && !text.isEmpty() && text.length() < 32 && SourceVersion.isIdentifier(text) && !SourceVersion.isKeyword(text);
        if (!isValidPropertyName) return false;
        for (ExportProperty property : exportBean.getProperties()) {
            if (property.getName().equals(text)) return false;
        }
        return true;
    }

    /**
     * This method initiates the export process itself. Upon calling, temporary directories will be generated, resources
     * collected, and all necessary classes generated and compiled. This temporary directory will then be packed into a
     * JAR file.
     *
     * @param directory The directory where the bean and the temporary directories should be generated.
     * @param filename  the name of the JAR
     * @throws Exception if there is any error during generation, compilation or packing there are quite a few exceptions
     *                   thrown so we just throw a generic exception since we don't really differentiate between them anyway. In every case
     *                   the export process failed and we have to cancel it and display the error information.
     */
    public void export(String directory, String filename) throws Exception {
        if (!filename.endsWith(".jar")) filename += ".jar";
        File target = new File(directory, filename);
        int counter = 0;
        while (new File(directory + tmpDirectoryName + counter).exists()) {
            counter++;
        }
        tmpDirectoryName += counter;
        if (validateConfiguration() == null) {
            File tmpDirectory = new File(directory + tmpDirectoryName);
            File tmpBeanDirectory = new File(tmpDirectory.getAbsolutePath() + File.separator + DEFAULT_BEAN_DIRECTORY_NAME);
            File tmpPropertiesDirectory = new File(tmpDirectory.getAbsolutePath() + File.separator + DEFAULT_SERIALIZED_PROPERTIES_DIRECTORY_NAME);
            File tmpManifestDirectory = new File(tmpDirectory.getAbsolutePath() + File.separator + DEFAULT_MANIFEST_DIRECTORY_NAME);
            File tmpAdapterDirectory = new File(tmpDirectory.getAbsolutePath() + File.separator + DEFAULT_ADAPTER_DIRECTORY_NAME);

            if (tmpBeanDirectory.mkdirs() && tmpPropertiesDirectory.mkdirs() && tmpManifestDirectory.mkdirs() && tmpAdapterDirectory.mkdirs()) {
                ArrayList<File> resources = collectResources();
                copyAndExtractResources(tmpDirectory, resources);
                resources.addAll(generatePropertyAdapters(tmpAdapterDirectory));
                for (ExportBean exportBean : exportBeans) {
                    generateBean(tmpDirectory, tmpBeanDirectory, tmpPropertiesDirectory, tmpAdapterDirectory, exportBean);
                }
                generateManifest(tmpManifestDirectory);
                compileSources(tmpBeanDirectory, resources);
                packJar(target, tmpDirectory);
                if (!keepSources) {
                    deleteDirectory(tmpDirectory.toPath());
                }
            } else {
                throw new IOException("Error creating temporary directories at: " + directory);
            }
        }
    }

    /**
     * This method analyzes all ExportBeans and collects any necessary dependencies. These are the JAR files of the BeanNodes
     * and any adapters if there are adapter compositions. This method could be optimized by already detecting conflicts so
     * we can interrupt earlier etc...
     *
     * @return returns a List of all necessary resources as files
     * @throws IOException if a file can not be found or there is an error reading the files
     */
    private ArrayList<File> collectResources() throws IOException {
        ArrayList<File> res = new ArrayList<>();
        for (ExportBean exportBean : exportBeans) {
            for (BeanNode node : exportBean.getBeans().getAllNodes()) {
                File resource = new File(node.getJarPath());
                if (resource.isFile()) {
                    if (!contentEquals(resource, res)) {
                        res.add(resource);
                    }
                } else {
                    throw new IOException("Source file not found or invalid: " + node.getJarPath());
                }
                for (AdapterCompositionEdge edge : node.getAdapterCompositionEdges()) {
                    File edgeResource = new File(edge.getAdapterClassPath());
                    if (edgeResource.isFile()) {
                        if (!contentEquals(edgeResource, res)) {
                            res.add(edgeResource);
                        }
                    } else {
                        throw new IOException("Source file not found or invalid: " + edge.getAdapterClassPath());
                    }
                }
            }
        }
        return res;
    }

    /**
     * Checks if a file has the same content as any file in a given list of files. This uses Apache Commons IO for
     * determining equality.
     *
     * @param testFile  the file to be tested
     * @param resources the list of files that are to be checked against
     * @return returns if a file with equal content is already in the list
     * @throws IOException if there is an error reading the files
     */
    private static boolean contentEquals(File testFile, ArrayList<File> resources) throws IOException {
        for (File res : resources) {
            if (FileUtils.contentEquals(res, testFile)) {
                return true;
            }
        }
        return false;
    }

    /**
     * This method copies all specified resources to the specified directory. JAR files will additionally be extracted
     * into the target directory while keeping the package structure. All other file types will be copied as is.
     *
     * @param targetDirectory the directory to copy all resources to
     * @param resources       the files that are being copied and extracted
     * @throws IOException if there is an error copying or extracting
     */
    private void copyAndExtractResources(File targetDirectory, Collection<File> resources) throws IOException {
        if (!targetDirectory.isDirectory()) {
            throw new IOException("Could not copy resource files: Target is not a directory.");
        }
        for (File resource : resources) {
            if (resource.getName().toLowerCase().endsWith(".jar")) {
                JarFile jarfile = new JarFile(resource);
                Enumeration<JarEntry> enu = jarfile.entries();
                while (enu.hasMoreElements()) {
                    JarEntry je = enu.nextElement();
                    File fl = new File(targetDirectory.getAbsolutePath(), je.getName());
                    if (!fl.exists()) {
                        fl.getParentFile().mkdirs();
                        fl = new File(targetDirectory.getAbsolutePath(), je.getName());
                    }
                    //exclude any Manifest files to avoid very likely conflicts
                    if (je.isDirectory() || je.getName().toUpperCase().contains("MANIFEST.MF")) {
                        continue;
                    }
                    try (InputStream in = jarfile.getInputStream(je); FileOutputStream out = new FileOutputStream(fl)) {
                        byte[] buf = new byte[1024];
                        int length;
                        while ((length = in.read(buf)) > 0) {
                            out.write(buf, 0, length);
                        }
                    }
                }
            } else {
                File file = new File(targetDirectory.getAbsolutePath(), HookupManager.getTmpDir() + File.separator + resource.getName());
                if (!file.exists()) {
                    file.getParentFile().mkdirs();
                    file = new File(targetDirectory.getAbsolutePath(), HookupManager.getTmpDir() + File.separator + resource.getName());
                }
                try (InputStream in = new FileInputStream(resource); OutputStream out = new FileOutputStream(file)) {
                    byte[] buf = new byte[1024];
                    int length;
                    while ((length = in.read(buf)) > 0) {
                        out.write(buf, 0, length);
                    }
                }
            }
        }
    }

    /**
     * CAUTION! This method deletes a directory and all its contents.
     *
     * @param path the path to the directory to be deleted
     * @throws IOException if there is an error deleting
     */
    private static void deleteDirectory(final Path path) throws IOException {
        Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) throws IOException {
                Files.delete(file);
                return CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed(final Path file, final IOException e) {
                return TERMINATE;
            }

            @Override
            public FileVisitResult postVisitDirectory(final Path dir, final IOException e) throws IOException {
                if (e != null) return TERMINATE;
                Files.delete(dir);
                return CONTINUE;
            }
        });
    }

    /**
     * Unfortunately the BeanBox does not use the direct way of binding but rather uses adapters
     * for this task. To be able to offer the same functionality as the BeanBox does (that is not prohibiting
     * property bindings where the BeanBox allows them) we need to generate adapter classes. These are very similar to the
     * hookups that the BeanBox generates for adapter composition.
     *
     * @param targetDirectory the directory where to generate the adapters
     * @return returns a list of files of the adapter classes
     * @throws IOException if there is an error writing the adapters
     */
    private List<File> generatePropertyAdapters(File targetDirectory) throws IOException {
        List<File> adapters = new ArrayList<>();
        for (ExportBean exportBean : exportBeans) {
            for (BeanNode node : exportBean.getBeans().getAllNodes()) {
                for (PropertyBindingEdge edge : node.getPropertyBindingEdges()) {
                    adapters.add(generatePropertyAdapter(targetDirectory, edge));
                }
            }
        }
        return adapters;
    }

    private File generatePropertyAdapter(File targetDirectory, PropertyBindingEdge propertyBindingEdge) throws IOException {
        File adapter = new File(targetDirectory, generateAdapterName() + ".java");
        while (adapter.exists()) {
            adapter = new File(targetDirectory, generateAdapterName() + ".java");
        }
        if (!adapter.createNewFile()) throw new IOException("Error creating File: " + adapter.getName());
        propertyBindingEdge.setAdapterName(adapter.getName().replace(".java", ""));
        PrintWriter writer = new PrintWriter(new FileWriter(adapter));
        writer.println("package " + DEFAULT_ADAPTER_DIRECTORY_NAME.replaceAll(Pattern.quote("/"), ".") + ";");
        writer.println();
        writer.println();
        writer.println("import java.io.Serializable;");
        writer.println("import java.beans.PropertyChangeListener;");
        writer.println("import java.beans.PropertyChangeEvent;");
        writer.println();
        writer.println("public class " + propertyBindingEdge.getAdapterName() + " implements PropertyChangeListener, Serializable {");
        writer.println();
        writer.println("    private " + propertyBindingEdge.getEnd().getData().getClass().getCanonicalName() + " target;");
        writer.println();
        writer.println("    public void setTarget(" + propertyBindingEdge.getEnd().getData().getClass().getCanonicalName() + " t) {");
        writer.println("        target = t;");
        writer.println("    }");
        writer.println();
        writer.println("    public void propertyChange(PropertyChangeEvent evt) {");
        writer.println("        try {");
        writer.println("            target." + propertyBindingEdge.getTargetMethod().getName() + "((" + propertyBindingEdge.getTargetMethod().getParameterTypes()[0].getCanonicalName() + ") evt.getNewValue());");
        writer.println("        } catch (Exception e) {");
        writer.println("            e.printStackTrace();");
        writer.println("        }");
        writer.println("    }");
        writer.println("}");
        writer.close();
        if (writer.checkError()) {
            throw new IOException("Error writing Adapter File: " + adapter.getName());
        }
        return adapter;
    }

    private void packJar(File target, File root) throws IOException {
        JarOutputStream jarOut = new JarOutputStream(new FileOutputStream(target));
        writeRecursively(jarOut, root.getAbsolutePath() + "\\", root);
        jarOut.close();
    }

    private void writeRecursively(JarOutputStream jarOut, String base, File resource) throws IOException {
        if (resource.isFile()) {
            String relativePath = getRelativePath(base, resource, true);
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

    private void compileSources(File folder, Collection<File> resources) {
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

    private List<File> findJavaFiles(File folder) {
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

    private void generateManifest(File manifestDirectory) throws IOException {
        File manifest = new File(manifestDirectory.getAbsolutePath(), "MANIFEST.MF");
        if (!manifest.createNewFile()) throw new IOException("Error creating File: " + manifest.getName());
        PrintWriter writer = new PrintWriter(new FileWriter(manifest));
        writer.println("Manifest-Version: 1.0");
        writer.println();
        for (ExportBean exportBean : exportBeans) {
            writer.println("Name: " + DEFAULT_BEAN_DIRECTORY_NAME + "/" + exportBean.getBeanName() + ".class");
            writer.println("Java-Bean: True");
            writer.println();
            for (BeanNode beanNode : exportBean.getBeans().getAllNodes()) {
                if (beanNode.isRegisterInManifest()) {
                    writer.println("Name: " + beanNode.getData().getClass().getCanonicalName().replaceAll(Pattern.quote("."), "/") + ".class");
                    writer.println("Java-Bean: True");
                    writer.println();
                }
            }
        }
        writer.close();
        if (writer.checkError()) {
            throw new IOException("Error writing Manifest");
        }
    }

    private void generateBean(File root, File beanDirectory, File propertyDirectory, File adapterDirectory, ExportBean exportBean) throws IOException, InvocationTargetException, IllegalAccessException, NoSuchMethodException {
        List<ExportProperty> exportProperties = exportBean.getProperties();
        List<ExportMethod> exportMethods = exportBean.getMethods();
        List<ExportEvent> exportEvents = exportBean.getEvents();
        List<Class> interfaces = new ArrayList<>();
        for (BeanNode node : exportBean.getBeans().getInputNodes()) {
            for (Class cls : node.getData().getClass().getInterfaces()) {
                if (EventListener.class.isAssignableFrom(cls) && !interfaces.contains(cls) && !cls.getName().contains("PropertyChange")) {
                    interfaces.add(cls);
                }
            }
        }

        File bean = new File(beanDirectory.getAbsolutePath(), exportBean.getBeanName() + ".java");
        File beanInfo = new File(beanDirectory.getAbsolutePath(), exportBean.getBeanName() + "BeanInfo.java");
        if (!bean.createNewFile()) throw new IOException("Error creating File: " + bean.getName());
        if (!beanInfo.createNewFile()) throw new IOException("Error creating File: " + beanInfo.getName());
        PrintWriter writer = new PrintWriter(new FileWriter(bean));
        writer.println("package " + DEFAULT_BEAN_DIRECTORY_NAME.replaceAll(Pattern.quote("/"), ".") + ";");
        writer.println();
        /*Set<String> imports = new HashSet<>();
        for (BeanNode node : exportBean.getBeans().getAllNodes()) {
            String nextImport = node.getData().getClass().getCanonicalName();
            if (!imports.contains(nextImport)) {
                writer.println("import " + nextImport + ";");
                imports.add(nextImport);
            }
            for (AdapterCompositionEdge edge : node.getAdapterCompositionEdges()) {
                String nextAdapterImport = edge.getHookup().getClass().getCanonicalName();
                if (!imports.contains(nextAdapterImport)) {
                    writer.println("import " + nextAdapterImport + ";");
                    imports.add(nextAdapterImport);
                }
            }
        }
        writer.println();
        for (ExportProperty exportProperty : exportProperties) {
            if (!exportProperty.getPropertyType().isPrimitive()) {
                String nextImport = exportProperty.getPropertyType().getCanonicalName();
                if (!imports.contains(nextImport)) {
                    writer.println("import " + nextImport + ";");
                    imports.add(nextImport);
                }
            }
        }*/
        writer.println("import java.io.Serializable;");
        if (exportBean.isAddPropertyChangeSupport()) {
            writer.println("import java.beans.PropertyChangeSupport;");
            writer.println("import java.beans.PropertyChangeListener;");
            writer.println("import java.beans.PropertyChangeEvent;");
            interfaces.add(PropertyChangeListener.class);
        }
        writer.println();

        StringBuilder implementations = new StringBuilder(" implements Serializable");
        for (Class cls : interfaces) {
            implementations.append(", ").append(cls.getCanonicalName());
        }
        writer.println("public class " + exportBean.getBeanName() + implementations + " {");
        writer.println();
        for (BeanNode node : exportBean.getBeans().getAllNodes()) {
            writer.println("private " + node.getData().getClass().getCanonicalName() + " " + node.lowercaseFirst() + ";");
        }
        if (exportBean.isAddPropertyChangeSupport()) {
            writer.println();
            writer.println("    private PropertyChangeSupport cs = new PropertyChangeSupport(this);");
        }

        /*writer.println();
        for (ExportProperty exportProperty : exportProperties) {
            writer.println("private " + exportProperty.getPropertyType().getCanonicalName() + " " + exportProperty.getName() + ";");
        }*/
        //TODO: Veto Support -> postponed
        writer.println();
        writer.println("    public " + exportBean.getBeanName() + "() {");
        for (BeanNode node : exportBean.getBeans().getAllNodes()) {
            writer.println("        " + node.lowercaseFirst() + " = new " + node.getData().getClass().getCanonicalName() + "();");
        }
        writer.println();
        int hookupCounter = 0;
        for (BeanNode node : exportBean.getBeans().getAllNodes()) {
            for (AdapterCompositionEdge edge : node.getAdapterCompositionEdges()) {
                writer.println("        " + edge.getHookup().getClass().getCanonicalName() + " "
                        + "hookup" + hookupCounter + " = new " + edge.getHookup().getClass().getCanonicalName() + "();");
                writer.println("        hookup" + hookupCounter + ".setTarget(" + edge.getEnd().lowercaseFirst() + ");");
                writer.println("        " + edge.getStart().lowercaseFirst() + ".add" + edge.getEventSetName() + "Listener(hookup" + hookupCounter + ");");
                hookupCounter++;
            }
            for (DirectCompositionEdge edge : node.getDirectCompositionEdges()) {
                writer.println("        " + edge.getStart().lowercaseFirst() + ".add" + edge.getEventSetName() + "Listener(" + edge.getEnd().lowercaseFirst() + ");");
            }
            for (PropertyBindingEdge edge : node.getPropertyBindingEdges()) {
                String canonicalAdaperName = DEFAULT_ADAPTER_DIRECTORY_NAME.replace("/", ".") + "." + edge.getAdapterName();
                writer.println("        " + canonicalAdaperName + " hookup" + hookupCounter + " = new " + canonicalAdaperName + "();");
                writer.println("        hookup" + hookupCounter + ".setTarget(" + edge.getEnd().lowercaseFirst() + ");");
                writer.println("        " + edge.getStart().lowercaseFirst() + ".add" + edge.getEventSetName() + "Listener(hookup" + hookupCounter + ");");
                hookupCounter++;
            }
        }
        writer.println();
        for (ExportProperty property : sortPropertiesByBinding(exportProperties.stream().filter(ExportProperty::isSetDefaultValue).collect(Collectors.toList()))) { //TODO: add veto support
            Object value = property.getPropertyDescriptor().getReadMethod().invoke(property.getNode().getData());
            if (value == null || value instanceof Void || isPrimitiveOrPrimitiveWrapperOrString(value.getClass())) {
                writer.println("        " + property.getNode().lowercaseFirst() + "." + property.getPropertyDescriptor().getWriteMethod().getName()
                        + "(" + convertPrimitive(value) + ");");
            } else {
                File ser = new File(propertyDirectory.getAbsolutePath(), generateSerName(value) + ".ser");
                while (ser.exists()) {
                    ser = new File(propertyDirectory.getAbsolutePath(), generateSerName(value) + ".ser");
                }
                ser.getParentFile().mkdirs();
                try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(ser))) {
                    out.writeObject(value);
                    writer.println("        try (java.io.ObjectInputStream in = new java.io.ObjectInputStream(getClass().getResourceAsStream(\"" + getRelativePath(beanDirectory.getAbsolutePath(), ser, false) + "\"))){");
                    writer.println("            " + property.getNode().lowercaseFirst() + "." + property.getPropertyDescriptor().getWriteMethod().getName()
                            + "((" + property.getPropertyType().getCanonicalName() + ") in.readObject());");
                    writer.println("        } catch (java.io.IOException | java.lang.ClassNotFoundException e) {");
                    writer.println("            e.printStackTrace();");
                    writer.println("        }");
                } catch (IOException i) {
                    throw new IOException("Error serializing property: " + property.getNode().getName() + ":" + property.getName());
                }
            }
        }
        //TODO: sort order to recognise property bindings -> postponed
        writer.println("    }");
        if (exportBean.isAddPropertyChangeSupport()) {
            writer.println();
            writer.println("\tpublic void addPropertyChangeListener(PropertyChangeListener listener) {");
            writer.println("\t\tcs.addPropertyChangeListener(listener);");
            writer.println("\t}");
            writer.println();
            writer.println("\tpublic void removePropertyChangeListener(PropertyChangeListener listener) {");
            writer.println("\t\tcs.removePropertyChangeListener(listener);");
            writer.println("\t}");
            writer.println();
            writer.println("\tpublic PropertyChangeListener[] getPropertyChangeListeners() {");
            writer.println("\t\treturn cs.getPropertyChangeListeners();");
            writer.println("\t}");
            writer.println();
            writer.println("\t@Override");
            writer.println("\tpublic void propertyChange(PropertyChangeEvent evt) {}");
            writer.println();
        }
        writer.println();
        for (ExportProperty property : exportProperties) {
            Method getter = property.getPropertyDescriptor().getReadMethod();
            StringBuilder getterSignature = new StringBuilder("\tpublic " + property.getPropertyType().getCanonicalName() + " " + getter.getName() + "(");
            for (int i = 0; i < getter.getParameterTypes().length; i++) {
                if (i == 0) {
                    getterSignature.append(getter.getParameterTypes()[i].getCanonicalName()).append(" arg").append(i);
                } else {
                    getterSignature.append(", ").append(getter.getParameterTypes()[i].getCanonicalName()).append(" arg").append(i);
                }
            }
            getterSignature.append(")");
            Class<?>[] getterExceptions = getter.getExceptionTypes();
            for (int i = 0; i < getterExceptions.length; i++) {
                if (i == 0) {
                    getterSignature.append("throws ").append(getterExceptions[i].getCanonicalName());
                } else {
                    getterSignature.append(",").append(getterExceptions[i].getCanonicalName());
                }
            }
            getterSignature.append("{");
            writer.println(getterSignature);
            StringBuilder getterCall = new StringBuilder("\t\treturn ").append(property.getNode().lowercaseFirst()).append(".").append(getter.getName()).append("(");
            for (int i = 0; i < getter.getParameterTypes().length; i++) {
                if (i == 0) {
                    getterCall.append("arg").append(i);
                } else {
                    getterCall.append(", arg").append(i);
                }
            }
            getterCall.append(");");
            writer.println(getterCall);
            writer.println("\t}");
            writer.println();

            Method setter = property.getPropertyDescriptor().getWriteMethod();
            StringBuilder setterSignature = new StringBuilder("\tpublic void " + setter.getName() + "(");
            for (int i = 0; i < setter.getParameterTypes().length; i++) {
                if (i == 0) {
                    setterSignature.append(setter.getParameterTypes()[i].getCanonicalName()).append(" arg").append(i);
                } else {
                    setterSignature.append(", ").append(setter.getParameterTypes()[i].getCanonicalName()).append(" arg").append(i);
                }
            }
            setterSignature.append(")");
            Class<?>[] setterExceptions = setter.getExceptionTypes();
            for (int i = 0; i < setterExceptions.length; i++) {
                if (i == 0) {
                    setterSignature.append("throws ").append(setterExceptions[i].getCanonicalName());
                } else {
                    setterSignature.append(",").append(setterExceptions[i].getCanonicalName());
                }
            }
            setterSignature.append("{");
            writer.println(setterSignature);
            if (exportBean.isAddPropertyChangeSupport()) {
                writer.println("\t\tcs.firePropertyChange(\"" + property.getName() + "\", " + property.getNode().lowercaseFirst() + ".get" + property.uppercaseFirst() + "(), arg0);");
            }
            StringBuilder setterCall = new StringBuilder("\t\t").append(property.getNode().lowercaseFirst()).append(".").append(setter.getName()).append("(");
            for (int i = 0; i < setter.getParameterTypes().length; i++) {
                if (i == 0) {
                    setterCall.append("arg").append(i);
                } else {
                    setterCall.append(", arg").append(i);
                }
            }
            setterCall.append(");");
            writer.println(setterCall);
            writer.println("\t}");
            writer.println();
        }
        writer.println();
        for (ExportEvent event : exportEvents) { //TODO: optimize
            writer.println("\tpublic void " + event.getEventSetDescriptor().getAddListenerMethod().getName() + "(" + event.getEventSetDescriptor().getListenerType().getCanonicalName() + " listener) {");
            writer.println("\t\t" + event.getBeanNode().lowercaseFirst() + "." + event.getEventSetDescriptor().getAddListenerMethod().getName() + "(listener);");
            writer.println("\t}");
            writer.println();
            if (event.getEventSetDescriptor().getGetListenerMethod() != null) {
                writer.println("\tpublic " + event.getEventSetDescriptor().getGetListenerMethod().getReturnType().getCanonicalName() + " " + event.getEventSetDescriptor().getGetListenerMethod().getName() + "() {");
                writer.println("\t\treturn " + event.getBeanNode().lowercaseFirst() + "." + event.getEventSetDescriptor().getGetListenerMethod().getName() + "();");
                writer.println("\t}");
                writer.println();
            }
            writer.println("\tpublic void " + event.getEventSetDescriptor().getRemoveListenerMethod().getName() + "(" + event.getEventSetDescriptor().getListenerType().getCanonicalName() + " listener) {");
            writer.println("\t\t" + event.getBeanNode().lowercaseFirst() + "." + event.getEventSetDescriptor().getRemoveListenerMethod().getName() + "(listener);");
            writer.println("\t}");
            writer.println();
        }
        for (ExportMethod exportMethod : exportMethods) {
            Method method = exportMethod.getMethodDescriptor().getMethod();
            StringBuilder methodSignature = new StringBuilder("\tpublic " + method.getReturnType().getCanonicalName() + " " + method.getName() + "(");
            for (int i = 0; i < method.getParameterTypes().length; i++) {
                if (i == 0) {
                    methodSignature.append(method.getParameterTypes()[i].getCanonicalName()).append(" arg").append(i);
                } else {
                    methodSignature.append(", ").append(method.getParameterTypes()[i].getCanonicalName()).append(" arg").append(i);
                }
            }
            methodSignature.append(")");
            Class<?>[] setterExceptions = method.getExceptionTypes();
            for (int i = 0; i < setterExceptions.length; i++) {
                if (i == 0) {
                    methodSignature.append("throws ").append(setterExceptions[i].getCanonicalName());
                } else {
                    methodSignature.append(",").append(setterExceptions[i].getCanonicalName());
                }
            }
            methodSignature.append("{");
            writer.println(methodSignature);
            StringBuilder methodCall = new StringBuilder("\t\t").append(exportMethod.getNode().lowercaseFirst()).append(".").append(method.getName()).append("(");
            for (int i = 0; i < method.getParameterTypes().length; i++) {
                if (i == 0) {
                    methodCall.append("arg").append(i);
                } else {
                    methodCall.append(", arg").append(i);
                }
            }
            methodCall.append(");");
            writer.println(methodCall);
            writer.println("\t}");
            writer.println();
        }
        writer.println("}");
        writer.println();
        writer.close();
        if (writer.checkError()) {
            throw new IOException("Error writing Bean File: " + exportBean.getBeanName());
        }

        writer = new PrintWriter(new FileWriter(beanInfo));
        writer.println("package " + DEFAULT_BEAN_DIRECTORY_NAME.replaceAll(Pattern.quote("/"), ".") + ";");
        writer.println();
        writer.println("import java.beans.*;");
        writer.println("import java.io.Serializable;");
        if (exportBean.isAddPropertyChangeSupport()) {
            writer.println("import java.beans.PropertyChangeListener;");
        }
        writer.println();
        writer.println("public class " + exportBean.getBeanName() + "BeanInfo extends SimpleBeanInfo implements Serializable {");
        writer.println();
        if (!exportProperties.isEmpty()) {
            writer.println("    @Override");
            writer.println("    public PropertyDescriptor[] getPropertyDescriptors() {");
            writer.println("        try {");
            writer.println("            Class cls = " + exportBean.getBeanName() + ".class;");
            StringBuilder propertyDescriptorArray = new StringBuilder("{");
            for (ExportProperty exportProperty : exportProperties) {
                String descriptorName = "pd" + exportProperty.uppercaseFirst();
                writer.println("            PropertyDescriptor " + descriptorName + " = new PropertyDescriptor(\"" + exportProperty.getName() + "\", cls);");
                writer.println("            " + descriptorName + ".setDisplayName(\"" + exportProperty.getPropertyDescriptor().getDisplayName() + "\");");
                if (exportProperty.getPropertyDescriptor().getPropertyEditorClass() != null) {
                    writer.println("            " + descriptorName + ".setPropertyEditorClass(" + exportProperty.getPropertyDescriptor().getPropertyEditorClass().getCanonicalName() + ".class);");
                }
                if (propertyDescriptorArray.length() > 1) {
                    propertyDescriptorArray.append(", ").append(descriptorName);
                } else {
                    propertyDescriptorArray.append(descriptorName);
                }
            }
            propertyDescriptorArray.append("}");
            writer.println("            return new PropertyDescriptor[]" + propertyDescriptorArray + ";");
            writer.println("        } catch (IntrospectionException e) {");
            writer.println("            e.printStackTrace();");
            writer.println("        }");
            writer.println("        return null;");
            writer.println("    }");
            writer.println();
        }

        if (!exportEvents.isEmpty()) {
            writer.println("    @Override");
            writer.println("    public EventSetDescriptor[] getEventSetDescriptors() {");
            writer.println("        try {");
            writer.println("            Class cls = " + exportBean.getBeanName() + ".class;");
            StringBuilder eventSetDescriptorArray = new StringBuilder("{");
            if (exportBean.isAddPropertyChangeSupport()) {
                writer.println("            EventSetDescriptor esdPropertyChange = new EventSetDescriptor(cls, \"propertyChange\", PropertyChangeListener.class, \"propertyChange\");");
                eventSetDescriptorArray.append("esdPropertyChange");
            }
            for (ExportEvent exportEvent : exportEvents) {
                String descriptorName = "esd" + exportEvent.uppercaseFirst();
                writer.println("            EventSetDescriptor " + descriptorName + " = new EventSetDescriptor(cls, \"" + exportEvent.getName() //TODO:generify
                        + "\", " + exportEvent.getEventSetDescriptor().getListenerType().getCanonicalName() + ".class, \"" + exportEvent.getEventSetDescriptor().getListenerMethods()[0].getName() + "\");");

                if (eventSetDescriptorArray.length() > 1) {
                    eventSetDescriptorArray.append(", ").append(descriptorName);
                } else {
                    eventSetDescriptorArray.append(descriptorName);
                }
            }
            eventSetDescriptorArray.append("}");
            writer.println("            return new EventSetDescriptor[]" + eventSetDescriptorArray + ";");
            writer.println("        } catch (IntrospectionException e) {");
            writer.println("            e.printStackTrace();");
            writer.println("        }");
            writer.println("        return null;");
            writer.println("    }");
            writer.println();
        }

        if (!exportEvents.isEmpty()) {
            writer.println("    @Override");
            writer.println("    public MethodDescriptor[] getMethodDescriptors() {");
            writer.println("        try {");
            writer.println("            Class cls = " + exportBean.getBeanName() + ".class;");
            StringBuilder methodDescriptorArray = new StringBuilder("{");
            for (ExportMethod exportMethod : exportMethods) {
                String descriptorName = "md" + exportMethod.uppercaseFirst();
                writer.println("            MethodDescriptor " + descriptorName + " = new MethodDescriptor(cls.getMethod(\"" + exportMethod.getName() //TODO:generify
                        + "\", new Class[]{" + exportMethod.getMethodDescriptor().getMethod().getParameterTypes()[0].getCanonicalName() + ".class}), null);");
                if (methodDescriptorArray.length() > 1) {
                    methodDescriptorArray.append(", ").append(descriptorName);
                } else {
                    methodDescriptorArray.append(descriptorName);
                }
            }
            methodDescriptorArray.append("}");
            writer.println("            return new MethodDescriptor[]" + methodDescriptorArray + ";");
            writer.println("        } catch (NoSuchMethodException e) {");
            writer.println("            e.printStackTrace();");
            writer.println("        }");
            writer.println("        return null;");
            writer.println("    }");
            writer.println("}");
            writer.println();
        }
        writer.close();
        if (writer.checkError()) {
            throw new IOException("Error writing BeanInfo File: " + exportBean.getBeanName());
        }
    }

    private List<ExportProperty> sortPropertiesByBinding(List<ExportProperty> properties) {
        //TODO: think
        return properties;
    }

    private String generateAdapterName() {
        Random rand = new Random();
        int random = 100000000 + rand.nextInt(900000000);
        return "__PropertyHookup_" + random;
    }

    private String generateSerName(Object type) {
        Random rand = new Random();
        int random = 100000000 + rand.nextInt(900000000);
        return type.getClass().getSimpleName().toLowerCase() + "_" + random;
    }

    private String getRelativePath(String base, File file, boolean leadingSlash) {
        String[] split = file.getAbsolutePath().split(Pattern.quote(base));
        String relativePath = split.length > 1 ? split[1] : split[0];
        if (relativePath != null && relativePath.length() > 2 && !leadingSlash) {
            relativePath = relativePath.substring(1, relativePath.length());
        }
        return relativePath.replace('\\', '/');
    }

    private String convertPrimitive(Object object) {
        if (object == null || object instanceof Void) return null;
        if (object instanceof Character) {
            return "'" + purifyString(object.toString()) + "'";
        } else if (object instanceof Float) {
            return object.toString() + "f";
        } else if (object instanceof Long) {
            return object.toString() + "L";
        } else if (object instanceof String) {
            return "\"" + purifyString(object.toString()) + "\"";
        } else if (object instanceof Short) {
            return "(short) " + object.toString();
        }
        return object.toString();
    }

    private String purifyString(String in) {
        in = in.replace("\\", "\\\\");
        in = in.replace(Pattern.quote("\""), "\\\"");
        in = in.replace(Pattern.quote("\'"), "\\\'");
        return in;
    }

    public static boolean isPrimitiveOrPrimitiveWrapperOrString(Class<?> type) {
        return (type.isPrimitive() && type != void.class) ||
                type == Double.class || type == Float.class || type == Long.class ||
                type == Integer.class || type == Short.class || type == Character.class ||
                type == Byte.class || type == Boolean.class || type == String.class;
    }

    private List<ExportConstraintViolation> validateConfiguration() {
        /*List<ExportConstraintViolation> violations = new ArrayList<>();
        Set<String> beanNamePool = new HashSet<>();
        for (ExportBean bean : exportBeans) {
            Set<String> methodNamePool = new HashSet<>();
            for (ExportProperty property : bean.getProperties()) {
                if (methodNamePool.contains(property.getName())) {
                    violations.add(new ExportConstraintViolation("Duplicate naming: " + property.getName()));
                }
                methodNamePool.add(property.getName());
            }
            for (ExportMethod method : bean.getMethods()) {
                if (methodNamePool.contains(method.getName())) {
                    violations.add(new ExportConstraintViolation("Duplicate naming: " + method.getName()));
                }
                methodNamePool.add(method.getName());
            }
            for (ExportEvent event : bean.getEvents()) {
                if (methodNamePool.contains(event.getName())) {
                    violations.add(new ExportConstraintViolation("Duplicate naming: " + event.getName()));
                }
                methodNamePool.add(event.getName());
            }
        }*/
        //TODO: Validate Unique property naming within ExportBean
        //TODO: Validate Unique Export bean naming, may not have the same name as another Bean in the generated JAR
        //TODO: Validate resource conflicts
        //TODO: Validate unique naming for all methods (input output interface)
        //TODO: Validate input output interface
        return null;
    }
}

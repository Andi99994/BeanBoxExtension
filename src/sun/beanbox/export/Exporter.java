package sun.beanbox.export;

import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;
import org.apache.commons.io.FileUtils;
import sun.beanbox.HookupManager;
import sun.beanbox.Wrapper;
import sun.beanbox.WrapperEventTarget;
import sun.beanbox.WrapperPropertyEventInfo;
import sun.beanbox.export.components.ExportConstraintViolation;
import sun.beanbox.export.components.NodeSelector;
import sun.beanbox.export.datastructure.*;
import sun.beanbox.export.util.JARCompiler;
import sun.beanbox.export.util.StringUtil;

import javax.lang.model.SourceVersion;
import javax.lang.model.element.Modifier;
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
import java.util.regex.Pattern;
import java.util.stream.Collectors;

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
    private boolean keepSources = false;

    private String tmpDirectoryName = "/tmp";
    private static final String DEFAULT_MANIFEST_DIRECTORY_NAME = "META-INF";
    private static final String DEFAULT_BEAN_DIRECTORY_NAME = "beanBox/generated/beans";
    private static final String DEFAULT_SERIALIZED_PROPERTIES_DIRECTORY_NAME = "beanBox/generated/beans/properties";
    private static final String DEFAULT_ADAPTER_DIRECTORY_NAME = "beanBox/generated/beans/adapters";

    private static final String DEFAULT_BEAN_NAME = "ExportBean";

    /**
     * Upon instantiation of the Exporter the selected Wrappers are grouped, processed and converted into a more suitable
     * data structure.
     *
     * @param beans the beans that were selected for export
     * @throws IntrospectionException   if there is an error reading bean information
     * @throws IllegalArgumentException if there is an error accessing bean properties
     */
    public Exporter(List<Wrapper> beans) throws IntrospectionException, IllegalArgumentException, NoSuchMethodException {
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
     * @throws IntrospectionException   if there is an error reading bean information
     * @throws IllegalArgumentException if there is an error reading properties
     */
    private ExportBean assembleExportBean(List<Wrapper> wrappers, String name) throws IntrospectionException, IllegalArgumentException, NoSuchMethodException {
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
     * @throws IntrospectionException if there is an error reading bean information
     */
    private BeanNode createBeanNode(Wrapper wrapper, HashMap<Wrapper, BeanNode> createdNodes) throws IntrospectionException, NoSuchMethodException {
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
        //also we check if the class or any superclass implements any EventListener interface for one of these methods.
        for (MethodDescriptor methodDescriptor : beanInfo.getMethodDescriptors()) {
            if (!methodDescriptor.isExpert() && !methodDescriptor.isHidden()
                    && !methodDescriptor.getName().equals("getClass") && !methodDescriptor.getName().equals("getPeer")
                    && !methodDescriptor.getName().equals("notify") && !methodDescriptor.getName().equals("wait")
                    && !methodDescriptor.getName().toLowerCase().contains("propertychange")
                    && !methodDescriptor.getName().equals("notifyAll") && methodDescriptor.getMethod().getReturnType().equals(Void.TYPE)) {
                Method checkMethod = methodDescriptor.getMethod();
                boolean addMethod = true;
                for (ExportMethod exportMethod : beanNode.getMethods()) {
                    Method method = exportMethod.getMethodDescriptor().getMethod();
                    if (method.getName().equals(checkMethod.getName()) && Arrays.equals(checkMethod.getParameterTypes(), method.getParameterTypes())) {
                        addMethod = false;
                        break;
                    }
                }
                for (ExportProperty exportProperty : beanNode.getProperties()) {
                    Method getter = exportProperty.getPropertyDescriptor().getReadMethod();
                    Method setter = exportProperty.getPropertyDescriptor().getWriteMethod();
                    if ((getter.getName().equals(checkMethod.getName()) && Arrays.equals(checkMethod.getParameterTypes(), getter.getParameterTypes())) ||
                            (setter.getName().equals(checkMethod.getName()) && Arrays.equals(checkMethod.getParameterTypes(), setter.getParameterTypes()))) {
                        addMethod = false;
                        break;
                    }
                }
                if (!addMethod) continue;
                List<Class> classTree = new ArrayList<>(getAllExtendedOrImplementedTypes(beanNode.getData().getClass()));
                List<Class> declaringInterfaces = new ArrayList<>();
                for (Class<?> cls : classTree) {
                    if (cls.isInterface() && EventListener.class.isAssignableFrom(cls)) {
                        try {
                            cls.getMethod(methodDescriptor.getMethod().getName(), methodDescriptor.getMethod().getParameterTypes());
                            declaringInterfaces.add(cls);
                        } catch (NoSuchMethodException e) {
                            //Method does not exist
                        }
                    }
                }
                Class<?> declaringClass = null;
                for (Class<?> cls : declaringInterfaces) {
                    boolean add = true;
                    for (Class<?> cls2 : declaringInterfaces) {
                        if (cls.equals(cls2)) continue;
                        if (cls2.isAssignableFrom(cls)) {
                            add = false;
                            break;
                        }
                    }
                    if (add) {
                        declaringClass = cls;
                        break;
                    }
                }
                beanNode.getMethods().add(new ExportMethod(methodDescriptor, beanNode, declaringClass));
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
            new NodeSelector(null, availableNodes, "Could not infer output Beans (maybe you have cyclic references in your composition?). Please select from the list below.").setVisible(true);
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
            new NodeSelector(null, availableBeans, "Could not infer input Beans (maybe you have cyclic references in your composition?). Please select from the list below.").setVisible(true);
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
                resources.addAll(generatePropertyAdapters(tmpDirectory));
                for (ExportBean exportBean : exportBeans) {
                    generateBeanJavaPoet(tmpBeanDirectory, tmpPropertiesDirectory, exportBean, tmpDirectory);
                }
                generateManifest(tmpManifestDirectory);
                JARCompiler.compileSources(tmpBeanDirectory, resources);
                JARCompiler.packJar(target, tmpDirectory);
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
                    adapters.add(generatePropertyAdapterJavaPoet(targetDirectory, edge));
                }
            }
        }
        return adapters;
    }

    /**
     * Generates a single PropertyBinding adapter from a PropertyBindingEdge into a target directory. Currently it only
     * supports the same functionality as the BeanBox that is simple 1:1 property to property binding. If the BeanBox gets
     * support for more complex scenarios like property to method binding, this would need to be changed.
     *
     * @param targetDirectory     the target directory
     * @param propertyBindingEdge the property binding from which the class should be generated
     * @return returns a file of the generated class
     * @throws IOException if there is an error writing
     */
    private File generatePropertyAdapter(File targetDirectory, PropertyBindingEdge propertyBindingEdge) throws IOException {
        File adapter = new File(targetDirectory, StringUtil.generateName("__PropertyHookup_", 100000000, 900000000) + ".java");
        while (adapter.exists()) {
            adapter = new File(targetDirectory, StringUtil.generateName("__PropertyHookup_", 100000000, 900000000) + ".java");
        }
        if (!adapter.createNewFile()) throw new IOException("Error creating File: " + adapter.getName());
        propertyBindingEdge.setAdapterName(adapter.getName().replace(".java", ""));
        try (PrintWriter writer = new PrintWriter(new FileWriter(adapter))) {
            writer.println("package " + DEFAULT_ADAPTER_DIRECTORY_NAME.replaceAll(Pattern.quote("/"), ".") + ";");
            writer.println();
            writer.println("import java.io.Serializable;");
            writer.println("import java.beans.PropertyChangeListener;");
            writer.println("import java.beans.PropertyChangeEvent;");
            writer.println();
            writer.println("public class " + propertyBindingEdge.getAdapterName() + " implements PropertyChangeListener, Serializable {");
            writer.println();
            writer.println("\tprivate " + propertyBindingEdge.getEnd().getData().getClass().getCanonicalName() + " target;");
            writer.println();
            writer.println("\tpublic void setTarget(" + propertyBindingEdge.getEnd().getData().getClass().getCanonicalName() + " t) {");
            writer.println("\t\ttarget = t;");
            writer.println("\t}");
            writer.println();
            writer.println("\tpublic void propertyChange(PropertyChangeEvent evt) {");
            writer.println("\t\ttry {");
            writer.println("\t\t\ttarget." + propertyBindingEdge.getTargetMethod().getName() + "((" + propertyBindingEdge.getTargetMethod().getParameterTypes()[0].getCanonicalName() + ") evt.getNewValue());");
            writer.println("\t\t} catch (Exception e) {");
            writer.println("\t\t\te.printStackTrace();");
            writer.println("\t\t}");
            writer.println("\t}");
            writer.println("}");
            if (writer.checkError()) {
                throw new IOException("Error writing Adapter File: " + adapter.getName());
            }
        }

        return adapter;
    }

    /**
     * Generates a single PropertyBinding adapter from a PropertyBindingEdge into a target directory. Currently it only
     * supports the same functionality as the BeanBox that is simple 1:1 property to property binding. If the BeanBox gets
     * support for more complex scenarios like property to method binding, this would need to be changed.
     * <p>
     * Note: Like the BeanBox, this does not support indexed properties
     *
     * @param targetDirectory     the target directory
     * @param propertyBindingEdge the property binding from which the class should be generated
     * @return returns a file of the generated class
     * @throws IOException if there is an error writing
     */
    private File generatePropertyAdapterJavaPoet(File targetDirectory, PropertyBindingEdge propertyBindingEdge) throws IOException {
        File adapter = new File(targetDirectory.getAbsolutePath() + File.separator + DEFAULT_ADAPTER_DIRECTORY_NAME,
                StringUtil.generateName("__PropertyHookup_", 100000000, 900000000) + ".java");
        while (adapter.exists()) {
            adapter = new File(targetDirectory, StringUtil.generateName("__PropertyHookup_", 100000000, 900000000) + ".java");
        }
        propertyBindingEdge.setAdapterName(adapter.getName().replace(".java", ""));

        MethodSpec setTarget = MethodSpec.methodBuilder("setTarget")
                .addModifiers(Modifier.PUBLIC)
                .returns(void.class)
                .addParameter(propertyBindingEdge.getEnd().getData().getClass(), "t")
                .addCode("target = t;")
                .build();

        MethodSpec propertyChange = MethodSpec.methodBuilder("propertyChange")
                .addModifiers(Modifier.PUBLIC)
                .returns(void.class)
                .addParameter(PropertyChangeEvent.class, "evt")
                .addCode("try {\n" +
                        "\ttarget." + propertyBindingEdge.getTargetMethod().getName() + "((" + propertyBindingEdge.getTargetMethod().getParameterTypes()[0].getCanonicalName() + ") evt.getNewValue());\n" +
                        "} catch (Exception e) {\n" +
                        "\te.printStackTrace();\n" +
                        "}")
                .build();

        FieldSpec target = FieldSpec.builder(propertyBindingEdge.getEnd().getData().getClass(), "target")
                .addModifiers(Modifier.PRIVATE)
                .build();

        FieldSpec suid = FieldSpec.builder(long.class, "serialVersionUID")
                .addModifiers(Modifier.FINAL, Modifier.PRIVATE, Modifier.STATIC)
                .initializer("1L")
                .build();

        TypeSpec helloWorld = TypeSpec.classBuilder(propertyBindingEdge.getAdapterName())
                .addModifiers(Modifier.PUBLIC)
                .addSuperinterface(PropertyChangeListener.class)
                .addSuperinterface(Serializable.class)
                .addMethod(setTarget)
                .addMethod(propertyChange)
                .addField(target)
                .addField(suid)
                .build();

        JavaFile javaFile = JavaFile.builder(DEFAULT_ADAPTER_DIRECTORY_NAME.replaceAll(Pattern.quote("/"), "."), helloWorld)
                .build();
        javaFile.writeTo(targetDirectory);

        return adapter;
    }

    /**
     * This method generates the bean class and the beanInfo class. It collects various information
     * about the bean and uses it to generate all required code. If there are any complex properties that need a default
     * value to be set, these are serialized.
     * <p>
     * Requirement: Bean must adhere to the JavaBeans Specification, any EventListener interfaces must be implemented directly
     * and declare no more than one method.
     * Possible bug: EventListener Interfaces that declare more than one method
     * Possible extension: Add PropertyVeto support
     *
     * @param targetDirectory   the target directory for the beans
     * @param propertyDirectory the target directory for any serialized properties
     * @param exportBean        the bean to be generated
     * @throws IOException               if there is an error writing
     * @throws InvocationTargetException if there is an error accessing properties
     * @throws IllegalAccessException    if there is an error accessing properties
     * @throws NoSuchMethodException     if there is an error accessing methods
     */
    private void generateBeanJavaPoet(File targetDirectory, File propertyDirectory, ExportBean exportBean, File tmpDirectory) throws IOException, InvocationTargetException, IllegalAccessException, NoSuchMethodException {
        if (new File(targetDirectory.getAbsolutePath(), exportBean.getBeanName() + ".java").exists()
                || new File(targetDirectory.getAbsolutePath(), exportBean.getBeanName() + "BeanInfo.java").exists()
                || new File(targetDirectory.getAbsolutePath(), exportBean.getBeanName() + ".class").exists()
                || new File(targetDirectory.getAbsolutePath(), exportBean.getBeanName() + "BeanInfo.class").exists()) {
            throw new IOException("Error creating Files for: " + exportBean + ". Maybe you have conflicting resources?");
        }
        //collect some necessary information beforehand to increase performance
        List<ExportProperty> exportProperties = exportBean.getProperties();
        List<ExportMethod> exportMethods = exportBean.getMethods();
        List<ExportEvent> exportEvents = exportBean.getEvents();
        List<BeanNode> beanNodes = exportBean.getBeans().getAllNodes();
        List<FieldSpec> fields = new ArrayList<>();
        List<MethodSpec> methods = new ArrayList<>();
        List<Class<?>> interfaces = new ArrayList<>();
        int hookupCounter = 0;

        for (ExportMethod exportMethod : exportMethods) {
            if (exportMethod.isImplementInterface()) {
                interfaces.add(exportMethod.getDeclaringClass());
            }
        }


        MethodSpec.Builder constructor = MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PUBLIC)
                .addCode("try {\n");

        fields.add(FieldSpec.builder(long.class, "serialVersionUID")
                .addModifiers(Modifier.FINAL, Modifier.PRIVATE, Modifier.STATIC)
                .initializer("1L")
                .build());

        for (BeanNode node : beanNodes) {
            fields.add(FieldSpec.builder(node.getData().getClass(), node.lowercaseFirst())
                    .addModifiers(Modifier.PRIVATE)
                    .build());
            constructor.addCode("\t" + node.lowercaseFirst() + " = new " + node.getData().getClass().getCanonicalName() + "();\n");
        }
        for (BeanNode node : beanNodes) {
            for (AdapterCompositionEdge edge : node.getAdapterCompositionEdges()) {
                constructor.addCode("\t" + edge.getHookup().getClass().getCanonicalName() + " " +
                        "hookup" + hookupCounter + " = new " + edge.getHookup().getClass().getCanonicalName() + "();\n" +
                        "\thookup" + hookupCounter + ".setTarget(" + edge.getEnd().lowercaseFirst() + ");\n" +
                        "\t" + edge.getStart().lowercaseFirst() + "." + edge.getEventSetDescriptor().getAddListenerMethod().getName() +
                        "(hookup" + hookupCounter + ");\n");
                hookupCounter++;
            }
            for (DirectCompositionEdge edge : node.getDirectCompositionEdges()) {
                constructor.addCode("\t" + edge.getStart().lowercaseFirst() + "." + edge.getEventSetDescriptor().getAddListenerMethod().getName() + "(" + edge.getEnd().lowercaseFirst() + ");\n");
            }
            for (PropertyBindingEdge edge : node.getPropertyBindingEdges()) {
                String canonicalAdaperName = DEFAULT_ADAPTER_DIRECTORY_NAME.replace("/", ".") + "." + edge.getAdapterName();
                constructor.addCode("\t" + canonicalAdaperName + " hookup" + hookupCounter + " = new " + canonicalAdaperName + "();\n" +
                        "\thookup" + hookupCounter + ".setTarget(" + edge.getEnd().lowercaseFirst() + ");\n" +
                        "\t" + edge.getStart().lowercaseFirst() + ".add" + edge.getEventSetName() + "Listener(hookup" + hookupCounter + ");\n");
                hookupCounter++;
            }
        }
        for (ExportProperty property : exportProperties) {
            Method getter = property.getPropertyDescriptor().getReadMethod();
            Method setter = property.getPropertyDescriptor().getWriteMethod();

            if (property.isSetDefaultValue()) {
                Object value = getter.invoke(property.getNode().getData());
                if (value == null || value instanceof Void || isPrimitiveOrPrimitiveWrapperOrString(value.getClass())) {
                    constructor.addCode("\t" + property.getNode().lowercaseFirst() + "." + getter.getName() + "(" + StringUtil.convertPrimitive(value) + ");\n");
                } else {
                    File ser = new File(propertyDirectory.getAbsolutePath(), StringUtil.generateName(value.getClass().getSimpleName().toLowerCase() + "_", 100000000, 900000000) + ".ser");
                    while (ser.exists()) {
                        ser = new File(propertyDirectory.getAbsolutePath(), StringUtil.generateName(value.getClass().getSimpleName().toLowerCase() + "_", 100000000, 900000000) + ".ser");
                    }
                    ser.getParentFile().mkdirs();
                    try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(ser))) {
                        out.writeObject(value);
                        constructor.addCode("\ttry (java.io.ObjectInputStream in = new java.io.ObjectInputStream(getClass().getResourceAsStream(\""
                                + StringUtil.getRelativePath(targetDirectory.getAbsolutePath(), ser, false) + "\"))){\n" +
                                "\t\t" + property.getNode().lowercaseFirst() + "." + getter.getName()
                                + "((" + property.getPropertyType().getCanonicalName() + ") in.readObject());\n" +
                                "\t}\n");
                    } catch (IOException i) {
                        throw new IOException("Error serializing property: " + property.getNode().getName() + ":" + property.getName());
                    }
                }
            }

            String prefix = property.getPropertyType() == boolean.class || property.getPropertyType() == Boolean.class ?
                    "is" : "get";

            MethodSpec.Builder getBuilder = MethodSpec.methodBuilder(prefix + property.uppercaseFirst())
                    .addModifiers(Modifier.PUBLIC)
                    .returns(property.getPropertyType());

            for (int i = 0; i < getter.getParameterTypes().length; i++) {
                getBuilder.addParameter(getter.getParameterTypes()[i], "arg" + i);
            }
            for (int i = 0; i < getter.getExceptionTypes().length; i++) {
                getBuilder.addException(getter.getExceptionTypes()[i]);
            }
            StringBuilder getterCall = new StringBuilder("return ").append(property.getNode().lowercaseFirst()).append(".").append(getter.getName()).append("(");
            for (int i = 0; i < getter.getParameterTypes().length; i++) {
                if (i == 0) {
                    getterCall.append("arg").append(i);
                } else {
                    getterCall.append(", arg").append(i);
                }
            }
            getterCall.append(");\n");
            methods.add(getBuilder.addCode(getterCall.toString()).build());

            MethodSpec.Builder setBuilder = MethodSpec.methodBuilder("set" + property.uppercaseFirst())
                    .addModifiers(Modifier.PUBLIC)
                    .returns(void.class);

            for (int i = 0; i < setter.getParameterTypes().length; i++) {
                setBuilder.addParameter(setter.getParameterTypes()[i], "arg" + i);
            }
            for (int i = 0; i < setter.getExceptionTypes().length; i++) {
                setBuilder.addException(setter.getExceptionTypes()[i]);
            }
            if (exportBean.isAddPropertyChangeSupport()) {
                setBuilder.addCode("propertyChangeSupport.firePropertyChange(\"" + property.getName() + "\", " + property.getNode().lowercaseFirst() + "." + getter.getName() + "(), arg0);\n");
            }
            StringBuilder setterCall = new StringBuilder("").append(property.getNode().lowercaseFirst()).append(".").append(setter.getName()).append("(");
            for (int i = 0; i < setter.getParameterTypes().length; i++) {
                if (i == 0) {
                    setterCall.append("arg").append(i);
                } else {
                    setterCall.append(", arg").append(i);
                }
            }
            setterCall.append(");\n");
            methods.add(setBuilder.addCode(setterCall.toString()).build());
        }

        for (ExportEvent event : exportEvents) {
            EventSetDescriptor esd = event.getEventSetDescriptor();
            methods.add(MethodSpec.methodBuilder("add" + event.uppercaseFirst() + "EventListener")
                    .addModifiers(Modifier.PUBLIC)
                    .returns(void.class)
                    .addParameter(esd.getListenerType(), "listener")
                    .addCode("" + event.getBeanNode().lowercaseFirst() + "." + esd.getAddListenerMethod().getName() + "(listener);")
                    .build());
            methods.add(MethodSpec.methodBuilder("remove" + event.uppercaseFirst() + "EventListener")
                    .addModifiers(Modifier.PUBLIC)
                    .returns(void.class)
                    .addParameter(esd.getListenerType(), "listener")
                    .addCode("" + event.getBeanNode().lowercaseFirst() + "." + esd.getRemoveListenerMethod().getName() + "(listener);")
                    .build());

            if (event.getEventSetDescriptor().getGetListenerMethod() != null) {
                methods.add(MethodSpec.methodBuilder("get" + event.uppercaseFirst() + "EventListeners")
                        .addModifiers(Modifier.PUBLIC)
                        .returns(esd.getGetListenerMethod().getReturnType())
                        .addCode("return " + event.getBeanNode().lowercaseFirst() + "." + esd.getGetListenerMethod().getName() + "();")
                        .build());
            }
        }

        for (ExportMethod exportMethod : exportMethods) {
            Method method = exportMethod.getMethodDescriptor().getMethod();
            MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder(method.getName())
                    .addModifiers(Modifier.PUBLIC)
                    .returns(method.getReturnType());

            for (int i = 0; i < method.getParameterTypes().length; i++) {
                methodBuilder.addParameter(method.getParameterTypes()[i], "arg" + i);
            }
            for (int i = 0; i < method.getExceptionTypes().length; i++) {
                methodBuilder.addException(method.getExceptionTypes()[i]);
            }
            StringBuilder methodCall = new StringBuilder("").append(exportMethod.getNode().lowercaseFirst()).append(".").append(method.getName()).append("(");
            for (int i = 0; i < method.getParameterTypes().length; i++) {
                if (i == 0) {
                    methodCall.append("arg").append(i);
                } else {
                    methodCall.append(", arg").append(i);
                }
            }
            methodCall.append(");\n");
            methods.add(methodBuilder.addCode(methodCall.toString()).build());
        }

        if (exportBean.isAddPropertyChangeSupport()) {
            interfaces.add(PropertyChangeListener.class);
            fields.add(FieldSpec.builder(PropertyChangeSupport.class, "propertyChangeSupport") //TODO: check that a bean does not have this name
                    .addModifiers(Modifier.PRIVATE)
                    .initializer("new PropertyChangeSupport(this)")
                    .build());

            methods.add(MethodSpec.methodBuilder("addPropertyChangeListener")
                    .addModifiers(Modifier.PUBLIC)
                    .returns(void.class)
                    .addParameter(PropertyChangeListener.class, "listener")
                    .addCode("propertyChangeSupport.addPropertyChangeListener(listener);")
                    .build());
            methods.add(MethodSpec.methodBuilder("removePropertyChangeListener")
                    .addModifiers(Modifier.PUBLIC)
                    .returns(void.class)
                    .addParameter(PropertyChangeListener.class, "listener")
                    .addCode("propertyChangeSupport.removePropertyChangeListener(listener);")
                    .build());
            methods.add(MethodSpec.methodBuilder("getPropertyChangeListeners")
                    .addModifiers(Modifier.PUBLIC)
                    .returns(PropertyChangeListener[].class)
                    .addParameter(PropertyChangeListener.class, "listener")
                    .addCode("return propertyChangeSupport.getPropertyChangeListeners();")
                    .build());
            methods.add(MethodSpec.methodBuilder("propertyChange")
                    .addModifiers(Modifier.PUBLIC)
                    .returns(void.class)
                    .addAnnotation(Override.class)
                    .addParameter(PropertyChangeEvent.class, "evt")
                    .build());
        }

        constructor.addCode("} catch (Exception e) {\n" +
                "\te.printStackTrace();\n" +
                "}\n");

        TypeSpec.Builder bean = TypeSpec.classBuilder(exportBean.getBeanName())
                .addModifiers(Modifier.PUBLIC)
                .addMethod(constructor.build())
                .addSuperinterface(Serializable.class);

        for (Class clz : interfaces) {
            bean.addSuperinterface(clz);
        }
        bean.addMethods(methods);
        bean.addFields(fields);

        JavaFile.builder(DEFAULT_BEAN_DIRECTORY_NAME.replaceAll(Pattern.quote("/"), "."), bean.build())
                .build().writeTo(tmpDirectory);

        //generate BeanInfo class

        MethodSpec.Builder propertyDescriptor = MethodSpec.methodBuilder("getPropertyDescriptors")
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Override.class)
                .returns(PropertyDescriptor[].class);

        if (exportProperties.isEmpty()) {
            propertyDescriptor.addCode("return new PropertyDescriptor[]{};");
        } else {
            propertyDescriptor.addCode("try {\n");
            propertyDescriptor.addCode("\tClass<?> cls = " + exportBean.getBeanName() + ".class;\n");
            StringBuilder propertyDescriptorArray = new StringBuilder("{");
            for (ExportProperty exportProperty : exportProperties) {
                String descriptorName = StringUtil.generateName("pd" + exportProperty.uppercaseFirst() + "_", 10000, 90000);
                propertyDescriptor.addCode("\tPropertyDescriptor " + descriptorName + " = new PropertyDescriptor(\"" + exportProperty.getName() + "\", cls);\n");
                propertyDescriptor.addCode("\t" + descriptorName + ".setDisplayName(\"" + exportProperty.getPropertyDescriptor().getDisplayName() + "\");\n");
                if (exportProperty.getPropertyDescriptor().getPropertyEditorClass() != null) {
                    propertyDescriptor.addCode("\t" + descriptorName + ".setPropertyEditorClass(" + exportProperty.getPropertyDescriptor().getPropertyEditorClass().getCanonicalName() + ".class);\n");
                }
                if (propertyDescriptorArray.length() > 1) {
                    propertyDescriptorArray.append(", ").append(descriptorName);
                } else {
                    propertyDescriptorArray.append(descriptorName);
                }
            }
            propertyDescriptorArray.append("}");
            propertyDescriptor.addCode("\treturn new PropertyDescriptor[]" + propertyDescriptorArray + ";\n");
            propertyDescriptor.addCode("} catch (java.beans.IntrospectionException e) {\n");
            propertyDescriptor.addCode("\te.printStackTrace();\n");
            propertyDescriptor.addCode("}\n");
            propertyDescriptor.addCode("return null;\n");
        }

        MethodSpec.Builder eventSetDescriptor = MethodSpec.methodBuilder("getEventSetDescriptors")
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Override.class)
                .returns(EventSetDescriptor[].class);

        if (!exportEvents.isEmpty()) {
            eventSetDescriptor.addCode("try {\n");
            eventSetDescriptor.addCode("\tClass<?> cls = " + exportBean.getBeanName() + ".class;\n");
            StringBuilder eventSetDescriptorArray = new StringBuilder("{");
            if (exportBean.isAddPropertyChangeSupport()) {
                eventSetDescriptor.addCode("\tEventSetDescriptor esdPropertyChange = new EventSetDescriptor(cls, \"propertyChange\", java.beans.PropertyChangeListener.class, \"propertyChange\");\n");
                eventSetDescriptorArray.append("esdPropertyChange");
            }
            for (ExportEvent exportEvent : exportEvents) {
                StringBuilder listenerMethodsArray = new StringBuilder("{");
                for (Method method : exportEvent.getEventSetDescriptor().getListenerMethods()) {
                    if (listenerMethodsArray.length() > 1) {
                        listenerMethodsArray.append(", ").append("\"").append(method.getName()).append("\"");
                    } else {
                        listenerMethodsArray.append("\"").append(method.getName()).append("\"");
                    }
                }
                String descriptorName = StringUtil.generateName("esd" + exportEvent.uppercaseFirst() + "_", 10000, 90000);
                eventSetDescriptor.addCode("\tEventSetDescriptor " + descriptorName + " = new EventSetDescriptor(cls, \"" + exportEvent.getName() + "\", "
                        + exportEvent.getEventSetDescriptor().getListenerType().getCanonicalName() + ".class, new String[]" + listenerMethodsArray.toString() + "}, " +
                        "\"add" + exportEvent.uppercaseFirst() + "EventListener\", \"remove" + exportEvent.uppercaseFirst() + "EventListener\");\n");

                if (eventSetDescriptorArray.length() > 1) {
                    eventSetDescriptorArray.append(", ").append(descriptorName);
                } else {
                    eventSetDescriptorArray.append(descriptorName);
                }
            }
            eventSetDescriptorArray.append("}");
            eventSetDescriptor.addCode("\treturn new EventSetDescriptor[]" + eventSetDescriptorArray + ";\n");
            eventSetDescriptor.addCode("} catch (java.beans.IntrospectionException e) {\n");
            eventSetDescriptor.addCode("\te.printStackTrace();\n");
            eventSetDescriptor.addCode("}\n");
            eventSetDescriptor.addCode("return null;\n");
        } else {
            eventSetDescriptor.addCode("\t\treturn new EventSetDescriptor[]{};\n");
        }

        MethodSpec.Builder methodDescriptor = MethodSpec.methodBuilder("getMethodDescriptors")
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Override.class)
                .returns(MethodDescriptor[].class);

        if (!exportEvents.isEmpty()) {
            methodDescriptor.addCode("try {\n");
            methodDescriptor.addCode("\tClass<?> cls = " + exportBean.getBeanName() + ".class;\n");
            StringBuilder methodDescriptorArray = new StringBuilder("{");
            for (ExportMethod exportMethod : exportMethods) {
                StringBuilder classArray = new StringBuilder();
                for (Class parameter : exportMethod.getMethodDescriptor().getMethod().getParameterTypes()) {
                    if (classArray.length() > 1) {
                        classArray.append(", ").append(parameter.getCanonicalName()).append(".class");
                    } else {
                        classArray.append(parameter.getCanonicalName()).append(".class");
                    }
                }
                String descriptorName = StringUtil.generateName("md" + exportMethod.uppercaseFirst() + "_", 10000, 90000);
                methodDescriptor.addCode("\tMethodDescriptor " + descriptorName + " = new MethodDescriptor(cls.getMethod(\"" + exportMethod.getName()
                        + "\", new Class<?>[]{" + classArray + "}), null);\n");
                if (methodDescriptorArray.length() > 1) {
                    methodDescriptorArray.append(", ").append(descriptorName);
                } else {
                    methodDescriptorArray.append(descriptorName);
                }
            }
            methodDescriptorArray.append("}");
            methodDescriptor.addCode("\treturn new MethodDescriptor[]" + methodDescriptorArray + ";\n");
            methodDescriptor.addCode("} catch (java.lang.NoSuchMethodException e) {\n");
            methodDescriptor.addCode("\te.printStackTrace();\n");
            methodDescriptor.addCode("}\n");
            methodDescriptor.addCode("return null;\n");
        } else {
            methodDescriptor.addCode("return new MethodDescriptor[]{};\n");
        }

        FieldSpec suid = FieldSpec.builder(long.class, "serialVersionUID")
                .addModifiers(Modifier.FINAL, Modifier.PRIVATE, Modifier.STATIC)
                .initializer("1L")
                .build();

        TypeSpec.Builder beanInfo = TypeSpec.classBuilder(exportBean.getBeanName() + "BeanInfo")
                .superclass(SimpleBeanInfo.class)
                .addModifiers(Modifier.PUBLIC)
                .addMethod(propertyDescriptor.build())
                .addMethod(eventSetDescriptor.build())
                .addMethod(methodDescriptor.build())
                .addField(suid)
                .addSuperinterface(Serializable.class);

        JavaFile.builder(DEFAULT_BEAN_DIRECTORY_NAME.replaceAll(Pattern.quote("/"), "."), beanInfo.build())
                .build().writeTo(tmpDirectory);
    }

    /**
     * Traverses the class tree upwards to collect all extended or implemented classes.
     *
     * @param clazz the class to get the information for
     * @return returns a list of extended or implemented classes
     */
    private static Set<Class<?>> getAllExtendedOrImplementedTypes(Class<?> clazz) {
        List<Class<?>> res = new ArrayList<>();

        do {
            res.add(clazz);
            Class<?>[] interfaces = clazz.getInterfaces();
            if (interfaces.length > 0) {
                res.addAll(Arrays.asList(interfaces));
                for (Class<?> interfaze : interfaces) {
                    res.addAll(getAllExtendedOrImplementedTypes(interfaze));
                }
            }

            Class<?> superClass = clazz.getSuperclass();
            if (superClass == null) {
                break;
            }
            clazz = superClass;
        } while (!"java.lang.Object".equals(clazz.getCanonicalName()));

        return new HashSet<>(res);
    }

    /**
     * This method generates the bean class and the beanInfo class. It collects various information
     * about the bean and uses it to generate all required code. If there are any complex properties that need a default
     * value to be set, these are serialized.
     * <p>
     * Requirement: Bean must adhere to the JavaBeans Specification, any EventListener interfaces must be implemented directly
     * and declare no more than one method.
     * Possible bug: EventListener Interfaces that declare more than one method
     * Possible extension: Add PropertyVeto support
     *
     * @param targetDirectory   the target directory for the beans
     * @param propertyDirectory the target directory for any serialized properties
     * @param exportBean        the bean to be generated
     * @throws IOException               if there is an error writing
     * @throws InvocationTargetException if there is an error accessing properties
     * @throws IllegalAccessException    if there is an error accessing properties
     * @throws NoSuchMethodException     if there is an error accessing methods
     */
    private void generateBean(File targetDirectory, File propertyDirectory, ExportBean exportBean) throws IOException, InvocationTargetException, IllegalAccessException, NoSuchMethodException {
        //collect some necessary information to increase performance
        List<ExportProperty> exportProperties = exportBean.getProperties();
        List<ExportMethod> exportMethods = exportBean.getMethods();
        List<ExportEvent> exportEvents = exportBean.getEvents();
        List<Class> interfaces = new ArrayList<>();
        for (ExportMethod exportMethod : exportBean.getMethods()) {
            if (exportMethod.isImplementInterface()) {
                interfaces.add(exportMethod.getDeclaringClass());
            }
        }

        //create the files
        File bean = new File(targetDirectory.getAbsolutePath(), exportBean.getBeanName() + ".java");
        File beanInfo = new File(targetDirectory.getAbsolutePath(), exportBean.getBeanName() + "BeanInfo.java");
        if (!bean.createNewFile())
            throw new IOException("Error creating File: " + bean.getName() + ". Maybe you have conflicting resources?");
        if (!beanInfo.createNewFile()) throw new IOException("Error creating File: " + beanInfo.getName());
        //start generating: package, interfaces
        PrintWriter writer = new PrintWriter(new FileWriter(bean));
        writer.println("package " + DEFAULT_BEAN_DIRECTORY_NAME.replaceAll(Pattern.quote("/"), ".") + ";");
        writer.println();
        writer.println("import java.io.Serializable;");
        if (exportBean.isAddPropertyChangeSupport()) {
            writer.println("import java.beans.PropertyChangeSupport;");
            writer.println("import java.beans.PropertyChangeListener;");
            writer.println("import java.beans.PropertyChangeEvent;");
            interfaces.add(PropertyChangeListener.class);
        }
        writer.println();

        //generate interface implementations
        StringBuilder implementations = new StringBuilder(" implements Serializable");
        for (Class cls : interfaces) {
            implementations.append(", ").append(cls.getCanonicalName());
        }
        writer.println("public class " + exportBean.getBeanName() + implementations + " {");
        writer.println();
        //generate properties (the beans)
        for (BeanNode node : exportBean.getBeans().getAllNodes()) {
            writer.println("\tprivate " + node.getData().getClass().getCanonicalName() + " " + node.lowercaseFirst() + ";");
        }
        if (exportBean.isAddPropertyChangeSupport()) {
            writer.println();
            writer.println("\tprivate PropertyChangeSupport cs = new PropertyChangeSupport(this);");
        }
        //generate constructor
        writer.println();
        writer.println("\tpublic " + exportBean.getBeanName() + "() {");
        writer.println("\t\ttry{");
        //generate constructor: bean instatiations
        for (BeanNode node : exportBean.getBeans().getAllNodes()) {
            writer.println("\t\t\t" + node.lowercaseFirst() + " = new " + node.getData().getClass().getCanonicalName() + "();");
        }
        writer.println();
        int hookupCounter = 0;
        //generate constructor: connect beans as configured (compostions, bindings, ...)
        for (BeanNode node : exportBean.getBeans().getAllNodes()) {
            for (AdapterCompositionEdge edge : node.getAdapterCompositionEdges()) {
                writer.println("\t\t\t" + edge.getHookup().getClass().getCanonicalName() + " "
                        + "hookup" + hookupCounter + " = new " + edge.getHookup().getClass().getCanonicalName() + "();");
                writer.println("\t\t\thookup" + hookupCounter + ".setTarget(" + edge.getEnd().lowercaseFirst() + ");");
                writer.println("\t\t\t" + edge.getStart().lowercaseFirst() + "." + edge.getEventSetDescriptor().getAddListenerMethod().getName() + "(hookup" + hookupCounter + ");");
                hookupCounter++;
            }
            for (DirectCompositionEdge edge : node.getDirectCompositionEdges()) {
                writer.println("\t\t\t" + edge.getStart().lowercaseFirst() + "." + edge.getEventSetDescriptor().getAddListenerMethod().getName() + "(" + edge.getEnd().lowercaseFirst() + ");");
            }
            for (PropertyBindingEdge edge : node.getPropertyBindingEdges()) {
                String canonicalAdaperName = DEFAULT_ADAPTER_DIRECTORY_NAME.replace("/", ".") + "." + edge.getAdapterName();
                writer.println("\t\t\t" + canonicalAdaperName + " hookup" + hookupCounter + " = new " + canonicalAdaperName + "();");
                writer.println("\t\t\thookup" + hookupCounter + ".setTarget(" + edge.getEnd().lowercaseFirst() + ");");
                writer.println("\t\t\t" + edge.getStart().lowercaseFirst() + ".add" + edge.getEventSetName() + "Listener(hookup" + hookupCounter + ");");
                hookupCounter++;
            }
        }
        //generate constructor: any default values for properties
        writer.println();
        for (ExportProperty property : sortPropertiesByBinding(exportProperties.stream().filter(ExportProperty::isSetDefaultValue).collect(Collectors.toList()))) {
            Object value = property.getPropertyDescriptor().getReadMethod().invoke(property.getNode().getData());
            if (value == null || value instanceof Void || isPrimitiveOrPrimitiveWrapperOrString(value.getClass())) {
                writer.println("\t\t\t" + property.getNode().lowercaseFirst() + "." + property.getPropertyDescriptor().getWriteMethod().getName() + "(" + StringUtil.convertPrimitive(value) + ");");
            } else {
                File ser = new File(propertyDirectory.getAbsolutePath(), StringUtil.generateName(value.getClass().getSimpleName().toLowerCase() + "_", 100000000, 900000000) + ".ser");
                while (ser.exists()) {
                    ser = new File(propertyDirectory.getAbsolutePath(), StringUtil.generateName(value.getClass().getSimpleName().toLowerCase() + "_", 100000000, 900000000) + ".ser");
                }
                if (!ser.getParentFile().mkdirs()) throw new IOException("Could not create property directory.");
                try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(ser))) {
                    out.writeObject(value);
                    writer.println("\t\t\ttry (java.io.ObjectInputStream in = new java.io.ObjectInputStream(getClass().getResourceAsStream(\""
                            + StringUtil.getRelativePath(targetDirectory.getAbsolutePath(), ser, false) + "\"))){");
                    writer.println("\t\t\t\t" + property.getNode().lowercaseFirst() + "." + property.getPropertyDescriptor().getWriteMethod().getName()
                            + "((" + property.getPropertyType().getCanonicalName() + ") in.readObject());");
                    writer.println("\t\t\t}");
                } catch (IOException i) {
                    throw new IOException("Error serializing property: " + property.getNode().getName() + ":" + property.getName());
                }
            }
        }
        writer.println("\t\t} catch (Exception e) {");
        writer.println("\t\t\te.printStackTrace();");
        writer.println("\t\t}");
        writer.println("\t}");
        //generate property change support
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
        //generate getter and setters for properties
        writer.println();
        for (ExportProperty property : exportProperties) {
            Method getter = property.getPropertyDescriptor().getReadMethod();
            StringBuilder getterSignature = new StringBuilder("\tpublic " + property.getPropertyType().getCanonicalName() + " get" + property.uppercaseFirst() + "(");
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
            StringBuilder setterSignature = new StringBuilder("\tpublic void set" + property.uppercaseFirst() + "(");
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
        //generate add/remove listener methods for events
        writer.println();
        for (ExportEvent event : exportEvents) {
            writer.println("\tpublic void add" + event.uppercaseFirst() + "EventListener(" + event.getEventSetDescriptor().getListenerType().getCanonicalName() + " listener) {");
            writer.println("\t\t" + event.getBeanNode().lowercaseFirst() + "." + event.getEventSetDescriptor().getAddListenerMethod().getName() + "(listener);");
            writer.println("\t}");
            writer.println();
            if (event.getEventSetDescriptor().getGetListenerMethod() != null) {
                writer.println("\tpublic " + event.getEventSetDescriptor().getGetListenerMethod().getReturnType().getCanonicalName() + " get" + event.uppercaseFirst() + "EventListeners() {");
                writer.println("\t\treturn " + event.getBeanNode().lowercaseFirst() + "." + event.getEventSetDescriptor().getGetListenerMethod().getName() + "();");
                writer.println("\t}");
                writer.println();
            }
            writer.println("\tpublic void remove" + event.uppercaseFirst() + "EventListener(" + event.getEventSetDescriptor().getListenerType().getCanonicalName() + " listener) {");
            writer.println("\t\t" + event.getBeanNode().lowercaseFirst() + "." + event.getEventSetDescriptor().getRemoveListenerMethod().getName() + "(listener);");
            writer.println("\t}");
            writer.println();
        }
        //generate any other methods
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

        //generate BeanInfo class
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
        writer.println("\t@Override");
        writer.println("\tpublic PropertyDescriptor[] getPropertyDescriptors() {");
        if (!exportProperties.isEmpty()) {
            writer.println("\t\ttry {");
            writer.println("\t\t\tClass<?> cls = " + exportBean.getBeanName() + ".class;");
            StringBuilder propertyDescriptorArray = new StringBuilder("{");
            for (ExportProperty exportProperty : exportProperties) {
                String descriptorName = StringUtil.generateName("pd" + exportProperty.uppercaseFirst() + "_", 10000, 90000);
                writer.println("\t\t\tPropertyDescriptor " + descriptorName + " = new PropertyDescriptor(\"" + exportProperty.getName() + "\", cls);");
                writer.println("\t\t\t" + descriptorName + ".setDisplayName(\"" + exportProperty.getPropertyDescriptor().getDisplayName() + "\");");
                if (exportProperty.getPropertyDescriptor().getPropertyEditorClass() != null) {
                    writer.println("\t\t\t" + descriptorName + ".setPropertyEditorClass(" + exportProperty.getPropertyDescriptor().getPropertyEditorClass().getCanonicalName() + ".class);");
                }
                if (propertyDescriptorArray.length() > 1) {
                    propertyDescriptorArray.append(", ").append(descriptorName);
                } else {
                    propertyDescriptorArray.append(descriptorName);
                }
            }
            propertyDescriptorArray.append("}");
            writer.println("\t\t\treturn new PropertyDescriptor[]" + propertyDescriptorArray + ";");
            writer.println("\t\t} catch (IntrospectionException e) {");
            writer.println("\t\t\te.printStackTrace();");
            writer.println("\t\t}");
            writer.println("\t\t\treturn null;");
        } else {
            writer.println("\t\treturn new PropertyDescriptor[]{};");
        }
        writer.println("\t}");
        writer.println();
        writer.println("\t@Override");
        writer.println("\tpublic EventSetDescriptor[] getEventSetDescriptors() {");
        if (!exportEvents.isEmpty()) {
            writer.println("\t\ttry {");
            writer.println("\t\t\tClass<?> cls = " + exportBean.getBeanName() + ".class;");
            StringBuilder eventSetDescriptorArray = new StringBuilder("{");
            if (exportBean.isAddPropertyChangeSupport()) {
                writer.println("\t\t\tEventSetDescriptor esdPropertyChange = new EventSetDescriptor(cls, \"propertyChange\", PropertyChangeListener.class, \"propertyChange\");");
                eventSetDescriptorArray.append("esdPropertyChange");
            }
            for (ExportEvent exportEvent : exportEvents) {
                StringBuilder listenerMethodsArray = new StringBuilder("{");
                for (Method method : exportEvent.getEventSetDescriptor().getListenerMethods()) {
                    if (listenerMethodsArray.length() > 1) {
                        listenerMethodsArray.append(", ").append("\"").append(method.getName()).append("\"");
                    } else {
                        listenerMethodsArray.append("\"").append(method.getName()).append("\"");
                    }
                }
                String descriptorName = StringUtil.generateName("esd" + exportEvent.uppercaseFirst() + "_", 10000, 90000);
                writer.println("\t\t\tEventSetDescriptor " + descriptorName + " = new EventSetDescriptor(cls, \"" + exportEvent.getName() + "\", "
                        + exportEvent.getEventSetDescriptor().getListenerType().getCanonicalName() + ".class, new String[]" + listenerMethodsArray.toString() + "}, " +
                        "\"add" + exportEvent.uppercaseFirst() + "EventListener\", \"remove" + exportEvent.uppercaseFirst() + "EventListener\");");

                if (eventSetDescriptorArray.length() > 1) {
                    eventSetDescriptorArray.append(", ").append(descriptorName);
                } else {
                    eventSetDescriptorArray.append(descriptorName);
                }
            }
            eventSetDescriptorArray.append("}");
            writer.println("\t\t\treturn new EventSetDescriptor[]" + eventSetDescriptorArray + ";");
            writer.println("\t\t} catch (IntrospectionException e) {");
            writer.println("\t\t\te.printStackTrace();");
            writer.println("\t\t}");
            writer.println("\t\t\treturn null;");
        } else {
            writer.println("\t\treturn new EventSetDescriptor[]{};");
        }
        writer.println("\t}");
        writer.println();
        writer.println("\t@Override");
        writer.println("\tpublic MethodDescriptor[] getMethodDescriptors() {");
        if (!exportEvents.isEmpty()) {
            writer.println("\t\ttry {");
            writer.println("\t\t\tClass<?> cls = " + exportBean.getBeanName() + ".class;");
            StringBuilder methodDescriptorArray = new StringBuilder("{");
            for (ExportMethod exportMethod : exportMethods) {
                StringBuilder classArray = new StringBuilder();
                for (Class parameter : exportMethod.getMethodDescriptor().getMethod().getParameterTypes()) {
                    if (classArray.length() > 1) {
                        classArray.append(", ").append(parameter.getCanonicalName()).append(".class");
                    } else {
                        classArray.append(parameter.getCanonicalName()).append(".class");
                    }
                }
                String descriptorName = StringUtil.generateName("md" + exportMethod.uppercaseFirst() + "_", 10000, 90000);
                writer.println("\t\t\tMethodDescriptor " + descriptorName + " = new MethodDescriptor(cls.getMethod(\"" + exportMethod.getName()
                        + "\", new Class[]{" + classArray + "}), null);");
                if (methodDescriptorArray.length() > 1) {
                    methodDescriptorArray.append(", ").append(descriptorName);
                } else {
                    methodDescriptorArray.append(descriptorName);
                }
            }
            methodDescriptorArray.append("}");
            writer.println("\t\t\treturn new MethodDescriptor[]" + methodDescriptorArray + ";");
            writer.println("\t\t} catch (NoSuchMethodException e) {");
            writer.println("\t\t\te.printStackTrace();");
            writer.println("\t\t}");
            writer.println("\t\treturn null;");
        } else {
            writer.println("\t\treturn new MethodDescriptor[]{};");
        }
        writer.println("\t}");
        writer.println("}");
        writer.println();
        writer.close();
        if (writer.checkError()) {
            throw new IOException("Error writing BeanInfo File: " + exportBean.getBeanName());
        }
    }

    /**
     * This method generates a MANIFEST.MF file according to the current configuration.
     *
     * @param manifestDirectory the directory to save the file
     * @throws IOException if there is an error writing the file
     */
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

    /**
     * This method is currently only a placeholder for a future improvement. Here we would sort the order in which property
     * values are set according to a strategy. Currently the order in which properties are set in the constructor is kind of
     * random or at least the user can not influence it.
     * <p>
     * Consider the following scenario: You have a property binding of Bean A to Bean B. You configure a value of a property
     * of Bean B. Now Bean A and Bean B have different values. You want Bean B to have the configured value up until the point
     * where you configure a value on Bean A. As soon as this happens you want both to have the same value.
     *
     * @param properties a list of properties to be set
     * @return returns a sorted list
     */
    private List<ExportProperty> sortPropertiesByBinding(List<ExportProperty> properties) {
        return properties;
    }

    /**
     * Checks if a class is any of the primitive types, primitive wrappers or a string.
     *
     * @param type the type to check
     * @return returns if a class is any of the primitive types, primitive wrappers or a string
     */
    private static boolean isPrimitiveOrPrimitiveWrapperOrString(Class<?> type) {
        return (type.isPrimitive() && type != void.class) ||
                type == Double.class || type == Float.class || type == Long.class ||
                type == Integer.class || type == Short.class || type == Character.class ||
                type == Byte.class || type == Boolean.class || type == String.class;
    }

    /**
     * Validates if the current configuration is exportable.
     * <p>
     * //TODO: Validate interfacing and maybe ask user
     * //TODO: Validate Unique property naming within ExportBean
     * //TODO: Validate Unique Export bean naming, may not have the same name as another Bean in the generated JAR
     * //TODO: Validate resource conflicts
     * //TODO: Validate unique naming for all methods (input output interface)
     * //TODO: Validate input output interface
     *
     * @return returns a list of constraint violations
     */
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

        return null;
    }
}

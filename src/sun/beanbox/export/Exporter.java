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

import static java.nio.file.FileVisitResult.CONTINUE;
import static java.nio.file.FileVisitResult.TERMINATE;

/**
 * Created by Andreas Ertlschweiger on 06.05.2017.
 * <p>
 * This is the main component responsible for the export process. It first converts all selected Wrapper objects into
 * a better suited data structure, a directed graph, that contains all relevant information. Any changes made to the
 * configuration will affect how the bean will be generated.
 */
public class Exporter {

    //Map between Bean and Wrapper. This is for easier and faster access and comparison
    private HashMap<Object, Wrapper> wrapperBeanMap = new HashMap<>();
    private List<ExportBean> exportBeans = new LinkedList<>();
    private List<File> resources = new ArrayList<>();
    //Set of all resource file names. This is for detecting conflicts during configuration
    private Set<String> resourceNames = new HashSet<>();
    private boolean keepSources = false;

    private static final String MANIFEST_DIRECTORY_NAME = "META-INF";
    private static final String BEAN_DIRECTORY_NAME = "beanBox/generated/beans";
    private static final String SERIALIZED_PROPERTIES_DIRECTORY_NAME = "beanBox/generated/beans/properties";
    private static final String ADAPTER_DIRECTORY_NAME = "beanBox/generated/beans/adapters";

    private static final String DEFAULT_BEAN_NAME = "ExportBean";

    private static final Set<String> RESERVED_METHOD_NAME_POOL = new HashSet<>(Arrays.asList(
            "getclass", "getpeer", "notify", "wait", "propertychange", "notifyall", "addpropertychangelistener",
            "removepropertychangelistener", "getpropertychangelisteners"));
    private static final Set<String> RESERVED_PROPERTY_NAME_POOL = new HashSet<>(Arrays.asList(
            "propertychangesupport", "serialversionuid"));
    private static final Set<String> RESERVED_EVENT_NAME_POOL = new HashSet<>(Arrays.asList(
            "propertychange"));
    private static final Set<String> RESERVED_CLASS_NAME_POOL = new HashSet<>(Arrays.asList(
            "PropertyChangeListener", "PropertyChangeSupport", "Object", "Class", "Serializable", "PropertyChangeEvent"));
    private static final int MAX_IDENTIFIER_LENGTH = 64;

    /**
     * Upon instantiation of the Exporter the selected Wrappers are grouped, processed and converted into a more suitable
     * data structure. Afterwards the resource dictionary is built by reading information about all required sources.
     *
     * @param beans the beans that were selected for export
     * @throws IntrospectionException   if there is an error reading bean information
     * @throws IllegalArgumentException if there is an error accessing bean properties
     */
    public Exporter(List<Wrapper> beans) throws IOException, IntrospectionException, IllegalArgumentException {
        for (List<Wrapper> group : groupWrappers(beans)) {
            exportBeans.add(assembleExportBean(group, DEFAULT_BEAN_NAME + exportBeans.size()));
        }
        resources = collectResources();
        for (File file : resources) {
            resourceNames.addAll(getAllRelativeFileNames(file));
        }
    }

    /**
     * This method analyzes all ExportBeans and collects any necessary dependencies. These are the JAR files of the BeanNodes
     * and any adapters if there are adapter compositions.
     *
     * @return returns a List of all necessary resources as files
     * @throws IOException if a file can not be found or there is an error reading the files
     */
    private ArrayList<File> collectResources() throws IOException {
        ArrayList<File> res = new ArrayList<>();
        for (ExportBean exportBean : exportBeans) {
            for (BeanNode node : exportBean.getBeans()) {
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
     * This method analyzes a resource (that is a JAR file or an adapter) and returns a relative path for every file.
     * For JAR files this means analyzing all entries and returning the package name. This method is used to generate
     * a dictionary of resource names. We use this to detect conflicting resources during initialization and configuration
     * in order to prevent the user from wasting time if the export is not possible anyways.
     *
     * @param file the file to get relative paths from
     * @return returns one or more relative paths of the input files
     * @throws IOException if there is an error reading the files
     */
    private List<String> getAllRelativeFileNames(File file) throws IOException {
        List<String> packageNames = new ArrayList<>();
        if (file.getName().toLowerCase().endsWith(".jar")) {
            JarFile jarfile = new JarFile(file);
            Enumeration<JarEntry> enu = jarfile.entries();
            while (enu.hasMoreElements()) {
                JarEntry je = enu.nextElement();
                if (!je.isDirectory()) {
                    if (packageNames.contains(je.getName())) {
                        throw new IOException("Detected conflicting resource: " + je.getName());
                    }
                    packageNames.add(je.getName());
                }
            }
        } else {
            String relativePath = HookupManager.getTmpDir() + File.separator + file.getName();
            if (packageNames.contains(relativePath)) {
                throw new IOException("Detected conflicting resource: " + relativePath);
            }
            packageNames.add(relativePath);
        }
        return packageNames;
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
                    //check if we have conflicting groups to handle special configurations. We need to check this
                    //explicitly AFTER putting the wrappers into the wrong group because each Wrapper only knows its
                    // direct successors and can't efficiently traverse the graph
                    if(groupMap.get(beanWrapper) != null) {
                        Integer conflictingGroup = groupMap.get(beanWrapper);
                        for (Map.Entry<Wrapper, Integer> entry : groupMap.entrySet()) {
                            if (entry.getValue().equals(curGroup)) {
                                groupMap.replace(entry.getKey(), conflictingGroup);
                            }
                        }
                    } else {
                        groupMap.replace(beanWrapper, curGroup);
                    }
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
    private ExportBean assembleExportBean(List<Wrapper> wrappers, String name) throws IntrospectionException, IllegalArgumentException {
        HashMap<Wrapper, BeanNode> createdNodes = new HashMap<>();
        for (Wrapper wrapper : wrappers) {
            createBeanNode(wrapper, createdNodes);
        }
        List<BeanNode> inputBeans = inferInputBeans(createdNodes);
        List<BeanNode> outputBeans = inferOutputBeans(createdNodes);
        return new ExportBean(inputBeans, outputBeans, createdNodes.values(), name);
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
    private BeanNode createBeanNode(Wrapper wrapper, HashMap<Wrapper, BeanNode> createdNodes) throws IntrospectionException {
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
            if (!methodDescriptor.isExpert() && !methodDescriptor.isHidden() && methodDescriptor.getMethod().getReturnType().equals(Void.TYPE)
                    && !RESERVED_METHOD_NAME_POOL.contains(methodDescriptor.getName().toLowerCase())) {
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
        beanNode.sortData();
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
        return new ArrayList<>(availableNodes);
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
    public List<ExportConstraintViolation> export(String directory, String filename) throws Exception {
        if (!filename.endsWith(".jar")) filename += ".jar";
        File target = new File(directory, filename);
        int counter = 0;
        String currentName = directory + "/" + filename.substring(0, filename.length() - 4) + "_tmp";
        while (new File(currentName).exists()) {
            currentName = directory + "/" + filename.substring(0, filename.length() - 4) + "_tmp" + counter;
            counter++;
        }
        List<ExportConstraintViolation> violations = validateConfiguration();
        if (violations == null) {
            File tmpDirectory = new File(currentName);
            File tmpBeanDirectory = new File(tmpDirectory.getAbsolutePath() + File.separator + BEAN_DIRECTORY_NAME);
            File tmpPropertiesDirectory = new File(tmpDirectory.getAbsolutePath() + File.separator + SERIALIZED_PROPERTIES_DIRECTORY_NAME);
            File tmpManifestDirectory = new File(tmpDirectory.getAbsolutePath() + File.separator + MANIFEST_DIRECTORY_NAME);
            File tmpAdapterDirectory = new File(tmpDirectory.getAbsolutePath() + File.separator + ADAPTER_DIRECTORY_NAME);

            if (tmpBeanDirectory.mkdirs() && tmpPropertiesDirectory.mkdirs() && tmpManifestDirectory.mkdirs() && tmpAdapterDirectory.mkdirs()) {
                copyAndExtractResources(tmpDirectory, resources);
                resources.addAll(generatePropertyAdapters(tmpDirectory));
                for (ExportBean exportBean : exportBeans) {
                    generateBean(tmpBeanDirectory, tmpPropertiesDirectory, exportBean, tmpDirectory);
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
        } else {
            return violations;
        }
        return null;
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
     * <p>
     * Depending on the size and number of source files this almost always takes the most time in the whole process.
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
            for (BeanNode node : exportBean.getBeans()) {
                for (PropertyBindingEdge edge : node.getPropertyBindingEdges()) {
                    adapters.add(generatePropertyAdapter(targetDirectory, edge));
                }
            }
        }
        return adapters;
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
    private File generatePropertyAdapter(File targetDirectory, PropertyBindingEdge propertyBindingEdge) throws IOException {
        File adapter = new File(targetDirectory.getAbsolutePath() + File.separator + ADAPTER_DIRECTORY_NAME,
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

        JavaFile javaFile = JavaFile.builder(ADAPTER_DIRECTORY_NAME.replaceAll(Pattern.quote("/"), "."), helloWorld)
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
     */
    private void generateBean(File targetDirectory, File propertyDirectory, ExportBean exportBean, File tmpDirectory) throws IOException, InvocationTargetException, IllegalAccessException {
        if (new File(targetDirectory.getAbsolutePath(), exportBean.getName() + ".java").exists()
                || new File(targetDirectory.getAbsolutePath(), exportBean.getName() + "BeanInfo.java").exists()
                || new File(targetDirectory.getAbsolutePath(), exportBean.getName() + ".class").exists()
                || new File(targetDirectory.getAbsolutePath(), exportBean.getName() + "BeanInfo.class").exists()) {
            throw new IOException("Error creating Files for: " + exportBean + ". Maybe you have conflicting resources?");
        }
        //collect some necessary information beforehand to increase performance
        List<ExportProperty> exportProperties = exportBean.getProperties();
        List<ExportMethod> exportMethods = exportBean.getMethods();
        List<ExportEvent> exportEvents = exportBean.getEvents();
        List<BeanNode> beanNodes = exportBean.getBeans();
        List<FieldSpec> fields = new ArrayList<>();
        List<MethodSpec> methods = new ArrayList<>();
        List<Class<?>> interfaces = new ArrayList<>();
        int hookupCounter = 0;

        for (ExportMethod exportMethod : exportMethods) {
            if (exportMethod.getDeclaringClass() != null) {
                interfaces.add(exportMethod.getDeclaringClass()); //TODO: possible error source
            }
        }

        //start building the constructor
        MethodSpec.Builder constructor = MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PUBLIC)
                .addCode("try {\n");

        //add a serialVersionUID
        fields.add(FieldSpec.builder(long.class, "serialVersionUID")
                .addModifiers(Modifier.FINAL, Modifier.PRIVATE, Modifier.STATIC)
                .initializer("1L")
                .build());

        //add fields for all beans and add their instantiation to the constructor
        for (BeanNode node : beanNodes) {
            fields.add(FieldSpec.builder(node.getData().getClass(), StringUtil.lowercaseFirst(node.getName()))
                    .addModifiers(Modifier.PRIVATE)
                    .build());
            constructor.addCode("\t" + StringUtil.lowercaseFirst(node.getName()) + " = new " + node.getData().getClass().getCanonicalName() + "();\n");
        }
        //hook the beans up with each other. We need to do this explicitly after instantiation of all beans hence the
        //redundant loop
        for (BeanNode node : beanNodes) {
            for (AdapterCompositionEdge edge : node.getAdapterCompositionEdges()) {
                constructor.addCode("\t" + edge.getHookup().getClass().getCanonicalName() + " " +
                        "hookup" + hookupCounter + " = new " + edge.getHookup().getClass().getCanonicalName() + "();\n" +
                        "\thookup" + hookupCounter + ".setTarget(" + StringUtil.lowercaseFirst(edge.getEnd().getName()) + ");\n" +
                        "\t" + StringUtil.lowercaseFirst(edge.getStart().getName()) + "." + edge.getEventSetDescriptor().getAddListenerMethod().getName() +
                        "(hookup" + hookupCounter + ");\n");
                hookupCounter++;
            }
            for (DirectCompositionEdge edge : node.getDirectCompositionEdges()) {
                constructor.addCode("\t" + StringUtil.lowercaseFirst(edge.getStart().getName()) + "." + edge.getEventSetDescriptor().getAddListenerMethod().getName() + "(" + StringUtil.lowercaseFirst(edge.getEnd().getName()) + ");\n");
            }
            for (PropertyBindingEdge edge : node.getPropertyBindingEdges()) {
                String canonicalAdaperName = ADAPTER_DIRECTORY_NAME.replace("/", ".") + "." + edge.getAdapterName();
                constructor.addCode("\t" + canonicalAdaperName + " hookup" + hookupCounter + " = new " + canonicalAdaperName + "();\n" +
                        "\thookup" + hookupCounter + ".setTarget(" + StringUtil.lowercaseFirst(edge.getEnd().getName()) + ");\n" +
                        "\t" + StringUtil.lowercaseFirst(edge.getStart().getName()) + ".add" + edge.getEventSetName() + "Listener(hookup" + hookupCounter + ");\n");
                hookupCounter++;
            }
        }
        //set all default values for properties, add getter and setter methods
        for (ExportProperty property : exportProperties) {
            Method getter = property.getPropertyDescriptor().getReadMethod();
            Method setter = property.getPropertyDescriptor().getWriteMethod();

            if (property.isSetDefaultValue()) {
                Object value = getter.invoke(property.getNode().getData());
                if (value == null || value instanceof Void || isPrimitiveOrPrimitiveWrapperOrString(value.getClass())) {
                    constructor.addCode("\t" + StringUtil.lowercaseFirst(property.getNode().getName()) + "." + setter.getName() + "(" + StringUtil.convertPrimitive(value) + ");\n");
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
                                "\t\t" + StringUtil.lowercaseFirst(property.getNode().getName()) + "." + setter.getName()
                                + "((" + property.getPropertyType().getCanonicalName() + ") in.readObject());\n" +
                                "\t}\n");
                    } catch (IOException i) {
                        throw new IOException("Error serializing property: " + property.getNode().getName() + ":" + property.getName());
                    }
                }
            }

            String prefix = property.getPropertyType() == boolean.class || property.getPropertyType() == Boolean.class ?
                    "is" : "get";

            MethodSpec.Builder getBuilder = MethodSpec.methodBuilder(prefix + StringUtil.uppercaseFirst(property.getName()))
                    .addModifiers(Modifier.PUBLIC)
                    .returns(property.getPropertyType());

            for (int i = 0; i < getter.getParameterTypes().length; i++) {
                getBuilder.addParameter(getter.getParameterTypes()[i], "arg" + i);
            }
            for (int i = 0; i < getter.getExceptionTypes().length; i++) {
                getBuilder.addException(getter.getExceptionTypes()[i]);
            }
            StringBuilder getterCall = new StringBuilder("return ").append(StringUtil.lowercaseFirst(property.getNode().getName())).append(".").append(getter.getName()).append("(");
            for (int i = 0; i < getter.getParameterTypes().length; i++) {
                if (i == 0) {
                    getterCall.append("arg").append(i);
                } else {
                    getterCall.append(", arg").append(i);
                }
            }
            getterCall.append(");\n");
            methods.add(getBuilder.addCode(getterCall.toString()).build());

            MethodSpec.Builder setBuilder = MethodSpec.methodBuilder("set" + StringUtil.uppercaseFirst(property.getName()))
                    .addModifiers(Modifier.PUBLIC)
                    .returns(void.class);

            for (int i = 0; i < setter.getParameterTypes().length; i++) {
                setBuilder.addParameter(setter.getParameterTypes()[i], "arg" + i);
            }
            for (int i = 0; i < setter.getExceptionTypes().length; i++) {
                setBuilder.addException(setter.getExceptionTypes()[i]);
            }
            if (exportBean.isAddPropertyChangeSupport()) {
                setBuilder.addCode("propertyChangeSupport.firePropertyChange(\"" + property.getName() + "\", " + StringUtil.lowercaseFirst(property.getNode().getName()) + "." + getter.getName() + "(), arg0);\n");
            }
            /*
             * if you wanted to implement the "transparent property binding" that just calls two setters
             * to change properties of different beans you would need to implement it here and think of a
             * good way to display this on the UI
             */
            StringBuilder setterCall = new StringBuilder("").append(StringUtil.lowercaseFirst(property.getNode().getName())).append(".").append(setter.getName()).append("(");
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

        //finalize constructor
        constructor.addCode("} catch (Exception e) {\n" +
                "\te.printStackTrace();\n" +
                "}\n");

        //create listener methods for events
        for (ExportEvent event : exportEvents) {
            EventSetDescriptor esd = event.getEventSetDescriptor();
            methods.add(MethodSpec.methodBuilder("add" + StringUtil.uppercaseFirst(event.getName()) + "EventListener")
                    .addModifiers(Modifier.PUBLIC)
                    .returns(void.class)
                    .addParameter(esd.getListenerType(), "listener")
                    .addCode("" + StringUtil.lowercaseFirst(event.getNode().getName()) + "." + esd.getAddListenerMethod().getName() + "(listener);\n")
                    .build());
            methods.add(MethodSpec.methodBuilder("remove" + StringUtil.uppercaseFirst(event.getName()) + "EventListener")
                    .addModifiers(Modifier.PUBLIC)
                    .returns(void.class)
                    .addParameter(esd.getListenerType(), "listener")
                    .addCode("" + StringUtil.lowercaseFirst(event.getNode().getName()) + "." + esd.getRemoveListenerMethod().getName() + "(listener);\n")
                    .build());

            if (event.getEventSetDescriptor().getGetListenerMethod() != null) {
                methods.add(MethodSpec.methodBuilder("get" + StringUtil.uppercaseFirst(event.getName()) + "EventListeners")
                        .addModifiers(Modifier.PUBLIC)
                        .returns(esd.getGetListenerMethod().getReturnType())
                        .addCode("return " + StringUtil.lowercaseFirst(event.getNode().getName()) + "." + esd.getGetListenerMethod().getName() + "();\n")
                        .build());
            }
        }

        //create methods for all methods
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
            StringBuilder methodCall = new StringBuilder("").append(StringUtil.lowercaseFirst(exportMethod.getNode().getName())).append(".").append(method.getName()).append("(");
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

        //add propertyChangeSupport
        if (exportBean.isAddPropertyChangeSupport()) {
            interfaces.add(PropertyChangeListener.class);
            fields.add(FieldSpec.builder(PropertyChangeSupport.class, "propertyChangeSupport")
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

        //build the class
        TypeSpec.Builder bean = TypeSpec.classBuilder(exportBean.getName())
                .addModifiers(Modifier.PUBLIC)
                .addMethod(constructor.build())
                .addSuperinterface(Serializable.class);

        for (Class clz : interfaces) {
            bean.addSuperinterface(clz);
        }
        bean.addMethods(methods);
        bean.addFields(fields);

        //build the file
        JavaFile.builder(BEAN_DIRECTORY_NAME.replaceAll(Pattern.quote("/"), "."), bean.build())
                .build().writeTo(tmpDirectory);

        //generate BeanInfo class. We could expand it with a BeanDescriptor but it is not neccessary for the beanbox
        MethodSpec.Builder propertyDescriptor = MethodSpec.methodBuilder("getPropertyDescriptors")
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Override.class)
                .returns(PropertyDescriptor[].class);

        if (exportProperties.isEmpty()) {
            propertyDescriptor.addCode("return new PropertyDescriptor[]{};");
        } else {
            propertyDescriptor.addCode("try {\n");
            propertyDescriptor.addCode("\tClass<?> cls = " + exportBean.getName() + ".class;\n");
            StringBuilder propertyDescriptorArray = new StringBuilder("{");
            for (ExportProperty exportProperty : exportProperties) {
                String descriptorName = StringUtil.generateName("pd" + StringUtil.uppercaseFirst(exportProperty.getName()) + "_", 10000, 90000);
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
            eventSetDescriptor.addCode("\tClass<?> cls = " + exportBean.getName() + ".class;\n");
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
                String descriptorName = StringUtil.generateName("esd" + StringUtil.uppercaseFirst(exportEvent.getName()) + "_", 10000, 90000);
                eventSetDescriptor.addCode("\tEventSetDescriptor " + descriptorName + " = new EventSetDescriptor(cls, \"" + exportEvent.getName() + "\", "
                        + exportEvent.getEventSetDescriptor().getListenerType().getCanonicalName() + ".class, new String[]" + listenerMethodsArray.toString() + "}, " +
                        "\"add" + StringUtil.uppercaseFirst(exportEvent.getName()) + "EventListener\", \"remove" + StringUtil.uppercaseFirst(exportEvent.getName()) + "EventListener\");\n");

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
            eventSetDescriptor.addCode("return new EventSetDescriptor[]{};\n");
        }

        MethodSpec.Builder methodDescriptor = MethodSpec.methodBuilder("getMethodDescriptors")
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Override.class)
                .returns(MethodDescriptor[].class);

        if (!exportMethods.isEmpty()) {
            methodDescriptor.addCode("try {\n");
            methodDescriptor.addCode("\tClass<?> cls = " + exportBean.getName() + ".class;\n");
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
                String descriptorName = StringUtil.generateName("md" + StringUtil.uppercaseFirst(exportMethod.getName()) + "_", 10000, 90000);
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

        TypeSpec.Builder beanInfo = TypeSpec.classBuilder(exportBean.getName() + "BeanInfo")
                .superclass(SimpleBeanInfo.class)
                .addModifiers(Modifier.PUBLIC)
                .addMethod(propertyDescriptor.build())
                .addMethod(eventSetDescriptor.build())
                .addMethod(methodDescriptor.build())
                .addField(suid)
                .addSuperinterface(Serializable.class);

        JavaFile.builder(BEAN_DIRECTORY_NAME.replaceAll(Pattern.quote("/"), "."), beanInfo.build())
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
            writer.println("Name: " + BEAN_DIRECTORY_NAME + "/" + exportBean.getName() + ".class");
            writer.println("Java-Bean: True");
            writer.println();
            for (BeanNode beanNode : exportBean.getBeans()) {
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
     * Checks if a String is a valid name for an ExportBean. It may not be empty, must not exceed 64 characters, be a valid Java identifier,
     * must not be a Java keyword and it must be unique among all ExportBeans in a single export. Additionally the String may not conflict
     * with any resources required to build the JAR file.
     *
     * @param text the text to be checked
     * @return returns if the name is valid
     */
    public List<ExportConstraintViolation> checkIfValidClassName(ExportBean exportBean, String text) {
        List<ExportConstraintViolation> violations = new ArrayList<>();
        if (text == null || text.isEmpty()) {
            violations.add(new ExportConstraintViolation("ExportBean has no name."));
            return violations;
        }
        if (text.length() > MAX_IDENTIFIER_LENGTH) {
            violations.add(new ExportConstraintViolation("ExportBean " + text + ": name exceeds maximum length of " + MAX_IDENTIFIER_LENGTH + "."));
        }
        if (!SourceVersion.isIdentifier(text) || SourceVersion.isKeyword(text)) {
            violations.add(new ExportConstraintViolation("ExportBean " + text + ": name not a valid Java identifier."));
        }
        if (RESERVED_CLASS_NAME_POOL.contains(text)) {
            violations.add(new ExportConstraintViolation("ExportBean " + text + ": name conflicts with reserved class name."));
        }
        if (resourceNames.contains(BEAN_DIRECTORY_NAME + "/" + text + ".java")
                || resourceNames.contains(BEAN_DIRECTORY_NAME + "/" + text + ".class")
                || resourceNames.contains(BEAN_DIRECTORY_NAME + "/" + text + "BeanInfo.java")
                || resourceNames.contains(BEAN_DIRECTORY_NAME + "/" + text + "BeanInfo.class")) {
            violations.add(new ExportConstraintViolation("ExportBean " + text + ": name conflicts with resource."));
        }
        for (ExportBean bean : exportBeans) {
            if (bean.getName().equals(text) && bean != exportBean) {
                violations.add(new ExportConstraintViolation("ExportBean " + text + ": name conflicts with ExportBean " + bean.getName() + "."));
            }
        }
        for (BeanNode node : exportBean.getBeans()) {
            if (node.getName().equals(text)) {
                violations.add(new ExportConstraintViolation("ExportBean " + text + ": name conflicts with BeanNode " + node.getName() + "."));
            }
        }
        return violations.isEmpty() ? null : violations;
    }

    /**
     * Checks if a String is a valid name for a BeanNode. It may not be empty, must not exceed 64 characters, be a valid Java identifier,
     * must not be a Java keyword and it must be unique among all BeanNodes and its ExportBean in a single export. This String is only used
     * internally and has no effect to the user. It is an optional configuration to help the user identify beans in the source code
     * after export.
     *
     * @param text the text to be checked
     * @return returns if the name is valid
     */
    public List<ExportConstraintViolation> checkIfValidNodeName(ExportBean exportBean, BeanNode beanNode, String text) {
        List<ExportConstraintViolation> violations = new ArrayList<>();
        if (text == null || text.isEmpty()) {
            violations.add(new ExportConstraintViolation("BeanNode has no name."));
            return violations;
        }
        if (text.length() > MAX_IDENTIFIER_LENGTH) {
            violations.add(new ExportConstraintViolation("BeanNode " + text + ": name exceeds maximum length of " + MAX_IDENTIFIER_LENGTH + "."));
        }
        if (!SourceVersion.isIdentifier(text) || SourceVersion.isKeyword(text)) {
            violations.add(new ExportConstraintViolation("BeanNode " + text + ": name not a valid Java identifier."));
        }
        if (RESERVED_PROPERTY_NAME_POOL.contains(text)) {
            violations.add(new ExportConstraintViolation("BeanNode " + text + ": name conflicts with reserved property name."));
        }
        if (exportBean.getName().toLowerCase().equals(text.toLowerCase())) {
            violations.add(new ExportConstraintViolation("BeanNode " + text + ": name conflicts with ExportBean " + exportBean.getName() + "."));
        }
        for (BeanNode node : exportBean.getBeans()) {
            if (node.getName().equals(text) && node != beanNode) {
                violations.add(new ExportConstraintViolation("BeanNode " + text + ": name conflicts with BeanNode " + node.getName() + "."));
            }
        }
        return violations.isEmpty() ? null : violations;
    }

    /**
     * Checks if a String is a valid name for an ExportProperty. It may not be empty, must not exceed 64 characters, be a valid Java identifier,
     * must not be a Java keyword and it must be unique among all ExportProperties in a single ExportBean. Additionally the String may not conflict
     * with any generated method or event names.
     *
     * @param text       the text to be checked
     * @param exportBean the exportBean to which the property belongs
     * @return returns if the name is valid
     */
    public List<ExportConstraintViolation> checkIfValidPropertyName(ExportBean exportBean, ExportProperty exportProperty, String text) {
        List<ExportConstraintViolation> violations = new ArrayList<>();
        if (text == null || text.isEmpty()) {
            violations.add(new ExportConstraintViolation("Property of Bean " +
                    exportProperty.getNode().getName() + "has no name."));
            return violations;
        }
        if (text.length() > MAX_IDENTIFIER_LENGTH) {
            violations.add(new ExportConstraintViolation("Property " + text + " of Bean " +
                    exportProperty.getNode().getName() + ": name exceeds maximum length of " + MAX_IDENTIFIER_LENGTH + "."));
        }
        if (!SourceVersion.isIdentifier(text) || SourceVersion.isKeyword(text)) {
            violations.add(new ExportConstraintViolation("Property " + text + " of Bean " +
                    exportProperty.getNode().getName() + ": name not a valid Java identifier."));
        }
        if (RESERVED_METHOD_NAME_POOL.contains(text.toLowerCase())
                || RESERVED_METHOD_NAME_POOL.contains("get" + text.toLowerCase())
                || RESERVED_METHOD_NAME_POOL.contains("set" + text.toLowerCase())) {
            violations.add(new ExportConstraintViolation("Property " + text + " of Bean " +
                    exportProperty.getNode().getName() + ": name conflicts with reserved property name."));
        }
        for (ExportProperty property : exportBean.getProperties()) {
            if (property.getName().equals(text) && property != exportProperty) {
                violations.add(new ExportConstraintViolation("Property " + text + " of Bean " +
                        exportProperty.getNode().getName() + ": name conflicts with property " + property.getName() +
                        " of Bean " + property.getNode().getName() + "."));
            }
        }
        for (ExportMethod method : exportBean.getMethods()) {
            if (method.getName().equals("get" + StringUtil.uppercaseFirst(text))
                    || method.getName().equals("set" + StringUtil.uppercaseFirst(text))) {
                violations.add(new ExportConstraintViolation("Property " + text + " of Bean " +
                        exportProperty.getNode().getName() + ": name conflicts with method " + method.getName() +
                        " of Bean " + method.getNode().getName() + "."));
            }
        }
        for (ExportEvent event : exportBean.getEvents()) {
            if (StringUtil.uppercaseFirst(text).equals(StringUtil.uppercaseFirst(event.getName()) + "EventListeners")) {
                violations.add(new ExportConstraintViolation("Property " + text + " of Bean " +
                        exportProperty.getNode().getName() + ": name conflicts with generated method get" +
                        StringUtil.uppercaseFirst(event.getName()) + "EventListeners of Bean " + event.getNode().getName() + "."));
            }
        }
        return violations.isEmpty() ? null : violations;
    }

    /**
     * Checks if a String is a valid name for an ExportEvent. It may not be empty, must not exceed 64 characters, be a valid Java identifier,
     * must not be a Java keyword and it must be unique among all ExportEvents in a single ExportBean. Additionally the String may not conflict
     * with any generated method or event names.
     *
     * @param text       the text to be checked
     * @param exportBean the exportBean to which the event belongs
     * @return returns if the name is valid
     */
    public List<ExportConstraintViolation> checkIfValidEventName(ExportBean exportBean, ExportEvent exportEvent, String text) {
        List<ExportConstraintViolation> violations = new ArrayList<>();
        if (text == null || text.isEmpty()) {
            violations.add(new ExportConstraintViolation("Event of Bean " +
                    exportEvent.getNode().getName() + " has no name."));
            return violations;
        }
        if (text.length() > MAX_IDENTIFIER_LENGTH) {
            violations.add(new ExportConstraintViolation("Event " + text + " of Bean " +
                    exportEvent.getNode().getName() + ": name exceeds maximum length of " + MAX_IDENTIFIER_LENGTH + "."));
        }
        if (!SourceVersion.isIdentifier(text) || SourceVersion.isKeyword(text)) {
            violations.add(new ExportConstraintViolation("Event " + text + " of Bean " +
                    exportEvent.getNode().getName() + ": name not a valid Java identifier."));
        }
        if (RESERVED_EVENT_NAME_POOL.contains(text.toLowerCase())) {
            violations.add(new ExportConstraintViolation("Event " + text + " of Bean " +
                    exportEvent.getNode().getName() + ": name conflicts with reserved event name."));
        }
        for (ExportProperty property : exportBean.getProperties()) {
            if ((StringUtil.uppercaseFirst(text) + "EventListeners").equals(StringUtil.uppercaseFirst(property.getName()))) {
                violations.add(new ExportConstraintViolation("Event " + text + " of Bean " +
                        exportEvent.getNode().getName() + ": generated method name conflicts with property " + property.getName() +
                        " of Bean " + property.getNode().getName() + "."));
            }
        }
        for (ExportMethod method : exportBean.getMethods()) {
            if (method.getName().equals("add" + StringUtil.uppercaseFirst(text) + "EventListener")
                    || method.getName().equals("remove" + StringUtil.uppercaseFirst(text) + "EventListener")
                    || method.getName().equals("get" + StringUtil.uppercaseFirst(text) + "EventListeners")) {
                violations.add(new ExportConstraintViolation("Event " + text + " of Bean " +
                        exportEvent.getNode().getName() + ": generated method name conflicts with method " + method.getName() +
                        " of Bean " + method.getNode().getName() + "."));
            }
        }
        for (ExportEvent event : exportBean.getEvents()) {
            if (event.getName().equals(text) && event != exportEvent) {
                violations.add(new ExportConstraintViolation("Event " + text + " of Bean " +
                        exportEvent.getNode().getName() + ": name conflicts with event " + event.getName() +
                        " of Bean " + event.getNode().getName() + "."));
            }
        }
        return violations.isEmpty() ? null : violations;
    }

    /**
     * Checks if a String is a valid name for an ExportMethod. It may not be empty, must not exceed 64 characters, be a valid Java identifier,
     * must not be a Java keyword and it must be unique among all ExportMethod in a single ExportBean. Additionally the String may not conflict
     * with any generated method or event names.
     *
     * @param text       the text to be checked
     * @param exportBean the exportBean to which the event belongs
     * @return returns if the name is valid
     */
    public List<ExportConstraintViolation> checkIfValidMethodName(ExportBean exportBean, ExportMethod exportMethod, String text) {
        List<ExportConstraintViolation> violations = new ArrayList<>();
        if (text == null || text.isEmpty()) {
            violations.add(new ExportConstraintViolation("Method of Bean " + exportMethod.getNode().getName() + " has no name."));
            return violations;
        }
        if (text.length() > MAX_IDENTIFIER_LENGTH) {
            violations.add(new ExportConstraintViolation("Method " + text + " of Bean " +
                    exportMethod.getNode().getName() + ": name exceeds maximum length of " + MAX_IDENTIFIER_LENGTH + "."));
        }
        if (!SourceVersion.isIdentifier(text) || SourceVersion.isKeyword(text)) {
            violations.add(new ExportConstraintViolation("Method " + text + " of Bean " +
                    exportMethod.getNode().getName() + ": name not a valid Java identifier."));
        }
        if (RESERVED_METHOD_NAME_POOL.contains(text.toLowerCase())) {
            violations.add(new ExportConstraintViolation("Method " + text + " of Bean " +
                    exportMethod.getNode().getName() + ": name conflicts with reserved method name."));
        }
        for (ExportProperty property : exportBean.getProperties()) {
            if (text.equals("get" + StringUtil.uppercaseFirst(property.getName()))
                    || text.equals("set" + StringUtil.uppercaseFirst(property.getName()))) {
                violations.add(new ExportConstraintViolation("Method " + text + " of Bean " +
                        exportMethod.getNode().getName() + ": name conflicts with generated property method name " + property.getName() + " of Bean " +
                        property.getNode().getName() + "."));
            }
        }
        for (ExportMethod method : exportBean.getMethods()) {
            if (method.getName().equals(text) && method != exportMethod
                    && Arrays.equals(method.getMethodDescriptor().getMethod().getParameterTypes(), exportMethod.getMethodDescriptor().getMethod().getParameterTypes())) {
                violations.add(new ExportConstraintViolation("Method " + text + " of Bean " +
                        exportMethod.getNode().getName() + ": name conflicts with method " + method.getName() + " of Bean " +
                        method.getNode().getName() + "."));
            }
        }
        for (ExportEvent event : exportBean.getEvents()) {
            if (text.equals("add" + StringUtil.uppercaseFirst(event.getName()) + "EventListener")
                    || text.equals("remove" + StringUtil.uppercaseFirst(event.getName()) + "EventListener")
                    || text.equals("get" + StringUtil.uppercaseFirst(event.getName()) + "EventListeners")) {
                violations.add(new ExportConstraintViolation("Method " + text + " of Bean " +
                        exportMethod.getNode().getName() + ": name conflicts with generated event method " + event.getName() + " of Bean " +
                        event.getNode().getName() + "."));
            }
        }
        return violations.isEmpty() ? null : violations;
    }

    /**
     * Validates if the current configuration is exportable.
     * <p>
     * //TODO: Validate interfacing to avoid partial interface implementation which leads to compiler errors
     *
     * @return returns a list of constraint violations or null if there are none
     */
    private List<ExportConstraintViolation> validateConfiguration() {
        List<ExportConstraintViolation> violations = new ArrayList<>();
        for (ExportBean bean : exportBeans) {
            addAllIfNotNull(checkIfValidClassName(bean, bean.getName()), violations);
            for (BeanNode node : bean.getBeans()) {
                addAllIfNotNull(checkIfValidNodeName(bean, node, node.getName()), violations);
            }
            for (ExportProperty property : bean.getProperties()) {
                addAllIfNotNull(checkIfValidPropertyName(bean, property, property.getName()), violations);
            }
            Map<Method, Class<?>> methodMap = new HashMap<>();
            for (Class<?> clz : bean.getImplementedInterfaces()) {
                for (Method method : clz.getDeclaredMethods()) {
                    methodMap.put(method, clz);
                }
            }
            List<ExportMethod> exportMethods = bean.getMethods();
            for (ExportMethod method : exportMethods) {
                addAllIfNotNull(checkIfValidMethodName(bean, method, method.getName()), violations);
                if (method.getDeclaringClass() != null) {
                    String methodName = method.getMethodDescriptor().getMethod().getName();
                    Class<?>[] parameters = method.getMethodDescriptor().getMethod().getParameterTypes();
                    List<Method> remove = new ArrayList<>();
                    for (Method method1 : methodMap.keySet()) {
                        if (method1.getName().equals(methodName) && Arrays.equals(method1.getParameterTypes(), parameters)) {
                            remove.add(method1);
                        }
                    }
                    for (Method rem : remove) {
                        methodMap.remove(rem);
                    }
                }
            }
            for (Map.Entry<Method, Class<?>> entry : methodMap.entrySet()) {
                violations.add(new ExportConstraintViolation("Method " + entry.getKey().getName() + " of interface " + entry.getValue().getName()
                        + " not implemented. EventListener interfaces with multiple methods can only be implemented fully!"));
            }
            for (ExportEvent event : bean.getEvents()) {
                addAllIfNotNull(checkIfValidEventName(bean, event, event.getName()), violations);
            }
        }

        return violations.isEmpty() ? null : violations;
    }

    private <T> void addAllIfNotNull(List<T> source, List<T> target) {
        if (source != null && target != null) {
            target.addAll(source);
        }
    }
}

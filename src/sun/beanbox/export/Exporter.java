package sun.beanbox.export;

import sun.beanbox.Wrapper;
import sun.beanbox.WrapperEventTarget;
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
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;

/**
 * Created by Andreas Ertlschweiger on 06.05.2017.
 */
public class Exporter {

    private HashMap<Object, Wrapper> wrapperBeanMap = new HashMap<>();
    private List<ExportBean> exportBeans = new LinkedList<>();

    private String tmpDirectoryName = "/tmp";
    private static final String DEFAULT_BEAN_PACKAGE_NAME = "beanBox/generated/beans";
    private static final String DEFAULT_SERIALIZED_PROPERTIES_PACKAGE_NAME = "beanBox/generated/properties";
    private static final String DEFAULT_ADAPTER_PACKAGE_NAME = "beanBox/generated/adapters";

    private static final String DEFAULT_BEAN_NAME = "ExportBean";

    public Exporter(List<Wrapper> beans) throws IntrospectionException, IllegalArgumentException, InvocationTargetException, IllegalAccessException {
        for (List<Wrapper> group : groupWrappers(beans)) {
            exportBeans.add(assembleExportBean(group, DEFAULT_BEAN_NAME + exportBeans.size()));
        }
    }

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

    private BeanNode createBeanNode(Wrapper wrapper, HashMap<Wrapper, BeanNode> createdNodes) throws IntrospectionException, InvocationTargetException, IllegalAccessException {
        if (createdNodes.get(wrapper) != null) {
            return createdNodes.get(wrapper);
        }
        BeanNode beanNode = new BeanNode(wrapper.getBean(), wrapper.getBeanLabel());
        beanNode.setJarPath(wrapper.getJarPath());
        BeanInfo beanInfo = Introspector.getBeanInfo(beanNode.getData().getClass());
        for (PropertyDescriptor propertyDescriptor : beanInfo.getPropertyDescriptors()) {
            if (!propertyDescriptor.isHidden() && !propertyDescriptor.isExpert() && propertyDescriptor.getReadMethod() != null && propertyDescriptor.getWriteMethod() != null) {
                beanNode.getProperties().add(new ExportProperty(propertyDescriptor, beanNode));
            }
        }
        for (MethodDescriptor methodDescriptor : beanInfo.getMethodDescriptors()) {
            beanNode.getMethods().add(new ExportMethod(methodDescriptor, beanNode));
        }
        createdNodes.put(wrapper, beanNode);
        for (WrapperEventTarget end : wrapper.getDirectTargets()) {
            Wrapper beanWrapper = wrapperBeanMap.get(end.getTargetBean());
            if (beanWrapper != null) {
                BeanNode childNode = createBeanNode(beanWrapper, createdNodes);
                beanNode.addEdge(new DirectCompositionEdge(beanNode, childNode, end));
            }
        }
        for (WrapperEventTarget end : wrapper.getEventHookupTargets()) {
            Wrapper beanWrapper = wrapperBeanMap.get(end.getTargetBean());
            if (beanWrapper != null) {
                BeanNode childNode = createBeanNode(beanWrapper, createdNodes);
                beanNode.addEdge(new AdapterCompositionEdge(beanNode, childNode, end));
            }
        }
        for (Object end : wrapper.getPropertyTargets()) {
            Wrapper beanWrapper = wrapperBeanMap.get(end);
            if (beanWrapper != null) {
                BeanNode childNode = createBeanNode(beanWrapper, createdNodes);
                beanNode.addEdge(new PropertyBindingEdge(beanNode, childNode));
            }
        }
        return beanNode;
    }

    private List<List<Wrapper>> groupWrappers(List<Wrapper> wrappers) {
        HashMap<Wrapper, Integer> groupMap = new HashMap<>();
        for (Wrapper wrapper : wrappers) {
            wrapperBeanMap.put(wrapper.getBean(), wrapper);
            groupMap.put(wrapper, null);
        }
        int groupCount = 0;
        for (Wrapper wrapper : wrappers) {
            Integer curGroup = groupMap.get(wrapper);
            if (curGroup == null) {
                for (Object bean : wrapper.getCompositionTargets()) {
                    Wrapper beanWrapper = wrapperBeanMap.get(bean);
                    if (beanWrapper != null && groupMap.get(beanWrapper) != null) {
                        curGroup = groupMap.get(beanWrapper);
                    }
                }
            }
            if (curGroup == null) {
                curGroup = groupCount;
                groupCount++;
            }
            groupMap.replace(wrapper, curGroup);
            for (Object bean : wrapper.getCompositionTargets()) {
                Wrapper beanWrapper = wrapperBeanMap.get(bean);
                if (beanWrapper != null) {
                    groupMap.replace(beanWrapper, curGroup);
                }
            }
        }
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

    public List<ExportBean> getBeans() {
        return exportBeans;
    }

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

    public boolean checkIfValidPropertyName(ExportBean exportBean, String text) {
        boolean isValidPropertyName = text != null && !text.isEmpty() && text.length() < 32 && SourceVersion.isIdentifier(text) && !SourceVersion.isKeyword(text);
        if (!isValidPropertyName) return false;
        for (ExportProperty property : exportBean.getProperties()) {
            if (property.getName().equals(text)) return false;
        }
        return true;
    }

    public void export(String directory, String filename) throws Exception {
        try {
            String[] filenameSplit = filename.split(Pattern.quote("."));
            if (!filenameSplit[filenameSplit.length - 1].equals("jar")) filename += ".jar";
            File target = new File(directory, filename);
            //if (target.isFile()) throw new IOException("File already exists!");
            int counter = 0;
            while (new File(directory + tmpDirectoryName + counter).isDirectory()) {
                counter++;
            }
            tmpDirectoryName += counter;
            if (validateConfiguration()) {
                File tmpDirectory = new File(directory + tmpDirectoryName);
                File tmpBeanDirectory = new File(tmpDirectory.getAbsolutePath() + "/" + DEFAULT_BEAN_PACKAGE_NAME);
                File tmpPropertiesDirectory = new File(tmpDirectory.getAbsolutePath() + "/" + DEFAULT_SERIALIZED_PROPERTIES_PACKAGE_NAME);
                File tmpManifestDirectory = new File(tmpDirectory.getAbsolutePath() + "/META-INF");
                File tmpAdapterDirectory = new File(tmpDirectory.getAbsolutePath() + "/" + DEFAULT_ADAPTER_PACKAGE_NAME);
                if (tmpBeanDirectory.mkdirs() && tmpPropertiesDirectory.mkdirs() && tmpManifestDirectory.mkdirs() && tmpAdapterDirectory.mkdirs()) {
                    ArrayList<File> resources = collectResources();
                    copyAndExtractResources(tmpDirectory, resources); //TODO: save moved resources
                    for (ExportBean exportBean : exportBeans) {
                        generateBean(tmpBeanDirectory, tmpPropertiesDirectory, tmpAdapterDirectory, exportBean);
                    }
                    generateManifest(tmpManifestDirectory);
                    compileSources(tmpBeanDirectory, resources);
                    resources.add(tmpDirectory);
                    packJar(target, tmpDirectory.getAbsolutePath(), resources);
                } else {
                    throw new IOException("Error creating temporary directories at: " + directory);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private void copyAndExtractResources(File tmpDirectory, Collection<File> resources) throws IOException {
        for (File resource : resources) {
            if(resource.getName().toLowerCase().endsWith(".jar")) {
                JarFile jarfile = new JarFile(resource);
                Enumeration<JarEntry> enu = jarfile.entries();
                while (enu.hasMoreElements()) {
                    JarEntry je = enu.nextElement();
                    File fl = new File(tmpDirectoryName, je.getName());
                    if (!fl.exists()) {
                        fl.getParentFile().mkdirs();
                        fl = new File(tmpDirectoryName, je.getName());
                    }
                    if (je.isDirectory() || je.getName().toUpperCase().contains("MANIFEST.MF")) {
                        continue;
                    }
                    InputStream is = jarfile.getInputStream(je);
                    FileOutputStream fo = new FileOutputStream(fl);
                    while (is.available() > 0) {
                        fo.write(is.read());
                    }
                    fo.close();
                    is.close();
                }
            } else {
                //TODO: write files recursively
            }
        }


    }

    private void packJar(File target, String base, Collection<File> resources) throws IOException {
        JarOutputStream jarOut = new JarOutputStream(new FileOutputStream(target));
        for (File resource : resources) {
            writeRecursively(jarOut, base, resource);
        }
        jarOut.close();
    }

    private void writeRecursively(JarOutputStream jarOut, String base, File resource) throws IOException {
        if (resource.isFile()) {
            System.out.println(resource.getAbsolutePath());
            if (resource.getName().toUpperCase().contains("MANIFEST.MF")) return;
            String relativePath = resource.getAbsolutePath().split(Pattern.quote(base))[0];
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
        Iterable<? extends JavaFileObject> files = fileManager.getJavaFileObjectsFromFiles(Arrays.asList(folder.listFiles()));
        compiler.getTask(null, fileManager, null, options, null, files).call();
    }

    private ArrayList<File> collectResources() throws IOException {
        Map<String, File> resources = new HashMap<>();
        for (ExportBean exportBean : exportBeans) {
            for (BeanNode node : exportBean.getBeans().getAllNodes()) {
                if (!resources.containsKey(node.getJarPath().substring(node.getJarPath().lastIndexOf('\\'), node.getJarPath().length()))) {
                    File resource = new File(node.getJarPath());
                    if (resource.exists()) {
                        resources.put(node.getJarPath().substring(node.getJarPath().lastIndexOf('\\'), node.getJarPath().length()), resource);
                    } else {
                        throw new IOException("Source file not found: " + node.getJarPath());
                    }
                }
                for (AdapterCompositionEdge edge : node.getAdapterCompositionEdges()) {
                    if (!resources.containsKey(edge.getAdapterClassPath().substring(node.getJarPath().lastIndexOf('\\'), node.getJarPath().length()))) {
                        File resource = new File(edge.getAdapterClassPath());
                        if (resource.exists()) {
                            resources.put(edge.getAdapterClassPath().substring(node.getJarPath().lastIndexOf('\\'), node.getJarPath().length()), resource);
                        } else {
                            throw new IOException("Source file not found: " + edge.getAdapterClassPath());
                        }
                    }
                }
            }
        }

        return new ArrayList<>(resources.values());
    }

    private void generateManifest(File manifestDirectory) throws IOException {
        File manifest = new File(manifestDirectory.getAbsolutePath(), "MANIFEST.MF");
        if (!manifest.createNewFile()) throw new IOException("Error creating File: " + manifest.getName());
        PrintWriter writer = new PrintWriter(new FileWriter(manifest));
        writer.println("Manifest-Version: 1.0");
        writer.println();
        for (ExportBean exportBean : exportBeans) {
            writer.println("Name: " + DEFAULT_BEAN_PACKAGE_NAME + "/" + exportBean.getBeanName() + ".class");
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

    private void generateBean(File beanDirectory, File propertyDirectory, File adapterDirectory, ExportBean exportBean) throws IOException {
        List<ExportProperty> exportProperties = exportBean.getProperties();

        File bean = new File(beanDirectory.getAbsolutePath(), exportBean.getBeanName() + ".java");
        File beanInfo = new File(beanDirectory.getAbsolutePath(), exportBean.getBeanName() + "BeanInfo.java");
        if (!bean.createNewFile()) throw new IOException("Error creating File: " + bean.getName());
        if (!beanInfo.createNewFile()) throw new IOException("Error creating File: " + beanInfo.getName());
        PrintWriter writer = new PrintWriter(new FileWriter(bean));
        writer.println("package " + DEFAULT_BEAN_PACKAGE_NAME.replaceAll(Pattern.quote("/"), ".") + ";");
        writer.println();
        Set<String> imports = new HashSet<>();
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
        }
        writer.println("import java.io.Serializable;");
        //TODO: print imports
        writer.println();
        writer.println("public class " + exportBean.getBeanName() + " implements Serializable {");
        writer.println();
        for (BeanNode node : exportBean.getBeans().getAllNodes()) {
            writer.println("private " + node.getData().getClass().getCanonicalName() + " " + node.lowercaseFirst() + ";");
        }
        writer.println();
        for (ExportProperty exportProperty : exportProperties) {
            writer.println("private " + exportProperty.getPropertyType().getCanonicalName() + " " + exportProperty.getName() + ";");
        }
        //TODO: PropertyChange & Veto Support
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
                writer.println("Property Binding: " + edge.getEnd().getData().getClass().getCanonicalName());
                //TODO
            }
        }
        writer.println();
        //TODO:initialize defaults
        writer.println("    }");
        writer.println();
        for (ExportProperty property : exportProperties) {
            writer.println("    public " + property.getPropertyType().getCanonicalName() + " get" + property.uppercaseFirst() + "() {");
            writer.println("        return " + property.getNode().lowercaseFirst() + "." + property.getPropertyDescriptor().getReadMethod().getName() + "();");
            writer.println("    }");
            writer.println();
            writer.println("    public void set" + property.uppercaseFirst() + "(" + property.getPropertyType().getCanonicalName() + " value) {");
            writer.println("        " + property.getNode().lowercaseFirst() + "." + property.getPropertyDescriptor().getWriteMethod().getName() + "(value);");
            writer.println("    }");
            writer.println();
        }
        //TODO:print property setter
        writer.println();
        //TODO: print input & output interface
        writer.println("}");
        writer.println();
        writer.close();
        if (writer.checkError()) {
            throw new IOException("Error writing Bean File: " + exportBean.getBeanName());
        }

        writer = new PrintWriter(new FileWriter(beanInfo));
        writer.println("package " + DEFAULT_BEAN_PACKAGE_NAME.replaceAll(Pattern.quote("/"), ".") + ";");
        writer.println();
        //TODO: print imports
        writer.println();
        writer.println("import java.beans.*;");
        writer.println("import java.io.Serializable;");
        writer.println();
        writer.println("public class " + exportBean.getBeanName() + "BeanInfo extends SimpleBeanInfo implements Serializable {");
        writer.println();
        if(!exportProperties.isEmpty()) {
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

        writer.println("    @Override");
        writer.println("    public EventSetDescriptor[] getEventSetDescriptors() {");
        //writer.println("        try {");
        writer.println("            Class cls = " + exportBean.getBeanName() + ".class;");
        //TODO: print event descriptors
        //writer.println("        } catch (IntrospectionException e) {");
        //writer.println("            e.printStackTrace();");
        //writer.println("        }");
        writer.println("        return null;");
        writer.println("    }");
        writer.println();
        writer.println("    @Override");
        writer.println("    public MethodDescriptor[] getMethodDescriptors() {");
        //writer.println("        try {");
        writer.println("            Class cls = " + exportBean.getBeanName() + ".class;");
        //TODO: print method descriptors
        //writer.println("        } catch (NoSuchMethodException e) {");
        //writer.println("            e.printStackTrace();");
        //writer.println("        }");
        writer.println("        return null;");
        writer.println("    }");
        writer.println("}");
        writer.println();
        writer.close();
        if (writer.checkError()) {
            throw new IOException("Error writing BeanInfo File: " + exportBean.getBeanName());
        }
    }

    private boolean validateConfiguration() {
        return true;
        //TODO
    }
}

package sun.beanbox.export;

import sun.beanbox.Wrapper;
import sun.beanbox.export.components.NodeSelector;
import sun.beanbox.export.datastructure.*;

import javax.lang.model.SourceVersion;
import java.beans.*;
import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Created by Andreas Ertlschweiger on 06.05.2017.
 */
public class Exporter {

    private HashMap<Object, Wrapper> wrapperBeanMap = new HashMap<>();
    private List<ExportBean> exportBeans = new LinkedList<>();

    private String tmpDirectoryName = "/tmp";
    private static final String DEFAULT_BEAN_PACKAGE_NAME = "beanBoxGeneratedBeans";
    private static final String DEFAULT_SERIALIZED_PROPERTIES_PACKAGE_NAME = "beanBoxGeneratedProperties";
    private static final String DEFAULT_ADAPTER_PACKAGE_NAME = "beanBoxGeneratedAdapters";

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
        if(availableNodes.isEmpty()) {
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
            for(BeanEdge edge : node.getEdges()) {
                if(availableNodes.contains(edge.getEnd())) {
                    availableNodes.remove(edge.getEnd());
                }
            }
        }
        if(availableNodes.isEmpty()) {
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
        if(createdNodes.get(wrapper) != null) {
            return createdNodes.get(wrapper);
        }
        BeanNode beanNode = new BeanNode(wrapper.getBean(), wrapper.getBeanLabel());
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
        for (Object end : wrapper.getDirectTargets()) {
            Wrapper beanWrapper = wrapperBeanMap.get(end);
            if (beanWrapper != null) {
                BeanNode childNode = createBeanNode(beanWrapper, createdNodes);
                beanNode.addEdge(new DirectCompositionEdge(beanNode, childNode));
            }
        }
        for (Object end : wrapper.getEventHookupTargets()) {
            Wrapper beanWrapper = wrapperBeanMap.get(end);
            if (beanWrapper != null) {
                BeanNode childNode = createBeanNode(beanWrapper, createdNodes);
                beanNode.addEdge(new AdapterCompositionEdge(beanNode, childNode));
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
        if(!isValidClassName) return false;
        for (ExportBean exportBean : exportBeans) {
            if(exportBean.getBeanName().equals(text)) return false;
        }
        return true;
    }

    public boolean checkIfValidPropertyName(ExportBean exportBean, String text) {
        boolean isValidPropertyName = text != null && !text.isEmpty() && text.length() < 32 && SourceVersion.isIdentifier(text) && !SourceVersion.isKeyword(text);
        if(!isValidPropertyName) return false;
        for (ExportProperty property : exportBean.getProperties()) {
            if(property.getName().equals(text)) return false;
        }
        return true;
    }

    public void export(String directory, String filename) throws Exception {
        String[] filenameSplit = filename.split(Pattern.quote("."));
        if(!filenameSplit[filenameSplit.length - 1].equals("jar")) filename+= ".jar";
        if(new File(directory, filename).isFile()) throw new IOException("File already exists!");
        int counter = 0;
        while (new File(directory + tmpDirectoryName + counter).isDirectory()) {
            counter++;
        }
        tmpDirectoryName+= counter;

        if (validateConfiguration()) {
            File tmpBeanDirectory = new File(directory + tmpDirectoryName + "/" + DEFAULT_BEAN_PACKAGE_NAME);
            File tmpPropertiesDirectory = new File(directory + tmpDirectoryName + "/" + DEFAULT_SERIALIZED_PROPERTIES_PACKAGE_NAME);
            File tmpManifestDirectory = new File(directory + tmpDirectoryName + "/META-INF");
            if(tmpBeanDirectory.mkdirs() && tmpPropertiesDirectory.mkdir() && tmpManifestDirectory.mkdir()) {
                for(ExportBean exportBean : exportBeans) {
                    generateBean(tmpBeanDirectory, tmpPropertiesDirectory, exportBean);
                }
                generateManifest(tmpManifestDirectory);
            } else {
                throw new IOException("Error creating temporary directories at: " + directory);
            }
        }
    }

    private void generateManifest(File manifestDirectory) throws IOException{
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
        if(writer.checkError()) {
            throw new IOException("Error writing Manifest");
        }
    }

    private void generateBean(File beanDirectory, File propertyDirectory, ExportBean exportBean) throws IOException {
        List<ExportProperty> exportProperties = exportBean.getProperties();

        File bean = new File(beanDirectory.getAbsolutePath(), exportBean.getBeanName() + ".java");
        File beanInfo = new File(beanDirectory.getAbsolutePath(), exportBean.getBeanName() + "BeanInfo.java");
        if (!bean.createNewFile()) throw new IOException("Error creating File: " + bean.getName());
        if (!beanInfo.createNewFile()) throw new IOException("Error creating File: " + beanInfo.getName());
        PrintWriter writer = new PrintWriter(new FileWriter(bean));
        writer.println("package " + DEFAULT_BEAN_PACKAGE_NAME + ";");
        writer.println();
        for (BeanNode node : exportBean.getBeans().getAllNodes()) {
            writer.println("import " + node.getData().getClass().getCanonicalName() + ";");
        }
        writer.println();
        for (ExportProperty exportProperty : exportProperties) {
            writer.println("import " + exportProperty.getPropertyType().getCanonicalName() + ";");
        }
        //TODO: print imports
        writer.println();
        writer.println("public class " + exportBean.getBeanName() + " {"); //TODO:implements Serializable?
        writer.println();
        for (BeanNode node : exportBean.getBeans().getAllNodes()) {
            writer.println("private " + node.getData().getClass().getCanonicalName() + " " + node.lowercaseFirst() + ";");
        }
        writer.println();
        for (ExportProperty exportProperty : exportProperties) {
            writer.println("private " + exportProperty.getPropertyType().getCanonicalName() + " " + exportProperty.getName() + ";");
        }
        writer.println();
        writer.println("    public " + exportBean.getBeanName() + "() {");
        //TODO:initialize pipeline
        writer.println("    }");
        writer.println();
        for (ExportProperty property : exportProperties) {
            writer.println("    public " + property.getPropertyType().getCanonicalName() + " get" + property.uppercaseFirst() + "() {");
            writer.println("        return " + property.getNode().lowercaseFirst() + "." + property.getPropertyDescriptor().getReadMethod().getName() + "();");
            writer.println("    }");
            writer.println();
        }
        //TODO:print property setter
        writer.println();
        //TODO: print input & output interface
        writer.println("}");
        writer.println();
        writer.close();
        if(writer.checkError()) {
            throw new IOException("Error writing Bean File: " + exportBean.getBeanName());
        }

        writer = new PrintWriter(new FileWriter(beanInfo));
        writer.println("package " + DEFAULT_BEAN_PACKAGE_NAME + ";");
        writer.println();
        //TODO: print imports
        writer.println();
        writer.println("import java.beans.*;");
        writer.println("import java.io.Serializable;");
        writer.println();
        writer.println("public class " + exportBean.getBeanName() + "BeanInfo extends SimpleBeanInfo implements Serializable {");
        writer.println();
        writer.println("    @Override");
        writer.println("    public PropertyDescriptor[] getPropertyDescriptors() {");
        writer.println("        try {");
        writer.println("            Class cls = " + exportBean.getBeanName() + ".class;");
        //TODO: print property descriptors
        writer.println("        } catch (IntrospectionException e) {");
        writer.println("            e.printStackTrace();");
        writer.println("        }");
        writer.println("        return null;");
        writer.println("    }");
        writer.println();
        writer.println("    @Override");
        writer.println("    public EventSetDescriptor[] getEventSetDescriptors() {");
        writer.println("        try {");
        //TODO: print event descriptors
        writer.println("        } catch (IntrospectionException e) {");
        writer.println("            e.printStackTrace();");
        writer.println("        }");
        writer.println("        return null;");
        writer.println("    }");
        writer.println();
        writer.println("    @Override");
        writer.println("    public MethodDescriptor[] getMethodDescriptors() {");
        writer.println("        try {");
        //TODO: print method descriptors
        writer.println("        } catch (NoSuchMethodException e) {");
        writer.println("            e.printStackTrace();");
        writer.println("        }");
        writer.println("        return null;");
        writer.println("    }");
        writer.println("}");
        writer.println();
        writer.close();
        if(writer.checkError()) {
            throw new IOException("Error writing BeanInfo File: " + exportBean.getBeanName());
        }
    }

    private boolean validateConfiguration() {
        return true;
        //TODO
    }
}

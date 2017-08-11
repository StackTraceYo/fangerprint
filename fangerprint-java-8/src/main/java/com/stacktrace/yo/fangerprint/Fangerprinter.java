package com.stacktrace.yo.fangerprint;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hasher;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.TypePath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Created by Stacktraceyo on 8/11/17.
 */

public class Fangerprinter {

    private static final Logger LOGGER = LoggerFactory.getLogger(Fangerprinter.class);

    private Map<String, ClassReader> _dependencies = Maps.newHashMap();
    private Set<String> _failedDependencies = Sets.newHashSet();
    private Set<Class> _resolvedDependencies = null;
    private LinkedList<ClassReader> _pendingTraversalList = Lists.newLinkedList();
    private Class rootClass;
    private HashFunction hashFunction;
    private Set<String> _classesToExclude = Sets.newHashSet();
    private Set<String> _excludedClasses = Sets.newHashSet();
    private ArrayList<Class> _excludedClassesFromExcludedJar = Lists.newArrayList();
    private Set<String> _excludedJarNames = Sets.newHashSet();
    private List<ClassLoader> _classesLoaders = new ArrayList<>();
    private boolean _excludeJavaBootstrap = false;

    public static FingerprintBuilder newBuilder(Class rootClass) {
        return new FingerprintBuilder(rootClass);
    }

    private ClassVisitor _classVisitor = new ClassVisitor(Opcodes.ASM5) {

        @Override
        public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
            recordObjectType(superName);
            recordObjectType(interfaces);
        }

        @Override
        public void visitInnerClass(String name, String outerName, String innerName, int access) {
            recordObjectType(name);
            recordObjectType(outerName);
        }

        @Override
        public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
            recordType(Type.getType(desc));
            return myFieldVisitor;
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
            recordObjectType(exceptions);
            recordType(Type.getMethodType(desc));
            return _methodVisitor;
        }

        @Override
        public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
            recordType(Type.getType(desc));
            return _annotationVisitor;
        }

        @Override
        public AnnotationVisitor visitTypeAnnotation(int typeRef, TypePath typePath, String desc, boolean visible) {
            recordType(Type.getType(desc));
            return _annotationVisitor;
        }
    };

    private AnnotationVisitor _annotationVisitor = new AnnotationVisitor(Opcodes.ASM5) {

        @Override
        public void visitEnum(String name, String desc, String value) {
            recordType(Type.getType(desc));
        }

        @Override
        public AnnotationVisitor visitAnnotation(String name, String desc) {
            recordType(Type.getType(desc));
            return _annotationVisitor;
        }
    };

    private FieldVisitor myFieldVisitor = new FieldVisitor(Opcodes.ASM5) {

        @Override
        public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
            recordType(Type.getType(desc));
            return _annotationVisitor;
        }

        @Override
        public AnnotationVisitor visitTypeAnnotation(int typeRef, TypePath typePath, String desc, boolean visible) {
            recordType(Type.getType(desc));
            return _annotationVisitor;
        }
    };

    private MethodVisitor _methodVisitor = new MethodVisitor(Opcodes.ASM5) {

        @Override
        public AnnotationVisitor visitAnnotationDefault() {
            return _annotationVisitor;
        }

        @Override
        public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
            recordType(Type.getType(desc));
            return _annotationVisitor;
        }

        @Override
        public AnnotationVisitor visitTypeAnnotation(int typeRef, TypePath typePath, String desc, boolean visible) {
            recordType(Type.getType(desc));
            return _annotationVisitor;
        }

        @Override
        public AnnotationVisitor visitParameterAnnotation(int parameter, String desc, boolean visible) {
            recordType(Type.getType(desc));
            return _annotationVisitor;
        }

        @Override
        public void visitFrame(int type, int nLocal, Object[] local, int nStack, Object[] stack) {
            visitLocals(local);
            visitLocals(stack);
        }

        @Override
        public void visitTypeInsn(int opcode, String type) {
            recordObjectType(type);
        }

        @Override
        public void visitFieldInsn(int opcode, String owner, String name, String desc) {
            recordObjectType(owner);
            recordType(Type.getType(desc));
        }

        @Override
        public void visitMethodInsn(int opcode, String owner, String name, String desc) {
            recordObjectType(owner);
            recordType(Type.getMethodType(desc));
        }

        @Override
        public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
            recordObjectType(owner);
            recordType(Type.getMethodType(desc));
        }

        @Override
        public void visitMultiANewArrayInsn(String desc, int dims) {
            recordObjectType(desc);
        }

        @Override
        public AnnotationVisitor visitInsnAnnotation(int typeRef, TypePath typePath, String desc, boolean visible) {
            recordType(Type.getType(desc));
            return _annotationVisitor;
        }

        @Override
        public void visitTryCatchBlock(Label start, Label end, Label handler, String type) {
            recordObjectType(type);
        }

        @Override
        public AnnotationVisitor visitTryCatchAnnotation(int typeRef, TypePath typePath, String desc, boolean visible) {
            recordType(Type.getType(desc));
            return _annotationVisitor;
        }

        @Override
        public void visitLocalVariable(String name, String desc, String signature, Label start, Label end, int index) {
            recordType(Type.getType(desc));
        }

        @Override
        public AnnotationVisitor visitLocalVariableAnnotation(int typeRef, TypePath typePath, Label[] start, Label[] end, int[] index,
                                                              String desc, boolean visible) {
            recordType(Type.getType(desc));
            return _annotationVisitor;
        }
    };

    private void visitLocals(Object[] locals) {
        if (locals != null) {
            Arrays.stream(locals)
                    .filter(local -> local instanceof String)
                    .forEach(local -> recordType(Type.getObjectType((String) local)));
        }
    }

    private void recordObjectType(String... names) {
        if (names != null) {
            Arrays.stream(names)
                    .filter(Objects::nonNull)
                    .forEach(typeName -> recordType(Type.getObjectType(typeName)));
        }
    }

    private void recordType(Type... types) {
        Arrays.stream(types)
                .forEach(this::recordType);
    }

    private void recordType(Type t) {
        switch (t.getSort()) {
            case Type.ARRAY:
                recordType(t.getElementType());
                break;
            case Type.OBJECT:
                if (!_dependencies.containsKey(t.getInternalName()) && !shouldIgnoreClass(t.getInternalName())) {
                    ClassReader r;
                    try {
                        r = new ClassReader(t.getClassName());
                        _pendingTraversalList.add(r);
                        _dependencies.put(t.getInternalName(), r);
                        LOGGER.trace("Saved {} to examine later", t.getClassName());
                    } catch (IOException e) {
                        try {
                            LOGGER.trace("Attempting to load as a stream");
                            //try reading the class from a stream
                            r = new ClassReader(getClassAsStream(t.getInternalName()));
                            _pendingTraversalList.add(r);
                            _dependencies.put(t.getInternalName(), r);
                            LOGGER.trace("Saved {} to examine later", t.getClassName());
                        } catch (IOException e1) {
                            LOGGER.trace("Looking for additional classloaders");
                            if (!_classesLoaders.isEmpty()) {
                                LOGGER.trace("Found more classloaders");
                                _classesLoaders
                                        .forEach(classLoader -> {
                                            try {
                                                ClassReader r2 = new ClassReader(classLoader.getResourceAsStream(t.getInternalName() + ".class"));
                                                _pendingTraversalList.add(r2);
                                                _dependencies.put(t.getInternalName(), r2);
                                                LOGGER.trace("Saved {} to examine later", t.getClassName());
                                            } catch (IOException e2) {
                                                _failedDependencies.add(t.getInternalName());
                                            }
                                        });
                            } else {
                                _failedDependencies.add(t.getInternalName());
                            }
                        }
                    }
                }
                break;
            case Type.METHOD:
                recordType(t.getReturnType());
                recordType(t.getArgumentTypes());
                break;
        }
    }

    public Set<String> getDependencies() {
        return _dependencies.keySet();
    }

    public Set<Class> getResolvedDependencies() {
        if (_resolvedDependencies != null) {
            return _resolvedDependencies;
        } else {
            _resolvedDependencies = Sets.newHashSet();
            ClassLoader loader = getClass().getClassLoader();
            for (String dep : _dependencies.keySet()) {
                try {
                    addResolvedDependency(loader, dep);
                } catch (NoClassDefFoundError e) {
                    //try loading from additional class loaders
                    if (!_classesLoaders.isEmpty()) {
                        for (ClassLoader additionalLoader : _classesLoaders) {
                            try {
                                addResolvedDependency(additionalLoader, dep);
                            } catch (ClassNotFoundException e1) {
                            } catch (NoClassDefFoundError e2) {
                            }
                        }
                    }
                } catch (ClassNotFoundException e) {
                }
            }
            return _resolvedDependencies;
        }

    }

    private void addResolvedDependency(ClassLoader loader, String dep) throws ClassNotFoundException {
        _resolvedDependencies.add(loader.loadClass(convertClassNameToDotNotation(dep)));
    }

    public Set<String> getFailedDependencies() {
        return _failedDependencies;
    }

    //Begins traversal
    public String computeHash() {

        //reset collections;
        _resolvedDependencies = null;
        _dependencies.clear();
        _pendingTraversalList.clear();
        _failedDependencies.clear();

        //begin traversal
        recordType(Type.getType(rootClass));

        //while the list of non traversed classes is not empty
        while (!_pendingTraversalList.isEmpty()) {
            ClassReader current = _pendingTraversalList.removeFirst();
            LOGGER.trace("Examining {}", Type.getObjectType(current.getClassName()).getClassName());
            current.accept(_classVisitor, 0);
        }
        if (_failedDependencies.size() > 0) {
            LOGGER.trace("Failed to load {} from root class: {}", Arrays.toString(_failedDependencies.toArray()), rootClass.getName());
        }
        Hasher hasher = hashFunction.newHasher();
        Map<String, ClassReader> sorted = Maps.newTreeMap();
        sorted.putAll(_dependencies);
        //if no dependencies are found
        if (sorted.size() <= 1) {
            LOGGER.error("Failed to find any dependencies from {}", rootClass.getName());
            throw new RuntimeException("NO DEPENDENCIES FOUND UNABLE TO FINGERPRINT");
        }

        //hash
        sorted.values()
                .forEach(reader -> hasher.putBytes(reader.b));
        String hash = hasher.hash().toString();
        LOGGER.debug("Softare Hash: {}", hash);
        LOGGER.debug("Total Number of Dependencies Used: {}", sorted.size());
        LOGGER.debug("Total Number of Dependencies Excluded: {}", _excludedClasses.size());
        LOGGER.debug("Total Number of Dependencies Failed: {}", _failedDependencies.size());
        return hash;
    }

    //is from bootstrapped classes
    private boolean isIgnoreLocationSource(String classNamePath) {
        classNamePath = convertClassNameFromDotNotation(classNamePath);
        return classNamePath.startsWith("java/") ||
                classNamePath.startsWith("javax/") ||
                classNamePath.startsWith("jdk/") ||
                classNamePath.startsWith("sun/") ||
                classNamePath.startsWith("com/sun");
    }

    private InputStream getClassAsStream(String classname) {
        return getClass().getClassLoader().getResourceAsStream(classname + ".class");
    }

    private void setHashType(HashFunction hashFunction) {
        this.hashFunction = hashFunction;
    }

    private void setRootClass(Class rootClass) {
        this.rootClass = rootClass;
    }

    private void addClassLoader(ClassLoader classLoader) {
        _classesLoaders.add(classLoader);
    }

    public Set<String> getExcludedClasses() {
        return _excludedClasses;
    }

    public Set<String> get_excludedJarNames() {
        return _excludedJarNames;
    }

    private void initIgnoreJars() {
        if (!_excludedClassesFromExcludedJar.isEmpty()) {
            for (Class klass : _excludedClassesFromExcludedJar) {
                try {
                    _excludedJarNames.add(getJarName(klass));
                } catch (Exception e) {
                    LOGGER.trace("Unable to find location of {}", klass.getName());
                }
            }
        }
    }

    private String getJarName(Class klass) {
        return new File(klass.getProtectionDomain()
                .getCodeSource()
                .getLocation()
                .getPath())
                .getName();
    }

    private String getJarName(URL classUrl) {
        return new File(classUrl
                .getPath())
                .getName();
    }

    private boolean isInIgnoredJar(String jarname) {
        return _excludedJarNames.contains(jarname);
    }

    private boolean isAnIgnoredClass(String className) {
        return _classesToExclude.contains(convertClassNameToDotNotation(className));
    }

    private boolean isIgnoreJava(String className) {
        return _excludeJavaBootstrap && isIgnoreLocationSource(className);
    }

    private boolean shouldIgnoreClass(String className) {
        URL source = getClassSource(className);
        if (source != null) {
            if (isInIgnoredJar(getJarName(source))) {
                LOGGER.trace("Ignoring {} - class was found in jar {} which is in ignore list", className, getJarName(source));
                _excludedClasses.add(className);
                return true;
            } else if (isAnIgnoredClass(className)) {
                LOGGER.trace("Ignoring {} - class was found in classname ignore list", className);
                _excludedClasses.add(className);
                return true;
            } else return isIgnoreJava(className);
        } else if (isIgnoreJava(className)) {
            LOGGER.trace("Ignoring {} - java classes are ignored", className);
            _excludedClasses.add(className);
            return true;
        } else {
            return false;
        }
    }

    private URL getClassSource(String className) {
        URL source = null;
        if (isIgnoreLocationSource(className)) {
            return source;
        }
        String actualClass = convertClassNameToDotNotation(className);
        ClassLoader loader = this.getClass().getClassLoader();
        try {
            source = loader.loadClass(actualClass).getProtectionDomain().getCodeSource().getLocation();
        } catch (NoClassDefFoundError e) {
            LOGGER.trace("Cannot find {}", e.getMessage());
        } catch (Exception e) {
            loader = ClassLoader.getSystemClassLoader();
            while (loader != null) {
                try {
                    source = loader.loadClass(actualClass).getProtectionDomain().getCodeSource().getLocation();
                } catch (Exception e2) {
                    loader = loader.getParent();
                }
            }
        }
        return source;
    }

    private static String convertClassName(String className, boolean isDotNotation) {
        return (isDotNotation ? className.replace('.', '/') : className.replace('/', '.'));
    }

    private static String convertClassNameToDotNotation(String className) {
        return convertClassName(className, false);
    }

    private static String convertClassNameFromDotNotation(String className) {
        return convertClassName(className, true);
    }


    private void addClassToIgnore(Class klass) {
        _classesToExclude.add(klass.getName());
    }

    private void addClassToIgnoreJar(Class klass) {
        _excludedClassesFromExcludedJar.add(klass);
    }

    private void addJarNameToIgnore(String jarName) {
        _excludedJarNames.add(jarName);
    }


    public static final class FingerprintBuilder {

        private Fangerprinter fangerprinter;

        private FingerprintBuilder(Class rootClass) {
            fangerprinter = new Fangerprinter();
            fangerprinter.setRootClass(rootClass);
        }

        public FingerprintBuilder withClassLoader(ClassLoader classLoader) {
            fangerprinter.addClassLoader(classLoader);
            return this;
        }

        public FingerprintBuilder withHashType(HashFunction hashFunction) {
            fangerprinter.setHashType(hashFunction);
            return this;
        }

        public FingerprintBuilder ignoreJarWithClass(Class klass) {
            fangerprinter.addClassToIgnoreJar(klass);
            return this;
        }

        public FingerprintBuilder ignoreJarWithName(String jarName) {
            fangerprinter.addJarNameToIgnore(jarName);
            return this;
        }

        public FingerprintBuilder ignoreClass(Class klass) {
            fangerprinter.addClassToIgnore(klass);
            return this;
        }

        public FingerprintBuilder ignoreJava(boolean ignore) {
            fangerprinter._excludeJavaBootstrap = ignore;
            return this;
        }

        public Fangerprinter build() {
            fangerprinter.initIgnoreJars();
            return fangerprinter;
        }
    }
}
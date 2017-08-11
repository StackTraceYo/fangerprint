package com.stacktrace.yo.fangerprint;

import com.google.common.hash.Hasher;
import com.google.common.hash.Hashing;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLStreamHandlerFactory;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import static org.assertj.core.api.Assertions.assertThat;


public class FangerprinterTest {

    private static Class FIRST_TEST_CLASS;
    private static Class SECOND_TEST_CLASS;
    private static Class THIRD_TEST_CLASS;
    private static Class FOURTH_TEST_CLASS;
    private static Class FIFTH_TEST_CLASS;

    private static ClassLoader FIRST_TEST_CLASSLOADER;
    private static ClassLoader SECOND_TEST_CLASSLOADER;
    private static ClassLoader THIRD_TEST_CLASSLOADER;
    private static ClassLoader FOURTH_TEST_CLASSLOADER;
    private static ClassLoader FIFTH_TEST_CLASSLOADER;

    @BeforeClass
    public static void setUp() throws Exception {

        compileFirstTestClass();
        compileSecondTestClass();
        compileThirdTestClass();
        compileFourthTestClass();
        compileFifthTestClass();

        FIRST_TEST_CLASSLOADER = getFirstTestClassloader();
        SECOND_TEST_CLASSLOADER = getSecondTestClassloader();
        THIRD_TEST_CLASSLOADER = getThirdTestClassloader();
        FOURTH_TEST_CLASSLOADER = getFourthTestClassloader();
        FIFTH_TEST_CLASSLOADER = getFifthTestClassloader();

        FIRST_TEST_CLASS = loadTestClass(FIRST_TEST_CLASSLOADER);
        SECOND_TEST_CLASS = loadTestClass(SECOND_TEST_CLASSLOADER);
        THIRD_TEST_CLASS = loadTestClass(THIRD_TEST_CLASSLOADER);
        FOURTH_TEST_CLASS = loadTestClass(FOURTH_TEST_CLASSLOADER);
        FIFTH_TEST_CLASS = loadTestClass(FIFTH_TEST_CLASSLOADER);
    }


    @Test
    public void testHashGeneratesSameValueTwiceAndfindSameNumberofDeps() throws Exception {
        Fangerprinter fangerprinter = Fangerprinter.newBuilder(FIRST_TEST_CLASS)
                .withHashType(Hashing.md5())
                .withClassLoader(FIRST_TEST_CLASSLOADER)
                .build();
        String hash = fangerprinter.computeHash();
        Integer depFound = fangerprinter.getDependencies().size();
        assertThat(hash).isEqualTo(fangerprinter.computeHash());
        assertThat(depFound).isEqualTo(fangerprinter.getDependencies().size());
    }

    @Test
    public void testHashGeneratesDifferentValueWithDifferentHashFunction() throws Exception {
        Fangerprinter fangerprinter = Fangerprinter.newBuilder(FIRST_TEST_CLASS)
                .withHashType(Hashing.md5())
                .withClassLoader(FIRST_TEST_CLASSLOADER)
                .build();
        Fangerprinter fangerprinter2 = Fangerprinter.newBuilder(FIRST_TEST_CLASS)
                .withHashType(Hashing.sha1())
                .withClassLoader(FIRST_TEST_CLASSLOADER)
                .build();
        String hash = fangerprinter.computeHash();
        String hash2 = fangerprinter2.computeHash();
        assertThat(hash).isNotEqualTo(hash2);
        assertThat(fangerprinter.getDependencies().size()).isEqualTo(fangerprinter2.getDependencies().size());
    }

    @Test
    public void testNumberOfDependenciesChangeWithDifferentClasses() throws Exception {
        Fangerprinter fangerprinter = Fangerprinter.newBuilder(FIRST_TEST_CLASS)
                .withHashType(Hashing.md5())
                .withClassLoader(FIRST_TEST_CLASSLOADER)
                .build();
        Fangerprinter fangerprinter2 = Fangerprinter.newBuilder(Fangerprinter.class)
                .withHashType(Hashing.md5())
                .build();
        fangerprinter.computeHash();
        fangerprinter2.computeHash();
        assertThat(fangerprinter.getDependencies().size()).isNotEqualTo(fangerprinter2.getDependencies().size());
    }

    @Test
    public void testCanGetFailedDependencies() throws Exception {
        Fangerprinter fangerprinter = Fangerprinter.newBuilder(FIRST_TEST_CLASS)
                .withHashType(Hashing.md5())
                .withClassLoader(FIRST_TEST_CLASSLOADER)
                .build();
        ;
        fangerprinter.computeHash();
        assertThat(fangerprinter.getFailedDependencies()).isNotNull();
    }

    @Test
    public void testCanGetResolvedDependencies() throws Exception {
        Fangerprinter fangerprinter = Fangerprinter.newBuilder(FIRST_TEST_CLASS)
                .withHashType(Hashing.md5())
                .withClassLoader(FIRST_TEST_CLASSLOADER)
                .build();
        fangerprinter.computeHash();
        Integer size = fangerprinter.getResolvedDependencies().size();
        assertThat(size).isNotNull();
        assertThat(size).isNotEqualTo(0);
    }

    @Test
    public void testHashChangesWithClassChanges() throws Exception {
        Set<String> hashes = new TreeSet<>();
        //Base
        Fangerprinter fangerprinter = Fangerprinter.newBuilder(FIRST_TEST_CLASS)
                .withHashType(Hashing.md5())
                .withClassLoader(FIRST_TEST_CLASSLOADER)
                .build();
        String hash = fangerprinter.computeHash();
        hashes.add(hash);

        //Adding a dependency in constructor

        Fangerprinter fangerprinter2 = Fangerprinter.newBuilder(SECOND_TEST_CLASS)
                .withHashType(Hashing.md5())
                .withClassLoader(SECOND_TEST_CLASSLOADER)
                .build();
        String hash2 = fangerprinter2.computeHash();
        hashes.add(hash2);

        //Changed interface name

        Fangerprinter fangerprinter3 = Fangerprinter.newBuilder(THIRD_TEST_CLASS)
                .withHashType(Hashing.md5())
                .withClassLoader(THIRD_TEST_CLASSLOADER)
                .build();
        String hash3 = fangerprinter3.computeHash();
        hashes.add(hash3);

        //Added New Method
        Fangerprinter fangerprinter4 = Fangerprinter.newBuilder(FOURTH_TEST_CLASS)
                .withHashType(Hashing.md5())
                .withClassLoader(FOURTH_TEST_CLASSLOADER)
                .build();
        String hash4 = fangerprinter4.computeHash();
        hashes.add(hash4);

        assertThat(hashes.size()).isEqualTo(4);

    }

    @Test
    public void testHashDoesNotChangeWithNoClassChange() throws Exception {
        Set<String> hashes = new TreeSet<>();
        //Base
        Fangerprinter fangerprinter = Fangerprinter.newBuilder(FIRST_TEST_CLASS)
                .withHashType(Hashing.md5())
                .withClassLoader(FIRST_TEST_CLASSLOADER)
                .build();
        String hash = fangerprinter.computeHash();
        hashes.add(hash);

        //FIFTH Class is the same as First Class
        // doing this to make sure same name directory returns same fingerprint
        Fangerprinter fangerprinter2 = Fangerprinter.newBuilder(FIFTH_TEST_CLASS)
                .withHashType(Hashing.md5())
                .withClassLoader(FIFTH_TEST_CLASSLOADER)
                .build();
        String hash2 = fangerprinter2.computeHash();
        hashes.add(hash2);
        assertThat(hashes.size()).isEqualTo(1);


    }

    @Test
    public void testIgnoringClassesAndJarsChangesHash() throws Exception {

        Set<String> hashes = new HashSet<>();
        Set<Integer> excludedSize = new HashSet<>();


        Fangerprinter fangerprinter = Fangerprinter.newBuilder(Fangerprinter.class)
                .withHashType(Hashing.md5())
                .build();

        hashes.add(fangerprinter.computeHash());

        Fangerprinter fangerprinter2 = Fangerprinter.newBuilder(Fangerprinter.class)
                .withHashType(Hashing.md5())
                .ignoreJarWithClass(Hasher.class)
                .build();
        hashes.add(fangerprinter2.computeHash());

        Fangerprinter fangerprinter3 = Fangerprinter.newBuilder(Fangerprinter.class)
                .withHashType(Hashing.md5())
                .ignoreClass(Hasher.class)
                .build();

        hashes.add(fangerprinter3.computeHash());
        assertThat(hashes.size()).isEqualTo(3);

        excludedSize.add(fangerprinter.getExcludedClasses().size());
        excludedSize.add(fangerprinter2.getExcludedClasses().size());
        excludedSize.add(fangerprinter3.getExcludedClasses().size());

        assertThat(excludedSize.size()).isEqualTo(3);
    }

    @Test
    public void testIgnoreJar() throws Exception {


        Fangerprinter fangerprinter = Fangerprinter.newBuilder(Fangerprinter.class)
                .withHashType(Hashing.md5())
                .ignoreJarWithClass(Hasher.class)
                .build();
        fangerprinter.computeHash();

        assertThat(fangerprinter.get_excludedJarNames().size()).isEqualTo(1);
        assertThat(fangerprinter.getExcludedClasses().size()).isGreaterThan(0);

        Iterator<String> iterator = fangerprinter.get_excludedJarNames().iterator();
        String jarName = iterator.next();
        assertThat(jarName).contains("guava");
    }


    @Test
    public void testIgnoreClass() throws Exception {


        Fangerprinter fangerprinter = Fangerprinter.newBuilder(Fangerprinter.class)
                .withHashType(Hashing.md5())
                .ignoreClass(Hasher.class)
                .build();
        fangerprinter.computeHash();

        assertThat(fangerprinter.getExcludedClasses().size()).isEqualTo(1);

        Iterator<String> excludedClasses = fangerprinter.getExcludedClasses().iterator();
        String excludedClass = excludedClasses.next();
        assertThat(excludedClass).isEqualTo("com/google/common/hash/Hasher");
    }

    @Test
    public void testIgnoreJava() throws Exception {

        Fangerprinter fangerprinter = Fangerprinter.newBuilder(Fangerprinter.class)
                .withHashType(Hashing.md5())
                .build();
        fangerprinter.computeHash();

        Fangerprinter fangerprinter2 = Fangerprinter.newBuilder(Fangerprinter.class)
                .withHashType(Hashing.md5())
                .ignoreJava(true)
                .build();
        fangerprinter2.computeHash();

        assertThat(fangerprinter.getExcludedClasses().size()).isEqualTo(0);
        assertThat(fangerprinter2.getExcludedClasses().size()).isEqualTo(179);
    }

    //Utility Methods
    private static void compileFirstTestClass() throws Exception {

        compileTestClass("com/stacktrace/yo/fangerprint/1/TestClassToHash.java");
    }

    private static void compileSecondTestClass() throws Exception {
        compileTestClass("com/stacktrace/yo/fangerprint/2/TestClassToHash.java");
    }

    private static void compileThirdTestClass() throws Exception {
        compileTestClass("com/stacktrace/yo/fangerprint/3/TestClassToHash.java");
    }

    private static void compileFourthTestClass() throws Exception {
        compileTestClass("com/stacktrace/yo/fangerprint/4/TestClassToHash.java");
    }

    private static void compileFifthTestClass() throws Exception {
        compileTestClass("com/stacktrace/yo/fangerprint/5/TestClassToHash.java");
    }

    private static ClassLoader getFirstTestClassloader() throws Exception {
        return getClassLoader("com/stacktrace/yo/fangerprint/1");
    }

    private static ClassLoader getSecondTestClassloader() throws Exception {
        return getClassLoader("com/stacktrace/yo/fangerprint/2");
    }

    private static ClassLoader getThirdTestClassloader() throws Exception {
        return getClassLoader("com/stacktrace/yo/fangerprint/3");
    }

    private static ClassLoader getFourthTestClassloader() throws Exception {
        return getClassLoader("com/stacktrace/yo/fangerprint/4");
    }

    private static ClassLoader getFifthTestClassloader() throws Exception {
        return getClassLoader("com/stacktrace/yo/fangerprint/5");
    }

    private static Class loadTestClass(ClassLoader loader) throws ClassNotFoundException {
        return FangerprinterTest.loadTestClass(loader, "TestClassToHash");
    }

    //Compiles a class given a directory
    static void compileTestClass(String directory) throws Exception {
        compileClassFromResource(FangerprinterTest.class.getClassLoader().getResource(directory).getPath());
    }

    //Returns a new StubClassLoader withthe given root directory
    static ClassLoader getClassLoader(String directory) throws Exception {
        List<URL> urls = new ArrayList();
        urls.add(new File(FangerprinterTest.class.getClassLoader().getResource(directory).getPath()).toURL());

        StubClassLoader classloader =
                new StubClassLoader(
                        urls.toArray(new URL[0]));

        return classloader;
    }


    //asks the ClassLoader to load a given classname
    static Class loadTestClass(ClassLoader loader, String className) throws ClassNotFoundException {
        return loader.loadClass(className);
    }


    //Opens a file, then gets runtime compiler and compiles the class
    private static void compileClassFromResource(String filepath) throws ClassNotFoundException, IllegalAccessException, InstantiationException, MalformedURLException {
        File javaFile = new File(filepath);
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        compiler.run(null, null, null, javaFile.getPath());
    }

    private static final class StubClassLoader extends URLClassLoader {
        public StubClassLoader(URL[] urls, ClassLoader parent) {
            super(urls, parent);
        }

        public StubClassLoader(URL[] urls) {
            super(urls);
        }

        public StubClassLoader(URL[] urls, ClassLoader parent, URLStreamHandlerFactory factory) {
            super(urls, parent, factory);
        }

        //Finds the class if it is not already defined
        public Class<?> loadClass(String name) throws ClassNotFoundException {
            Class loadedClass = findLoadedClass(name);
            if (loadedClass == null) {
                try {
                    loadedClass = findClass(name);
                } catch (ClassNotFoundException e) {
                }
                if (loadedClass == null) {
                    loadedClass = super.loadClass(name);
                }
            }
            return loadedClass;
        }
    }


}

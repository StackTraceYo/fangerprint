
import com.stacktrace.yo.fangerprint.Fangerprinter;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.ArrayList;
import java.util.List;


//new dependency in innerclass

public class TestClassToHash {

    public static class TestClass implements TestIface {
        @TestAnnotation(TestAnnotationEnum.VALUE)
        public int field;
        public TestDependency dependency;
        public final List<String> list;

        public TestClass() {
            list = new ArrayList<String>();
            Fangerprinter newDependency = new Fangerprinter();
        }

        @Override
        public TestEnum enumValue() {
            return TestEnum.VALUE;
        }
    }

    public static enum TestEnum {
        VALUE
    }

    public static enum TestAnnotationEnum {
        VALUE
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.FIELD})
    public @interface TestAnnotation {
        TestAnnotationEnum value();
    }

    public static class TestDependency {
        public int field;
    }

    public static interface TestIface {
        public TestEnum enumValue();
    }

}

package name.remal.gradleplugins.testsourcesets;

import static name.remal.gradleplugins.toolkit.reflection.ReflectionUtils.unwrapGeneratedSubclass;

interface Utils {

    @SuppressWarnings("unchecked")
    static <T> Class<T> classOf(T object) {
        return (Class<T>) unwrapGeneratedSubclass(object.getClass());
    }

}

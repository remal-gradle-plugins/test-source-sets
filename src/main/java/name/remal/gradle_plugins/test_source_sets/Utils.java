package name.remal.gradle_plugins.test_source_sets;

import static java.util.Collections.emptyMap;
import static java.util.Collections.emptySet;
import static name.remal.gradle_plugins.toolkit.reflection.ReflectionUtils.unwrapGeneratedSubclass;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

interface Utils {

    @SuppressWarnings("unchecked")
    static <T> Class<T> classOf(T object) {
        return (Class<T>) unwrapGeneratedSubclass(object.getClass());
    }

    static <T, E> void adjustSetProperty(
        T object,
        Function<T, Set<E>> getter,
        BiConsumer<T, Set<E>> setter,
        Consumer<Set<E>> action
    ) {
        var set = new LinkedHashSet<>(
            Optional.ofNullable(getter.apply(object)).orElse(emptySet())
        );
        action.accept(set);
        setter.accept(object, set);
    }

    static <T, K, V> void adjustMapProperty(
        T object,
        Function<T, Map<K, V>> getter,
        BiConsumer<T, Map<K, V>> setter,
        Consumer<Map<K, V>> action
    ) {
        var map = new LinkedHashMap<>(
            Optional.ofNullable(getter.apply(object)).orElse(emptyMap())
        );
        action.accept(map);
        setter.accept(object, map);
    }

    static <K, E> void adjustMapSetValue(
        Map<K, Iterable<E>> map,
        K key,
        Consumer<Set<E>> action
    ) {
        Set<E> set = new LinkedHashSet<>();
        Optional.ofNullable(map.get(key))
            .ifPresent(it -> it.forEach(set::add));

        action.accept(set);

        map.put(key, set);
    }

}

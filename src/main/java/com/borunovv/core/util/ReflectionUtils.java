package com.borunovv.core.util;

import org.reflections.Reflections;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.Set;

/**
 * Хелпер по созданию экземпляров классов и сканированию пакетов.
 */
public final class ReflectionUtils {

    /**
     * Создает экземпляр заданного класса.
     * Убедитесь, что clazz имеет пустой public-конструктор
     * и не является подклассом др. класса (кроме статических подклассов).
     */
    public static Object createInstance(Class<?> clazz) {
        try {
            return clazz.newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            throw new RuntimeException("Failed to instantiate class '" + clazz.getName() + "'", e);
        }
    }

    /**
     * Сканирует заданный пакет, ищет классы с заданной аннотацией.
     */
    public static Set<Class<?>> scanAnnotatedClasses(String packageName,
                                                     Class<? extends Annotation> annotation,
                                                     boolean ensureHasEmptyCtor) {
        Set<Class<?>> result = new Reflections(packageName).getTypesAnnotatedWith(annotation);
        if (ensureHasEmptyCtor) {
            for (Class<?> clazz : result) {
                ensureClassHasPublicEmptyCtor(clazz);
            }
        }
        return result;
    }

    private static void ensureClassHasPublicEmptyCtor(Class<?> clazz) {
        for (Constructor<?> ctor : clazz.getConstructors()) {
            if (ctor.getParameterTypes().length == 0 && Modifier.isPublic(ctor.getModifiers())) {
                return;
            }
        }
        throw new RuntimeException("Class '" + clazz.getName() + "' must have public empty c-tor!");
    }
}

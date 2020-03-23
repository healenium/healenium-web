package com.epam.healenium.utils;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;

@Slf4j
@UtilityClass
public class StackUtils {

    /**
     *
     * @return
     */
    public boolean isAnnotationPresent(Class<? extends Annotation> aClass){
        StackTraceElement[] trace = Thread.currentThread().getStackTrace();
        return findAnnotatedInTrace(trace, aClass).isPresent();
    }

    /**
     *
     * @param elements
     * @param targetClass
     * @return
     */
    public Optional<StackTraceElement> getElementByClass(StackTraceElement[] elements, String targetClass) {
        return Arrays.stream(elements)
                .filter(redundantPackages())
                .filter(element -> {
                    String className = element.getClassName();
                    String simpleClassName = className.substring(className.lastIndexOf('.') + 1);
                    return simpleClassName.equals(targetClass);
                })
                .findFirst();
    }

    /**
     * Lookup for method annotated as restorable in the stack trace. The first occurrence will be taken
     *
     * @param elements that represent invocation stack trace
     * @return
     */
    private Optional<StackTraceElement> findAnnotatedInTrace(StackTraceElement[] elements, Class<? extends Annotation> clazz) {
        return Arrays.stream(elements)
                .filter(redundantPackages())
                .filter(it -> {
                    try {
                        Class<?> aClass = Class.forName(it.getClassName());
                        String methodName = it.getMethodName();
                        return Arrays.stream(aClass.getMethods())
                                .filter(m -> m.getName().equals(methodName))
                                .anyMatch(m -> {
                                    for (Annotation annotation : m.getDeclaredAnnotations()) {
                                        if (clazz.isInstance(annotation)) {
                                            log.debug("Found at ={},{}", it.getClassName(), methodName);
                                            return true;
                                        }
                                    }
                                    return false;
                                });
                    } catch (ClassNotFoundException ex) {
                        log.warn("Failed to check class: {}", it.getClassName());
                        return false;
                    }
                })
                .findFirst();
    }

    /**
     *
     * @return
     */
    private Predicate<StackTraceElement> redundantPackages() {
        return value -> {
            Stream<String> skippingPackageStream = Stream.of("sun.reflect", "java.lang", "org.gradle", "org.junit", "java.util", "com.sun", "com.google");
            return skippingPackageStream.noneMatch(s -> value.getClassName().startsWith(s));
        };
    }

}

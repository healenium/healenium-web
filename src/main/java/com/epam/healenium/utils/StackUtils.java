/**
 * Healenium-web Copyright (C) 2019 EPAM
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.epam.healenium.utils;

import com.google.common.collect.ImmutableMap;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.By;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

@Slf4j(topic = "healenium")
@UtilityClass
public class StackUtils {

    public static final Map<String, Function<String, By>> BY_MAP = ImmutableMap.<String, Function<String, By>>builder()
            .put("By.className", By::className)
            .put("By.cssSelector", By::cssSelector)
            .put("By.xpath", By::xpath)
            .put("By.tagName", By::tagName)
            .put("By.name", By::name)
            .put("By.partialLinkText", By::partialLinkText)
            .put("By.linkText", By::linkText)
            .put("By.id", By::id)
            .build();

    /**
     * @param aClass annotation class
     * @return true of false
     */
    public boolean isAnnotationPresent(Class<? extends Annotation> aClass) {
        StackTraceElement[] trace = Thread.currentThread().getStackTrace();
        return findAnnotatedInTrace(trace, aClass).isPresent();
    }

    /**
     * @param elements    list of StackTraceElements
     * @param targetClass targetClass
     * @return StackTraceElement
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

    public Predicate<StackTraceElement> redundantPackages() {
        return value -> {
            Stream<String> skippingPackageStream = Stream.of("java.base", "sun.reflect", "java.lang", "org.gradle",
                    "org.junit", "java.util", "com.sun", "com.google", "jdk.internal", "org.openqa", "com.codeborne",
                    "ru.yandex", "jdk.proxy2", "io.appium", "jdk.proxy1");
            return skippingPackageStream.noneMatch(s -> value.getClassName().startsWith(s));
        };
    }



}

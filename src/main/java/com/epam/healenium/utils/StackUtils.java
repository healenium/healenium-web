/**
 * Healenium-web Copyright (C) 2019 EPAM
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *        http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.epam.healenium.utils;

import com.epam.healenium.handlers.proxy.BaseHandler;
import com.google.common.collect.Iterables;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import one.util.streamex.StreamEx;
import org.apache.commons.lang3.StringUtils;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;
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

    public Optional<StackTraceElement> findOriginCaller(){
        StackTraceElement[] trace = Thread.currentThread().getStackTrace();
        return findOriginCaller(trace);
    }

    public Optional<StackTraceElement> findOriginCaller(StackTraceElement[] elements){
        List<StackTraceElement> elementList = normalize(elements);
        String callerName = getCallerPackageName(elementList);
        if(StringUtils.isBlank(callerName)) return Optional.empty();
        return elementList.stream()
                .filter(it -> it.getClassName().startsWith(callerName))
                .findFirst();
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
            Stream<String> skippingPackageStream = Stream.of("java.base","sun.reflect", "java.lang", "org.gradle", "org.junit", "java.util", "com.sun", "com.google","jdk.internal","org.openqa");
            return skippingPackageStream.noneMatch(s -> value.getClassName().startsWith(s));
        };
    }

    private List<StackTraceElement> normalize(StackTraceElement[] traceElements){
        List<StackTraceElement> elementList = Arrays.stream(traceElements)
                .filter(redundantPackages())
                .collect(Collectors.toList());
        Collections.reverse(elementList);
        elementList = StreamEx.of(elementList)
                .takeWhile(it-> !it.getClassName().equals(BaseHandler.class.getName()))
                .toList();
        return elementList.subList(0, elementList.size() -1);
    }

    private String getCallerPackageName(List<StackTraceElement> traceElements){
        String result = "";
        try{
            StackTraceElement element = Iterables.getLast(traceElements);
            String className = element.getClassName();
            int dotPos = lastDotPosition(className);
            result = element.getClassName().substring(0, Math.max(dotPos,0));
        } catch (Exception ex){
            log.warn("Failed to find caller package name", ex);
        }
        return result;
    }

    private int lastDotPosition(String input){
        int dot1 = input.indexOf(".");
        int dot2 = input.indexOf(".", dot1 + 1);
        return Math.max(dot1, dot2);
    }
}

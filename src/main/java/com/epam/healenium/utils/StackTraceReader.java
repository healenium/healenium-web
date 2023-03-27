package com.epam.healenium.utils;

import com.epam.healenium.handlers.proxy.SelfHealingProxyInvocationHandler;
import com.epam.healenium.handlers.proxy.WebElementProxyHandler;
import com.google.common.collect.Iterables;
import lombok.extern.slf4j.Slf4j;
import one.util.streamex.StreamEx;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j(topic = "healenium")
public class StackTraceReader {

    /**
     * @param elements - StackTrace Elements by Caller
     * @return caller method
     */
    public Optional<StackTraceElement> findOriginCaller(StackTraceElement[] elements) {
        List<StackTraceElement> elementList = normalize(elements);
        String callerName = getCallerPackageName(elementList);
        if (StringUtils.isBlank(callerName)) return Optional.empty();
        Collections.reverse(elementList);
        return elementList.stream()
                .filter(it -> it.getClassName().startsWith(callerName))
                .findFirst();
    }

    /**
     * @param traceElements - StackTrace Elements by Caller
     * @return sorted and filtered list Stack Trace Elements
     */
    private List<StackTraceElement> normalize(StackTraceElement[] traceElements) {
        List<StackTraceElement> elementList = Arrays.stream(traceElements)
                .filter(StackUtils.redundantPackages())
                .collect(Collectors.toList());
        Collections.reverse(elementList);
        elementList = StreamEx.of(elementList)
                .takeWhile(it -> !getProxyHandlerNames().contains(it.getClassName()))
                .toList();
        return elementList.subList(0, elementList.size());
    }

    private String getCallerPackageName(List<StackTraceElement> traceElements) {
        String result = "";
        try {
            StackTraceElement element = Iterables.getLast(traceElements);
            String className = element.getClassName();
            int dotPos = lastDotPosition(className);
            result = dotPos == -1 ? className : element.getClassName().substring(0, Math.max(dotPos, 0));
        } catch (Exception ex) {
            log.warn("Failed to find caller package name", ex);
        }
        return result;
    }

    public List<String> getProxyHandlerNames() {
        return Arrays.asList(SelfHealingProxyInvocationHandler.class.getName(), WebElementProxyHandler.class.getName());
    }

    private int lastDotPosition(String input) {
        int dot1 = input.indexOf(".");
        int dot2 = input.indexOf(".", dot1 + 1);
        return Math.max(dot1, dot2);
    }
}

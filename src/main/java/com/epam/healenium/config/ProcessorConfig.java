package com.epam.healenium.config;

import com.epam.healenium.processor.BaseProcessor;
import com.epam.healenium.processor.FillMetricsProcessor;
import com.epam.healenium.processor.FindChildElementProcessor;
import com.epam.healenium.processor.FindChildElementsProcessor;
import com.epam.healenium.processor.FindElementProcessor;
import com.epam.healenium.processor.FindElementsProcessor;
import com.epam.healenium.processor.GetReferenceElementsProcessor;
import com.epam.healenium.processor.HealingElementsProcessor;
import com.epam.healenium.processor.HealingProcessor;
import com.epam.healenium.processor.ImitateProcessor;
import com.epam.healenium.processor.SaveHealingResultsProcessor;
import lombok.SneakyThrows;

import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.Collections;

/**
 * Main purpose - create chain of corresponding processors for each case (findElement/findElements)
 */
public class ProcessorConfig {

    public static BaseProcessor findElementChainProcessor() {
        return buildChain(
                FindElementProcessor.class,
                GetReferenceElementsProcessor.class,
                HealingProcessor.class,
                ImitateProcessor.class,
                FillMetricsProcessor.class,
                SaveHealingResultsProcessor.class
        );
    }

    public static BaseProcessor findElementsChainProcessor() {
        return buildChain(
                FindElementsProcessor.class,
                GetReferenceElementsProcessor.class,
                HealingElementsProcessor.class,
                FillMetricsProcessor.class,
                SaveHealingResultsProcessor.class);
    }

    public static BaseProcessor findChildElementChainProcessor() {
        return buildChain(
                FindChildElementProcessor.class,
                GetReferenceElementsProcessor.class,
                HealingProcessor.class,
                ImitateProcessor.class,
                FillMetricsProcessor.class,
                SaveHealingResultsProcessor.class
        );
    }

    public static BaseProcessor findChildElementsChainProcessor() {
        return buildChain(
                FindChildElementsProcessor.class,
                GetReferenceElementsProcessor.class,
                HealingElementsProcessor.class,
                FillMetricsProcessor.class,
                SaveHealingResultsProcessor.class);
    }

    /**
     * @param clazz list of streamlined classes for build processor's chain
     * @return BaseProcessor root processor
     */
    @SneakyThrows
    private static BaseProcessor buildChain(Class<?>... clazz) {
        Collections.reverse(Arrays.asList(clazz));
        Constructor<?> constructor;
        BaseProcessor param = null;
        for (Class<?> aClass : clazz) {
            constructor = aClass.getConstructor(BaseProcessor.class);
            param = (BaseProcessor) constructor.newInstance(param);
        }
        return param;
    }
}

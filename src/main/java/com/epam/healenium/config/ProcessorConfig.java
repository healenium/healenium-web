package com.epam.healenium.config;

import com.epam.healenium.processor.BaseProcessor;
import com.epam.healenium.processor.FillMetricsProcessor;
import com.epam.healenium.processor.FindChildElementProcessor;
import com.epam.healenium.processor.FindChildElementsProcessor;
import com.epam.healenium.processor.FindElementProcessor;
import com.epam.healenium.processor.FindElementsProcessor;
import com.epam.healenium.processor.GetLastHealingDataProcessor;
import com.epam.healenium.processor.HealingElementsProcessor;
import com.epam.healenium.processor.HealingProcessor;
import com.epam.healenium.processor.ImitateProcessor;
import com.epam.healenium.processor.SaveHealingResultsProcessor;
import com.epam.healenium.processor.SaveSelectorsProcessor;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Collections;


public class ProcessorConfig {

    public BaseProcessor findElementChainProcessor() throws InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        return buildChain(
                FindElementProcessor.class,
                GetLastHealingDataProcessor.class,
                HealingProcessor.class,
                ImitateProcessor.class,
                FillMetricsProcessor.class,
                SaveHealingResultsProcessor.class
        );
    }

    public BaseProcessor findElementsChainProcessor() throws InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        return buildChain(
                FindElementsProcessor.class,
                GetLastHealingDataProcessor.class,
                HealingElementsProcessor.class,
                SaveSelectorsProcessor.class,
                FillMetricsProcessor.class,
                SaveHealingResultsProcessor.class);
    }

    public BaseProcessor findChildElementChainProcessor() throws InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        return buildChain(
                FindChildElementProcessor.class,
                GetLastHealingDataProcessor.class,
                HealingProcessor.class,
                ImitateProcessor.class,
                FillMetricsProcessor.class,
                SaveHealingResultsProcessor.class
        );
    }

    public BaseProcessor findChildElementsChainProcessor() throws InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        return buildChain(
                FindChildElementsProcessor.class,
                GetLastHealingDataProcessor.class,
                HealingElementsProcessor.class,
                SaveSelectorsProcessor.class,
                FillMetricsProcessor.class,
                SaveHealingResultsProcessor.class);
    }

    /**
     * @param clazz list of streamlined classes for build processor's chain
     * @return BaseProcessor root processor
     */
    private BaseProcessor buildChain(Class<?>... clazz) throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
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

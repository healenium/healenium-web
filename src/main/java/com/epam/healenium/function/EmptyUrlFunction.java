package com.epam.healenium.function;

import com.epam.healenium.SelfHealingEngine;
import org.apache.commons.lang3.StringUtils;

import java.util.function.BiFunction;

public class EmptyUrlFunction implements BiFunction<SelfHealingEngine, String, String> {

    @Override
    public String apply(SelfHealingEngine engine, String url) {
        return StringUtils.EMPTY;
    }
}

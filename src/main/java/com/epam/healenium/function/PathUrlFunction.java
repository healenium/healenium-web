package com.epam.healenium.function;

import com.epam.healenium.SelfHealingEngine;

import java.util.function.BiFunction;

public class PathUrlFunction implements BiFunction<SelfHealingEngine, String, String> {

    @Override
    public String apply(SelfHealingEngine engine, String url) {
        if (url == null) {
            url = engine.getCurrentUrl();
        }
        return url.substring(url.indexOf("/"));
    }
}
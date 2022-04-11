package com.epam.healenium.handlers.processor;


public interface ProcessorHandler {

    /**
     * validate data before run 'execute' method. And skip whole chain in case fail validate
     * @return
     * validating result
     */
    boolean validate();

    /**
     * main method for each processor
     */
    void execute();
}

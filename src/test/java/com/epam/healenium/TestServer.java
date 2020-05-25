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
package com.epam.healenium;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.DefaultHandler;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.util.resource.Resource;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

@Slf4j
public class TestServer implements BeforeAllCallback, AfterAllCallback {

    private final String folder;
    private final int port;
    private Server server;

    /**
     * @param folder the folder that contains server root resources, like index.html
     * @param port a port application should be run on
     */
    public TestServer(String folder, int port) {
        this.folder = folder;
        this.port = port;
    }

    public TestServer(String folder) {
        this(folder, 8090);
    }

    public int getPort() {
        return port;
    }

    public String getPageName() {
        return folder;
    }

    @Override
    public void beforeAll(ExtensionContext context) {
        ResourceHandler resourceHandler = new ResourceHandler();
        resourceHandler.setBaseResource(Resource.newClassPathResource(folder));
        resourceHandler.setWelcomeFiles(new String[]{"index.html"});
        resourceHandler.setDirectoriesListed(true);
        server = new Server(port);
        HandlerList handlers = new HandlerList(resourceHandler, new DefaultHandler());
        server.setHandler(handlers);
        try {
            server.start();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void afterAll(ExtensionContext context) {
        try {
            server.stop();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}

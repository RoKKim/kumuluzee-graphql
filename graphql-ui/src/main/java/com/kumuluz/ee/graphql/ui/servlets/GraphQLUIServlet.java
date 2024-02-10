/*
 *  Copyright (c) 2014-2018 Kumuluz and/or its affiliates
 *  and other contributors as indicated by the @author tags and
 *  the contributor list.
 *
 *  Licensed under the MIT License (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  https://opensource.org/licenses/MIT
 *
 *  The software is provided "AS IS", WITHOUT WARRANTY OF ANY KIND, express or
 *  implied, including but not limited to the warranties of merchantability,
 *  fitness for a particular purpose and noninfringement. in no event shall the
 *  authors or copyright holders be liable for any claim, damages or other
 *  liability, whether in an action of contract, tort or otherwise, arising from,
 *  out of or in connection with the software or the use or other dealings in the
 *  software. See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.kumuluz.ee.graphql.ui.servlets;

import com.kumuluz.ee.configuration.utils.ConfigurationUtil;
import com.kumuluz.ee.graphql.ui.utils.GraphQLUIUtils;

import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.io.ByteArrayOutputStream;
import java.util.ResourceBundle;

/**
 * GraphQLUIServlet class - HttpServlet, that serves graphiql.html to user
 *
 * @author Domen Kajdic
 * @since 1.0.0
 */
public class GraphQLUIServlet extends HttpServlet {

    private String graphQlPath = null;
    private String graphQlUiPath = null;
    private static final ResourceBundle versionsBundle = ResourceBundle
            .getBundle("META-INF/kumuluzee/graphql-ui/versions");

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        ConfigurationUtil configurationUtil = ConfigurationUtil.getInstance();
        String contextPath = configurationUtil.get("kumuluzee.server.context-path").orElse("");

        if (this.graphQlPath == null) {
            this.graphQlPath = initializePath(resp, contextPath, "kumuluzee.graphql.mapping", "graphql");
            if (this.graphQlPath == null) {
                return;
            }
        }

        if (this.graphQlUiPath == null) {
            this.graphQlUiPath = initializePath(resp, contextPath, "kumuluzee.graphql.ui.mapping", "graphiql");
            if (this.graphQlUiPath == null) {
                return;
            }
        }
        if (req.getPathInfo() == null) {
            // no trailing slash, redirect to trailing slash in order to fix relative requests
            resp.sendRedirect(req.getContextPath() + req.getServletPath() + "/");
            return;
        }

        // if the request is at base path, send index.html
        if (req.getPathInfo().equals("/")) {
            sendFile(resp, "index.html", graphQlPath, graphQlUiPath);
        } else {
            String filePath = req.getPathInfo();
            if (filePath.startsWith("/")) {
                filePath = filePath.substring(1);
            }
            sendFile(resp, filePath, graphQlPath, graphQlUiPath);
        }
    }

    private String initializePath(HttpServletResponse resp, String contextPath, String configKey,
                                  String defaultMapping) throws IOException {
        String path = GraphQLUIUtils.getPath(configKey, defaultMapping);
        path = contextPath + path;

        try {
            URI u = new URI(path);
            if (u.isAbsolute()) {
                resp.getWriter().println("URL must be relative : " + path + ". Extension not initialized.");
                return null;
            }
        } catch (Exception e) {
            resp.getWriter().println("Malformed url: " + path + ". Extension not initialized.");
            return null;
        }

        return path;
    }

    private void sendFile(HttpServletResponse resp, String file, String graphQlPath,
                          String graphQlUiPath) throws IOException {
        InputStream in;
        // Determine the correct resource path based on the file name
        if ("logo.png".equals(file)) {
            in = this.getClass().getResourceAsStream("/html/" + file);
        } else if ("favicon.ico".equals(file)) {
            in = this.getClass().getResourceAsStream("/html/favicon-32x32.png");
        } else {
            // files are loaded resource folder of smallrye-graphql-ui dependency
            in = this.getClass().getResourceAsStream("/META-INF/resources/graphql-ui/" + file);
        }
        OutputStream out = resp.getOutputStream();

        byte[] buffer = new byte[10000];
        int length;

        if ("render.js".equals(file) || "index.html".equals(file)) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            while ((length = in.read(buffer)) > 0) {
                baos.write(buffer, 0, length);
            }
            baos.flush();

            String content = baos.toString();

            if ("render.js".equals(file)) {
                content = content.replace("alt='SmallRye Graphql'", "alt='KumuluzEE GraphQL'");
                if (graphQlPath != null) {
                    content = content.replace("const api = '/graphql';", "const api = '" + graphQlPath + "';");
                }
                if (graphQlUiPath != null) {
                    content = content.replace("const logo = '/graphql-ui';", "const logo = '" + graphQlUiPath + "';");
                }
            } else if ("index.html".equals(file)) {
                content = content.replace(String.format("<title>SmallRye GraphQL (v%s)</title>",
                                versionsBundle.getString("smallrye-graphql-version")),
                        "<title>KumuluzEE GraphiQL</title>");
            }

            // write the modified content to the output stream
            out.write(content.getBytes());
        } else {
            // directly write other files to the output stream
            while ((length = in.read(buffer)) > 0) {
                out.write(buffer, 0, length);
            }
        }

        in.close();
        out.close();
    }
}

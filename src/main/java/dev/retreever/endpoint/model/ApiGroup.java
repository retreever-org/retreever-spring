/*
 * Copyright (c) 2025 Retreever Contributors
 *
 * Licensed under the MIT License.
 * You may obtain a copy of the License at:
 *     https://opensource.org/licenses/MIT
 */

package dev.retreever.endpoint.model;

import java.util.List;

/**
 * Represents a documentation group derived from a controller.
 * Groups sharing the same name are merged in the final view.
 */
public class ApiGroup {

    private String name;
    private String description;
    private List<ApiEndpoint> endpoints;

    private boolean deprecated = false;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<ApiEndpoint> getEndpoints() {
        return endpoints;
    }

    public void setEndpoints(List<ApiEndpoint> endpoints) {
        this.endpoints = endpoints;
    }

    public boolean isDeprecated() {
        return deprecated;
    }

    public void deprecate() {
        this.deprecated = true;
    }
}

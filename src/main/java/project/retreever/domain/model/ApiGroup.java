/*
 * Copyright (c) 2025 Retreever Contributors
 *
 * Licensed under the MIT License.
 * You may obtain a copy of the License at:
 *     https://opensource.org/licenses/MIT
 */

package project.retreever.domain.model;

import java.util.List;

/**
 * Controller level Documentation
 */
public class ApiGroup {
    /**
     * Groups with the same name will be merged in view.
     */
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

package project.retreever.domain.model;

import java.util.List;

/**
 * App Level Documentation
 */
public class ApiDoc {
    private String name; // default is class name of @SpringBootApplication
    private String description;
    private String version;
    private String uriPrefix; // prefix on servlet context level if present, Controller Level prefixed are resolved while resolving endpoints.
    private List<ApiGroup> groups;

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

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getUriPrefix() {
        return uriPrefix;
    }

    public void setUriPrefix(String uriPrefix) {
        this.uriPrefix = uriPrefix;
    }

    public List<ApiGroup> getGroups() {
        return groups;
    }

    public void setGroups(List<ApiGroup> groups) {
        this.groups = groups;
    }
}

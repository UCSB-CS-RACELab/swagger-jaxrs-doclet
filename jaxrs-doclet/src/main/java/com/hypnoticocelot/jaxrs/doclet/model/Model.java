package com.hypnoticocelot.jaxrs.doclet.model;

import com.google.common.base.Objects;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class Model {

    private String id;
    private Map<String, Property> properties;
    private Set<String> required = new HashSet<String>();

    public Model() {
    }

    public Model(String id, Map<String, Property> properties) {
        this.id = id;
        this.properties = properties;
        this.required.addAll(properties.keySet());
    }

    public String getId() {
        return id;
    }

    public void markAsOptional(String property) {
        this.required.remove(property);
    }

    public Map<String, Property> getProperties() {
        return properties;
    }

    public Set<String> getRequired() {
        return required;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Model other = (Model) o;
        return Objects.equal(id, other.id)
                && Objects.equal(properties, other.properties);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id, properties);
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(this)
                .add("id", id)
                .add("properties", properties)
                .toString();
    }
}

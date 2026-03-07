package dev.prathya.core.model;

import java.time.LocalDate;

public class ModuleInfo {

    private String id;
    private String name;
    private String description;
    private String owner;
    private LocalDate created;
    private String version;

    public ModuleInfo() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getOwner() { return owner; }
    public void setOwner(String owner) { this.owner = owner; }

    public LocalDate getCreated() { return created; }
    public void setCreated(LocalDate created) { this.created = created; }

    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }
}

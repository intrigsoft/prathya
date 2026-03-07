package dev.prathya.core.mutator;

import java.util.List;

/**
 * Partial-update DTO for modifying a requirement. Null fields are left unchanged.
 */
public class RequirementUpdate {

    private String title;
    private String description;
    private String version;
    private List<String> acceptanceCriteria;
    private String changelogNote;

    public RequirementUpdate() {}

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }

    public List<String> getAcceptanceCriteria() { return acceptanceCriteria; }
    public void setAcceptanceCriteria(List<String> acceptanceCriteria) { this.acceptanceCriteria = acceptanceCriteria; }

    public String getChangelogNote() { return changelogNote; }
    public void setChangelogNote(String changelogNote) { this.changelogNote = changelogNote; }
}

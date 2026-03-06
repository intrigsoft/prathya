package dev.pratya.core.model;

import java.util.ArrayList;
import java.util.List;

public class RequirementDefinition {

    private String id;
    private String version;
    private String title;
    private String description;
    private RequirementStatus status;
    private String supersedes;
    private String supersededBy;
    private List<String> acceptanceCriteria = new ArrayList<>();
    private List<CornerCase> cornerCases = new ArrayList<>();
    private List<ChangelogEntry> changelog = new ArrayList<>();

    public RequirementDefinition() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public RequirementStatus getStatus() { return status; }
    public void setStatus(RequirementStatus status) { this.status = status; }

    public String getSupersedes() { return supersedes; }
    public void setSupersedes(String supersedes) { this.supersedes = supersedes; }

    public String getSupersededBy() { return supersededBy; }
    public void setSupersededBy(String supersededBy) { this.supersededBy = supersededBy; }

    public List<String> getAcceptanceCriteria() { return acceptanceCriteria; }
    public void setAcceptanceCriteria(List<String> acceptanceCriteria) { this.acceptanceCriteria = acceptanceCriteria; }

    public List<CornerCase> getCornerCases() { return cornerCases; }
    public void setCornerCases(List<CornerCase> cornerCases) { this.cornerCases = cornerCases; }

    public List<ChangelogEntry> getChangelog() { return changelog; }
    public void setChangelog(List<ChangelogEntry> changelog) { this.changelog = changelog; }
}

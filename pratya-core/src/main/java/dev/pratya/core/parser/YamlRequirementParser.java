package dev.pratya.core.parser;

import dev.pratya.core.PratyaException;
import dev.pratya.core.model.*;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Parses {@code REQUIREMENT.yaml} using SnakeYAML and maps to the domain model.
 */
public class YamlRequirementParser implements RequirementParser {

    private static final Pattern REQ_ID_PATTERN = Pattern.compile("^[A-Z]+-\\d+$");
    private static final Pattern CC_ID_PATTERN = Pattern.compile("^[A-Z]+-\\d+-CC-\\d+$");

    @Override
    public ModuleContract parse(Path requirementYaml) throws PratyaException {
        Map<String, Object> root;
        try (InputStream in = Files.newInputStream(requirementYaml)) {
            Yaml yaml = new Yaml();
            root = yaml.load(in);
        } catch (IOException e) {
            throw new PratyaException("Failed to read " + requirementYaml, e);
        }

        if (root == null) {
            throw new PratyaException("Empty YAML file: " + requirementYaml);
        }

        ModuleInfo module = parseModule(root);
        List<RequirementDefinition> requirements = parseRequirements(root, module.getId());

        return new ModuleContract(module, requirements);
    }

    @SuppressWarnings("unchecked")
    private ModuleInfo parseModule(Map<String, Object> root) throws PratyaException {
        Object moduleObj = root.get("module");
        if (!(moduleObj instanceof Map)) {
            throw new PratyaException("Missing or invalid 'module' section");
        }
        Map<String, Object> m = (Map<String, Object>) moduleObj;

        ModuleInfo info = new ModuleInfo();
        info.setId(requireString(m, "id", "module"));
        info.setName(requireString(m, "name", "module"));
        info.setDescription(optionalString(m, "description"));
        info.setOwner(optionalString(m, "owner"));
        info.setVersion(optionalString(m, "version"));

        Object created = m.get("created");
        info.setCreated(toLocalDate(created));

        return info;
    }

    @SuppressWarnings("unchecked")
    private List<RequirementDefinition> parseRequirements(Map<String, Object> root, String moduleId) throws PratyaException {
        Object reqsObj = root.get("requirements");
        if (reqsObj == null) {
            return Collections.emptyList();
        }
        if (!(reqsObj instanceof List)) {
            throw new PratyaException("'requirements' must be a list");
        }

        List<RequirementDefinition> result = new ArrayList<>();
        for (Object item : (List<?>) reqsObj) {
            if (!(item instanceof Map)) {
                throw new PratyaException("Each requirement must be a map");
            }
            result.add(parseRequirement((Map<String, Object>) item, moduleId));
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private RequirementDefinition parseRequirement(Map<String, Object> m, String moduleId) throws PratyaException {
        RequirementDefinition req = new RequirementDefinition();

        String id = requireString(m, "id", "requirement");
        validateRequirementId(id, moduleId);
        req.setId(id);

        req.setVersion(optionalString(m, "version"));
        req.setTitle(requireString(m, "title", "requirement " + id));
        req.setDescription(optionalString(m, "description"));
        req.setSupersedes(optionalString(m, "supersedes"));
        req.setSupersededBy(optionalString(m, "superseded_by"));

        String status = optionalString(m, "status");
        if (status != null) {
            req.setStatus(RequirementStatus.valueOf(status.toUpperCase()));
        } else {
            req.setStatus(RequirementStatus.DRAFT);
        }

        Object criteria = m.get("acceptance_criteria");
        if (criteria instanceof List) {
            List<String> ac = new ArrayList<>();
            for (Object c : (List<?>) criteria) {
                ac.add(String.valueOf(c));
            }
            req.setAcceptanceCriteria(ac);
        }

        Object cornerCasesObj = m.get("corner_cases");
        if (cornerCasesObj instanceof List) {
            List<CornerCase> ccs = new ArrayList<>();
            for (Object ccObj : (List<?>) cornerCasesObj) {
                if (ccObj instanceof Map) {
                    Map<String, Object> ccMap = (Map<String, Object>) ccObj;
                    String ccId = requireString(ccMap, "id", "corner_case in " + id);
                    validateCornerCaseId(ccId, moduleId);
                    String desc = optionalString(ccMap, "description");
                    ccs.add(new CornerCase(ccId, desc));
                }
            }
            req.setCornerCases(ccs);
        }

        Object changelogObj = m.get("changelog");
        if (changelogObj instanceof List) {
            List<ChangelogEntry> entries = new ArrayList<>();
            for (Object clObj : (List<?>) changelogObj) {
                if (clObj instanceof Map) {
                    Map<String, Object> clMap = (Map<String, Object>) clObj;
                    ChangelogEntry entry = new ChangelogEntry();
                    entry.setVersion(optionalString(clMap, "version"));
                    entry.setNote(optionalString(clMap, "note"));
                    entry.setDate(toLocalDate(clMap.get("date")));

                    entries.add(entry);
                }
            }
            req.setChangelog(entries);
        }

        return req;
    }

    private void validateRequirementId(String id, String moduleId) throws PratyaException {
        if (!REQ_ID_PATTERN.matcher(id).matches()) {
            throw new PratyaException("Invalid requirement ID format: '" + id
                    + "'. Expected {MODULE}-{SEQ} (e.g. " + moduleId + "-001)");
        }
    }

    private void validateCornerCaseId(String id, String moduleId) throws PratyaException {
        if (!CC_ID_PATTERN.matcher(id).matches()) {
            throw new PratyaException("Invalid corner case ID format: '" + id
                    + "'. Expected {MODULE}-{SEQ}-CC-{N} (e.g. " + moduleId + "-001-CC-001)");
        }
    }

    private LocalDate toLocalDate(Object value) {
        if (value instanceof java.util.Date) {
            return ((java.util.Date) value).toInstant()
                    .atZone(ZoneId.systemDefault()).toLocalDate();
        } else if (value instanceof String) {
            return LocalDate.parse((String) value);
        }
        return null;
    }

    private String requireString(Map<String, Object> map, String key, String context) throws PratyaException {
        Object value = map.get(key);
        if (value == null) {
            throw new PratyaException("Missing required field '" + key + "' in " + context);
        }
        return String.valueOf(value);
    }

    private String optionalString(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value != null ? String.valueOf(value) : null;
    }
}

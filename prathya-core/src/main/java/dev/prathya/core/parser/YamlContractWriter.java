package dev.prathya.core.parser;

import dev.prathya.core.PrathyaException;
import dev.prathya.core.model.*;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.*;

/**
 * Serialises a {@link ModuleContract} to YAML using SnakeYAML,
 * mirroring the structure expected by {@link YamlRequirementParser}.
 */
public class YamlContractWriter implements ContractWriter {

    @Override
    public void write(ModuleContract contract, Path outputFile) throws PrathyaException {
        String yaml = toYaml(contract);
        try {
            Files.writeString(outputFile, yaml);
        } catch (IOException e) {
            throw new PrathyaException("Failed to write contract to " + outputFile, e);
        }
    }

    @Override
    public String toYaml(ModuleContract contract) throws PrathyaException {
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("module", buildModule(contract.getModule()));

        List<RequirementDefinition> reqs = contract.getRequirements();
        if (reqs != null && !reqs.isEmpty()) {
            List<Map<String, Object>> reqList = new ArrayList<>();
            for (RequirementDefinition req : reqs) {
                reqList.add(buildRequirement(req));
            }
            root.put("requirements", reqList);
        }

        DumperOptions opts = new DumperOptions();
        opts.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        opts.setWidth(120);
        opts.setDefaultScalarStyle(DumperOptions.ScalarStyle.PLAIN);

        Yaml yaml = new Yaml(opts);
        return yaml.dump(root);
    }

    private Map<String, Object> buildModule(ModuleInfo module) {
        Map<String, Object> m = new LinkedHashMap<>();
        putIfNotNull(m, "id", module.getId());
        putIfNotNull(m, "name", module.getName());
        putIfNotNull(m, "description", module.getDescription());
        putIfNotNull(m, "owner", module.getOwner());
        putIfNotNull(m, "created", dateToString(module.getCreated()));
        putIfNotNull(m, "version", module.getVersion());
        return m;
    }

    private Map<String, Object> buildRequirement(RequirementDefinition req) {
        Map<String, Object> m = new LinkedHashMap<>();
        putIfNotNull(m, "id", req.getId());
        putIfNotNull(m, "version", req.getVersion());
        if (req.getStatus() != null) {
            m.put("status", req.getStatus().name().toLowerCase());
        }
        putIfNotNull(m, "title", req.getTitle());
        putIfNotNull(m, "description", req.getDescription());
        putIfNotNull(m, "supersedes", req.getSupersedes());
        putIfNotNull(m, "superseded_by", req.getSupersededBy());

        if (req.getAcceptanceCriteria() != null && !req.getAcceptanceCriteria().isEmpty()) {
            m.put("acceptance_criteria", new ArrayList<>(req.getAcceptanceCriteria()));
        }

        if (req.getCornerCases() != null && !req.getCornerCases().isEmpty()) {
            List<Map<String, Object>> ccList = new ArrayList<>();
            for (CornerCase cc : req.getCornerCases()) {
                Map<String, Object> ccMap = new LinkedHashMap<>();
                putIfNotNull(ccMap, "id", cc.getId());
                putIfNotNull(ccMap, "description", cc.getDescription());
                if (cc.getTestEnvironment() != null) {
                    ccMap.put("test_environment", cc.getTestEnvironment().toYaml());
                }
                ccList.add(ccMap);
            }
            m.put("corner_cases", ccList);
        }

        if (req.getChangelog() != null && !req.getChangelog().isEmpty()) {
            List<Map<String, Object>> clList = new ArrayList<>();
            for (ChangelogEntry entry : req.getChangelog()) {
                Map<String, Object> clMap = new LinkedHashMap<>();
                putIfNotNull(clMap, "version", entry.getVersion());
                putIfNotNull(clMap, "date", dateToString(entry.getDate()));
                putIfNotNull(clMap, "note", entry.getNote());
                clList.add(clMap);
            }
            m.put("changelog", clList);
        }

        return m;
    }

    private static String dateToString(LocalDate date) {
        return date != null ? date.toString() : null;
    }

    private static void putIfNotNull(Map<String, Object> map, String key, Object value) {
        if (value != null) {
            map.put(key, value);
        }
    }
}

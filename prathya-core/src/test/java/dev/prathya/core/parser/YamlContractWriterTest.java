package dev.prathya.core.parser;

import dev.prathya.core.PrathyaException;
import dev.prathya.core.model.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class YamlContractWriterTest {

    private final YamlContractWriter writer = new YamlContractWriter();
    private final YamlRequirementParser parser = new YamlRequirementParser();

    @Test
    void roundTrip_parseThenWriteThenParse(@TempDir Path tmp) throws Exception {
        // Parse the sample contract
        Path sampleContract = Path.of("../samples/sample-maven/CONTRACT.yaml");
        ModuleContract original = parser.parse(sampleContract);

        // Write it out
        Path output = tmp.resolve("CONTRACT.yaml");
        writer.write(original, output);

        // Re-parse
        ModuleContract reparsed = parser.parse(output);

        // Assert structural equality
        assertEquals(original.getModule().getId(), reparsed.getModule().getId());
        assertEquals(original.getModule().getName(), reparsed.getModule().getName());
        assertEquals(original.getModule().getOwner(), reparsed.getModule().getOwner());
        assertEquals(original.getModule().getVersion(), reparsed.getModule().getVersion());
        assertEquals(original.getRequirements().size(), reparsed.getRequirements().size());

        for (int i = 0; i < original.getRequirements().size(); i++) {
            RequirementDefinition origReq = original.getRequirements().get(i);
            RequirementDefinition repReq = reparsed.getRequirements().get(i);
            assertEquals(origReq.getId(), repReq.getId());
            assertEquals(origReq.getTitle(), repReq.getTitle());
            assertEquals(origReq.getStatus(), repReq.getStatus());
            assertEquals(origReq.getAcceptanceCriteria(), repReq.getAcceptanceCriteria());
            assertEquals(origReq.getCornerCases().size(), repReq.getCornerCases().size());
            for (int j = 0; j < origReq.getCornerCases().size(); j++) {
                assertEquals(origReq.getCornerCases().get(j).getId(),
                        repReq.getCornerCases().get(j).getId());
                assertEquals(origReq.getCornerCases().get(j).getDescription(),
                        repReq.getCornerCases().get(j).getDescription());
            }
        }
    }

    @Test
    void toYaml_statusIsLowercase() throws Exception {
        ModuleContract contract = minimalContract();
        contract.getRequirements().get(0).setStatus(RequirementStatus.APPROVED);

        String yaml = writer.toYaml(contract);
        assertTrue(yaml.contains("status: approved"), "Status should be lowercase in YAML");
        assertFalse(yaml.contains("APPROVED"), "Status should not be uppercase");
    }

    @Test
    void toYaml_dateAsIsoString() throws Exception {
        ModuleContract contract = minimalContract();
        contract.getModule().setCreated(LocalDate.of(2026, 3, 7));

        String yaml = writer.toYaml(contract);
        assertTrue(yaml.contains("created: '2026-03-07'") || yaml.contains("created: 2026-03-07"),
                "Date should be ISO format string");
    }

    @Test
    void toYaml_nullFieldsOmitted() throws Exception {
        ModuleContract contract = minimalContract();
        contract.getRequirements().get(0).setDescription(null);
        contract.getRequirements().get(0).setSupersedes(null);

        String yaml = writer.toYaml(contract);
        assertFalse(yaml.contains("description:"), "Null description should be omitted");
        assertFalse(yaml.contains("supersedes:"), "Null supersedes should be omitted");
        assertFalse(yaml.contains("superseded_by:"), "Null superseded_by should be omitted");
    }

    @Test
    void toYaml_emptyRequirementList() throws Exception {
        ModuleContract contract = new ModuleContract();
        ModuleInfo module = new ModuleInfo();
        module.setId("TST");
        module.setName("Test");
        contract.setModule(module);
        contract.setRequirements(List.of());

        String yaml = writer.toYaml(contract);
        assertFalse(yaml.contains("requirements:"), "Empty requirements list should be omitted");
    }

    @Test
    void write_toFile(@TempDir Path tmp) throws Exception {
        ModuleContract contract = minimalContract();
        Path output = tmp.resolve("out.yaml");
        writer.write(contract, output);

        assertTrue(Files.exists(output));
        String content = Files.readString(output);
        assertTrue(content.contains("module:"));
        assertTrue(content.contains("requirements:"));
    }

    private ModuleContract minimalContract() {
        ModuleInfo module = new ModuleInfo();
        module.setId("TST");
        module.setName("Test Module");

        RequirementDefinition req = new RequirementDefinition();
        req.setId("TST-001");
        req.setTitle("Test requirement");
        req.setStatus(RequirementStatus.DRAFT);

        return new ModuleContract(module, new java.util.ArrayList<>(List.of(req)));
    }
}

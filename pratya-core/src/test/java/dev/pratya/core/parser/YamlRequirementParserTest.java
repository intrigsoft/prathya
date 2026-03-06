package dev.pratya.core.parser;

import dev.pratya.core.PratyaException;
import dev.pratya.core.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class YamlRequirementParserTest {

    private YamlRequirementParser parser;

    @BeforeEach
    void setUp() {
        parser = new YamlRequirementParser();
    }

    @Test
    void parseValidYaml() throws PratyaException {
        Path path = Paths.get("src/test/resources/valid-requirement.yaml");
        ModuleContract contract = parser.parse(path);

        // Module info
        ModuleInfo module = contract.getModule();
        assertEquals("AUTH", module.getId());
        assertEquals("Authentication Module", module.getName());
        assertEquals("team@example.com", module.getOwner());
        assertEquals("1.2.0", module.getVersion());

        // Requirements
        List<RequirementDefinition> reqs = contract.getRequirements();
        assertEquals(4, reqs.size());

        // AUTH-001
        RequirementDefinition auth001 = reqs.get(0);
        assertEquals("AUTH-001", auth001.getId());
        assertEquals("1.1.0", auth001.getVersion());
        assertEquals(RequirementStatus.APPROVED, auth001.getStatus());
        assertEquals("User login with email and password", auth001.getTitle());
        assertEquals(3, auth001.getAcceptanceCriteria().size());
        assertEquals(3, auth001.getCornerCases().size());
        assertEquals("AUTH-001-CC-001", auth001.getCornerCases().get(0).getId());
        assertEquals(2, auth001.getChangelog().size());

        // AUTH-003 — superseded
        RequirementDefinition auth003 = reqs.get(2);
        assertEquals("AUTH-003", auth003.getId());
        assertEquals(RequirementStatus.SUPERSEDED, auth003.getStatus());
        assertEquals("AUTH-005", auth003.getSupersededBy());

        // AUTH-005 — supersedes AUTH-003
        RequirementDefinition auth005 = reqs.get(3);
        assertEquals("AUTH-005", auth005.getId());
        assertEquals("AUTH-003", auth005.getSupersedes());
    }

    @Test
    void parseMissingModuleId_throwsException() {
        Path path = Paths.get("src/test/resources/missing-module-id.yaml");
        PratyaException ex = assertThrows(PratyaException.class, () -> parser.parse(path));
        assertTrue(ex.getMessage().contains("id"), "Should mention missing 'id' field");
    }

    @Test
    void parseInvalidRequirementId_throwsException() {
        Path path = Paths.get("src/test/resources/invalid-req-id.yaml");
        PratyaException ex = assertThrows(PratyaException.class, () -> parser.parse(path));
        assertTrue(ex.getMessage().contains("Invalid requirement ID format"),
                "Should report invalid ID format, got: " + ex.getMessage());
    }
}

package dev.pratya.mcp;

import dev.pratya.core.model.ModuleContract;
import dev.pratya.core.parser.YamlRequirementParser;
import dev.pratya.mcp.tools.WriteToolHandlers;
import io.modelcontextprotocol.spec.McpSchema;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class WriteToolHandlersTest {

    @TempDir
    Path tmpDir;
    private Path contractPath;
    private WriteToolHandlers handlers;
    private final YamlRequirementParser parser = new YamlRequirementParser();

    @BeforeEach
    void setUp() throws IOException {
        URL resource = getClass().getClassLoader().getResource("test-contract.yaml");
        assertNotNull(resource);
        contractPath = tmpDir.resolve("CONTRACT.yaml");
        Files.copy(Path.of(resource.getPath()), contractPath);

        PratyaServerConfig config = PratyaServerConfig.parse(new String[]{
                "--contract", contractPath.toString()
        });
        handlers = new WriteToolHandlers(config);
    }

    @Test
    void addRequirement_autoId() throws Exception {
        McpSchema.CallToolResult result = handlers.addRequirement(Map.of(
                "title", "New requirement"
        ));
        assertFalse(result.isError());
        String text = textContent(result);
        assertTrue(text.contains("TST-003"));

        // Verify persisted
        ModuleContract contract = parser.parse(contractPath);
        assertEquals(3, contract.getRequirements().size());
        assertEquals("TST-003", contract.getRequirements().get(2).getId());
    }

    @Test
    void addRequirement_explicitId() throws Exception {
        McpSchema.CallToolResult result = handlers.addRequirement(Map.of(
                "title", "Custom ID requirement",
                "id", "TST-010"
        ));
        assertFalse(result.isError());
        assertTrue(textContent(result).contains("TST-010"));
    }

    @Test
    void addRequirement_duplicateId() {
        McpSchema.CallToolResult result = handlers.addRequirement(Map.of(
                "title", "Duplicate",
                "id", "TST-001"
        ));
        assertTrue(result.isError());
    }

    @Test
    void updateRequirement() throws Exception {
        McpSchema.CallToolResult result = handlers.updateRequirement(Map.of(
                "id", "TST-001",
                "title", "Updated title",
                "note", "Title changed"
        ));
        assertFalse(result.isError());

        ModuleContract contract = parser.parse(contractPath);
        assertEquals("Updated title", contract.getRequirements().get(0).getTitle());
    }

    @Test
    void addCornerCase() throws Exception {
        McpSchema.CallToolResult result = handlers.addCornerCase(Map.of(
                "req_id", "TST-001",
                "description", "New edge case"
        ));
        assertFalse(result.isError());
        assertTrue(textContent(result).contains("TST-001-CC-002"));

        ModuleContract contract = parser.parse(contractPath);
        assertEquals(2, contract.getRequirements().get(0).getCornerCases().size());
    }

    @Test
    void updateCornerCase() throws Exception {
        McpSchema.CallToolResult result = handlers.updateCornerCase(Map.of(
                "req_id", "TST-001",
                "cc_id", "TST-001-CC-001",
                "description", "Updated edge case"
        ));
        assertFalse(result.isError());

        ModuleContract contract = parser.parse(contractPath);
        assertEquals("Updated edge case",
                contract.getRequirements().get(0).getCornerCases().get(0).getDescription());
    }

    @Test
    void deprecateRequirement() throws Exception {
        McpSchema.CallToolResult result = handlers.deprecateRequirement(Map.of(
                "id", "TST-001",
                "reason", "No longer needed"
        ));
        assertFalse(result.isError());

        ModuleContract contract = parser.parse(contractPath);
        assertEquals(dev.pratya.core.model.RequirementStatus.DEPRECATED,
                contract.getRequirements().get(0).getStatus());
    }

    @Test
    void supersedeRequirement() throws Exception {
        McpSchema.CallToolResult result = handlers.supersedeRequirement(Map.of(
                "old_id", "TST-001",
                "title", "Replacement requirement"
        ));
        assertFalse(result.isError());
        assertTrue(textContent(result).contains("TST-003"));

        ModuleContract contract = parser.parse(contractPath);
        assertEquals(3, contract.getRequirements().size());
        assertEquals(dev.pratya.core.model.RequirementStatus.SUPERSEDED,
                contract.getRequirements().get(0).getStatus());
    }

    private static String textContent(McpSchema.CallToolResult result) {
        return ((McpSchema.TextContent) result.content().get(0)).text();
    }
}

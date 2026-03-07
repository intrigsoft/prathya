package dev.prathya.mcp;

import dev.prathya.mcp.tools.ReadToolHandlers;
import io.modelcontextprotocol.spec.McpSchema;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ReadToolHandlersTest {

    private ReadToolHandlers handlers;

    @BeforeEach
    void setUp() {
        URL resource = getClass().getClassLoader().getResource("test-contract.yaml");
        assertNotNull(resource);
        Path contractPath = Paths.get(resource.getPath());
        PrathyaServerConfig config = PrathyaServerConfig.parse(new String[]{
                "--contract", contractPath.toString()
        });
        handlers = new ReadToolHandlers(config);
    }

    @Test
    void getContract_returnsModuleInfo() {
        McpSchema.CallToolResult result = handlers.getContract(Map.of());
        assertFalse(result.isError());
        String text = textContent(result);
        assertTrue(text.contains("TST"));
        assertTrue(text.contains("Test Service"));
        assertTrue(text.contains("Requirements: 2"));
    }

    @Test
    void listRequirements_all() {
        McpSchema.CallToolResult result = handlers.listRequirements(Map.of());
        assertFalse(result.isError());
        String text = textContent(result);
        assertTrue(text.contains("TST-001"));
        assertTrue(text.contains("TST-002"));
    }

    @Test
    void listRequirements_filteredByStatus() {
        McpSchema.CallToolResult result = handlers.listRequirements(Map.of("status", "DRAFT"));
        assertFalse(result.isError());
        String text = textContent(result);
        assertFalse(text.contains("TST-001"));
        assertTrue(text.contains("TST-002"));
    }

    @Test
    void getRequirement_found() {
        McpSchema.CallToolResult result = handlers.getRequirement(Map.of("id", "TST-001"));
        assertFalse(result.isError());
        String text = textContent(result);
        assertTrue(text.contains("First requirement"));
        assertTrue(text.contains("Criterion one"));
        assertTrue(text.contains("TST-001-CC-001"));
    }

    @Test
    void getRequirement_notFound() {
        McpSchema.CallToolResult result = handlers.getRequirement(Map.of("id", "TST-999"));
        assertTrue(result.isError());
    }

    @Test
    void validateContract_valid() {
        McpSchema.CallToolResult result = handlers.validateContract(Map.of());
        assertFalse(result.isError());
        String text = textContent(result);
        assertTrue(text.contains("valid"));
    }

    @Test
    void listUntested_noTestClasses() {
        // Without test-classes configured, all traced to empty → all untested
        McpSchema.CallToolResult result = handlers.listUntested(Map.of());
        assertFalse(result.isError());
    }

    @Test
    void getContract_customContractFile() {
        // Create handlers with no --contract flag (defaults to CONTRACT.yaml in cwd)
        ReadToolHandlers defaultHandlers = new ReadToolHandlers(
                PrathyaServerConfig.parse(new String[]{})
        );

        // Pass contract_file arg to override
        URL resource = getClass().getClassLoader().getResource("test-contract.yaml");
        assertNotNull(resource);
        McpSchema.CallToolResult result = defaultHandlers.getContract(
                Map.of("contract_file", Paths.get(resource.getPath()).toString())
        );
        assertFalse(result.isError());
        String text = textContent(result);
        assertTrue(text.contains("TST"));
        assertTrue(text.contains("Test Service"));
    }

    private static String textContent(McpSchema.CallToolResult result) {
        return ((McpSchema.TextContent) result.content().get(0)).text();
    }
}

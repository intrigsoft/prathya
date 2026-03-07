package dev.pratya.mcp;

import io.modelcontextprotocol.json.McpJsonDefaults;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.server.transport.StdioServerTransportProvider;
import io.modelcontextprotocol.spec.McpSchema.ServerCapabilities;

/**
 * Entry point for the Pratya MCP server.
 * Starts a sync MCP server over stdio that exposes 13 contract tools.
 */
public class PratyaMcpServer {

    public static void main(String[] args) {
        PratyaServerConfig config = PratyaServerConfig.parse(args);

        StdioServerTransportProvider transport =
                new StdioServerTransportProvider(McpJsonDefaults.getMapper());

        McpSyncServer server = McpServer.sync(transport)
                .serverInfo("pratya", "1.0.0")
                .capabilities(ServerCapabilities.builder()
                        .tools(true)
                        .build())
                .build();

        PratyaToolRegistry registry = new PratyaToolRegistry(config);
        registry.registerAll(server);

        // Server runs until stdin closes
    }
}

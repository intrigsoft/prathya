package com.intrigsoft.prathya.mcp;

import io.modelcontextprotocol.json.McpJsonDefaults;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.server.transport.StdioServerTransportProvider;
import io.modelcontextprotocol.spec.McpSchema.ServerCapabilities;

/**
 * Entry point for the Prathya MCP server.
 * Starts a sync MCP server over stdio that exposes 14 contract tools.
 */
public class PrathyaMcpServer {

    public static void main(String[] args) {
        PrathyaServerConfig config = PrathyaServerConfig.parse(args);

        StdioServerTransportProvider transport =
                new StdioServerTransportProvider(McpJsonDefaults.getMapper());

        McpSyncServer server = McpServer.sync(transport)
                .serverInfo("prathya", "1.0.0")
                .capabilities(ServerCapabilities.builder()
                        .tools(true)
                        .build())
                .instructions(ServerInstructions.INSTRUCTIONS)
                .build();

        PrathyaToolRegistry registry = new PrathyaToolRegistry(config);
        registry.registerAll(server);

        // Server runs until stdin closes
    }
}

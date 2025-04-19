package com.minecraft.cheatdetector;

import com.minecraft.cheatdetector.report.ViolationManager;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Handles the registration and execution of commands for the CheatDetector mod.
 */
public class CommandHandler {
    
    // Command prefix
    private static final String COMMAND_PREFIX = "cheatdetector";
    private static final String COMMAND_ALIAS = "cd";
    
    /**
     * Registers all CheatDetector commands.
     * @param dispatcher The command dispatcher
     */
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        // Main command
        dispatcher.register(
            CommandManager.literal(COMMAND_PREFIX)
                .requires(source -> source.hasPermissionLevel(2)) // Require permission level 2 (by default: server operators)
                .executes(CommandHandler::showHelp)
                .then(CommandManager.literal("report")
                    .executes(CommandHandler::showReportHelp)
                    .then(CommandManager.argument("player", net.minecraft.command.argument.EntityArgumentType.player())
                        .executes(CommandHandler::showPlayerReport)))
                .then(CommandManager.literal("reports")
                    .executes(CommandHandler::listReports))
                .then(CommandManager.literal("check")
                    .then(CommandManager.argument("player", net.minecraft.command.argument.EntityArgumentType.player())
                        .executes(CommandHandler::checkPlayer)))
                .then(CommandManager.literal("reload")
                    .executes(CommandHandler::reloadConfig))
        );
        
        // Alias (shorter command)
        dispatcher.register(
            CommandManager.literal(COMMAND_ALIAS)
                .requires(source -> source.hasPermissionLevel(2))
                .executes(CommandHandler::showHelp)
                .then(CommandManager.literal("report")
                    .executes(CommandHandler::showReportHelp)
                    .then(CommandManager.argument("player", net.minecraft.command.argument.EntityArgumentType.player())
                        .executes(CommandHandler::showPlayerReport)))
                .then(CommandManager.literal("reports")
                    .executes(CommandHandler::listReports))
                .then(CommandManager.literal("check")
                    .then(CommandManager.argument("player", net.minecraft.command.argument.EntityArgumentType.player())
                        .executes(CommandHandler::checkPlayer)))
                .then(CommandManager.literal("reload")
                    .executes(CommandHandler::reloadConfig))
        );
    }
    
    /**
     * Shows the help message for the mod.
     */
    private static int showHelp(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        
        source.sendFeedback(() -> Text.literal("=== CheatDetector Commands ===").formatted(Formatting.GOLD), false);
        source.sendFeedback(() -> Text.literal("/cd report <player> - Show a player's cheat report").formatted(Formatting.YELLOW), false);
        source.sendFeedback(() -> Text.literal("/cd reports - List all available cheat reports").formatted(Formatting.YELLOW), false);
        source.sendFeedback(() -> Text.literal("/cd check <player> - Run a manual check on a player").formatted(Formatting.YELLOW), false);
        source.sendFeedback(() -> Text.literal("/cd reload - Reload the configuration").formatted(Formatting.YELLOW), false);
        
        return 1;
    }
    
    /**
     * Shows help for the report command.
     */
    private static int showReportHelp(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        
        source.sendFeedback(() -> Text.literal("=== CheatDetector Report Command ===").formatted(Formatting.GOLD), false);
        source.sendFeedback(() -> Text.literal("Usage: /cd report <player>").formatted(Formatting.YELLOW), false);
        source.sendFeedback(() -> Text.literal("Shows detailed information about a player's cheat violations.").formatted(Formatting.WHITE), false);
        
        return 1;
    }
    
    /**
     * Shows a report for a specific player.
     */
    private static int showPlayerReport(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        ServerPlayerEntity player;
        
        try {
            player = net.minecraft.command.argument.EntityArgumentType.getPlayer(context, "player");
        } catch (Exception e) {
            source.sendError(Text.literal("Error: Could not find the specified player"));
            return 0;
        }
        
        ViolationManager violationManager = CheatDetector.getInstance().getViolationManager();
        List<ViolationManager.Violation> violations = violationManager.getPlayerViolations(player.getUuid());
        
        if (violations.isEmpty()) {
            source.sendFeedback(() -> Text.literal("No violations recorded for " + player.getName().getString()).formatted(Formatting.GREEN), false);
            return 1;
        }
        
        // Show report header
        source.sendFeedback(() -> Text.literal("=== Cheat Report for " + player.getName().getString() + " ===").formatted(Formatting.GOLD), false);
        
        // Count violations by type
        int speedViolations = 0;
        int flightViolations = 0;
        int xrayViolations = 0;
        int killAuraViolations = 0;
        int reachViolations = 0;
        int noFallViolations = 0;
        
        for (ViolationManager.Violation violation : violations) {
            switch (violation.type()) {
                case "SpeedHack" -> speedViolations++;
                case "FlightHack" -> flightViolations++;
                case "XRay" -> xrayViolations++;
                case "KillAura" -> killAuraViolations++;
                case "ReachHack" -> reachViolations++;
                case "NoFall" -> noFallViolations++;
            }
        }
        
        // Show violation counts
        if (speedViolations > 0) {
            source.sendFeedback(() -> Text.literal("Speed Hack: " + speedViolations + " violations").formatted(Formatting.RED), false);
        }
        if (flightViolations > 0) {
            source.sendFeedback(() -> Text.literal("Flight Hack: " + flightViolations + " violations").formatted(Formatting.RED), false);
        }
        if (xrayViolations > 0) {
            source.sendFeedback(() -> Text.literal("X-Ray: " + xrayViolations + " violations").formatted(Formatting.RED), false);
        }
        if (killAuraViolations > 0) {
            source.sendFeedback(() -> Text.literal("KillAura: " + killAuraViolations + " violations").formatted(Formatting.RED), false);
        }
        if (reachViolations > 0) {
            source.sendFeedback(() -> Text.literal("Reach Hack: " + reachViolations + " violations").formatted(Formatting.RED), false);
        }
        if (noFallViolations > 0) {
            source.sendFeedback(() -> Text.literal("No Fall: " + noFallViolations + " violations").formatted(Formatting.RED), false);
        }
        
        // Show recent violations (limit to 5)
        source.sendFeedback(() -> Text.literal("Recent Violations:").formatted(Formatting.YELLOW), false);
        
        int recentCount = Math.min(5, violations.size());
        for (int i = 0; i < recentCount; i++) {
            ViolationManager.Violation violation = violations.get(violations.size() - 1 - i);
            source.sendFeedback(() -> Text.literal(" - " + violation.type() + ": " + violation.details() + " (" + violation.timestamp() + ")").formatted(Formatting.WHITE), false);
        }
        
        // Show report file location
        source.sendFeedback(() -> Text.literal("Full report saved to: reports/" + player.getUuid() + ".txt").formatted(Formatting.AQUA), false);
        
        return 1;
    }
    
    /**
     * Lists all available reports.
     */
    private static int listReports(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        File reportsDir = new File("reports");
        
        if (!reportsDir.exists() || !reportsDir.isDirectory()) {
            source.sendFeedback(() -> Text.literal("No reports available yet.").formatted(Formatting.YELLOW), false);
            return 1;
        }
        
        File[] reportFiles = reportsDir.listFiles((dir, name) -> name.endsWith(".txt"));
        
        if (reportFiles == null || reportFiles.length == 0) {
            source.sendFeedback(() -> Text.literal("No reports available yet.").formatted(Formatting.YELLOW), false);
            return 1;
        }
        
        source.sendFeedback(() -> Text.literal("=== Available Cheat Reports ===").formatted(Formatting.GOLD), false);
        
        for (File file : reportFiles) {
            String uuidStr = file.getName().replace(".txt", "");
            try {
                UUID uuid = UUID.fromString(uuidStr);
                String playerName = "Unknown";
                
                // Try to get player name from server
                ServerPlayerEntity player = context.getSource().getServer().getPlayerManager().getPlayer(uuid);
                if (player != null) {
                    playerName = player.getName().getString();
                } else {
                    // Try to read first line of report which should contain player name
                    try {
                        List<String> lines = Files.readAllLines(Paths.get(file.getPath()));
                        if (!lines.isEmpty() && lines.get(0).startsWith("Player:")) {
                            playerName = lines.get(0).substring("Player:".length()).trim();
                        }
                    } catch (Exception ignored) {
                    }
                }
                
                source.sendFeedback(() -> Text.literal("- " + playerName + " (Use: /cd report " + playerName + ")").formatted(Formatting.YELLOW), false);
            } catch (Exception e) {
                // Invalid UUID filename, ignore
            }
        }
        
        return 1;
    }
    
    /**
     * Runs a manual check on a player.
     */
    private static int checkPlayer(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        ServerPlayerEntity player;
        
        try {
            player = net.minecraft.command.argument.EntityArgumentType.getPlayer(context, "player");
        } catch (Exception e) {
            source.sendError(Text.literal("Error: Could not find the specified player"));
            return 0;
        }
        
        source.sendFeedback(() -> Text.literal("Running manual check on " + player.getName().getString() + "...").formatted(Formatting.YELLOW), false);
        
        // Run all checks on the player
        CheatDetector instance = CheatDetector.getInstance();
        instance.getSpeedHackDetector().check(player);
        instance.getFlightDetector().check(player);
        instance.getXrayDetector().check(player);
        instance.getKillAuraDetector().check(player);
        instance.getNoFallDetector().check(player);
        
        source.sendFeedback(() -> Text.literal("Check completed. Use '/cd report " + player.getName().getString() + "' to see results.").formatted(Formatting.GREEN), false);
        
        return 1;
    }
    
    /**
     * Reloads the configuration.
     */
    private static int reloadConfig(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        
        try {
            CheatDetector.getInstance().getConfig().load();
            source.sendFeedback(() -> Text.literal("Configuration reloaded successfully!").formatted(Formatting.GREEN), false);
        } catch (Exception e) {
            source.sendError(Text.literal("Error reloading configuration: " + e.getMessage()));
            return 0;
        }
        
        return 1;
    }
} 
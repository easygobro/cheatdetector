package com.minecraft.cheatdetector;

import com.minecraft.cheatdetector.cheat.*;
import com.minecraft.cheatdetector.config.ModConfig;
import com.minecraft.cheatdetector.data.PlayerDataManager;
import com.minecraft.cheatdetector.event.EventManager;
import com.minecraft.cheatdetector.report.ViolationManager;
import com.minecraft.cheatdetector.test.TestSpeedHackDetector;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main class for the CheatDetector anti-cheat mod.
 * This is a server-side only mod designed to detect and report cheaters on Fabric 1.21.5 servers.
 */
public class CheatDetector implements ModInitializer {
    // Logger for our mod
    public static final Logger LOGGER = LoggerFactory.getLogger("cheatdetector");
    
    // Singleton instance
    private static CheatDetector instance;
    
    // Current server instance
    private MinecraftServer server;
    
    // Managers
    private PlayerDataManager playerDataManager;
    private ViolationManager violationManager;
    private EventManager eventManager;
    private ModConfig config;
    
    // Cheat detectors
    private SpeedHackDetector speedHackDetector;
    private XrayDetector xrayDetector;
    private FlightDetector flightDetector;
    private KillAuraDetector killAuraDetector;
    private ReachHackDetector reachHackDetector;
    private NoFallDetector noFallDetector;
    
    /**
     * Initializes the CheatDetector mod.
     */
    @Override
    public void onInitialize() {
        LOGGER.info("Initializing CheatDetector anti-cheat mod");
        
        // Set singleton instance
        instance = this;
        
        // Initialize configuration
        this.config = new ModConfig();
        
        // Initialize managers
        this.playerDataManager = new PlayerDataManager();
        this.violationManager = new ViolationManager(this.config);
        this.eventManager = new EventManager();
        
        // Initialize cheat detectors
        this.speedHackDetector = new SpeedHackDetector(this.violationManager, this.config);
        this.xrayDetector = new XrayDetector(this.violationManager, this.config);
        this.flightDetector = new FlightDetector(this.violationManager, this.config);
        this.killAuraDetector = new KillAuraDetector(this.violationManager, this.config);
        this.reachHackDetector = new ReachHackDetector(this.violationManager, this.config);
        this.noFallDetector = new NoFallDetector(this.violationManager, this.config);
        
        // Register server lifecycle events
        registerServerEvents();
        
        // Register commands
        registerCommands();
        
        // Register test commands
        TestSpeedHackDetector.register();
        
        LOGGER.info("CheatDetector initialized successfully!");
    }
    
    /**
     * Register server events needed by the mod.
     */
    private void registerServerEvents() {
        // Register server start event
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            this.server = server;
            LOGGER.info("CheatDetector connected to server");
        });
        
        // Register server stop event
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            LOGGER.info("CheatDetector disconnecting from server");
            this.playerDataManager.saveAllData();
        });
        
        // Register server tick event for regular checks
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            // Skip if there are no players
            if (server.getPlayerManager().getPlayerList().isEmpty()) {
                return;
            }
            
            // Perform checks on each player
            for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                // Skip players with bypass permission
                if (hasAntiCheatBypassPermission(player)) {
                    continue;
                }
                
                // Run cheat detections
                speedHackDetector.check(player);
                flightDetector.check(player);
                xrayDetector.check(player);
                killAuraDetector.check(player);
                noFallDetector.check(player);
                // Note: Reach hack detection is handled by attack event
            }
        });
    }
    
    /**
     * Register commands for the mod.
     */
    private void registerCommands() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            // Register commands
            CommandHandler.register(dispatcher);
        });
    }
    
    /**
     * Check if a player has permission to bypass anti-cheat checks.
     * @param player The player to check
     * @return true if the player can bypass checks, false otherwise
     */
    private boolean hasAntiCheatBypassPermission(ServerPlayerEntity player) {
        // For vanilla servers, typically just check op level
        return player.hasPermissionLevel(config.getBypassPermissionLevel());
    }
    
    /**
     * Get the current server instance.
     * @return The current MinecraftServer instance
     */
    public MinecraftServer getServer() {
        return server;
    }
    
    /**
     * Get the player data manager.
     * @return The player data manager
     */
    public PlayerDataManager getPlayerDataManager() {
        return playerDataManager;
    }
    
    /**
     * Get the violation manager.
     * @return The violation manager
     */
    public ViolationManager getViolationManager() {
        return violationManager;
    }
    
    /**
     * Get the event manager.
     * @return The event manager
     */
    public EventManager getEventManager() {
        return eventManager;
    }
    
    /**
     * Get the mod configuration.
     * @return The mod configuration
     */
    public ModConfig getConfig() {
        return config;
    }
    
    /**
     * Get the speed hack detector.
     * @return The speed hack detector
     */
    public SpeedHackDetector getSpeedHackDetector() {
        return speedHackDetector;
    }
    
    /**
     * Get the singleton instance of the CheatDetector mod.
     * @return The CheatDetector instance
     */
    public static CheatDetector getInstance() {
        return instance;
    }
} 
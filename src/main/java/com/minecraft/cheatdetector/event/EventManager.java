package com.minecraft.cheatdetector.event;

import com.minecraft.cheatdetector.CheatDetector;
import com.minecraft.cheatdetector.data.PlayerDataManager;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;

/**
 * Manages event registration and handling for the anti-cheat system.
 */
public class EventManager {
    
    /**
     * Create and initialize the event manager.
     */
    public EventManager() {
        registerEvents();
    }
    
    /**
     * Register all event listeners needed by the anti-cheat system.
     */
    private void registerEvents() {
        // Register player join and leave events
        registerPlayerConnectionEvents();
        
        // Register block break events for X-ray detection
        registerBlockBreakEvents();
        
        // Register attack events for combat hack detection
        registerAttackEvents();
    }
    
    /**
     * Register player connection events.
     */
    private void registerPlayerConnectionEvents() {
        // Player join event
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            CheatDetector.LOGGER.info("Player joined: {}", handler.getPlayer().getName().getString());
            
            // Initialize player data when a player joins
            ServerPlayerEntity player = handler.getPlayer();
            CheatDetector.getInstance().getPlayerDataManager().getPlayerData(player);
            
            // Send welcome message
            // No welcome message needed for our anti-cheat
        });
        
        // Player leave event
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            CheatDetector.LOGGER.info("Player left: {}", handler.getPlayer().getName().getString());
            
            // Clean up player data when a player leaves
            ServerPlayerEntity player = handler.getPlayer();
            UUID playerUuid = player.getUuid();
            PlayerDataManager playerDataManager = CheatDetector.getInstance().getPlayerDataManager();
            
            // Remove player data to prevent memory leaks
            playerDataManager.removePlayerData(playerUuid);
        });
    }
    
    /**
     * Register block break events for X-ray detection.
     */
    private void registerBlockBreakEvents() {
        // Register block break event
        PlayerBlockBreakEvents.AFTER.register((world, player, pos, state, blockEntity) -> {
            // Skip if not on the server or if player is not a server player
            if (world.isClient() || !(player instanceof ServerPlayerEntity)) {
                return;
            }
            
            ServerPlayerEntity serverPlayer = (ServerPlayerEntity) player;
            
            // Skip if the player has permission to bypass anti-cheat
            if (serverPlayer.hasPermissionLevel(
                    CheatDetector.getInstance().getConfig().getBypassPermissionLevel())) {
                return;
            }
            
            // Get block ID for tracking
            String blockId = state.getBlock().toString();
            
            // Track mined blocks in player data
            PlayerDataManager.PlayerData playerData = 
                    CheatDetector.getInstance().getPlayerDataManager().getPlayerData(serverPlayer);
            playerData.addMinedBlock(blockId);
            
            // X-ray detection is done periodically in the XrayDetector class
        });
    }
    
    /**
     * Register attack events for combat hack detection.
     */
    private void registerAttackEvents() {
        // Register attack event
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            // Skip if not on the server or if player is not a server player
            if (world.isClient() || !(player instanceof ServerPlayerEntity)) {
                return ActionResult.PASS;
            }
            
            ServerPlayerEntity serverPlayer = (ServerPlayerEntity) player;
            
            // Skip if the player has permission to bypass anti-cheat
            if (serverPlayer.hasPermissionLevel(
                    CheatDetector.getInstance().getConfig().getBypassPermissionLevel())) {
                return ActionResult.PASS;
            }
            
            // Record the attack in player data
            PlayerDataManager.PlayerData playerData = 
                    CheatDetector.getInstance().getPlayerDataManager().getPlayerData(serverPlayer);
            playerData.recordAttack(entity.getUuid());
            
            // KillAura and Reach detection is handled in their respective detector classes
            
            // Allow the attack to proceed
            return ActionResult.PASS;
        });
    }
} 
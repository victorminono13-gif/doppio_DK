package com.victorminono.clonemod.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.logging.LogUtils;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;

import com.victorminono.clonemod.CloneMod;
import com.victorminono.clonemod.data.PlayerDataCollector;

public class CloneCommand {
    
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final PlayerDataCollector dataCollector = new PlayerDataCollector();
    
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            Commands.literal("clone")
                .requires(source -> source.hasPermission(0)) // Permite todos
                .then(Commands.literal("analisar")
                    .then(Commands.argument("jogador", StringArgumentType.word())
                        .executes(CloneCommand::analyzePlayer)))
                .then(Commands.literal("spawn")
                    .then(Commands.argument("jogador", StringArgumentType.word())
                        .executes(CloneCommand::spawnClone)))
                .then(Commands.literal("force")
                    .then(Commands.argument("jogador", StringArgumentType.word())
                        .executes(CloneCommand::forceSpawn)))
                .then(Commands.literal("stop")
                    .then(Commands.argument("jogador", StringArgumentType.word())
                        .executes(CloneCommand::stopAnalysis)))
                .then(Commands.literal("memory")
                    .then(Commands.literal("save")
                        .then(Commands.argument("slot", StringArgumentType.word())
                            .executes(CloneCommand::memorySave)))
                    .then(Commands.literal("load")
                        .then(Commands.argument("slot", StringArgumentType.word())
                            .executes(CloneCommand::memoryLoad)))
                    .then(Commands.literal("list")
                        .executes(CloneCommand::memoryList)))
                .then(Commands.literal("status")
                    .executes(CloneCommand::showStatus))
        );
    }
    
    /**
     * /clone analisar <jogador>
     * Inicia coleta silenciosa de dados do jogador via WebSocket
     */
    private static int analyzePlayer(CommandContext<CommandSourceStack> context) {
        String playerName = StringArgumentType.getString(context, "jogador");
        CommandSourceStack source = context.getSource();
        
        try {
            LOGGER.info("[CloneMod] Iniciando análise do jogador: " + playerName);
            
            // Inicia coleta de dados
            dataCollector.startCollection(playerName);
            
            // Envia mensagem ao comando executor
            source.sendSuccess(
                () -> Component.literal("§6[CloneMod] Análise iniciada para " + playerName),
                false
            );
            
            // Envia para Python via WebSocket
            if (CloneMod.webSocketClient != null && CloneMod.webSocketClient.isOpen()) {
                String payload = String.format(
                    "{\"action\": \"start_analysis\", \"player\": \"%s\"}",
                    playerName
                );
                CloneMod.webSocketClient.send(payload);
                LOGGER.info("[CloneMod] Payload de análise enviado: " + payload);
            } else {
                source.sendFailure(Component.literal("§c[CloneMod] WebSocket não conectado!"));
                return 0;
            }
            
            return 1;
        } catch (Exception e) {
            LOGGER.error("[CloneMod] Erro ao analisar jogador", e);
            source.sendFailure(Component.literal("§c[CloneMod] Erro: " + e.getMessage()));
            return 0;
        }
    }
    
    /**
     * /clone spawn <jogador>
     * Spawna o Fake Player se houver dados suficientes
     */
    private static int spawnClone(CommandContext<CommandSourceStack> context) {
        String playerName = StringArgumentType.getString(context, "jogador");
        CommandSourceStack source = context.getSource();
        
        try {
            LOGGER.info("[CloneMod] Tentando spawnar clone de: " + playerName);
            
            if (CloneMod.webSocketClient != null && CloneMod.webSocketClient.isOpen()) {
                String payload = String.format(
                    "{\"action\": \"spawn_clone\", \"player\": \"%s\", \"force\": false}",
                    playerName
                );
                CloneMod.webSocketClient.send(payload);
                
                source.sendSuccess(
                    () -> Component.literal("§a[CloneMod] Tentativa de spawn do clone: " + playerName),
                    false
                );
                return 1;
            } else {
                source.sendFailure(Component.literal("§c[CloneMod] WebSocket não conectado!"));
                return 0;
            }
        } catch (Exception e) {
            LOGGER.error("[CloneMod] Erro ao spawnar clone", e);
            source.sendFailure(Component.literal("§c[CloneMod] Erro: " + e.getMessage()));
            return 0;
        }
    }
    
    /**
     * /clone force <jogador>
     * Força o spawn do clone imediatamente
     */
    private static int forceSpawn(CommandContext<CommandSourceStack> context) {
        String playerName = StringArgumentType.getString(context, "jogador");
        CommandSourceStack source = context.getSource();
        
        try {
            LOGGER.info("[CloneMod] Forçando spawn do clone de: " + playerName);
            
            if (CloneMod.webSocketClient != null && CloneMod.webSocketClient.isOpen()) {
                String payload = String.format(
                    "{\"action\": \"spawn_clone\", \"player\": \"%s\", \"force\": true}",
                    playerName
                );
                CloneMod.webSocketClient.send(payload);
                
                source.sendSuccess(
                    () -> Component.literal("§a[CloneMod] Clone forçado para: " + playerName),
                    false
                );
                return 1;
            } else {
                source.sendFailure(Component.literal("§c[CloneMod] WebSocket não conectado!"));
                return 0;
            }
        } catch (Exception e) {
            LOGGER.error("[CloneMod] Erro ao forçar spawn", e);
            source.sendFailure(Component.literal("§c[CloneMod] Erro: " + e.getMessage()));
            return 0;
        }
    }
    
    /**
     * /clone stop <jogador>
     * Para a coleta de dados de um jogador
     */
    private static int stopAnalysis(CommandContext<CommandSourceStack> context) {
        String playerName = StringArgumentType.getString(context, "jogador");
        CommandSourceStack source = context.getSource();
        
        try {
            LOGGER.info("[CloneMod] Parando análise do jogador: " + playerName);
            
            dataCollector.stopCollection(playerName);
            
            if (CloneMod.webSocketClient != null && CloneMod.webSocketClient.isOpen()) {
                String payload = String.format(
                    "{\"action\": \"stop_analysis\", \"player\": \"%s\"}",
                    playerName
                );
                CloneMod.webSocketClient.send(payload);
            }
            
            source.sendSuccess(
                () -> Component.literal("§e[CloneMod] Análise parada para: " + playerName),
                false
            );
            return 1;
        } catch (Exception e) {
            LOGGER.error("[CloneMod] Erro ao parar análise", e);
            source.sendFailure(Component.literal("§c[CloneMod] Erro: " + e.getMessage()));
            return 0;
        }
    }
    
    /**
     * /clone memory save <slot>
     * Salva os pesos da IA em um slot
     */
    private static int memorySave(CommandContext<CommandSourceStack> context) {
        String slot = StringArgumentType.getString(context, "slot");
        CommandSourceStack source = context.getSource();
        
        try {
            LOGGER.info("[CloneMod] Salvando memória no slot: " + slot);
            
            if (CloneMod.webSocketClient != null && CloneMod.webSocketClient.isOpen()) {
                String payload = String.format(
                    "{\"action\": \"memory_save\", \"slot\": \"%s\"}",
                    slot
                );
                CloneMod.webSocketClient.send(payload);
                
                source.sendSuccess(
                    () -> Component.literal("§a[CloneMod] Memória salva no slot: " + slot),
                    false
                );
                return 1;
            } else {
                source.sendFailure(Component.literal("§c[CloneMod] WebSocket não conectado!"));
                return 0;
            }
        } catch (Exception e) {
            LOGGER.error("[CloneMod] Erro ao salvar memória", e);
            source.sendFailure(Component.literal("§c[CloneMod] Erro: " + e.getMessage()));
            return 0;
        }
    }
    
    /**
     * /clone memory load <slot>
     * Carrega um perfil de um slot
     */
    private static int memoryLoad(CommandContext<CommandSourceStack> context) {
        String slot = StringArgumentType.getString(context, "slot");
        CommandSourceStack source = context.getSource();
        
        try {
            LOGGER.info("[CloneMod] Carregando memória do slot: " + slot);
            
            if (CloneMod.webSocketClient != null && CloneMod.webSocketClient.isOpen()) {
                String payload = String.format(
                    "{\"action\": \"memory_load\", \"slot\": \"%s\"}",
                    slot
                );
                CloneMod.webSocketClient.send(payload);
                
                source.sendSuccess(
                    () -> Component.literal("§a[CloneMod] Memória carregada do slot: " + slot),
                    false
                );
                return 1;
            } else {
                source.sendFailure(Component.literal("§c[CloneMod] WebSocket não conectado!"));
                return 0;
            }
        } catch (Exception e) {
            LOGGER.error("[CloneMod] Erro ao carregar memória", e);
            source.sendFailure(Component.literal("§c[CloneMod] Erro: " + e.getMessage()));
            return 0;
        }
    }
    
    /**
     * /clone memory list
     * Lista todos os slots salvos
     */
    private static int memoryList(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        
        try {
            LOGGER.info("[CloneMod] Listando slots de memória");
            
            if (CloneMod.webSocketClient != null && CloneMod.webSocketClient.isOpen()) {
                String payload = "{\"action\": \"memory_list\"}";
                CloneMod.webSocketClient.send(payload);
                
                source.sendSuccess(
                    () -> Component.literal("§6[CloneMod] Solicitando lista de slots..."),
                    false
                );
                return 1;
            } else {
                source.sendFailure(Component.literal("§c[CloneMod] WebSocket não conectado!"));
                return 0;
            }
        } catch (Exception e) {
            LOGGER.error("[CloneMod] Erro ao listar memória", e);
            source.sendFailure(Component.literal("§c[CloneMod] Erro: " + e.getMessage()));
            return 0;
        }
    }
    
    /**
     * /clone status
     * Mostra o status de todas as análises ativas
     */
    private static int showStatus(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        
        try {
            boolean wsConnected = CloneMod.webSocketClient != null && CloneMod.webSocketClient.isOpen();
            
            source.sendSuccess(
                () -> Component.literal("§e=== [CloneMod Status] ==="),
                false
            );
            
            source.sendSuccess(
                () -> Component.literal("§6WebSocket: " + (wsConnected ? "§a✓ Conectado" : "§c✗ Desconectado")),
                false
            );
            
            source.sendSuccess(
                () -> Component.literal("§6Análises ativas: " + dataCollector.getActiveSessions().size()),
                false
            );
            
            dataCollector.getActiveSessions().forEach((playerName, session) -> {
                source.sendSuccess(
                    () -> Component.literal("  §7→ " + session.getSummary()),
                    false
                );
            });
            
            source.sendSuccess(
                () -> Component.literal("§e==================="),
                false
            );
            
            return 1;
        } catch (Exception e) {
            LOGGER.error("[CloneMod] Erro ao mostrar status", e);
            source.sendFailure(Component.literal("§c[CloneMod] Erro: " + e.getMessage()));
            return 0;
        }
    }
}

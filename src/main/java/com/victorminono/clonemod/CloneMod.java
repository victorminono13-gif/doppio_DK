package com.victorminono.clonemod;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.bus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import org.slf4j.Logger;

import com.mojang.logging.LogUtils;
import com.victorminono.clonemod.command.CloneCommand;
import com.victorminono.clonemod.events.PlayerEventHandler;
import com.victorminono.clonemod.network.WebSocketClient;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.javafxmod.FXModLanguageProvider;

@Mod("clonemod")
public class CloneMod {
    
    public static final String MODID = "clonemod";
    private static final Logger LOGGER = LogUtils.getLogger();
    
    public static WebSocketClient webSocketClient;
    
    public CloneMod() {
        IEventBus modEventBus = FXModLanguageProvider.modEventBus;
        IEventBus forgeEventBus = MinecraftForge.EVENT_BUS;
        
        // Registra eventos de ciclo de vida do mod
        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(this::clientSetup);
        
        // Registra handlers de eventos
        forgeEventBus.register(new PlayerEventHandler());
        forgeEventBus.register(new ServerEvents());
        
        LOGGER.info("CloneMod inicializado!");
    }
    
    private void commonSetup(FMLCommonSetupEvent event) {
        LOGGER.info("Common setup do CloneMod");
    }
    
    private void clientSetup(FMLClientSetupEvent event) {
        LOGGER.info("Client setup do CloneMod");
    }
    
    @Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static class ServerEvents {
        
        @SubscribeEvent
        public static void onServerStart(ServerStartingEvent event) {
            LOGGER.info("[CloneMod] Servidor iniciando...");
            
            // Inicializa WebSocket quando o servidor começar
            if (webSocketClient == null) {
                webSocketClient = new WebSocketClient("ws://localhost:8765");
                webSocketClient.connect();
                LOGGER.info("[CloneMod] WebSocket conectando em ws://localhost:8765");
            }
            
            // Registra comando /clone
            CloneCommand.register(event.getServer().getCommands().getDispatcher());
            LOGGER.info("[CloneMod] Comando /clone registrado");
        }
    }
}

package ru.example.emccrafting;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

@Mod(EmcCrafting.MODID)
public class EmcCrafting {
    public static final String MODID = "emccrafting";

    public EmcCrafting(IEventBus modBus) {
        modBus.addListener(EmcCrafting::registerPayloads);
    }

    private static void registerPayloads(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToServer(
                FillGridPayload.TYPE, FillGridPayload.CODEC, FillGridPayload::handle);
    }
}

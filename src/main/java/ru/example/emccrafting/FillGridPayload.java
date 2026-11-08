package ru.example.emccrafting;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Клиент -> сервер: "заполни сетку крафта рецептом с этим id". */
public record FillGridPayload(ResourceLocation recipeId) implements CustomPacketPayload {
    public static final Type<FillGridPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(EmcCrafting.MODID, "fill_grid"));
    public static final StreamCodec<RegistryFriendlyByteBuf, FillGridPayload> CODEC =
            StreamCodec.composite(ResourceLocation.STREAM_CODEC, FillGridPayload::recipeId, FillGridPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handle(FillGridPayload payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (ctx.player() instanceof ServerPlayer sp) {
                EmcGridFiller.fill(sp, payload.recipeId());
            }
        });
    }
}

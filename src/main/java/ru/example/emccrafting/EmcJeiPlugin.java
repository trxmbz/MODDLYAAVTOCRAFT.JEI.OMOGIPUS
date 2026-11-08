package ru.example.emccrafting;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.transfer.IRecipeTransferError;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandler;
import mezz.jei.api.registration.IRecipeTransferRegistration;
import mezz.jei.api.constants.RecipeTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

@JeiPlugin
public class EmcJeiPlugin implements IModPlugin {
    @Override
    public ResourceLocation getPluginUid() {
        return ResourceLocation.fromNamespaceAndPath(EmcCrafting.MODID, "jei");
    }

    @Override
    public void registerRecipeTransferHandlers(IRecipeTransferRegistration reg) {
        reg.addRecipeTransferHandler(new Handler(), RecipeTypes.CRAFTING);
    }

    /** Кнопка "+" в JEI на верстаке: сервер сам собирает ингредиенты (инвентарь -> EMC). */
    private static class Handler implements IRecipeTransferHandler<CraftingMenu, RecipeHolder<CraftingRecipe>> {
        @Override public Class<? extends CraftingMenu> getContainerClass() { return CraftingMenu.class; }
        @Override public Optional<MenuType<CraftingMenu>> getMenuType() { return Optional.of(net.minecraft.world.inventory.MenuType.CRAFTING); }
        @Override public RecipeType<RecipeHolder<CraftingRecipe>> getRecipeType() { return RecipeTypes.CRAFTING; }

        @Override
        public @Nullable IRecipeTransferError transferRecipe(CraftingMenu menu, RecipeHolder<CraftingRecipe> recipe,
                IRecipeSlotsView slots, Player player, boolean maxTransfer, boolean doTransfer) {
            if (doTransfer) PacketDistributor.sendToServer(new FillGridPayload(recipe.id()));
            return null;
        }
    }
}

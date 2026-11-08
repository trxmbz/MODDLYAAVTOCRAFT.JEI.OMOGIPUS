package ru.example.emccrafting;

import moze_intel.projecte.api.ItemInfo;
import moze_intel.projecte.api.capabilities.IKnowledgeProvider;
import moze_intel.projecte.api.proxy.IEMCProxy;
import moze_intel.projecte.api.proxy.ITransmutationProxy;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.RecipeBookMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.ShapedRecipe;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

/**
 * Заполняет сетку крафта (верстак / 2x2 инвентаря) по рецепту.
 * Для каждого ингредиента: сначала берём предмет из инвентаря,
 * иначе - самый дешёвый ИЗУЧЕННЫЙ в столе трансмутации подходящий предмет за EMC.
 * Всё или ничего: если EMC не хватает - ничего не списывается.
 */
public final class EmcGridFiller {
    private EmcGridFiller() {}

    private record Plan(int gridIndex, ItemStack stack, int invSlot, BigInteger cost) {}

    public static void fill(ServerPlayer player, ResourceLocation recipeId) {
        if (!(player.containerMenu instanceof RecipeBookMenu<?, ?> menu)) return;

        RecipeHolder<?> holder = player.server.getRecipeManager().byKey(recipeId).orElse(null);
        if (holder == null || !(holder.value() instanceof CraftingRecipe recipe)) return;

        int gw = menu.getGridWidth(), gh = menu.getGridHeight();
        int gridSize = gw * gh;
        List<Ingredient> ings = recipe.getIngredients();

        // раскладка по сетке
        Ingredient[] layout = new Ingredient[gridSize];
        if (recipe instanceof ShapedRecipe shaped) {
            if (shaped.getWidth() > gw || shaped.getHeight() > gh) {
                player.displayClientMessage(Component.literal("Рецепт не помещается в эту сетку"), true);
                return;
            }
            for (int i = 0; i < ings.size(); i++) {
                int x = i % shaped.getWidth(), y = i / shaped.getWidth();
                layout[y * gw + x] = ings.get(i);
            }
        } else {
            if (ings.size() > gridSize) {
                player.displayClientMessage(Component.literal("Рецепт не помещается в эту сетку"), true);
                return;
            }
            for (int i = 0; i < ings.size(); i++) layout[i] = ings.get(i);
        }

        IKnowledgeProvider provider = ITransmutationProxy.INSTANCE.getKnowledgeProviderFor(player.getUUID());
        Inventory inv = player.getInventory();

        // --- планирование (без изменений мира) ---
        int[] reserved = new int[inv.getContainerSize()];
        BigInteger total = BigInteger.ZERO;
        List<Plan> plans = new ArrayList<>();

        for (int g = 0; g < gridSize; g++) {
            Ingredient ing = layout[g];
            if (ing == null || ing.isEmpty()) continue;

            int found = -1;
            for (int s = 0; s < inv.getContainerSize(); s++) {
                ItemStack st = inv.getItem(s);
                if (!st.isEmpty() && st.getCount() - reserved[s] > 0 && ing.test(st)) { found = s; break; }
            }
            if (found >= 0) {
                reserved[found]++;
                plans.add(new Plan(g, inv.getItem(found).copyWithCount(1), found, BigInteger.ZERO));
                continue;
            }

            ItemStack best = ItemStack.EMPTY;
            long bestEmc = Long.MAX_VALUE;
            for (ItemInfo info : provider.getKnowledge()) {
                ItemStack cand = info.createStack();
                if (cand.isEmpty() || !ing.test(cand)) continue;
                long emc = IEMCProxy.INSTANCE.getValue(info);
                if (emc > 0 && emc < bestEmc) { bestEmc = emc; best = cand.copyWithCount(1); }
            }
            if (best.isEmpty()) {
                player.displayClientMessage(Component.literal("Нет изученного предмета для ингредиента"), true);
                return;
            }
            BigInteger cost = BigInteger.valueOf(bestEmc);
            total = total.add(cost);
            plans.add(new Plan(g, best, -1, cost));
        }

        if (provider.getEmc().compareTo(total) < 0) {
            player.displayClientMessage(Component.literal("Не хватает EMC: нужно " + total), true);
            return;
        }

        // --- применение ---
        for (int i = 1; i <= gridSize; i++) {                       // слот 0 - результат
            ItemStack old = menu.getSlot(i).getItem();
            if (!old.isEmpty()) {
                inv.placeItemBackInInventory(old.copy());
                menu.getSlot(i).set(ItemStack.EMPTY);
            }
        }
        for (Plan p : plans) {
            if (p.invSlot() >= 0) inv.getItem(p.invSlot()).shrink(1);
        }
        for (Plan p : plans) menu.getSlot(p.gridIndex() + 1).set(p.stack());

        if (total.signum() > 0) {
            provider.setEmc(provider.getEmc().subtract(total));
            provider.syncEmc(player);
        }
        menu.broadcastChanges();
    }
}

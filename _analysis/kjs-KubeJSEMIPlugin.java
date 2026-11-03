package dev.latvian.mods.kubejs.integration.emi;

import dev.emi.emi.api.EmiEntrypoint;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.EmiInfoRecipe;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.latvian.mods.kubejs.client.KubeSessionData;
import dev.latvian.mods.kubejs.plugin.builtin.event.RecipeViewerEvents;
import dev.latvian.mods.kubejs.recipe.viewer.RecipeViewerEntryType;
import dev.latvian.mods.kubejs.recipe.viewer.server.CategoryData;
import dev.latvian.mods.kubejs.recipe.viewer.server.RecipeViewerData;
import dev.latvian.mods.kubejs.recipe.viewer.server.ItemData.Info;
import dev.latvian.mods.kubejs.script.ScriptType;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.crafting.FluidIngredient;

@EmiEntrypoint
public class KubeJSEMIPlugin implements EmiPlugin {
   public void register(EmiRegistry registry) {
      KubeSessionData sessionData = KubeSessionData.of(Minecraft.getInstance());
      RecipeViewerData remote = sessionData == null ? null : sessionData.recipeViewerData;
      if (RecipeViewerEvents.REMOVE_CATEGORIES.hasListeners()) {
         HashSet<ResourceLocation> removedCategories = new HashSet<>();
         RecipeViewerEvents.REMOVE_CATEGORIES.post(ScriptType.CLIENT, new EMIRemoveCategoriesKubeEvent(removedCategories));
         if (!removedCategories.isEmpty()) {
            registry.removeRecipes(r -> r.getCategory() != null && removedCategories.contains(r.getCategory().getId()));
         }
      }

      if (remote != null) {
         Set<ResourceLocation> removedCategories = Set.copyOf(remote.removedCategories());
         Set<ResourceLocation> globalRemovedRecipes = Set.copyOf(remote.removedGlobalRecipes());
         HashMap<ResourceLocation, Set<ResourceLocation>> removedRecipes = new HashMap<>();

         for (CategoryData data : remote.categoryData()) {
            removedRecipes.put(data.category(), Set.copyOf(data.removedRecipes()));
         }

         registry.removeRecipes(r -> {
            ResourceLocation cat = r.getCategory().getId();
            if (cat == null) {
               return false;
            } else if (removedCategories.contains(cat)) {
               return true;
            } else {
               ResourceLocation id = r.getId();
               return id == null ? false : globalRemovedRecipes.contains(id) || removedRecipes.getOrDefault(cat, Set.of()).contains(id);
            }
         });
      }

      for (RecipeViewerEntryType type : (List)RecipeViewerEntryType.ALL_TYPES.get()) {
         if (RecipeViewerEvents.REMOVE_ENTRIES.hasListeners(type)) {
            RecipeViewerEvents.REMOVE_ENTRIES.post(ScriptType.CLIENT, type, new EMIRemoveEntriesKubeEvent(type, registry));
         }

         if (RecipeViewerEvents.REMOVE_ENTRIES_COMPLETELY.hasListeners(type)) {
            RecipeViewerEvents.REMOVE_ENTRIES_COMPLETELY.post(ScriptType.CLIENT, type, new EMIRemoveEntriesKubeEvent(type, registry));
         }
      }

      if (remote != null) {
         for (Ingredient ingredient : remote.itemData().removedEntries()) {
            registry.removeEmiStacks(EMIIntegration.predicate(ingredient));
         }

         for (Ingredient ingredient : remote.itemData().completelyRemovedEntries()) {
            registry.removeEmiStacks(EMIIntegration.predicate(ingredient));
         }

         for (FluidIngredient ingredient : remote.fluidData().removedEntries()) {
            registry.removeEmiStacks(EMIIntegration.predicate(ingredient));
         }

         for (FluidIngredient ingredient : remote.fluidData().completelyRemovedEntries()) {
            registry.removeEmiStacks(EMIIntegration.predicate(ingredient));
         }
      }

      for (RecipeViewerEntryType type : (List)RecipeViewerEntryType.ALL_TYPES.get()) {
         if (RecipeViewerEvents.ADD_ENTRIES.hasListeners(type)) {
            RecipeViewerEvents.ADD_ENTRIES.post(ScriptType.CLIENT, type, new EMIAddEntriesKubeEvent(type, registry));
         }
      }

      if (remote != null) {
         for (ItemStack stack : remote.itemData().addedEntries()) {
            registry.addEmiStack(EmiStack.of(stack));
         }

         for (FluidStack stack : remote.fluidData().addedEntries()) {
            registry.addEmiStack(EmiStack.of(stack.getFluid(), stack.getComponentsPatch(), (long)stack.getAmount()));
         }
      }

      for (RecipeViewerEntryType typex : (List)RecipeViewerEntryType.ALL_TYPES.get()) {
         if (RecipeViewerEvents.ADD_INFORMATION.hasListeners(typex)) {
            RecipeViewerEvents.ADD_INFORMATION.post(ScriptType.CLIENT, typex, new EMIAddInformationKubeEvent(typex, registry));
         }
      }

      if (remote != null) {
         for (Info info : remote.itemData().info()) {
            registry.addRecipe(new EmiInfoRecipe(List.of(EmiIngredient.of(info.filter())), info.info(), null));
         }

         for (dev.latvian.mods.kubejs.recipe.viewer.server.FluidData.Info info : remote.fluidData().info()) {
            registry.addRecipe(new EmiInfoRecipe(List.of(EMIIntegration.fluidIngredient(info.filter())), info.info(), null));
         }
      }
   }
}

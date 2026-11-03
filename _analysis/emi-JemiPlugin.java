package dev.emi.emi.jemi;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import dev.emi.emi.EmiPort;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.EmiCraftingRecipe;
import dev.emi.emi.api.recipe.EmiInfoRecipe;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.recipe.VanillaEmiRecipeCategories;
import dev.emi.emi.api.recipe.handler.EmiRecipeHandler;
import dev.emi.emi.api.stack.Comparison;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.stack.EmiStackInteraction;
import dev.emi.emi.api.widget.Bounds;
import dev.emi.emi.jemi.JemiPlugin.1;
import dev.emi.emi.jemi.impl.JemiIngredientAcceptor;
import dev.emi.emi.jemi.impl.JemiRecipeLayoutBuilder;
import dev.emi.emi.jemi.runtime.JemiBookmarkOverlay;
import dev.emi.emi.jemi.runtime.JemiDragDropHandler;
import dev.emi.emi.jemi.runtime.JemiIngredientFilter;
import dev.emi.emi.jemi.runtime.JemiIngredientListOverlay;
import dev.emi.emi.jemi.runtime.JemiRecipesGui;
import dev.emi.emi.platform.EmiAgnos;
import dev.emi.emi.registry.EmiPluginContainer;
import dev.emi.emi.registry.EmiRecipeFiller;
import dev.emi.emi.registry.EmiRecipes;
import dev.emi.emi.runtime.EmiLog;
import dev.emi.emi.runtime.EmiReloadLog;
import dev.emi.emi.runtime.EmiReloadManager;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.Map.Entry;
import java.util.function.BiPredicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.ingredients.IIngredientType;
import mezz.jei.api.ingredients.IIngredientTypeWithSubtypes;
import mezz.jei.api.ingredients.ITypedIngredient;
import mezz.jei.api.ingredients.subtypes.ISubtypeManager;
import mezz.jei.api.ingredients.subtypes.UidContext;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.recipe.vanilla.IJeiIngredientInfoRecipe;
import mezz.jei.api.registration.IModIngredientRegistration;
import mezz.jei.api.registration.IRuntimeRegistration;
import mezz.jei.api.registration.ISubtypeRegistration;
import mezz.jei.api.runtime.IClickableIngredient;
import mezz.jei.api.runtime.IIngredientManager;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.material.Fluid;

@JeiPlugin
public class JemiPlugin implements IModPlugin, EmiPlugin {
   private static final Map<EmiRecipeCategory, IRecipeCategory<?>> CATEGORY_MAP = Maps.newHashMap();
   private static ISubtypeManager subtypeManager;
   public static IJeiRuntime runtime;
   public static BiPredicate<IIngredientTypeWithSubtypes<? extends Object, ? extends Object>, Object> hasSubtype = (a, b) -> true;

   public ResourceLocation getPluginUid() {
      return EmiPort.id("emi:jemi");
   }

   public void registerItemSubtypes(ISubtypeRegistration registration) {
      hasSubtype = (type, ingredient) -> subtypeManager.hasSubtypes(type, ingredient);
   }

   public void registerIngredients(IModIngredientRegistration registration) {
      subtypeManager = registration.getSubtypeManager();
   }

   public void registerRuntime(IRuntimeRegistration registration) {
      registration.setIngredientListOverlay(new JemiIngredientListOverlay());
      registration.setBookmarkOverlay(new JemiBookmarkOverlay());
      registration.setRecipesGui(new JemiRecipesGui());
      registration.setIngredientFilter(new JemiIngredientFilter());
   }

   public void onRuntimeAvailable(IJeiRuntime runtime) {
      JemiPlugin.runtime = runtime;
   }

   public void onRuntimeUnavailable() {
      runtime = null;
   }

   public void register(EmiRegistry registry) {
      EmiLog.info("[JEMI] Waiting for JEI to finish reloading...");
      EmiReloadManager.step(EmiPort.literal("Waiting for JEI to finish..."), 20000L);

      try {
         while (runtime == null) {
            Thread.sleep(100L);
         }
      } catch (Exception var18) {
         return;
      }

      EmiLog.info("[JEMI] JEI reloaded!");
      Set<String> handledNamespaces = EmiAgnos.getPlugins().stream().<String>map(EmiPluginContainer::id).collect(Collectors.toSet());
      EmiReloadManager.step(EmiPort.literal("Loading information from JEI..."), 5000L);
      registry.addGenericExclusionArea((screen, consumer) -> {
         if (runtime != null && runtime.getScreenHelper() != null) {
            for (Rect2i rx : runtime.getScreenHelper().getGuiExclusionAreas(screen).toList()) {
               if (rx != null) {
                  consumer.accept(new Bounds(rx.getX(), rx.getY(), rx.getWidth(), rx.getHeight()));
               }
            }
         }
      });
      registry.addGenericStackProvider(
         (screen, x, y) -> new EmiStackInteraction(
               runtime.getScreenHelper()
                  .getClickableIngredientUnderMouse(screen, (double)x, (double)y)
                  .map(IClickableIngredient::getTypedIngredient)
                  .<EmiIngredient>map(JemiUtil::getStack)
                  .findFirst()
                  .orElse(EmiStack.EMPTY),
               null,
               false
            )
      );
      registry.addGenericDragDropHandler(new JemiDragDropHandler());
      registry.addIngredientSerializer(JemiStack.class, new JemiStackSerializer(runtime.getIngredientManager()));
      EmiReloadManager.step(EmiPort.literal("Processing JEI stacks..."), 5000L);

      for (IIngredientType<?> type : runtime.getIngredientManager().getRegisteredIngredientTypes()) {
         if (type != JemiUtil.getFluidType() && type != VanillaTypes.ITEM_STACK) {
            for (Object o : runtime.getIngredientManager().getAllIngredients(type)) {
               EmiStack stack = JemiUtil.getStack(type, o);
               if (!stack.isEmpty()) {
                  registry.addEmiStack(stack);
               }
            }
         }
      }

      registry.removeEmiStacks(s -> {
         try {
            Optional<ITypedIngredient<?>> opt = JemiUtil.getTyped(s);
            if (opt.isPresent()) {
               return !runtime.getIngredientVisibility().isIngredientVisible(opt.get());
            }
         } catch (Throwable var2x) {
         }

         return false;
      });
      EmiReloadManager.step(EmiPort.literal("Processing JEI subtypes..."), 5000L);
      safely("subtype comparison", () -> this.parseSubtypes(registry));
      EmiReloadManager.step(EmiPort.literal("Processing JEI recipes..."), 5000L);
      Set<ResourceLocation> existingCategories = EmiRecipes.categories.stream().<ResourceLocation>map(EmiRecipeCategory::getId).collect(Collectors.toSet());
      Map<RecipeType, EmiRecipeCategory> categoryMap = Maps.newHashMap();
      categoryMap.put(RecipeTypes.CRAFTING, VanillaEmiRecipeCategories.CRAFTING);
      categoryMap.put(RecipeTypes.SMELTING, VanillaEmiRecipeCategories.SMELTING);
      categoryMap.put(RecipeTypes.BLASTING, VanillaEmiRecipeCategories.BLASTING);
      categoryMap.put(RecipeTypes.SMOKING, VanillaEmiRecipeCategories.SMOKING);
      categoryMap.put(RecipeTypes.CAMPFIRE_COOKING, VanillaEmiRecipeCategories.CAMPFIRE_COOKING);
      categoryMap.put(RecipeTypes.STONECUTTING, VanillaEmiRecipeCategories.STONECUTTING);
      categoryMap.put(RecipeTypes.SMITHING, VanillaEmiRecipeCategories.SMITHING);
      categoryMap.put(RecipeTypes.ANVIL, VanillaEmiRecipeCategories.ANVIL_REPAIRING);
      categoryMap.put(RecipeTypes.BREWING, VanillaEmiRecipeCategories.BREWING);
      categoryMap.put(RecipeTypes.FUELING, VanillaEmiRecipeCategories.FUEL);
      categoryMap.put(RecipeTypes.COMPOSTING, VanillaEmiRecipeCategories.COMPOSTING);
      categoryMap.put(RecipeTypes.INFORMATION, VanillaEmiRecipeCategories.INFO);
      CATEGORY_MAP.clear();
      EmiRecipeFiller.extraHandlers = JemiPlugin::getRecipeHandler;

      for (IRecipeCategory<?> c : runtime.getRecipeManager().createRecipeCategoryLookup().includeHidden().get().toList()) {
         EmiLog.info("[JEMI] Collecting data for " + c.getTitle().getString());
         EmiReloadManager.step(EmiPort.literal("Loading JEI data for ").append(c.getTitle()), 5000L);

         try {
            RecipeType typex = c.getRecipeType();
            ResourceLocation id = typex.getUid();
            List<EmiStack> catalysts = runtime.getRecipeManager()
               .createRecipeCatalystLookup(typex)
               .includeHidden()
               .get()
               .<EmiStack>map(JemiUtil::getStack)
               .toList();
            if (categoryMap.containsKey(typex)) {
               EmiRecipeCategory category = categoryMap.get(typex);
               CATEGORY_MAP.put(category, c);

               for (EmiStack catalyst : catalysts) {
                  if (!catalyst.isEmpty()) {
                     registry.addWorkstation(category, catalyst);
                  }
               }

               if (typex == RecipeTypes.INFORMATION) {
                  this.addInfoRecipes(registry, (IRecipeCategory<IJeiIngredientInfoRecipe>)c);
               } else if (typex == RecipeTypes.CRAFTING) {
                  this.addCraftingRecipes(registry, (IRecipeCategory<RecipeHolder<CraftingRecipe>>)c);
               }
            } else if (handledNamespaces.contains(id.getNamespace())) {
               EmiLog.info("[JEMI] Skipping recipe category " + id + " because mod is already handled");
            } else if (existingCategories.contains(id)) {
               EmiLog.info("[JEMI] Skipping recipe category " + id + " because native EMI recipe category already exists");
            } else {
               EmiRecipeCategory category = new JemiCategory(c);
               CATEGORY_MAP.put(category, c);
               registry.addCategory(category);

               for (EmiStack catalystx : catalysts) {
                  if (!catalystx.isEmpty()) {
                     registry.addWorkstation(category, catalystx);
                  }
               }

               for (Object r : runtime.getRecipeManager().createRecipeLookup(typex).includeHidden().get().toList()) {
                  try {
                     registry.addRecipe(new JemiRecipe<>(category, (IRecipeCategory<Object>)c, r));
                  } catch (Throwable var16) {
                     EmiLog.error("Exception thrown adding adding JEI recipe", var16);
                  }
               }
            }
         } catch (Throwable var17) {
            EmiLog.error("Exception thrown adding adding JEI recipes", var17);
         }
      }
   }

   private void addInfoRecipes(EmiRegistry registry, IRecipeCategory<IJeiIngredientInfoRecipe> category) {
      List<IJeiIngredientInfoRecipe> recipes = runtime.getRecipeManager().createRecipeLookup(RecipeTypes.INFORMATION).includeHidden().get().toList();
      Map<List<EmiStack>, List<IJeiIngredientInfoRecipe>> grouped = Maps.newHashMap();

      for (IJeiIngredientInfoRecipe recipe : recipes) {
         grouped.computeIfAbsent(recipe.getIngredients().stream().<EmiStack>map(JemiUtil::getStack).toList(), k -> Lists.newArrayList()).add(recipe);
      }

      Map<Component, List<EmiStack>> identical = Maps.newHashMap();

      for (Entry<List<EmiStack>, List<IJeiIngredientInfoRecipe>> group : grouped.entrySet()) {
         MutableComponent text = EmiPort.literal("");

         for (IJeiIngredientInfoRecipe recipe : group.getValue()) {
            for (FormattedText sv : recipe.getDescription()) {
               MutableComponent current = EmiPort.literal("");
               sv.visit((style, string) -> {
                  current.append(EmiPort.literal(string, style));
                  return Optional.empty();
               }, Style.EMPTY);
               if (!current.getString().isBlank()) {
                  if (!text.getString().isEmpty()) {
                     text.append(" ");
                  }

                  text.append(current);
               }
            }
         }

         identical.computeIfAbsent(text, k -> Lists.newArrayList()).addAll(group.getKey());
      }

      for (Component text : identical.keySet()) {
         registry.addRecipe(new EmiInfoRecipe(identical.get(text).stream().map(s -> (EmiIngredient)s).toList(), List.of(text), null));
      }
   }

   private void addCraftingRecipes(EmiRegistry registry, IRecipeCategory<RecipeHolder<CraftingRecipe>> category) {
      Set<ResourceLocation> replaced = Sets.newHashSet();
      Set<EmiRecipe> replacements = Sets.newHashSet();

      for (RecipeHolder<CraftingRecipe> recipe : Stream.concat(
            runtime.getRecipeManager().createRecipeLookup(category.getRecipeType()).includeHidden().get(),
            registry.getRecipeManager()
               .getAllRecipesFor(net.minecraft.world.item.crafting.RecipeType.CRAFTING)
               .stream()
               .filter(r -> r.value() instanceof CustomRecipe)
         )
         .distinct()
         .toList()) {
         try {
            if (category.isHandled(recipe)) {
               JemiRecipeLayoutBuilder builder = new JemiRecipeLayoutBuilder();
               category.setRecipe(builder, recipe, runtime.getJeiHelpers().getFocusFactory().getEmptyFocusGroup());
               List<EmiIngredient> inputs = Lists.newArrayList();
               List<EmiStack> outputs = Lists.newArrayList();

               for (JemiIngredientAcceptor acceptor : builder.ingredients) {
                  EmiIngredient stack = acceptor.build();
                  if (acceptor.role == RecipeIngredientRole.INPUT) {
                     inputs.add(stack);
                  } else if (acceptor.role == RecipeIngredientRole.CATALYST) {
                     inputs.add(stack);
                  } else if (acceptor.role == RecipeIngredientRole.OUTPUT) {
                     outputs.addAll(stack.getEmiStacks());
                  }
               }

               if (inputs.stream().anyMatch(i -> !i.isEmpty()) && outputs.stream().anyMatch(o -> !o.isEmpty())) {
                  EmiRecipe replacement;
                  if (outputs.size() > 1) {
                     replacement = new 1(this, inputs, EmiStack.EMPTY, category.getRegistryName(recipe), builder.shapeless, outputs, inputs, recipe);
                  } else {
                     replacement = new EmiCraftingRecipe(inputs, outputs.get(0), category.getRegistryName(recipe), builder.shapeless);
                  }

                  if (replacement.getId() != null) {
                     replaced.add(replacement.getId());
                  }

                  replacements.add(replacement);
                  registry.addRecipe(replacement);
               }
            }
         } catch (Throwable var14) {
            EmiLog.error("[JEMI] Exception thrown setting JEI crafting recipe", var14);
         }
      }

      registry.removeRecipes(r -> r instanceof EmiCraftingRecipe && replaced.contains(r.getId()) && !replacements.contains(r));
   }

   private void parseSubtypes(EmiRegistry registry) {
      if (subtypeManager != null) {
         IIngredientManager im = runtime.getIngredientManager();
         List<IIngredientType<?>> types = Lists.newArrayList(im.getRegisteredIngredientTypes());

         for (Item item : EmiPort.getItemRegistry()) {
            if (hasSubtype.test(VanillaTypes.ITEM_STACK, item.getDefaultInstance())) {
               registry.setDefaultComparison(item, Comparison.compareData(stack -> subtypeManager.getSubtypeInfo(stack.getItemStack(), UidContext.Recipe)));
            }
         }

         for (Fluid fluid : EmiPort.getFluidRegistry()) {
            IIngredientTypeWithSubtypes<Object, Object> type = (IIngredientTypeWithSubtypes<Object, Object>)JemiUtil.getFluidType();
            if (hasSubtype.test(type, JemiUtil.getFluidHelper().create(fluid.builtInRegistryHolder(), 1000L))) {
               registry.setDefaultComparison(fluid, Comparison.compareData(stack -> {
                  ITypedIngredient<?> typed = (ITypedIngredient<?>)JemiUtil.getTyped(stack).orElse(null);
                  return typed != null ? subtypeManager.getSubtypeInfo(type, typed.getIngredient(), UidContext.Recipe) : null;
               }));
            }
         }

         for (IIngredientType<?> type : types) {
            if (type != VanillaTypes.ITEM_STACK && type != JemiUtil.getFluidType() && type instanceof IIngredientTypeWithSubtypes) {
               IIngredientTypeWithSubtypes iitws = (IIngredientTypeWithSubtypes)type;

               for (Object o : Lists.newArrayList(im.getAllIngredients(type))) {
                  try {
                     if (hasSubtype.test(iitws, o)) {
                        registry.setDefaultComparison(
                           iitws.getBase(o),
                           Comparison.compareData(
                              stack -> stack instanceof JemiStack jemi ? subtypeManager.getSubtypeInfo(iitws, jemi.ingredient, UidContext.Recipe) : null
                           )
                        );
                     }
                  } catch (Throwable var11) {
                     EmiReloadLog.warn("Exception adding default comparison for JEI ingredient", var11);
                  }
               }
            }
         }
      }
   }

   private static EmiRecipeHandler<?> getRecipeHandler(AbstractContainerMenu handler, EmiRecipe recipe) {
      IRecipeCategory<?> category = CATEGORY_MAP.getOrDefault(recipe.getCategory(), null);
      return category != null
         ? runtime.getRecipeTransferManager().getRecipeTransferHandler(handler, category).<EmiRecipeHandler<?>>map(JemiRecipeHandler::new).orElse(null)
         : null;
   }

   private static void safely(String name, Runnable runnable) {
      try {
         runnable.run();
      } catch (Throwable var3) {
         EmiReloadLog.warn("Exception thrown when reloading " + name + " step in JEMI plugin", var3);
      }
   }
}

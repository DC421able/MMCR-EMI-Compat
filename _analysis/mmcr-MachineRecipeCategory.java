package cn.howxu.mmcr.compat.jei;

import cn.howxu.mmcr.MMCR;
import cn.howxu.mmcr.api.machine.MachineDefinitions;
import cn.howxu.mmcr.api.machine.MachineRegistration;
import cn.howxu.mmcr.api.machine.level.LevelType;
import cn.howxu.mmcr.api.machine.level.MachineLevel;
import cn.howxu.mmcr.api.machine.level.MachineLevelRegistry;
import cn.howxu.mmcr.api.recipe.MachineRecipe;
import cn.howxu.mmcr.api.recipe.requirement.FluidRequirement;
import cn.howxu.mmcr.api.recipe.requirement.ItemRequirement;
import cn.howxu.mmcr.api.recipe.requirement.LevelRequirement;
import cn.howxu.mmcr.api.recipe.requirement.StageRequirement;
import cn.howxu.mmcr.compat.ae2lt.LightningRequirement;
import cn.howxu.mmcr.compat.ae2lt.client.LightningJeiDisplay;
import cn.howxu.mmcr.compat.ars_nouveau.ArsSourceIds;
import cn.howxu.mmcr.compat.ars_nouveau.client.SourceJeiIngredient;
import cn.howxu.mmcr.compat.botania.BotaniaManaIds;
import cn.howxu.mmcr.compat.botania.client.ManaJeiDisplay;
import cn.howxu.mmcr.compat.jei.MachineRecipeCategory.1;
import cn.howxu.mmcr.compat.jei.MachineRecipeCategory.2;
import cn.howxu.mmcr.compat.jei.MachineRecipeCategory.3;
import cn.howxu.mmcr.compat.jei.MachineRecipeDisplay.AirDisplay;
import cn.howxu.mmcr.compat.jei.MachineRecipeDisplay.FluidInputDisplay;
import cn.howxu.mmcr.compat.jei.MachineRecipeDisplay.FluidOutputDisplay;
import cn.howxu.mmcr.compat.jei.MachineRecipeDisplay.ItemInputDisplay;
import cn.howxu.mmcr.compat.jei.MachineRecipeDisplay.ItemOutputDisplay;
import cn.howxu.mmcr.compat.jei.MachineRecipeDisplay.SmartInterfaceDisplay;
import cn.howxu.mmcr.compat.jei.MachineRecipeDisplay.StressDisplay;
import cn.howxu.mmcr.compat.jei.MachineRecipeLayout.EntryPlan;
import cn.howxu.mmcr.compat.jei.MachineRecipeLayout.Kind;
import cn.howxu.mmcr.compat.jei.MachineRecipeLayout.ManaRowPlan;
import cn.howxu.mmcr.compat.jei.MachineRecipeLayout.RegionPlan;
import cn.howxu.mmcr.compat.jei.MachineRecipeLayout.SlotPlan;
import cn.howxu.mmcr.compat.jei.MachineRecipeLayout.TextPlan;
import cn.howxu.mmcr.compat.jei.RecipeSlotLayout.Arrow;
import cn.howxu.mmcr.compat.mekanism.MekanismRecipeTypes;
import cn.howxu.mmcr.compat.pneumaticcraft.PneumaticIds;
import cn.howxu.mmcr.internal.client.RecipeInformationRegistry;
import cn.howxu.mmcr.registry.ModBlocks;
import cn.howxu.mmcr.util.ReadableNumber;
import cn.howxu.mmcr.util.SaturatingLong;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.ToIntFunction;
import java.util.stream.Collectors;
import mekanism.api.chemical.ChemicalStack;
import mekanism.client.recipe_viewer.jei.MekanismJEI;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.IRecipeSlotBuilder;
import mezz.jei.api.gui.builder.ITooltipBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.ingredients.IIngredientRenderer;
import mezz.jei.api.ingredients.IIngredientTypeWithSubtypes;
import mezz.jei.api.neoforge.NeoForgeTypes;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.StringSplitter;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.neoforged.neoforge.common.MutableDataComponentHolder;
import net.neoforged.neoforge.fluids.FluidStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class MachineRecipeCategory implements IRecipeCategory<MachineRecipeDisplay> {
   private static final int FLUID_SLOT_CAPACITY = 1000;
   private static final float TEXT_SCALE = 0.85F;
   private static final int TEXT_LINE_SPACING = 10;
   private static final float SMART_INTERFACE_TEXT_SCALE = 0.85F;
   private static final int SMART_INTERFACE_LINE_SPACING = 10;
   private static final int JEI_SLOT_SIZE = 16;
   private static final int LEVEL_LABEL_SLOT_GAP = 2;
   private static final int LEVEL_ITEM_CYCLE_TICKS = 60;
   static final int ITEM_OVERLAY_X = 0;
   static final int ITEM_OVERLAY_Y = 0;
   static final float ITEM_OVERLAY_SCALE = 0.6F;
   private static final IIngredientRenderer<FluidStack> FULL_FLUID_RENDERER = new 1();
   private final Component title;
   private final RecipeType<MachineRecipeDisplay> recipeType;
   private final IDrawable icon;
   private final IDrawable sourceSlotBackground;
   @Nullable
   private final IDrawable stressIcon;
   @Nullable
   private final IDrawable airIcon;
   @Nullable
   private final IDrawable manaIcon;
   @Nullable
   private final IDrawable heatIcon;
   private final IGuiHelper guiHelper;

   public MachineRecipeCategory(IGuiHelper guiHelper, ResourceLocation poolId, ResourceLocation iconMachineId) {
      this.guiHelper = guiHelper;
      this.title = Component.translatable(poolId.toLanguageKey("recipe_pool"));
      this.recipeType = JeiMachineRecipeTypes.forPool(poolId);
      this.icon = guiHelper.createDrawableItemLike((ItemLike)ModBlocks.controllerFor(iconMachineId).get());
      this.sourceSlotBackground = guiHelper.drawableBuilder(MMCR.id("textures/gui/ars_nouveau/jei_source_slot.png"), 0, 0, 18, 18)
         .setTextureSize(18, 18)
         .build();
      ResourceLocation cogwheel = ResourceLocation.parse("create:cogwheel");
      this.stressIcon = BuiltInRegistries.ITEM.containsKey(cogwheel) ? guiHelper.createDrawableItemLike((ItemLike)BuiltInRegistries.ITEM.get(cogwheel)) : null;
      this.airIcon = BuiltInRegistries.ITEM.containsKey(PneumaticIds.ADVANCED_PRESSURE_TUBE)
         ? guiHelper.createDrawableItemLike((ItemLike)BuiltInRegistries.ITEM.get(PneumaticIds.ADVANCED_PRESSURE_TUBE))
         : null;
      this.manaIcon = BuiltInRegistries.ITEM.containsKey(BotaniaManaIds.CREATIVE_POOL)
         ? guiHelper.createDrawableItemLike((ItemLike)BuiltInRegistries.ITEM.get(BotaniaManaIds.CREATIVE_POOL))
         : null;
      ResourceLocation superheatingElement = ResourceLocation.parse("mekanism:superheating_element");
      this.heatIcon = BuiltInRegistries.ITEM.containsKey(superheatingElement)
         ? guiHelper.createDrawableItemLike((ItemLike)BuiltInRegistries.ITEM.get(superheatingElement))
         : null;
   }

   @NotNull
   public RecipeType<MachineRecipeDisplay> getRecipeType() {
      return this.recipeType;
   }

   @NotNull
   public Component getTitle() {
      return this.title;
   }

   public int getWidth() {
      return 168;
   }

   public int getHeight() {
      return MachineRecipeLayout.categoryHeight((int)Minecraft.getInstance().getWindow().getGuiScale());
   }

   @Nullable
   public IDrawable getIcon() {
      return this.icon;
   }

   public void setRecipe(IRecipeLayoutBuilder builder, MachineRecipeDisplay recipe, IFocusGroup focuses) {
      this.setRecipe(builder, recipe, MachineRecipeLayout.forDisplay(recipe), Minecraft.getInstance().font::width);
   }

   void setRecipe(IRecipeLayoutBuilder builder, MachineRecipeDisplay recipe, MachineRecipeLayout layout, ToIntFunction<Component> textWidth) {
      this.addRegion(builder, recipe, layout.inputs(), true);
      this.addRegion(builder, recipe, layout.outputs(), false);
      addLevelRequirementSlots(builder, layout, recipe, textWidth);
      addTransferSlots(builder, recipe);
      LightningJeiDisplay.addInvisibleIngredients(builder, recipe);
      builder.moveRecipeTransferButton(layout.transferButtonX(), layout.transferButtonY() - 3);
   }

   public void createRecipeExtras(IRecipeExtrasBuilder builder, MachineRecipeDisplay recipe, IFocusGroup focuses) {
      MachineRecipeLayout layout = MachineRecipeLayout.forDisplay(recipe);
      if (layout.arrow() != null) {
         addRecipeArrow(builder, this.guiHelper, layout.arrow());
      }

      if (!layout.metadataPages().isEmpty()) {
         List<IRecipeSlotDrawable> slots = builder.getRecipeSlots()
            .getSlots()
            .stream()
            .filter(slot -> slot.getSlotName().filter(name -> name.startsWith("mmcr_details_")).isPresent())
            .toList();
         MachineRecipeMetadataWidget widget = new MachineRecipeMetadataWidget(this, recipe, layout, slots);
         builder.addSlottedWidget(widget, slots);
         builder.addGuiEventListener(widget);
      }
   }

   static void addRecipeArrow(IRecipeExtrasBuilder builder, IGuiHelper guiHelper, Arrow arrow) {
      boolean vertical = arrow.height() > arrow.width();
      IDrawable drawable = new RecipeArrowDrawable(guiHelper.createAnimatedRecipeArrow(200), vertical);
      builder.addDrawable(drawable, arrow.x(), arrow.y());
   }

   public void draw(MachineRecipeDisplay recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics guiGraphics, double mouseX, double mouseY) {
      MachineRecipeLayout layout = MachineRecipeLayout.forDisplay(recipe);
      drawTextEntries(recipe, layout.inputs(), true, guiGraphics);
      drawTextEntries(recipe, layout.outputs(), false, guiGraphics);
      if (layout.metadataPages().isEmpty()) {
         this.drawMetadata(recipe, layout, guiGraphics);
      }
   }

   void drawMetadata(MachineRecipeDisplay recipe, MachineRecipeLayout layout, GuiGraphics guiGraphics) {
      long gameTime = Minecraft.getInstance().level == null ? 0L : Minecraft.getInstance().level.getGameTime();
      guiGraphics.pose().pushPose();
      guiGraphics.pose().scale(0.85F, 0.85F, 1.0F);
      int textX = (int)((float)layout.durationTextX() / 0.85F);
      guiGraphics.drawString(
         Minecraft.getInstance().font,
         Component.translatable("jei.mmcr.machine_recipe.duration", new Object[]{recipe.durationTicks(), seconds(recipe.durationTicks())}),
         textX,
         (int)((float)layout.durationTextY() / 0.85F),
         -12566464,
         false
      );
      int y = layout.durationTextY() + 10;

      for (EnergyIngredient energy : recipe.energyInputs()) {
         guiGraphics.drawString(
            Minecraft.getInstance().font,
            Component.translatable(
               "jei.mmcr.machine_recipe.energy_in",
               new Object[]{ReadableNumber.format(energy.fePerTick()), ReadableNumber.format(saturatedEnergyTotal(energy.fePerTick(), recipe.durationTicks()))}
            ),
            textX,
            (int)((float)y / 0.85F),
            -12566464,
            false
         );
         y += 10;
      }

      for (EnergyIngredient energy : recipe.energyOutputs()) {
         guiGraphics.drawString(
            Minecraft.getInstance().font,
            Component.translatable("jei.mmcr.machine_recipe.energy_out", new Object[]{ReadableNumber.format(energy.fePerTick())}),
            textX,
            (int)((float)y / 0.85F),
            -12566464,
            false
         );
         y += 10;
      }

      for (StressDisplay stress : recipe.stressInputs()) {
         this.drawStressLine(stress.label(true), guiGraphics, textX, (int)((float)y / 0.85F));
         y += 10;
      }

      for (StressDisplay stress : recipe.stressOutputs()) {
         this.drawStressLine(stress.label(false), guiGraphics, textX, (int)((float)y / 0.85F));
         y += 10;
      }

      y = layout.airTextY(recipe);

      for (AirDisplay air : recipe.airInputs()) {
         this.drawAirLine(air.label(true), guiGraphics, textX, (int)((float)y / 0.85F));
         y += 10;
      }

      for (AirDisplay air : recipe.airOutputs()) {
         this.drawAirLine(air.label(false), guiGraphics, textX, (int)((float)y / 0.85F));
         y += 10;
      }

      if (recipe.minimumTemperature().isPresent()) {
         this.drawHeatLine(MachineRecipeDisplay.minimumTemperatureLabel(recipe.minimumTemperature().getAsDouble()), guiGraphics, textX, (int)((float)y / 0.85F));
         y += 10;
      }

      if (recipe.outputHeat().isPresent()) {
         this.drawHeatLine(MachineRecipeDisplay.outputHeatLabel(recipe.outputHeat().getAsDouble()), guiGraphics, textX, (int)((float)y / 0.85F));
         y += 10;
      }

      for (ManaRowPlan row : layout.manaRows()) {
         ManaJeiDisplay mana = (ManaJeiDisplay)row.entry().ingredient();
         this.drawManaLine(
            MachineRecipeDisplay.withChanceLabel(mana.label(), row.entry().chance(), mana.input()), guiGraphics, textX, (int)((float)row.y() / 0.85F)
         );
      }

      y = layout.lightningTextY(recipe);
      int lightningWidth = (int)((float)(this.getWidth() - layout.durationTextX() * 2) / 0.85F);

      for (LightningRequirement lightning : LightningJeiDisplay.requirements(recipe)) {
         LightningJeiDisplay.drawLine(guiGraphics, lightning, textX, (int)((float)y / 0.85F), lightningWidth);
         y += 10;
      }

      Component hostRequirement = hostRequirementComponent(recipe, gameTime);
      if (!hostRequirement.getString().isEmpty()) {
         y = layout.hostRequirementTextY();
         guiGraphics.drawString(Minecraft.getInstance().font, hostRequirement, textX, (int)((float)y / 0.85F), -12566464, false);
         y += 10;
      }

      drawLevelRequirementLabels(recipe, layout, guiGraphics, textX);
      drawStageRequirementLabels(recipe, layout, guiGraphics, textX);
      y = layout.smartInterfaceTextY(recipe);
      guiGraphics.pose().popPose();
      guiGraphics.pose().pushPose();
      guiGraphics.pose().scale(0.85F, 0.85F, 1.0F);
      textX = (int)((float)layout.durationTextX() / 0.85F);

      for (SmartInterfaceDisplay smartInterface : recipe.smartInterfaceInputs()) {
         guiGraphics.drawString(Minecraft.getInstance().font, smartInterface.label(), textX, (int)((float)y / 0.85F), -12566464, false);
         y += 10;
      }

      for (SmartInterfaceDisplay smartInterface : recipe.smartInterfaceOutputs()) {
         guiGraphics.drawString(Minecraft.getInstance().font, smartInterface.label(), textX, (int)((float)y / 0.85F), -12566464, false);
         y += 10;
      }

      guiGraphics.pose().popPose();
      drawSourceTextLines(layout, guiGraphics);
      this.drawRecipeInformation(recipe, layout, guiGraphics);
   }

   private void drawStressLine(Component label, GuiGraphics guiGraphics, int x, int y) {
      Font font = Minecraft.getInstance().font;
      if (this.stressIcon != null) {
         float iconScale = 9.0F / (float)this.stressIcon.getHeight();
         guiGraphics.pose().pushPose();
         guiGraphics.pose().translate((float)x, (float)y, 0.0F);
         guiGraphics.pose().scale(iconScale, iconScale, 1.0F);
         this.stressIcon.draw(guiGraphics, 0, 0);
         guiGraphics.pose().popPose();
         x += (int)((float)this.stressIcon.getWidth() * iconScale) + 2;
      }

      guiGraphics.drawString(font, label, x, y, -12566464, false);
   }

   private void drawAirLine(Component label, GuiGraphics guiGraphics, int x, int y) {
      Font font = Minecraft.getInstance().font;
      if (this.airIcon != null) {
         float iconScale = 9.0F / (float)this.airIcon.getHeight();
         guiGraphics.pose().pushPose();
         guiGraphics.pose().translate((float)x, (float)y, 0.0F);
         guiGraphics.pose().scale(iconScale, iconScale, 1.0F);
         this.airIcon.draw(guiGraphics, 0, 0);
         guiGraphics.pose().popPose();
         x += (int)((float)this.airIcon.getWidth() * iconScale) + 2;
      }

      guiGraphics.drawString(font, label, x, y, -12566464, false);
   }

   private void drawManaLine(Component label, GuiGraphics guiGraphics, int x, int y) {
      Font font = Minecraft.getInstance().font;
      if (this.manaIcon != null) {
         float iconScale = 9.0F / (float)this.manaIcon.getHeight();
         guiGraphics.pose().pushPose();
         guiGraphics.pose().translate((float)x, (float)y, 0.0F);
         guiGraphics.pose().scale(iconScale, iconScale, 1.0F);
         this.manaIcon.draw(guiGraphics, 0, 0);
         guiGraphics.pose().popPose();
         x += (int)((float)this.manaIcon.getWidth() * iconScale) + 2;
      }

      guiGraphics.drawString(font, label, x, y, -12566464, false);
   }

   private void drawHeatLine(Component label, GuiGraphics guiGraphics, int x, int y) {
      Font font = Minecraft.getInstance().font;
      if (this.heatIcon != null) {
         float iconScale = 9.0F / (float)this.heatIcon.getHeight();
         guiGraphics.pose().pushPose();
         guiGraphics.pose().translate((float)x, (float)y, 0.0F);
         guiGraphics.pose().scale(iconScale, iconScale, 1.0F);
         this.heatIcon.draw(guiGraphics, 0, 0);
         guiGraphics.pose().popPose();
         x += (int)((float)this.heatIcon.getWidth() * iconScale) + 2;
      }

      guiGraphics.drawString(font, label, x, y, -12566464, false);
   }

   private void drawRecipeInformation(MachineRecipeDisplay recipe, MachineRecipeLayout layout, GuiGraphics guiGraphics) {
      List<Component> information = RecipeInformationRegistry.componentsFor(recipe.recipePoolId(), recipe.recipeId());
      int lineCount = Math.min(information.size(), layout.informationLineCapacity(recipe, this.getHeight()));
      if (lineCount != 0) {
         Font font = Minecraft.getInstance().font;
         int margin = layout.durationTextX();
         int maxTextWidth = (int)((float)(this.getWidth() - margin * 2) / 0.85F);
         int textX = (int)((float)margin / 0.85F);
         int y = layout.informationTextY(recipe);
         guiGraphics.pose().pushPose();
         guiGraphics.pose().scale(0.85F, 0.85F, 1.0F);

         for (int index = 0; index < lineCount; index++) {
            List<FormattedCharSequence> lines = font.split((FormattedText)information.get(index), maxTextWidth);
            if (!lines.isEmpty()) {
               guiGraphics.drawString(font, lines.getFirst(), textX, (int)((float)y / 0.85F), -12566464, false);
            }

            y += 10;
         }

         guiGraphics.pose().popPose();
      }
   }

   public void getTooltip(ITooltipBuilder tooltip, MachineRecipeDisplay recipe, IRecipeSlotsView recipeSlotsView, double mouseX, double mouseY) {
      MachineRecipeLayout layout = MachineRecipeLayout.forDisplay(recipe);
      if (layout.metadataPages().isEmpty()) {
         this.metadataTooltip(tooltip, recipe, layout, mouseX, mouseY);
      }
   }

   void metadataTooltip(ITooltipBuilder tooltip, MachineRecipeDisplay recipe, MachineRecipeLayout layout, double mouseX, double mouseY) {
      if (!this.appendStressTooltip(tooltip, recipe, layout, mouseX, mouseY)) {
         if (!appendAirTooltip(tooltip, recipe, layout, mouseX, mouseY)) {
            for (ManaRowPlan row : layout.manaRows()) {
               if (mouseX >= (double)row.x()
                  && mouseX < (double)(row.x() + row.width())
                  && mouseY >= (double)row.y()
                  && mouseY < (double)(row.y() + row.height())
                  && row.entry().chance() < 1.0F) {
                  ManaJeiDisplay mana = (ManaJeiDisplay)row.entry().ingredient();
                  tooltip.add(
                     MachineRecipeDisplay.withChanceLabel(
                        Component.translatable(
                           mana.input() ? "jei.mmcr.machine_recipe.mana_input" : "jei.mmcr.machine_recipe.mana_output",
                           new Object[]{ReadableNumber.formatExact(mana.amount())}
                        ),
                        row.entry().chance(),
                        mana.input()
                     )
                  );
                  return;
               }
            }

            lightningTooltip(recipe, layout, mouseX, mouseY).ifPresent(tooltip::add);
            sourceTooltip(layout, mouseX, mouseY).or(() -> smartInterfaceTooltip(recipe, layout, mouseX, mouseY)).ifPresent(tooltip::add);
         }
      }
   }

   @Nullable
   public ResourceLocation getRegistryName(MachineRecipeDisplay recipe) {
      return recipe.recipeId();
   }

   private static String seconds(int ticks) {
      return String.format(Locale.ROOT, "%.1f", (float)ticks / 20.0F);
   }

   static String inputOverlayText(float consumeChance, String language) {
      if (consumeChance == 0.0F) {
         return language.startsWith("zh") ? "不消耗" : "Keep";
      } else {
         return consumeChance < 1.0F ? Math.round(consumeChance * 100.0F) + "%" : "";
      }
   }

   static String outputOverlayText(float chance) {
      return chance < 1.0F ? Math.round(chance * 100.0F) + "%" : "";
   }

   static Component levelLabel(LevelRequirement requirement) {
      LevelType type = MachineLevelRegistry.getType(requirement.typeId());
      return type == null ? Component.empty() : type.displayName().copy().append(Component.literal(": "));
   }

   static List<ItemStack> levelCandidates(LevelRequirement requirement) {
      MachineLevel required = MachineLevelRegistry.getLevel(requirement.levelId());
      return required == null
         ? List.of()
         : MachineLevelRegistry.levelsForType(requirement.typeId())
            .stream()
            .filter(level -> level.priority() >= required.priority())
            .sorted(Comparator.comparingInt(MachineLevel::priority).thenComparing(level -> level.id().toString()))
            .map(MachineRecipeCategory::levelDisplayStack)
            .filter(stack -> !stack.isEmpty())
            .toList();
   }

   private static ItemStack levelDisplayStack(MachineLevel level) {
      ItemStack representative = level.representative();
      return !representative.isEmpty()
         ? new ItemStack(representative.getItem().builtInRegistryHolder(), 1, representative.getComponentsPatch())
         : level.statePredicate()
            .preferredState()
            .map(state -> new ItemStack(state.getBlock().asItem()))
            .map(MachineRecipeCategory::jeiItemStack)
            .orElse(ItemStack.EMPTY);
   }

   static ItemStack levelCandidate(LevelRequirement requirement, long gameTime) {
      List<ItemStack> candidates = levelCandidates(requirement);
      return candidates.isEmpty() ? ItemStack.EMPTY : candidates.get((int)(gameTime / 60L % (long)candidates.size()));
   }

   private static boolean isMinimumLevelCandidate(LevelRequirement requirement) {
      List<ItemStack> candidates = levelCandidates(requirement);
      if (candidates.isEmpty()) {
         return false;
      } else {
         Minecraft minecraft = Minecraft.getInstance();
         long gameTime = minecraft.level == null ? 0L : minecraft.level.getGameTime();
         return ItemStack.isSameItemSameComponents(levelCandidate(requirement, gameTime), candidates.getFirst());
      }
   }

   static Component minimumLevelTooltip(LevelRequirement requirement) {
      return MachineLevelRegistry.getLevel(requirement.levelId()) == null ? Component.empty() : Component.translatable("jei.mmcr.machine_recipe.minimum_level");
   }

   static void addLevelRequirementSlots(
      IRecipeLayoutBuilder builder, MachineRecipeLayout layout, MachineRecipeDisplay recipe, ToIntFunction<Component> labelWidth
   ) {
      int index = 0;

      for (LevelRequirement requirement : sortedLevelRequirements(recipe.recipe())) {
         Component label = levelLabel(requirement);
         int slotX = levelSlotX(layout.durationTextX(), labelWidth.applyAsInt(label));
         int slotY = layout.levelRequirementSlotY(recipe, index);
         IRecipeSlotBuilder slot = builder.addSlot(RecipeIngredientRole.RENDER_ONLY, slotX, slotY);
         slot.setSlotName("mmcr_details_level_" + index++);
         slot.setStandardSlotBackground();
         slot.setCustomRenderer(VanillaTypes.ITEM_STACK, levelItemRenderer(requirement));
         List<ItemStack> candidates = levelCandidates(requirement);
         if (!candidates.isEmpty()) {
            slot.addItemStack(candidates.getFirst());
         }

         slot.addRichTooltipCallback((view, tooltip) -> {
            if (isMinimumLevelCandidate(requirement)) {
               tooltip.add(minimumLevelTooltip(requirement));
            }
         });
      }
   }

   private static int levelSlotX(int textX, int labelWidth) {
      return textX + labelWidth + 2;
   }

   private static IIngredientRenderer<ItemStack> levelItemRenderer(LevelRequirement requirement) {
      return new 2(requirement);
   }

   private static void drawLevelRequirementLabels(MachineRecipeDisplay recipe, MachineRecipeLayout layout, GuiGraphics guiGraphics, int textX) {
      int index = 0;

      for (LevelRequirement requirement : sortedLevelRequirements(recipe.recipe())) {
         int slotY = layout.levelRequirementSlotY(recipe, index++);
         int labelY = slotY + (16 - 9) / 2 + 2;
         guiGraphics.drawString(Minecraft.getInstance().font, levelLabel(requirement), textX, (int)((float)labelY / 0.85F), -12566464, false);
      }
   }

   private static void drawStageRequirementLabels(MachineRecipeDisplay recipe, MachineRecipeLayout layout, GuiGraphics guiGraphics, int textX) {
      int y = layout.stageRequirementTextY(recipe);

      for (StageRequirement requirement : recipe.recipe().stageRequirements()) {
         guiGraphics.drawString(Minecraft.getInstance().font, stageRequirementLabel(requirement), textX, (int)((float)y / 0.85F), -12566464, false);
         y += 10;
      }
   }

   static Component stageRequirementLabel(StageRequirement requirement) {
      return Component.translatable("jei.mmcr.machine_recipe.minimum_stage", new Object[]{requirement.minStage()});
   }

   static Component hostRequirementComponent(MachineRecipeDisplay recipe, long gameTime) {
      if (recipe.requiredHostIds().isEmpty()) {
         return Component.empty();
      } else {
         List<ResourceLocation> hostIds = List.copyOf(recipe.requiredHostIds());
         int index = (int)(gameTime / 20L % (long)hostIds.size());
         ResourceLocation hostId = hostIds.get(index);
         MachineRegistration registration = MachineDefinitions.getRegistration(hostId);
         Component hostName = (Component)(registration == null ? Component.literal(hostId.toString()) : registration.displayName());
         return Component.translatable("jei.mmcr.machine_recipe.required_host", new Object[]{hostName});
      }
   }

   private static List<LevelRequirement> sortedLevelRequirements(MachineRecipe recipe) {
      return recipe.levelRequirements()
         .stream()
         .sorted(
            Comparator.<LevelRequirement, String>comparing(requirement -> requirement.typeId().toString())
               .thenComparing(requirement -> requirement.levelId().toString())
         )
         .toList();
   }

   private void addRegion(IRecipeLayoutBuilder builder, MachineRecipeDisplay recipe, RegionPlan region, boolean input) {
      for (SlotPlan slot : region.slots()) {
         this.addEntry(builder, recipe, slot, input);
      }
   }

   private void addEntry(IRecipeLayoutBuilder builder, MachineRecipeDisplay recipe, SlotPlan slot, boolean input) {
      Optional<JeiDisplayEntry> entry = displayEntry(recipe, slot.entry(), input);
      boolean source = entry.isPresent() && entry.get().typeId().equals(ArsSourceIds.SOURCE);
      IRecipeSlotBuilder jeiSlot = builder.addSlot(source ? entry.get().role() : RecipeIngredientRole.RENDER_ONLY, slot.x(), slot.y());
      if (source) {
         jeiSlot.setBackground(this.sourceSlotBackground, -1, -1);
      } else {
         jeiSlot.setStandardSlotBackground();
      }

      if (!entry.isEmpty()) {
         if (entry.get().typeId().equals(ItemRequirement.TYPE.id())) {
            addItem(jeiSlot, recipe, slot.entry(), input);
         } else if (entry.get().typeId().equals(FluidRequirement.TYPE.id())) {
            addFluid(jeiSlot, recipe, slot.entry(), entry.get(), input);
         } else {
            addGeneric(jeiSlot, entry.get());
         }
      }
   }

   // $VF: Unable to simplify switch on enum
   // Please report this to the Vineflower issue tracker, at https://github.com/Vineflower/vineflower/issues with a copy of the class file (if you have the rights to distribute it!)
   private static Optional<JeiDisplayEntry> displayEntry(MachineRecipeDisplay recipe, EntryPlan plan, boolean input) {
      if (plan.displayEntry() != null) {
         return Optional.of(plan.displayEntry());
      } else {
         IIngredientTypeWithSubtypes<? extends Object, ? extends MutableDataComponentHolder> type = switch (3.$SwitchMap$cn$howxu$mmcr$compat$jei$MachineRecipeLayout$Kind[plan.kind()
            .ordinal()]) {
            case 1 -> VanillaTypes.ITEM_STACK;
            case 2 -> NeoForgeTypes.FLUID_STACK;
            case 3 -> null;
            case 4, 5 -> null;
            default -> throw new MatchException(null, null);
         };
         return recipe.entries()
            .stream()
            .filter(entry -> entry.role() == (input ? RecipeIngredientRole.INPUT : RecipeIngredientRole.OUTPUT))
            .filter(entry -> plan.kind() == Kind.CHEMICAL ? entry.typeId().equals(MekanismRecipeTypes.CHEMICAL) : entry.ingredientType() == type)
            .skip((long)plan.index())
            .findFirst();
      }
   }

   static void addGeneric(IRecipeSlotBuilder slot, JeiDisplayEntry entry) {
      if (entry.isTextOnly()) {
         slot.addRichTooltipCallback((view, tooltip) -> tooltip.add((Component)entry.ingredient()));
      } else if (entry.ingredient() instanceof SourceJeiIngredient source) {
         String chance = source.input() ? inputOverlayText(entry.chance(), selectedLanguage()) : outputOverlayText(entry.chance());
         setItemOverlay(slot, chance, ReadableNumber.formatForSlot(source.amount(), 0, ""));
         slot.addIngredient(SourceJeiIngredient.TYPE, source);
         if (source.input()) {
            slot.addRichTooltipCallback((view, tooltip) -> appendConsumeChanceTooltip(tooltip, entry.chance(), true));
         } else {
            slot.addRichTooltipCallback((view, tooltip) -> appendOutputChanceTooltip(tooltip, entry.chance()));
         }
      } else {
         boolean chemical = entry.typeId().equals(MekanismRecipeTypes.CHEMICAL);
         String quantity = entry.ingredientType() == VanillaTypes.ITEM_STACK
            ? itemQuantityText((long)entry.count())
            : (chemical ? chemicalQuantityText((long)entry.count()) : fluidQuantityText(entry.count()));
         if (entry.ingredientType() == VanillaTypes.ITEM_STACK) {
            String chance = entry.role() == RecipeIngredientRole.INPUT
               ? inputOverlayText(entry.chance(), selectedLanguage())
               : outputOverlayText(entry.chance());
            setItemOverlay(slot, chance, quantity);
         } else if (chemical) {
            if (entry.role() == RecipeIngredientRole.INPUT) {
               setItemOverlay(slot, inputOverlayText(entry.chance(), selectedLanguage()), quantity);
            } else {
               setItemOverlay(slot, outputOverlayText(entry.chance()), quantity);
            }

            slot.addRichTooltipCallback((view, tooltip) -> {
               appendChemicalQuantityTooltip(tooltip, (long)entry.count());
               if (entry.role() == RecipeIngredientRole.INPUT) {
                  appendConsumeChanceTooltip(tooltip, entry.chance(), true);
               } else {
                  appendOutputChanceTooltip(tooltip, entry.chance());
               }
            });
         } else {
            setQuantityOverlay(slot, quantity);
         }

         if (entry.renderer() != null) {
            slot.setCustomRenderer(entry.ingredientType(), entry.renderer());
         }

         if (entry.ingredientType() == NeoForgeTypes.FLUID_STACK) {
            slot.setCustomRenderer(NeoForgeTypes.FLUID_STACK, FULL_FLUID_RENDERER);
         }

         if (entry.ingredient() instanceof List<?> ingredients) {
            slot.addIngredients(entry.ingredientType(), ingredients);
         } else {
            slot.addIngredient(entry.ingredientType(), entry.ingredient());
         }
      }
   }

   private static void drawTextEntries(MachineRecipeDisplay recipe, RegionPlan region, boolean input, GuiGraphics guiGraphics) {
      for (SlotPlan slot : region.slots()) {
         if (slot.entry().kind() == Kind.TEXT) {
            displayEntry(recipe, slot.entry(), input)
               .ifPresent(
                  entry -> guiGraphics.drawString(Minecraft.getInstance().font, (Component)entry.ingredient(), slot.x(), slot.y() + 4, -12566464, false)
               );
         }
      }
   }

   private static void drawSourceTextLines(MachineRecipeLayout layout, GuiGraphics guiGraphics) {
      Font font = Minecraft.getInstance().font;
      guiGraphics.pose().pushPose();
      guiGraphics.pose().scale(0.85F, 0.85F, 1.0F);

      for (TextPlan line : layout.sourceTextLines()) {
         guiGraphics.drawString(
            font,
            Language.getInstance().getVisualOrder(sourceTextLine(line, font.getSplitter())),
            (int)((float)line.x() / 0.85F),
            (int)((float)line.y() / 0.85F),
            -12566464,
            false
         );
      }

      guiGraphics.pose().popPose();
   }

   static FormattedText sourceTextLine(TextPlan line, StringSplitter splitter) {
      return splitter.headByWidth((Component)line.entry().ingredient(), (int)((float)line.width() / 0.85F), Style.EMPTY);
   }

   static Optional<Component> sourceTooltip(MachineRecipeLayout layout, double mouseX, double mouseY) {
      return layout.sourceTextLines().stream().filter(line -> line.contains(mouseX, mouseY)).map(line -> (Component)line.entry().ingredient()).findFirst();
   }

   private static void addTransferSlots(IRecipeLayoutBuilder builder, MachineRecipeDisplay recipe) {
      for (FluidInputDisplay fluidDisplay : recipe.fluidInputs()) {
         IRecipeSlotBuilder slot = builder.addInputSlot(-1000, -1000);
         if (fluidDisplay.ingredient() != null) {
            int amount = fluidDisplay.amount();

            for (FluidStack fluid : fluidDisplay.ingredient().getStacks()) {
               slot.addFluidStack(fluid.getFluid(), (long)amount);
            }
         }
      }

      for (ItemInputDisplay item : recipe.itemInputs()) {
         IRecipeSlotBuilder slot = builder.addInputSlot(-1000, -1000);
         addActualItem(slot, item);
      }

      for (FluidOutputDisplay output : recipe.fluidOutputs()) {
         FluidStack fluid = output.stack();
         builder.addOutputSlot(-1000, -1000).addFluidStack(fluid.getFluid(), (long)fluid.getAmount(), fluid.getComponentsPatch());
      }

      for (ItemOutputDisplay output : recipe.itemOutputs()) {
         builder.addOutputSlot(-1000, -1000).addItemStack(output.stack());
      }

      for (JeiDisplayEntry entry : recipe.entries()) {
         if (!entry.typeId().equals(ItemRequirement.TYPE.id()) && !entry.typeId().equals(FluidRequirement.TYPE.id())) {
            if (entry.typeId().equals(MekanismRecipeTypes.CHEMICAL)) {
               addChemicalTransferSlot(builder, entry);
            } else if (entry.role() == RecipeIngredientRole.INPUT && entry.transferable()) {
               IRecipeSlotBuilder slot = builder.addInputSlot(-1000, -1000);
               addGeneric(slot, entry);
            }
         }
      }
   }

   private static void addChemicalTransferSlot(IRecipeLayoutBuilder builder, JeiDisplayEntry entry) {
      IRecipeSlotBuilder slot = entry.role() == RecipeIngredientRole.INPUT ? builder.addInputSlot(-1000, -1000) : builder.addOutputSlot(-1000, -1000);

      for (ChemicalStack chemical : actualChemicalStacks(entry.ingredient())) {
         slot.addIngredient(MekanismJEI.TYPE_CHEMICAL, chemical.copyWithAmount((long)entry.count()));
      }
   }

   private static List<ChemicalStack> actualChemicalStacks(Object ingredient) {
      if (ingredient instanceof List<?> ingredients) {
         return ingredients.stream().filter(ChemicalStack.class::isInstance).map(ChemicalStack.class::cast).filter(stack -> !stack.isEmpty()).toList();
      } else {
         if (ingredient instanceof ChemicalStack chemical && !chemical.isEmpty()) {
            return List.of(chemical);
         }

         return List.of();
      }
   }

   private static void addActualItem(IRecipeSlotBuilder slot, ItemInputDisplay item) {
      List<ItemStack> stacks = item.stacks();
      if (stacks.isEmpty() && item.ingredient() != null) {
         slot.addIngredients(item.ingredient());
      } else {
         slot.addItemStacks(stacks);
      }
   }

   private static void addItem(IRecipeSlotBuilder jeiSlot, MachineRecipeDisplay recipe, EntryPlan entry, boolean input) {
      if (input) {
         ItemInputDisplay item = (ItemInputDisplay)recipe.itemInputs().get(entry.index());
         String overlayText = inputOverlayText(item.consumeChance(), selectedLanguage());
         setItemOverlay(jeiSlot, overlayText, itemQuantityText((long)item.count()));
         jeiSlot.addRichTooltipCallback((view, tooltip) -> appendInputTooltip(tooltip, item));
         List<ItemStack> stacks = item.stacks().stream().map(MachineRecipeCategory::jeiItemStack).toList();
         if (stacks.isEmpty() && item.ingredient() != null) {
            jeiSlot.addIngredients(item.ingredient());
         } else {
            jeiSlot.addItemStacks(stacks);
         }
      } else {
         ItemOutputDisplay output = (ItemOutputDisplay)recipe.itemOutputs().get(entry.index());
         ItemStack stack = output.stack();
         String overlayText = outputOverlayText(output.chance());
         setItemOverlay(jeiSlot, overlayText, itemQuantityText((long)stack.getCount()));
         jeiSlot.addRichTooltipCallback((view, tooltip) -> appendOutputTooltip(tooltip, output));
         jeiSlot.addItemStack(jeiItemStack(stack));
      }
   }

   static ItemStack jeiItemStack(ItemStack stack) {
      return stack.copyWithCount(1);
   }

   static String describeAddedItemStack(ItemStack stack) {
      if (stack != null && !stack.isEmpty()) {
         String itemId = stack.getItem().builtInRegistryHolder().getRegisteredName();
         String components = stack.getComponents().keySet().stream().map(type -> type + "=" + stack.get(type)).collect(Collectors.joining(", ", "[", "]"));
         String patch = stack.getComponentsPatch()
            .entrySet()
            .stream()
            .map(entry -> entry.getKey() + "=" + entry.getValue())
            .collect(Collectors.joining(", ", "[", "]"));
         return itemId + " x" + stack.getCount() + " name=" + stack.getHoverName().getString() + " components=" + components + " patch=" + patch;
      } else {
         return "<empty>";
      }
   }

   private static void addFluid(IRecipeSlotBuilder jeiSlot, MachineRecipeDisplay recipe, EntryPlan entry, JeiDisplayEntry display, boolean input) {
      if (input) {
         FluidInputDisplay fluidDisplay = (FluidInputDisplay)recipe.fluidInputs().get(entry.index());
         if (!(display.ingredient() instanceof FluidStack fluid) || fluid.isEmpty()) {
            return;
         }

         setItemOverlay(jeiSlot, inputOverlayText(fluidDisplay.consumeChance(), selectedLanguage()), fluidQuantityText(fluidDisplay.amount()));
         jeiSlot.setCustomRenderer(NeoForgeTypes.FLUID_STACK, FULL_FLUID_RENDERER).addFluidStack(fluid.getFluid(), 1000L, fluid.getComponentsPatch());
         jeiSlot.addRichTooltipCallback((view, tooltip) -> {
            appendFluidQuantityTooltip(tooltip, fluidDisplay.amount());
            appendConsumeChanceTooltip(tooltip, fluidDisplay.consumeChance(), true);
         });
      } else {
         FluidOutputDisplay output = (FluidOutputDisplay)recipe.fluidOutputs().get(entry.index());
         FluidStack stack = output.stack();
         setItemOverlay(jeiSlot, outputOverlayText(output.chance()), fluidQuantityText(stack.getAmount()));
         jeiSlot.setCustomRenderer(NeoForgeTypes.FLUID_STACK, FULL_FLUID_RENDERER).addFluidStack(stack.getFluid(), 1000L, stack.getComponentsPatch());
         jeiSlot.addRichTooltipCallback((view, tooltip) -> {
            appendFluidQuantityTooltip(tooltip, stack.getAmount());
            appendOutputChanceTooltip(tooltip, output.chance());
         });
      }
   }

   private static String selectedLanguage() {
      Minecraft minecraft = Minecraft.getInstance();
      return minecraft != null && minecraft.getLanguageManager() != null ? minecraft.getLanguageManager().getSelected() : "en_us";
   }

   private static void setItemOverlay(IRecipeSlotBuilder jeiSlot, String chanceText, String quantityText) {
      if (!chanceText.isEmpty() || !quantityText.isEmpty()) {
         jeiSlot.setOverlay(new JeiSlotOverlayDrawable(chanceText, quantityText), 0, 0);
      }
   }

   private static void setQuantityOverlay(IRecipeSlotBuilder jeiSlot, String quantityText) {
      if (!quantityText.isEmpty()) {
         jeiSlot.setOverlay(new JeiSlotOverlayDrawable("", quantityText), 0, 0);
      }
   }

   static String itemQuantityText(long count) {
      return count > 1L ? ReadableNumber.formatForSlot(count, 0, "") : "";
   }

   static long saturatedEnergyTotal(long fePerTick, int durationTicks) {
      return SaturatingLong.multiply(fePerTick, (long)durationTicks);
   }

   static String fluidQuantityText(int amount) {
      return amount <= 10 ? ReadableNumber.formatForSlot((long)amount, 0, "mB") : ReadableNumber.formatForSlot((long)amount, 3, "B");
   }

   static String chemicalQuantityText(long amount) {
      return ReadableNumber.formatForSlot(amount, 3, "B");
   }

   static List<Component> fluidTooltip(FluidStack fluid) {
      return List.of(fluid.getHoverName());
   }

   static String itemTooltipQuantity(int count) {
      return count > 1 ? ReadableNumber.formatExact((long)count) : "";
   }

   static String fluidTooltipQuantity(int amount) {
      return amount <= 10 ? ReadableNumber.formatExact((long)amount) + "mB" : fluidBucketText(amount, 3);
   }

   static String chemicalTooltipQuantity(long amount) {
      return BigDecimal.valueOf(amount, 3).setScale(3, RoundingMode.DOWN).stripTrailingZeros().toPlainString() + "B";
   }

   private static String fluidBucketText(int amount, int decimalPlaces) {
      return BigDecimal.valueOf((long)amount, 3).setScale(decimalPlaces, RoundingMode.DOWN).stripTrailingZeros().toPlainString() + "B";
   }

   private static void appendInputTooltip(ITooltipBuilder tooltip, ItemInputDisplay item) {
      String quantity = itemTooltipQuantity(item.count());
      if (!quantity.isEmpty()) {
         tooltip.add(Component.translatable("jei.mmcr.machine_recipe.item_count", new Object[]{quantity}));
      }

      if (item.consumeChance() == 0.0F) {
         tooltip.add(Component.translatable("jei.mmcr.machine_recipe.keep"));
      } else if (item.consumeChance() < 1.0F) {
         tooltip.add(Component.translatable("jei.mmcr.machine_recipe.consume_chance", new Object[]{Math.round(item.consumeChance() * 100.0F) + "%"}));
      }

      if (item.hasUnexportedComponentConstraints()) {
         tooltip.add(Component.translatable("jei.mmcr.machine_recipe.component_constraints"));
      }
   }

   private static void appendOutputTooltip(ITooltipBuilder tooltip, ItemOutputDisplay output) {
      String quantity = itemTooltipQuantity(output.stack().getCount());
      if (!quantity.isEmpty()) {
         tooltip.add(Component.translatable("jei.mmcr.machine_recipe.item_count", new Object[]{quantity}));
      }

      appendOutputChanceTooltip(tooltip, output.chance());
   }

   private static void appendOutputChanceTooltip(ITooltipBuilder tooltip, float chance) {
      if (chance < 1.0F) {
         tooltip.add(Component.translatable("jei.mmcr.machine_recipe.output_chance", new Object[]{Math.round(chance * 100.0F) + "%"}));
      }
   }

   private static void appendFluidQuantityTooltip(ITooltipBuilder tooltip, int amount) {
      String quantity = fluidTooltipQuantity(amount);
      if (!quantity.isEmpty()) {
         tooltip.add(Component.translatable("jei.mmcr.machine_recipe.fluid_amount", new Object[]{quantity}));
      }
   }

   private static void appendChemicalQuantityTooltip(ITooltipBuilder tooltip, long amount) {
      tooltip.add(Component.translatable("jei.mmcr.machine_recipe.chemical_amount", new Object[]{chemicalTooltipQuantity(amount)}));
   }

   static void appendConsumeChanceTooltip(ITooltipBuilder tooltip, float consumeChance, boolean input) {
      if (input) {
         if (consumeChance == 0.0F) {
            tooltip.add(Component.translatable("jei.mmcr.machine_recipe.keep"));
         } else if (consumeChance < 1.0F) {
            tooltip.add(Component.translatable("jei.mmcr.machine_recipe.consume_chance", new Object[]{Math.round(consumeChance * 100.0F) + "%"}));
         }
      }
   }

   private boolean appendStressTooltip(ITooltipBuilder tooltip, MachineRecipeDisplay recipe, MachineRecipeLayout layout, double mouseX, double mouseY) {
      if (!(mouseX < (double)layout.durationTextX()) && !(mouseX >= (double)(this.getWidth() - layout.durationTextX()))) {
         int y = layout.stressTextY(recipe);

         for (StressDisplay stress : recipe.stressInputs()) {
            if (mouseY >= (double)y && mouseY < (double)(y + 10)) {
               tooltip.addAll(stress.tooltip(true));
               return true;
            }

            y += 10;
         }

         for (StressDisplay stress : recipe.stressOutputs()) {
            if (mouseY >= (double)y && mouseY < (double)(y + 10)) {
               tooltip.addAll(stress.tooltip(false));
               return true;
            }

            y += 10;
         }

         return false;
      } else {
         return false;
      }
   }

   private static Optional<Component> smartInterfaceTooltip(MachineRecipeDisplay recipe, MachineRecipeLayout layout, double mouseX, double mouseY) {
      if (mouseX < (double)layout.durationTextX()) {
         return Optional.empty();
      } else {
         int y = layout.smartInterfaceTextY(recipe);

         for (SmartInterfaceDisplay smartInterface : recipe.smartInterfaceInputs()) {
            if (mouseY >= (double)y && mouseY < (double)(y + 10)) {
               return Optional.of(smartInterface.tooltip());
            }

            y += 10;
         }

         for (SmartInterfaceDisplay smartInterface : recipe.smartInterfaceOutputs()) {
            if (mouseY >= (double)y && mouseY < (double)(y + 10)) {
               return Optional.of(smartInterface.tooltip());
            }

            y += 10;
         }

         return Optional.empty();
      }
   }

   static Optional<Component> lightningTooltip(MachineRecipeDisplay recipe, MachineRecipeLayout layout, double mouseX, double mouseY) {
      if (!(mouseX < (double)layout.durationTextX()) && !(mouseX >= (double)(168 - layout.durationTextX()))) {
         int y = layout.lightningTextY(recipe);

         for (LightningRequirement lightning : LightningJeiDisplay.requirements(recipe)) {
            if (mouseY >= (double)y && mouseY < (double)(y + 10) && !lightning.io().isInput()) {
               return Optional.of(Component.translatable("jei.mmcr.machine_recipe.output_chance", new Object[]{Math.round(lightning.chance() * 100.0F) + "%"}));
            }

            y += 10;
         }

         return Optional.empty();
      } else {
         return Optional.empty();
      }
   }

   static boolean appendAirTooltip(ITooltipBuilder tooltip, MachineRecipeDisplay recipe, MachineRecipeLayout layout, double mouseX, double mouseY) {
      if (!(mouseX < (double)layout.durationTextX()) && !(mouseX >= (double)(168 - layout.durationTextX()))) {
         int y = layout.airTextY(recipe);

         for (AirDisplay air : recipe.airInputs()) {
            if (mouseY >= (double)y && mouseY < (double)(y + 10)) {
               tooltip.addAll(air.tooltip(true));
               return true;
            }

            y += 10;
         }

         for (AirDisplay air : recipe.airOutputs()) {
            if (mouseY >= (double)y && mouseY < (double)(y + 10)) {
               tooltip.addAll(air.tooltip(false));
               return true;
            }

            y += 10;
         }

         return false;
      } else {
         return false;
      }
   }
}

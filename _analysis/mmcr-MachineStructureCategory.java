package cn.howxu.mmcr.compat.jei;

import cn.howxu.mmcr.MMCR;
import cn.howxu.mmcr.client.preview.StructureMaterialSummary;
import cn.howxu.mmcr.client.preview.StructureMaterialSummary.Entry;
import cn.howxu.mmcr.registry.ModBlocks;
import cn.howxu.mmcr.registry.ModItems;
import java.util.List;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.IRecipeSlotBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.runtime.IIngredientManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;

public final class MachineStructureCategory implements IRecipeCategory<MachineStructureDisplay> {
   private static final int MACHINE_NAME_X = 5;
   private static final int MACHINE_NAME_Y = 5;
   private static final int MACHINE_NAME_WIDTH = 160;
   private static final int MACHINE_NAME_HEIGHT = 16;
   private static final float MACHINE_NAME_SCALE = 1.1F;
   private static final int PREVIEW_X = 4;
   private static final int PREVIEW_Y = 23;
   private static final int MATERIAL_X = 0;
   private static final int MATERIAL_Y_4X = 131;
   private static final int MATERIAL_Y_3X = 201;
   private static final int MATERIAL_Y_2X = 261;
   private static final int MATERIAL_Y_1X = 281;
   private static final int MATERIAL_STEP = 19;
   private static final int MATERIAL_SLOT_COUNT = 9;
   static final int TRANSFER_BUTTON_X = 152;
   private static final int TRANSFER_BUTTON_Y = 258;
   private static final int TRANSFER_BUTTON_Y_2X = 241;
   private static final int TRANSFER_BUTTON_Y_3X = 181;
   private static final int TRANSFER_BUTTON_Y_4X = 111;
   private final IDrawable icon;
   private final IIngredientManager ingredientManager;

   public MachineStructureCategory(IGuiHelper guiHelper, IIngredientManager ingredientManager) {
      this.icon = guiHelper.createDrawableItemLike((ItemLike)ModBlocks.BASIC_CASING.get());
      this.ingredientManager = ingredientManager;
   }

   public RecipeType<MachineStructureDisplay> getRecipeType() {
      return JeiMachineRecipeTypes.STRUCTURE;
   }

   public Component getTitle() {
      return Component.translatable("jei.mmcr.multiblock_structure");
   }

   public int getWidth() {
      return switch ((int)Minecraft.getInstance().getWindow().getGuiScale()) {
         case 1 -> 168;
         case 2 -> 168;
         case 3 -> 168;
         default -> 168;
      };
   }

   public int getHeight() {
      return switch ((int)Minecraft.getInstance().getWindow().getGuiScale()) {
         case 1 -> 300;
         case 2 -> 280;
         case 3 -> 220;
         default -> 150;
      };
   }

   public IDrawable getIcon() {
      return this.icon;
   }

   public void setRecipe(IRecipeLayoutBuilder builder, MachineStructureDisplay display, IFocusGroup focuses) {
      StructureMaterialSummary summary = display.materials();
      int materialY = materialY();

      for (int slotIndex = 0; slotIndex < 9; slotIndex++) {
         int entryIndex = slotIndex;
         IRecipeSlotBuilder slot = builder.addSlot(RecipeIngredientRole.RENDER_ONLY, 0 + slotIndex * 19, materialY);
         if (entryIndex < summary.entries().size()) {
            slot.addItemStack(((Entry)summary.entries().get(entryIndex)).stack());
         }

         slot.addRichTooltipCallback((view, tooltip) -> {
            int page = StructureMaterialWidget.pageFor(System.currentTimeMillis(), summary.entries().size());
            int pageEntry = page * 9 + entryIndex;
            if (pageEntry < summary.entries().size()) {
               ((Entry)summary.entries().get(pageEntry)).tooltipLines().forEach(tooltip::add);
            }
         });
      }

      for (ItemStack stack : summary.transferStacks()) {
         builder.addInputSlot(-1000, -1000).addItemStack(stack);
      }

      builder.addOutputSlot(-1000, -1000).addItemStack(structureOutput(display));
      builder.moveRecipeTransferButton(152, transferButtonY());
   }

   public void createRecipeExtras(IRecipeExtrasBuilder builder, MachineStructureDisplay display, IFocusGroup focuses) {
      JeiStructurePreviewWidget preview = new JeiStructurePreviewWidget(display.machine(), 4, 23, previewWidth(), previewHeight(), this.ingredientManager);
      JeiPreviewLifecycle.registerActive(preview);
      builder.addWidget(preview);
      builder.addInputHandler(preview);
      builder.addWidget(
         new StructureMaterialWidget(
            display.materials(), builder.getRecipeSlots().getSlots(RecipeIngredientRole.RENDER_ONLY), 0, materialY(), this.ingredientManager
         )
      );
   }

   public void onDisplayedIngredientsUpdate(MachineStructureDisplay display, List<IRecipeSlotDrawable> recipeSlots, IFocusGroup focuses) {
      List<IRecipeSlotDrawable> materialSlots = recipeSlots.stream().filter(slot -> slot.getRole() == RecipeIngredientRole.RENDER_ONLY).toList();
      StructureMaterialWidget.refreshPage(display.materials(), materialSlots, System.currentTimeMillis(), this.ingredientManager);
   }

   public void draw(MachineStructureDisplay display, IRecipeSlotsView recipeSlotsView, GuiGraphics graphics, double mouseX, double mouseY) {
      Font font = Minecraft.getInstance().font;
      Component machineName = display.machine().displayName();
      int textWidth = font.width(machineName);
      if (textWidth > 0) {
         float scale = Math.min(1.1F, Math.min(160.0F / (float)textWidth, 16.0F / 9.0F));
         float scaledWidth = (float)textWidth * scale;
         float scaledHeight = 9.0F * scale;
         graphics.pose().pushPose();
         graphics.pose().translate(85.0F - scaledWidth / 2.0F, 13.0F - scaledHeight, 0.0F);
         graphics.pose().scale(scale, scale, 1.0F);
         graphics.drawString(font, machineName, 0, 0, -1, false);
         graphics.pose().popPose();
      }
   }

   public ResourceLocation getRegistryName(MachineStructureDisplay display) {
      ResourceLocation machineId = display.machine().registryName();
      return MMCR.id("structure/" + machineId.getNamespace() + "/" + machineId.getPath());
   }

   static ItemStack structureOutput(MachineStructureDisplay display) {
      ItemStack output = new ItemStack((ItemLike)ModItems.BLUEPRINT.get());
      output.set(DataComponents.CUSTOM_NAME, display.machine().displayName());
      return output;
   }

   private static int materialY() {
      return switch ((int)Minecraft.getInstance().getWindow().getGuiScale()) {
         case 1 -> 281;
         case 2 -> 261;
         case 3 -> 201;
         default -> 131;
      };
   }

   private static int transferButtonY() {
      return switch ((int)Minecraft.getInstance().getWindow().getGuiScale()) {
         case 1 -> 258;
         case 2 -> 241;
         case 3 -> 181;
         default -> 111;
      };
   }

   private static int previewWidth() {
      return 161;
   }

   private static int previewHeight() {
      return switch ((int)Minecraft.getInstance().getWindow().getGuiScale()) {
         case 1 -> 253;
         case 2 -> 233;
         case 3 -> 173;
         default -> 103;
      };
   }
}

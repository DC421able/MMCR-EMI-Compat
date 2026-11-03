package cn.howxu.mmcr.compat.jei;

import cn.howxu.mmcr.client.preview.StructureMaterialSummary;
import cn.howxu.mmcr.client.preview.StructureMaterialSummary.Entry;
import java.util.List;
import java.util.Optional;
import java.util.function.LongSupplier;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.ingredient.IRecipeSlotDrawable;
import mezz.jei.api.gui.widgets.IRecipeWidget;
import mezz.jei.api.ingredients.ITypedIngredient;
import mezz.jei.api.runtime.IIngredientManager;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.navigation.ScreenPosition;

public final class StructureMaterialWidget implements IRecipeWidget {
   public static final int SLOT_COUNT = 9;
   public static final int SLOT_STEP = 19;
   public static final long PAGE_DURATION_MILLIS = 8000L;
   private final StructureMaterialSummary summary;
   private final List<IRecipeSlotDrawable> slots;
   private final int x;
   private final int y;
   private final LongSupplier clock;
   private final IIngredientManager ingredientManager;
   private int displayedPage = -1;

   public StructureMaterialWidget(StructureMaterialSummary summary, List<IRecipeSlotDrawable> slots, int x, int y, IIngredientManager ingredientManager) {
      this(summary, slots, x, y, System::currentTimeMillis, ingredientManager);
   }

   StructureMaterialWidget(
      StructureMaterialSummary summary, List<IRecipeSlotDrawable> slots, int x, int y, LongSupplier clock, IIngredientManager ingredientManager
   ) {
      if (slots.size() != 9) {
         throw new IllegalArgumentException("nine material slots required");
      } else {
         this.summary = summary;
         this.slots = List.copyOf(slots);
         this.x = x;
         this.y = y;
         this.clock = clock;
         this.ingredientManager = ingredientManager;
         this.applyPage(pageFor(clock.getAsLong(), summary.entries().size()));
      }
   }

   public ScreenPosition getPosition() {
      return new ScreenPosition(this.x, this.y);
   }

   public void tick() {
      int page = pageFor(this.clock.getAsLong(), this.summary.entries().size());
      if (page != this.displayedPage) {
         this.applyPage(page);
      }
   }

   static void refreshPage(StructureMaterialSummary summary, List<IRecipeSlotDrawable> slots, long timeMillis, IIngredientManager ingredientManager) {
      if (slots.size() != 9) {
         throw new IllegalArgumentException("nine material slots required");
      } else {
         int page = pageFor(timeMillis, summary.entries().size());
         applyPage(summary, slots, page, ingredientManager);
      }
   }

   public void drawWidget(GuiGraphics graphics, double mouseX, double mouseY) {
      graphics.pose().pushPose();
      graphics.pose().translate(0.0F, 0.0F, 200.0F);

      try {
         int first = this.displayedPage * 9;

         for (int slotIndex = 0; slotIndex < 9; slotIndex++) {
            int entryIndex = first + slotIndex;
            if (entryIndex < this.summary.entries().size()) {
               String quantity = MachineRecipeCategory.itemQuantityText((long)((Entry)this.summary.entries().get(entryIndex)).count());
               if (!quantity.isEmpty()) {
                  new JeiSlotOverlayDrawable("", quantity).draw(graphics, slotIndex * 19, 0);
               }
            }
         }
      } finally {
         graphics.pose().popPose();
      }
   }

   static int pageFor(long timeMillis, int materialCount) {
      int pageCount = (materialCount + 9 - 1) / 9;
      return pageCount == 0 ? 0 : (int)Math.floorDiv(timeMillis, 8000L) % pageCount;
   }

   private void applyPage(int page) {
      this.displayedPage = page;
      applyPage(this.summary, this.slots, page, this.ingredientManager);
   }

   private static void applyPage(StructureMaterialSummary summary, List<IRecipeSlotDrawable> slots, int page, IIngredientManager ingredientManager) {
      int first = page * 9;

      for (int slotIndex = 0; slotIndex < 9; slotIndex++) {
         int entryIndex = first + slotIndex;
         Optional<ITypedIngredient<?>> value = entryIndex < summary.entries().size()
            ? ingredientManager.createTypedIngredient(VanillaTypes.ITEM_STACK, ((Entry)summary.entries().get(entryIndex)).stack().copyWithCount(1), true)
               .map(ingredient -> (ITypedIngredient<?>)ingredient)
            : Optional.empty();
         IRecipeSlotDrawable slot = slots.get(slotIndex);
         slot.clearDisplayOverrides();
         slot.createDisplayOverrides().addOptionalTypedIngredients(List.of(value));
      }
   }
}

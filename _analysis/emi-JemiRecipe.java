package dev.emi.emi.jemi;

import com.google.common.collect.Lists;
import dev.emi.emi.EmiPort;
import dev.emi.emi.EmiUtil;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.WidgetHolder;
import dev.emi.emi.jemi.JemiRecipe.JemiWidget;
import dev.emi.emi.jemi.impl.JemiIngredientAcceptor;
import dev.emi.emi.jemi.impl.JemiRecipeLayoutBuilder;
import dev.emi.emi.jemi.impl.JemiRecipeSlot;
import dev.emi.emi.jemi.impl.JemiRecipeSlotBuilder;
import dev.emi.emi.jemi.impl.extras.JemiRecipeExtrasBuilder;
import dev.emi.emi.jemi.impl.extras.JemiWidgetBuilder;
import dev.emi.emi.jemi.widget.JemiSlotWidget;
import dev.emi.emi.jemi.widget.JemiTankWidget;
import dev.emi.emi.runtime.EmiLog;
import java.util.List;
import java.util.Optional;
import mezz.jei.api.gui.IRecipeLayoutDrawable;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.library.focus.FocusGroup;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeHolder;
import org.jetbrains.annotations.Nullable;

public class JemiRecipe<T> implements EmiRecipe {
   public List<EmiIngredient> inputs = Lists.newArrayList();
   public List<EmiIngredient> catalysts = Lists.newArrayList();
   public List<EmiStack> outputs = Lists.newArrayList();
   public EmiRecipeCategory recipeCategory;
   public ResourceLocation originalId;
   public ResourceLocation id;
   public IRecipeCategory<T> category;
   public T recipe;
   public boolean allowTree = true;

   public JemiRecipe(EmiRecipeCategory recipeCategory, IRecipeCategory<T> category, T recipe) {
      this.recipeCategory = recipeCategory;
      this.category = category;
      this.recipe = recipe;
      this.originalId = category.getRegistryName(recipe);
      if (this.originalId != null) {
         this.id = EmiPort.id("jei", "/" + EmiUtil.subId(this.originalId));
      }

      JemiRecipeLayoutBuilder builder = new JemiRecipeLayoutBuilder();
      category.setRecipe(builder, recipe, JemiPlugin.runtime.getJeiHelpers().getFocusFactory().getEmptyFocusGroup());

      for (JemiRecipeSlotBuilder jrsb : builder.slots) {
         jrsb.acceptor.coerceStacks(jrsb.tooltipCallback, jrsb.renderers);
      }

      for (JemiIngredientAcceptor acceptor : builder.ingredients) {
         EmiIngredient stack = acceptor.build();
         if (acceptor.role == RecipeIngredientRole.INPUT) {
            this.inputs.add(stack);
         } else if (acceptor.role == RecipeIngredientRole.CATALYST) {
            this.catalysts.add(stack);
         } else if (acceptor.role == RecipeIngredientRole.OUTPUT) {
            if (stack.getEmiStacks().size() > 1) {
               this.allowTree = false;
            }

            this.outputs.addAll(stack.getEmiStacks());
         }
      }
   }

   public EmiRecipeCategory getCategory() {
      return this.recipeCategory;
   }

   @Nullable
   public RecipeHolder<?> getBackingRecipe() {
      return EmiPort.getRecipe(this.originalId);
   }

   @Nullable
   public ResourceLocation getId() {
      return this.id;
   }

   public List<EmiIngredient> getInputs() {
      return this.inputs;
   }

   public List<EmiIngredient> getCatalysts() {
      return this.catalysts;
   }

   public List<EmiStack> getOutputs() {
      return this.outputs;
   }

   public int getDisplayWidth() {
      return this.category.getWidth();
   }

   public int getDisplayHeight() {
      return this.category.getHeight();
   }

   public boolean supportsRecipeTree() {
      return this.allowTree && super.supportsRecipeTree();
   }

   public void addWidgets(WidgetHolder widgets) {
      Optional<IRecipeLayoutDrawable<T>> opt = JemiPlugin.runtime.getRecipeManager().createRecipeLayoutDrawable(this.category, this.recipe, FocusGroup.EMPTY);
      JemiRecipeLayoutBuilder builder = new JemiRecipeLayoutBuilder();
      this.category.setRecipe(builder, this.recipe, JemiPlugin.runtime.getJeiHelpers().getFocusFactory().getEmptyFocusGroup());

      for (JemiRecipeSlotBuilder jrsb : builder.slots) {
         jrsb.acceptor.coerceStacks(jrsb.tooltipCallback, jrsb.renderers);
      }

      if (opt.isPresent()) {
         widgets.add(new JemiWidget(this, 0, 0, this.getDisplayWidth(), this.getDisplayHeight(), opt.get()));

         for (JemiRecipeSlotBuilder sb : builder.slots) {
            JemiRecipeSlot slot = new JemiRecipeSlot(sb);
            if (slot.tankInfo != null && !slot.getIngredients(JemiUtil.getFluidType()).toList().isEmpty()) {
               widgets.add(new JemiTankWidget(slot, this));
            } else {
               widgets.add(new JemiSlotWidget(slot, this));
            }
         }
      }

      try {
         JemiRecipeExtrasBuilder extras = new JemiRecipeExtrasBuilder(null);
         this.category.createRecipeExtras(extras, this.recipe, JemiPlugin.runtime.getJeiHelpers().getFocusFactory().getEmptyFocusGroup());

         for (JemiWidgetBuilder b : extras.widgets) {
            b.addWidgets(widgets);
         }
      } catch (Throwable var7) {
         EmiLog.error("Exception adding JEMI extras", var7);
      }
   }
}

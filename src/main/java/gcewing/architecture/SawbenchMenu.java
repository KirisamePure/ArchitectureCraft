package gcewing.architecture;

import gcewing.architecture.shape.Shape;
import java.util.Optional;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;

/** All selection and crafting is validated on the server; no client-provided item/material data. */
public final class SawbenchMenu extends AbstractContainerMenu {
  public static final int CLADDING = Shape.values().length;
  public static final int CUT = CLADDING + 1;
  private final Container inventory;
  private final DataSlot selected;

  public SawbenchMenu(int id, Inventory player) {
    this(id, player, new SimpleContainer(2));
  }

  public SawbenchMenu(int id, Inventory player, Container inventory) {
    super(ArchitectureCraft.SAWBENCH_MENU, id);
    this.inventory = inventory;
    selected =
        inventory instanceof SawbenchBlockEntity be
            ? new DataSlot() {
              public int get() {
                return be.selected;
              }

              public void set(int n) {
                be.selected = n;
                be.setChanged();
              }
            }
            : DataSlot.standalone();
    addSlot(
        new Slot(inventory, 0, 20, 133) {
          @Override
          public boolean mayPlace(ItemStack stack) {
            return ArchitectureCraft.validMaterial(stack);
          }
        });
    addSlot(
        new Slot(inventory, 1, 236, 133) {
          @Override
          public boolean mayPlace(ItemStack stack) {
            return false;
          }
        });
    addStandardInventorySlots(player, 56, 154);
    addDataSlot(selected);
  }

  public int selected() {
    return selected.get();
  }

  @Override
  public boolean stillValid(Player player) {
    return inventory.stillValid(player);
  }

  @Override
  public boolean clickMenuButton(Player player, int button) {
    if (!stillValid(player)) return false;
    if (button >= 0 && button <= CLADDING) {
      selected.set(button);
      broadcastChanges();
      return true;
    }
    if (button != CUT) return false;
    ItemStack input = inventory.getItem(0);
    int index = selected.get();
    if (!ArchitectureCraft.validMaterial(input) || index < 0 || index > CLADDING) return false;
    boolean cladding = index == CLADDING;
    Shape shape = cladding ? Shape.CladdingSheet : Shape.values()[index];
    int cost = materialCost(shape, input, cladding), count = cladding ? 16 : shape.itemsProduced;
    if (input.getCount() < cost) return false;
    var material = ((BlockItem) input.getItem()).getBlock().defaultBlockState();
    ShapeData data = new ShapeData(shape.id, material, Optional.empty(), 0, shape.offset ? 6 : 0);
    ItemStack made =
        cladding ? new ItemStack(ArchitectureCraft.CLADDING, count) : data.stack(count);
    if (cladding) made.set(ArchitectureCraft.SHAPE_DATA, data);
    ItemStack output = inventory.getItem(1);
    if (!output.isEmpty()
        && (!ItemStack.isSameItemSameComponents(output, made)
            || output.getCount() + count > output.getMaxStackSize())) return false;
    inventory.removeItem(0, cost);
    if (output.isEmpty()) inventory.setItem(1, made);
    else {
      output.grow(count);
      inventory.setChanged();
    }
    broadcastChanges();
    return true;
  }

  public static int materialCost(Shape shape, ItemStack input, boolean cladding) {
    int factor =
        input.getItem() instanceof BlockItem bi
                && bi.getBlock() instanceof net.minecraft.world.level.block.SlabBlock
            ? 2
            : 1;
    return factor * (cladding ? 1 : shape.materialUsed);
  }

  @Override
  public ItemStack quickMoveStack(Player player, int index) {
    Slot slot = slots.get(index);
    if (!slot.hasItem()) return ItemStack.EMPTY;
    ItemStack stack = slot.getItem(), copy = stack.copy();
    if (index < 2) {
      if (!moveItemStackTo(stack, 2, slots.size(), true)) return ItemStack.EMPTY;
    } else if (ArchitectureCraft.validMaterial(stack)) {
      if (!moveItemStackTo(stack, 0, 1, false)) return ItemStack.EMPTY;
    } else if (index < 29) {
      if (!moveItemStackTo(stack, 29, 38, false)) return ItemStack.EMPTY;
    } else if (!moveItemStackTo(stack, 2, 29, false)) return ItemStack.EMPTY;
    if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
    else slot.setChanged();
    if (stack.getCount() == copy.getCount()) return ItemStack.EMPTY;
    slot.onTake(player, stack);
    return copy;
  }
}

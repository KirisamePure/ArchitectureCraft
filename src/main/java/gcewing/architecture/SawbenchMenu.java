package gcewing.architecture;

import gcewing.architecture.shape.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.BlockItemStateProperties;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockState;

/** Original layout and take-to-craft semantics, synchronized with ordinary vanilla menu packets. */
public final class SawbenchMenu extends AbstractContainerMenu {
  public static final int WIDTH = 242, HEIGHT = 224;
  private final Container inventory;
  private final DataSlot page;
  private final DataSlot[] selectedSlots = new DataSlot[8];

  public SawbenchMenu(int id, Inventory player) {
    this(id, player, new SimpleContainer(2));
  }

  public SawbenchMenu(int id, Inventory player, Container inventory) {
    super(ArchitectureCraft.SAWBENCH_MENU, id);
    this.inventory = inventory;
    page =
        inventory instanceof SawbenchBlockEntity be
            ? new DataSlot() {
              public int get() {
                return be.page();
              }

              public void set(int n) {}
            }
            : DataSlot.standalone();
    addDataSlot(page);
    for (int i = 0; i < 8; i++) {
      final int p = i;
      selectedSlots[i] =
          inventory instanceof SawbenchBlockEntity be
              ? new DataSlot() {
                public int get() {
                  return be.selectedSlot(p);
                }

                public void set(int n) {}
              }
              : DataSlot.standalone();
      addDataSlot(selectedSlots[i]);
    }
    addSlot(new Slot(inventory, 0, 12, 19));
    addSlot(
        new Slot(inventory, 1, 12, 57) {
          @Override
          public boolean mayPlace(ItemStack stack) {
            return false;
          }
        });
    for (int row = 0; row < 3; row++)
      for (int col = 0; col < 9; col++)
        addSlot(new Slot(player, col + row * 9 + 9, 8 + col * 18, 143 + row * 18));
    for (int col = 0; col < 9; col++) addSlot(new Slot(player, col, 8 + col * 18, 200));
  }

  public int page() {
    return page.get();
  }

  public int selectedSlot(int p) {
    return p >= 0 && p < 8 ? selectedSlots[p].get() : 0;
  }

  public Shape selectedShape() {
    return ShapePage.shape(page(), selectedSlot(page()));
  }

  @Override
  public boolean stillValid(Player player) {
    return inventory.stillValid(player);
  }

  @Override
  public void broadcastChanges() {
    if (inventory instanceof SawbenchBlockEntity be) be.refreshResult();
    super.broadcastChanges();
  }

  @Override
  public boolean clickMenuButton(Player player, int button) {
    int p = button / 32, slot = button % 32;
    if (button < 0
        || !stillValid(player)
        || !ShapePage.valid(p, slot)
        || !(inventory instanceof SawbenchBlockEntity be)) return false;
    boolean result = be.select(p, slot);
    broadcastChanges();
    return result;
  }

  public static BlockState materialState(ItemStack input) {
    return input
        .getOrDefault(DataComponents.BLOCK_STATE, BlockItemStateProperties.EMPTY)
        .apply(((BlockItem) input.getItem()).getBlock().defaultBlockState());
  }

  public static int materialCost(Shape shape, ItemStack input) {
    int factor =
        input.getItem() instanceof BlockItem bi && bi.getBlock() instanceof SlabBlock ? 2 : 1;
    return shape == null ? 0 : factor * shape.materialUsed;
  }

  @Override
  public ItemStack quickMoveStack(Player player, int index) {
    Slot slot = slots.get(index);
    if (!slot.hasItem()) return ItemStack.EMPTY;
    ItemStack stack = slot.getItem(), copy = stack.copy();
    if (index == 1) {
      // Transfer a copy first: vanilla mutates source stacks without calling Container.removeItem.
      ItemStack transferable = copy.copy();
      if (!moveItemStackTo(transferable, 2, 38, true)) return ItemStack.EMPTY;
      int moved = copy.getCount() - transferable.getCount();
      if (moved == 0) return ItemStack.EMPTY;
      inventory.removeItem(1, moved);
      slot.onTake(player, copy.copyWithCount(moved));
      broadcastChanges();
      return copy;
    }
    if (index == 0) {
      if (!moveItemStackTo(stack, 2, 38, true)) return ItemStack.EMPTY;
    } else if (!moveItemStackTo(stack, 0, 1, false)) return ItemStack.EMPTY;
    if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
    else slot.setChanged();
    if (stack.getCount() == copy.getCount()) return ItemStack.EMPTY;
    slot.onTake(player, stack);
    broadcastChanges();
    return copy;
  }
}

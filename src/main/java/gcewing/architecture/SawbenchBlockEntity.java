package gcewing.architecture;

import com.mojang.serialization.Codec;
import gcewing.architecture.shape.*;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.network.chat.Component;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.*;

/**
 * The result is a preview until the first item of a batch is removed. Partial paid batches survive
 * selection/input changes.
 */
public final class SawbenchBlockEntity extends BlockEntity
    implements WorldlyContainer, MenuProvider {
  private NonNullList<ItemStack> items = NonNullList.withSize(2, ItemStack.EMPTY);
  private int page;
  private final int[] selectedSlots = new int[8];
  private boolean pendingMaterial;
  private boolean refreshing;

  public SawbenchBlockEntity(BlockPos pos, BlockState state) {
    super(ArchitectureCraft.SAWBENCH_ENTITY, pos, state);
  }

  public int page() {
    return page;
  }

  public int selectedSlot(int page) {
    return page >= 0 && page < 8 ? selectedSlots[page] : 0;
  }

  public Shape selectedShape() {
    return ShapePage.shape(page, selectedSlots[page]);
  }

  public boolean previewPending() {
    return pendingMaterial;
  }

  public boolean select(int newPage, int slot) {
    if (!ShapePage.valid(newPage, slot)) return false;
    page = newPage;
    selectedSlots[page] = slot;
    setChanged();
    return true;
  }

  public void refreshResult() {
    if (refreshing) return;
    refreshing = true;
    try {
      if (items.get(1).isEmpty() || pendingMaterial) {
        ItemStack result = makeResult();
        items.set(1, result);
        pendingMaterial = !result.isEmpty();
      }
    } finally {
      refreshing = false;
    }
  }

  private ItemStack makeResult() {
    Shape shape = selectedShape();
    ItemStack input = items.get(0);
    if (shape == null
        || !ArchitectureCraft.validMaterial(input)
        || input.getCount() < SawbenchMenu.materialCost(shape, input)) return ItemStack.EMPTY;
    BlockState material = SawbenchMenu.materialState(input);
    return new ShapeData(shape.id, material, Optional.empty(), 0, 0).stack(shape.itemsProduced);
  }

  @Override
  public void setChanged() {
    refreshResult();
    super.setChanged();
  }

  @Override
  protected void loadAdditional(ValueInput input) {
    super.loadAdditional(input);
    items = NonNullList.withSize(2, ItemStack.EMPTY);
    ContainerHelper.loadAllItems(input, items);
    page = Math.clamp(input.getIntOr("page", 0), 0, 7);
    var slots = input.read("selected_slots", Codec.INT.listOf()).orElse(List.of());
    for (int i = 0; i < 8; i++)
      selectedSlots[i] = i < slots.size() && ShapePage.valid(i, slots.get(i)) ? slots.get(i) : 0;
    pendingMaterial = input.getBooleanOr("pending_material", false);
    refreshResult();
  }

  @Override
  protected void saveAdditional(ValueOutput output) {
    super.saveAdditional(output);
    ContainerHelper.saveAllItems(output, items);
    output.putInt("page", page);
    output.store(
        "selected_slots", Codec.INT.listOf(), Arrays.stream(selectedSlots).boxed().toList());
    output.putBoolean("pending_material", pendingMaterial);
  }

  @Override
  public Component getDisplayName() {
    return Component.translatable("container.architecturecraft.sawbench");
  }

  @Override
  public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
    return new SawbenchMenu(id, inventory, this);
  }

  @Override
  public int getContainerSize() {
    return 2;
  }

  @Override
  public boolean isEmpty() {
    return items.get(0).isEmpty() && (pendingMaterial || items.get(1).isEmpty());
  }

  @Override
  public ItemStack getItem(int slot) {
    return items.get(slot);
  }

  @Override
  public ItemStack removeItem(int slot, int amount) {
    if (amount <= 0) return ItemStack.EMPTY;
    if (slot == 1) {
      refreshResult();
      if (items.get(1).isEmpty()) return ItemStack.EMPTY;
      if (pendingMaterial) {
        int cost = SawbenchMenu.materialCost(selectedShape(), items.get(0));
        if (items.get(0).getCount() < cost) return ItemStack.EMPTY;
        items.get(0).shrink(cost);
        pendingMaterial = false;
      }
    }
    ItemStack result = ContainerHelper.removeItem(items, slot, amount);
    setChanged();
    return result;
  }

  @Override
  public ItemStack removeItemNoUpdate(int slot) {
    return removeItem(slot, Integer.MAX_VALUE);
  }

  @Override
  public void setItem(int slot, ItemStack stack) {
    items.set(slot, stack);
    stack.limitSize(getMaxStackSize(stack));
    if (slot == 1) pendingMaterial = false;
    setChanged();
  }

  @Override
  public boolean stillValid(Player player) {
    return Container.stillValidBlockEntity(this, player);
  }

  @Override
  public void clearContent() {
    items.clear();
    pendingMaterial = false;
    super.setChanged();
  }

  @Override
  public boolean canPlaceItem(int slot, ItemStack stack) {
    return slot == 0;
  }

  @Override
  public int[] getSlotsForFace(Direction side) {
    return side == Direction.UP ? new int[] {0} : new int[] {1};
  }

  @Override
  public boolean canPlaceItemThroughFace(int slot, ItemStack stack, Direction side) {
    return slot == 0;
  }

  @Override
  public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
    return side != Direction.DOWN && slot == 0;
  }

  @Override
  public void preRemoveSideEffects(BlockPos pos, BlockState state) {
    if (level != null) {
      Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), items.get(0));
      if (!pendingMaterial)
        Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), items.get(1));
    }
    items.clear();
    pendingMaterial = false;
  }
}

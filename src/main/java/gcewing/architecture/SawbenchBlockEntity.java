package gcewing.architecture;

import net.minecraft.core.*;
import net.minecraft.network.chat.Component;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.*;

public final class SawbenchBlockEntity extends BlockEntity implements Container, MenuProvider {
  private NonNullList<ItemStack> items = NonNullList.withSize(2, ItemStack.EMPTY);
  public int selected;

  public SawbenchBlockEntity(BlockPos pos, BlockState state) {
    super(ArchitectureCraft.SAWBENCH_ENTITY, pos, state);
  }

  @Override
  protected void loadAdditional(ValueInput input) {
    super.loadAdditional(input);
    ContainerHelper.loadAllItems(input, items);
    selected = input.getIntOr("selected", 0);
  }

  @Override
  protected void saveAdditional(ValueOutput output) {
    super.saveAdditional(output);
    ContainerHelper.saveAllItems(output, items);
    output.putInt("selected", selected);
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
    return items.stream().allMatch(ItemStack::isEmpty);
  }

  @Override
  public ItemStack getItem(int slot) {
    return items.get(slot);
  }

  @Override
  public ItemStack removeItem(int slot, int amount) {
    ItemStack s = ContainerHelper.removeItem(items, slot, amount);
    if (!s.isEmpty()) setChanged();
    return s;
  }

  @Override
  public ItemStack removeItemNoUpdate(int slot) {
    return ContainerHelper.takeItem(items, slot);
  }

  @Override
  public void setItem(int slot, ItemStack stack) {
    items.set(slot, stack);
    stack.limitSize(getMaxStackSize(stack));
    setChanged();
  }

  @Override
  public boolean stillValid(Player player) {
    return Container.stillValidBlockEntity(this, player);
  }

  @Override
  public void clearContent() {
    items.clear();
    setChanged();
  }

  @Override
  public boolean canPlaceItem(int slot, ItemStack stack) {
    return slot == 0 && ArchitectureCraft.validMaterial(stack);
  }
}

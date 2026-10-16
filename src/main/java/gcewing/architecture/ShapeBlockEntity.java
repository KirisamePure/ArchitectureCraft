package gcewing.architecture;

import net.minecraft.core.*;
import net.minecraft.core.component.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.*;

public final class ShapeBlockEntity extends BlockEntity {
  // Rendering threads only read immutable snapshots.
  private volatile ShapeData data = ShapeData.defaults();

  public ShapeBlockEntity(BlockPos pos, BlockState state) {
    super(ArchitectureCraft.SHAPE_ENTITY, pos, state);
  }

  public ShapeData data() {
    return data;
  }

  public void setData(ShapeData data) {
    this.data = data;
    setChanged();
    if (level != null) {
      level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
      level.updateNeighborsAt(worldPosition, getBlockState().getBlock());
    }
  }

  @Override
  protected void loadAdditional(ValueInput input) {
    super.loadAdditional(input);
    data = input.read("shape_data", ShapeData.CODEC).orElse(ShapeData.defaults());
    if (level != null && level.isClientSide())
      level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
  }

  @Override
  protected void saveAdditional(ValueOutput output) {
    super.saveAdditional(output);
    output.store("shape_data", ShapeData.CODEC, data);
  }

  @Override
  public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
    return saveWithoutMetadata(registries);
  }

  @Override
  public ClientboundBlockEntityDataPacket getUpdatePacket() {
    return ClientboundBlockEntityDataPacket.create(this);
  }

  @Override
  protected void applyImplicitComponents(DataComponentGetter getter) {
    super.applyImplicitComponents(getter);
    data = getter.getOrDefault(ArchitectureCraft.SHAPE_DATA, ShapeData.defaults());
  }

  @Override
  protected void collectImplicitComponents(DataComponentMap.Builder builder) {
    super.collectImplicitComponents(builder);
    builder.set(ArchitectureCraft.SHAPE_DATA, data);
  }
}

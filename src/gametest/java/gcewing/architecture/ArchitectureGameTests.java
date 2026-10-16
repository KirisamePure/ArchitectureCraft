package gcewing.architecture;

import gcewing.architecture.shape.Shape;
import java.util.Optional;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.*;
import net.minecraft.world.phys.shapes.CollisionContext;

public final class ArchitectureGameTests {
  private static void check(GameTestHelper h, boolean condition, String message) {
    h.assertTrue(condition, Component.literal(message));
  }

  private Player player(GameTestHelper h, BlockPos pos) {
    Player p = h.makeMockPlayer(GameType.SURVIVAL);
    p.setPos(h.absolutePos(pos).getCenter());
    return p;
  }

  @GameTest
  public void sawbenchConsumesExactlyOneBatch(GameTestHelper h) {
    BlockPos pos = new BlockPos(1, 1, 1);
    h.setBlock(pos, ArchitectureCraft.SAWBENCH);
    var bench = h.getBlockEntity(pos, SawbenchBlockEntity.class);
    Player player = player(h, pos);
    var menu = new SawbenchMenu(1, player.getInventory(), bench);
    bench.setItem(0, new ItemStack(Blocks.STONE, 4));
    menu.clickMenuButton(player, Shape.Stairs.ordinal());
    check(h, menu.clickMenuButton(player, SawbenchMenu.CUT), "Stairs must craft");
    check(h, bench.getItem(0).getCount() == 1, "Stairs consume three blocks");
    check(h, bench.getItem(1).getCount() == 4, "Stairs yield four shapes");
    check(h, ShapeData.fromStack(bench.getItem(1)).base().is(Blocks.STONE), "Material retained");
    check(h, !menu.clickMenuButton(player, SawbenchMenu.CUT), "Insufficient material rejected");
    check(h, !menu.clickMenuButton(player, -1), "Negative selection rejected");
    check(h, !menu.clickMenuButton(player, 999), "Unknown button rejected");
    h.succeed();
  }

  @GameTest
  public void claddingAndFullOutputCannotDuplicate(GameTestHelper h) {
    BlockPos pos = new BlockPos(1, 1, 1);
    h.setBlock(pos, ArchitectureCraft.SAWBENCH);
    var bench = h.getBlockEntity(pos, SawbenchBlockEntity.class);
    Player player = player(h, pos);
    var menu = new SawbenchMenu(1, player.getInventory(), bench);
    bench.setItem(0, new ItemStack(Blocks.OAK_PLANKS, 8));
    menu.clickMenuButton(player, SawbenchMenu.CLADDING);
    for (int i = 0; i < 4; i++)
      check(h, menu.clickMenuButton(player, SawbenchMenu.CUT), "Cladding batch must fit");
    check(
        h,
        bench.getItem(1).is(ArchitectureCraft.CLADDING) && bench.getItem(1).getCount() == 64,
        "Four batches produce 64 sheets");
    check(h, !menu.clickMenuButton(player, SawbenchMenu.CUT), "Full output rejects crafting");
    check(h, bench.getItem(0).getCount() == 4, "Failed craft leaves input untouched");
    check(
        h, menu.quickMoveStack(player, 1).getCount() == 64, "Shift click extracts crafted output");
    check(h, bench.getItem(1).isEmpty(), "Output extracted once");
    h.succeed();
  }

  @GameTest
  public void shapeDropsPreserveMaterialsAndCollision(GameTestHelper h) {
    BlockPos pos = new BlockPos(1, 1, 1);
    h.setBlock(pos, ArchitectureCraft.SHAPE);
    var be = h.getBlockEntity(pos, ShapeBlockEntity.class);
    ShapeData data =
        new ShapeData(
            Shape.ArchD3A.id,
            Blocks.BRICKS.defaultBlockState(),
            Optional.of(Blocks.QUARTZ_BLOCK.defaultBlockState()),
            5,
            0);
    be.setData(data);
    var level = h.getLevel();
    var absolute = h.absolutePos(pos);
    var drops = Block.getDrops(be.getBlockState(), level, absolute, be);
    check(
        h,
        drops.size() == 1 && ShapeData.fromStack(drops.getFirst()).equals(data),
        "Drop retains shape, both materials and connections");
    check(
        h,
        !be.getBlockState().getShape(level, absolute, CollisionContext.empty()).isEmpty(),
        "Arch collision exists");
    var encoded = be.saveWithFullMetadata(level.registryAccess());
    var loaded =
        (ShapeBlockEntity)
            net.minecraft.world.level.block.entity.BlockEntity.loadStatic(
                absolute, be.getBlockState(), encoded, level.registryAccess());
    check(h, loaded != null && loaded.data().equals(data), "Save/load retains shape data");
    h.succeed();
  }

  @GameTest
  public void allShapesPlaceAndHaveCollision(GameTestHelper h) {
    BlockPos pos = new BlockPos(1, 1, 1);
    h.setBlock(pos, ArchitectureCraft.SHAPE);
    var be = h.getBlockEntity(pos, ShapeBlockEntity.class);
    for (Shape shape : Shape.values()) {
      be.setData(
          new ShapeData(
              shape.id,
              Blocks.STONE.defaultBlockState(),
              Optional.empty(),
              0,
              shape.offset ? 6 : 0));
      check(
          h,
          !be.getBlockState()
              .getShape(h.getLevel(), h.absolutePos(pos), CollisionContext.empty())
              .isEmpty(),
          shape.name() + " collision");
    }
    h.succeed();
  }

  @GameTest
  public void craftingRecipesLoaded(GameTestHelper h) {
    for (String name :
        new String[] {"sawbench", "sawblade", "large_pulley", "hammer", "chisel", "glowbrush"})
      check(
          h,
          h.getLevel()
              .getServer()
              .getRecipeManager()
              .byKey(ResourceKey.create(Registries.RECIPE, ArchitectureCraft.id(name)))
              .isPresent(),
          name + " recipe loaded");
    h.succeed();
  }

  @GameTest
  public void toolsPreserveDataAndConnections(GameTestHelper h) {
    BlockPos pos = new BlockPos(1, 1, 1);
    h.setBlock(pos, ArchitectureCraft.SHAPE);
    h.setBlock(pos.east(), ArchitectureCraft.SHAPE);
    var be = h.getBlockEntity(pos, ShapeBlockEntity.class);
    var other = h.getBlockEntity(pos.east(), ShapeBlockEntity.class);
    ShapeData original =
        new ShapeData(
            Shape.RoofTile.id,
            Blocks.BRICKS.defaultBlockState(),
            Optional.of(Blocks.QUARTZ_BLOCK.defaultBlockState()),
            0,
            0);
    be.setData(original);
    Player player = player(h, pos);
    var absolute = h.absolutePos(pos);
    var hit =
        new net.minecraft.world.phys.BlockHitResult(
            absolute.getCenter(), Direction.EAST, absolute, false);
    java.util.function.Function<Item, net.minecraft.world.item.context.UseOnContext> use =
        item ->
            new net.minecraft.world.item.context.UseOnContext(
                h.getLevel(),
                player,
                net.minecraft.world.InteractionHand.MAIN_HAND,
                new ItemStack(item),
                hit);
    ArchitectureCraft.GLOWBRUSH.useOn(use.apply(ArchitectureCraft.GLOWBRUSH));
    check(h, be.getBlockState().getValue(ShapeBlock.GLOWING), "Glowbrush emits light");
    check(h, be.data().equals(original), "Glowbrush preserves both materials");
    int turn = be.getBlockState().getValue(ShapeBlock.TURN);
    ArchitectureCraft.HAMMER.useOn(use.apply(ArchitectureCraft.HAMMER));
    check(h, be.getBlockState().getValue(ShapeBlock.TURN) == (turn + 1) % 4, "Hammer rotates");
    ArchitectureCraft.CHISEL.useOn(use.apply(ArchitectureCraft.CHISEL));
    check(h, be.data().secondary().isEmpty(), "Chisel removes cladding");
    player.setShiftKeyDown(true);
    ArchitectureCraft.CHISEL.useOn(use.apply(ArchitectureCraft.CHISEL));
    check(
        h,
        (be.data().disconnected() & (1 << Direction.EAST.get3DDataValue())) != 0,
        "Chisel disconnects clicked face");
    check(
        h,
        (other.data().disconnected() & (1 << Direction.WEST.get3DDataValue())) != 0,
        "Connection changes at both ends");
    ArchitectureCraft.GLOWBRUSH.useOn(use.apply(ArchitectureCraft.GLOWBRUSH));
    check(h, !be.getBlockState().getValue(ShapeBlock.GLOWING), "Sneaking removes glow");
    h.succeed();
  }

  @GameTest
  public void shapeItemPlacesWithItsMaterialComponents(GameTestHelper h) {
    BlockPos ground = new BlockPos(1, 1, 1);
    h.setBlock(ground, Blocks.STONE);
    ShapeData data =
        new ShapeData(Shape.Cylinder.id, Blocks.BRICKS.defaultBlockState(), Optional.empty(), 0, 0);
    ItemStack stack = data.stack(2);
    Player player = player(h, ground);
    player.setPos(h.absolutePos(ground).offset(3, 0, 0).getCenter());
    BlockPos absolute = h.absolutePos(ground);
    var hit =
        new net.minecraft.world.phys.BlockHitResult(
            absolute.getCenter().add(0, .5, 0), Direction.UP, absolute, false);
    var context =
        new net.minecraft.world.item.context.UseOnContext(
            h.getLevel(), player, net.minecraft.world.InteractionHand.MAIN_HAND, stack, hit);
    stack.getItem().useOn(context);
    var be = h.getBlockEntity(ground.above(), ShapeBlockEntity.class);
    check(h, be.data().equals(data), "Placement applies shape and material components");
    check(h, stack.getCount() == 1, "Survival placement consumes one item");
    var buf =
        new net.minecraft.network.RegistryFriendlyByteBuf(
            io.netty.buffer.Unpooled.buffer(), h.getLevel().registryAccess());
    try {
      ArchitectureCraft.SHAPE_DATA.streamCodec().encode(buf, data);
      check(
          h,
          ArchitectureCraft.SHAPE_DATA.streamCodec().decode(buf).equals(data),
          "Component network roundtrip preserves materials");
    } finally {
      buf.release();
    }
    h.succeed();
  }

  @GameTest
  public void sawbenchRejectsPlantsAndChargesForSlabs(GameTestHelper h) {
    check(
        h,
        !ArchitectureCraft.validMaterial(new ItemStack(Blocks.DANDELION)),
        "Plants cannot become building materials");
    BlockPos pos = new BlockPos(1, 1, 1);
    h.setBlock(pos, ArchitectureCraft.SAWBENCH);
    var bench = h.getBlockEntity(pos, SawbenchBlockEntity.class);
    Player player = player(h, pos);
    var menu = new SawbenchMenu(1, player.getInventory(), bench);
    bench.setItem(0, new ItemStack(Blocks.OAK_SLAB, 2));
    menu.clickMenuButton(player, Shape.RoofTile.ordinal());
    check(h, menu.clickMenuButton(player, SawbenchMenu.CUT), "Slabs accepted");
    check(h, bench.getItem(0).isEmpty(), "Two slabs pay for a one-block batch");
    check(h, bench.getItem(1).getCount() == 2, "Original shape yield retained");
    h.succeed();
  }
}

package gcewing.architecture.client;

import gcewing.architecture.*;
import gcewing.architecture.shape.Shape;
import java.util.*;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.core.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;

/** Exercises model loading, every materialized shape item, chunk meshes and the sawbench screen. */
public final class ArchitectureClientTest implements FabricClientGameTest {
  @Override
  public void runTest(ClientGameTestContext context) {
    context.runOnClient(
        mc -> {
          var resolver = mc.getItemModelResolver();
          for (Shape shape : Shape.values()) {
            ShapeData data =
                new ShapeData(
                    shape.id,
                    Blocks.BRICKS.defaultBlockState(),
                    shape.window()
                        ? Optional.of(Blocks.GLASS.defaultBlockState())
                        : Optional.empty(),
                    0,
                    0);
            ItemStackRenderState state = new ItemStackRenderState();
            resolver.updateForTopItem(state, data.stack(1), ItemDisplayContext.GUI, null, null, 42);
            if (state.isEmpty()) throw new AssertionError("Empty item model for " + shape);
            var model =
                mc.getBlockRenderer().getBlockModel(ArchitectureCraft.SHAPE.defaultBlockState());
            if (!(model instanceof ArchitectureBlockModel))
              throw new AssertionError("Custom shape model not installed");
          }
        });
    try (var world = context.worldBuilder().create()) {
      world
          .getServer()
          .runOnServer(
              server -> {
                var level = server.overworld();
                var player = server.getPlayerList().getPlayers().getFirst();
                player.setGameMode(net.minecraft.world.level.GameType.CREATIVE);
                player.getAbilities().flying = true;
                player.onUpdateAbilities();
                player.setNoGravity(true);
                player.connection.teleport(0, 8, 16, 180, 25);
                for (int slot = 0; slot < 9; slot++)
                  player
                      .getInventory()
                      .setItem(
                          slot,
                          new ShapeData(
                                  Shape.values()[slot * 5].id,
                                  Blocks.BRICKS.defaultBlockState(),
                                  Optional.empty(),
                                  0,
                                  0)
                              .stack(1));
                int i = 0;
                for (Shape shape : Shape.values()) {
                  BlockPos pos = new BlockPos((i % 12) * 2 - 12, 3, (i / 12) * 2 - 12);
                  i++;
                  level.setBlockAndUpdate(pos, ArchitectureCraft.SHAPE.defaultBlockState());
                  var be = (ShapeBlockEntity) level.getBlockEntity(pos);
                  be.setData(
                      new ShapeData(
                          shape.id,
                          Blocks.BRICKS.defaultBlockState(),
                          shape.window()
                              ? Optional.of(Blocks.GLASS.defaultBlockState())
                              : Optional.empty(),
                          0,
                          0));
                }
                level.setBlockAndUpdate(
                    new BlockPos(0, 3, 8), ArchitectureCraft.SAWBENCH.defaultBlockState());
              });
      context.waitFor(mc -> mc.player != null && mc.player.getY() > 5);
      world.getClientWorld().waitForChunksRender();
      context.runOnClient(
          mc -> {
            if (!mc.level.getBlockState(new BlockPos(0, 3, 8)).is(ArchitectureCraft.SAWBENCH))
              throw new AssertionError("Sawbench chunk was not synchronized");
          });
      context.takeScreenshot("architecturecraft-shapes");
      world
          .getServer()
          .runOnServer(
              server -> {
                var player = server.getPlayerList().getPlayers().getFirst();
                player.connection.teleport(0, 6, 10, 180, 20);
                var bench =
                    (SawbenchBlockEntity) server.overworld().getBlockEntity(new BlockPos(0, 3, 8));
                bench.setItem(0, new ItemStack(Blocks.BRICKS, 32));
                player.openMenu(bench);
              });
      context.waitForScreen(SawbenchScreen.class);
      context.clickScreenButton("gui.architecturecraft.cut");
      context.waitFor(mc -> mc.player.containerMenu.getSlot(1).hasItem());
      context.runOnClient(
          mc -> {
            var output = mc.player.containerMenu.getSlot(1).getItem();
            if (output.getCount() != 2 || !ShapeData.fromStack(output).base().is(Blocks.BRICKS))
              throw new AssertionError("Sawbench network craft failed");
          });
      context.takeScreenshot("architecturecraft-sawbench");
      context.setScreen(() -> null);
    }
  }
}

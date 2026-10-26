package gcewing.architecture.client;

import gcewing.architecture.*;
import gcewing.architecture.shape.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.fabricmc.fabric.api.renderer.v1.Renderer;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.core.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Real mouse input, real server menus and actual preview extraction/rendering, not a synthetic
 * replacement GUI.
 */
public final class ArchitectureClientTest implements FabricClientGameTest {
  private static void click(ClientGameTestContext context, int x, int y, int button) {
    double[] pos =
        context.computeOnClient(
            mc -> {
              var screen = (SawbenchScreen) mc.screen;
              var window = mc.getWindow();
              return new double[] {
                (screen.guiLeft() + x)
                    * (double) window.getScreenWidth()
                    / window.getGuiScaledWidth(),
                (screen.guiTop() + y)
                    * (double) window.getScreenHeight()
                    / window.getGuiScaledHeight()
              };
            });
    context.getInput().setCursorPos(pos[0], pos[1]);
    context.getInput().pressMouse(button);
  }

  private static void assertPreviewDrawn(java.nio.file.Path off, java.nio.file.Path on) {
    try {
      var before = javax.imageio.ImageIO.read(off.toFile());
      var after = javax.imageio.ImageIO.read(on.toFile());
      // This fixed camera targets the preview above the anchor. Exclude the
      // hotbar, held item and recipe toasts, which also change when selecting items.
      int left = (int) (before.getWidth() * .44), right = (int) (before.getWidth() * .56);
      int top = (int) (before.getHeight() * .28), bottom = (int) (before.getHeight() * .51);
      int changed = 0;
      for (int y = top; y < bottom; y++)
        for (int x = left; x < right; x++) {
          int a = before.getRGB(x, y), b = after.getRGB(x, y);
          for (int shift = 0; shift <= 16; shift += 8) {
            if (Math.abs(((a >>> shift) & 255) - ((b >>> shift) & 255)) > 15) {
              changed++;
              break;
            }
          }
        }
      if (changed < (right - left) * (bottom - top) / 10)
        throw new AssertionError("Preview snapshot exists but no visible preview was drawn");
    } catch (java.io.IOException e) {
      throw new java.io.UncheckedIOException(e);
    }
  }

  @Override
  public void runTest(ClientGameTestContext context) {
    context.runOnClient(
        mc -> {
          for (Shape shape : Shape.values()) {
            var data =
                new ShapeData(
                    shape.id,
                    Blocks.BRICKS.defaultBlockState(),
                    shape.window()
                        ? Optional.of(Blocks.GLASS_PANE.defaultBlockState())
                        : Optional.empty(),
                    0,
                    0);
            var item = new ItemStackRenderState();
            mc.getItemModelResolver()
                .updateForTopItem(item, data.stack(1), ItemDisplayContext.GUI, null, null, 42);
            if (item.isEmpty()) throw new AssertionError("Empty item model: " + shape);
          }
          if (!(mc.getBlockRenderer().getBlockModel(ArchitectureCraft.SHAPE.defaultBlockState())
              instanceof ArchitectureBlockModel))
            throw new AssertionError("Custom shape model missing");
          var data = new ShapeData(0, Blocks.BRICKS.defaultBlockState(), Optional.empty(), 0, 0);
          List<BakedQuad> quads = new ArrayList<>();
          MeshRendering.emit(
              RoofMesh.create(Shape.RoofTile, (x, y, z) -> null),
              data,
              false,
              false,
              null,
              null,
              Renderer.get().mutableMesh().emitter(),
              quads);
          var sprite = quads.getFirst().sprite();
          double min = Double.POSITIVE_INFINITY, max = Double.NEGATIVE_INFINITY;
          for (var quad : quads)
            for (int v = 0; v < 4; v++) {
              float u = Float.intBitsToFloat(quad.vertices()[v * 8 + 4]);
              min = Math.min(min, u);
              max = Math.max(max, u);
            }
          if (max - min < (sprite.getU1() - sprite.getU0()) * .9)
            throw new AssertionError("Model UVs must cover the material texture");
        });
    context.runOnClient(
        mc -> {
          // Mixed axis-aligned and diagonal faces must keep daylight lighting, with no
          // cube-corner AO on the oblique faces. Exercise the actual emitted FRAPI flags.
          mc.options.ambientOcclusion().set(true);
          int[] counts = new int[2];
          for (Shape shape :
              List.of(
                  Shape.RoofTile,
                  Shape.SlopeTileA1,
                  Shape.SlopeTileB1,
                  Shape.SlopeTileC1,
                  Shape.SphereFull)) {
            var data =
                new ShapeData(
                    shape.id, Blocks.STONE_BRICKS.defaultBlockState(), Optional.empty(), 0, 0);
            var emitter = Renderer.get().mutableMesh().emitter();
            emitter.pushTransform(
                q -> {
                  var n = q.faceNormal();
                  boolean diagonal =
                      Math.max(Math.abs(n.x()), Math.max(Math.abs(n.y()), Math.abs(n.z())))
                          < .9999f;
                  if (diagonal) {
                    counts[0]++;
                    if (q.ambientOcclusion() != net.fabricmc.fabric.api.util.TriState.FALSE)
                      throw new AssertionError(
                          "Oblique surfaces must not use cube-corner AO: " + shape);
                  } else if (q.ambientOcclusion() == net.fabricmc.fabric.api.util.TriState.TRUE)
                    counts[1]++;
                  if (q.emissive() || !q.diffuseShade())
                    throw new AssertionError(
                        "Slope lighting must retain world light and directional shading");
                  return true;
                });
            MeshRendering.emit(
                ShapeGeometry.mesh(data, ArchitectureCraft.SHAPE.defaultBlockState(), null, null),
                data,
                false,
                false,
                null,
                null,
                emitter,
                null);
            emitter.popTransform();
          }
          if (counts[0] == 0 || counts[1] == 0)
            throw new AssertionError(
                "Regression must exercise both oblique and axis-aligned lighting");
        });
    AtomicReference<PlacementPreview.Snapshot> preview = new AtomicReference<>();
    WorldRenderEvents.END_EXTRACTION.register(
        frame -> {
          var data = frame.worldState().getData(PlacementPreview.PREVIEW);
          preview.set(data);
        });
    try (var world = context.worldBuilder().create()) {
      world
          .getServer()
          .runOnServer(
              server -> {
                var level = server.overworld();
                NativeSeamRegression.setup(level);
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
                  if (shape == Shape.CladdingSheet) continue;
                  BlockPos pos = new BlockPos((i % 12) * 2 - 12, 3, (i / 12) * 2 - 12);
                  i++;
                  level.setBlockAndUpdate(pos, ArchitectureCraft.SHAPE.defaultBlockState());
                  ((ShapeBlockEntity) level.getBlockEntity(pos))
                      .setData(
                          new ShapeData(
                              shape.id,
                              Blocks.BRICKS.defaultBlockState(),
                              shape.window()
                                  ? Optional.of(Blocks.GLASS_PANE.defaultBlockState())
                                  : Optional.empty(),
                              0,
                              0));
                }
                level.setBlockAndUpdate(
                    new BlockPos(0, 3, 8), ArchitectureCraft.SAWBENCH.defaultBlockState());
                // Isolated seam cases: identical glass, a frame with holes, opaque material,
                // and a native glass neighbor. No block-ID-specific culling rules.
                for (int pair = 0; pair < 4; pair++) {
                  BlockPos p = new BlockPos(24 + pair * 3, 3, 8);
                  level.setBlockAndUpdate(p, ArchitectureCraft.SHAPE.defaultBlockState());
                  ((ShapeBlockEntity) level.getBlockEntity(p))
                      .setData(
                          new ShapeData(
                              Shape.Slab.id,
                              Blocks.GLASS.defaultBlockState(),
                              Optional.empty(),
                              0,
                              0));
                  if (pair == 3) {
                    level.setBlockAndUpdate(p.east(), Blocks.GLASS.defaultBlockState());
                  } else {
                    level.setBlockAndUpdate(p.east(), ArchitectureCraft.SHAPE.defaultBlockState());
                    ((ShapeBlockEntity) level.getBlockEntity(p.east()))
                        .setData(
                            new ShapeData(
                                (pair == 1 ? Shape.WindowFrame : Shape.Slab).id,
                                (pair == 2 ? Blocks.STONE : Blocks.GLASS).defaultBlockState(),
                                Optional.empty(),
                                0,
                                0));
                  }
                }
              });
      context.waitFor(mc -> mc.player != null && mc.player.getY() > 5);
      context.runOnClient(
          mc -> {
            var data =
                new ShapeData(0, Blocks.REDSTONE_BLOCK.defaultBlockState(), Optional.empty(), 0, 0);
            List<BakedQuad> quads = new ArrayList<>();
            MeshRendering.emit(
                RoofMesh.create(Shape.RoofTile, (x, y, z) -> null),
                data,
                false,
                false,
                null,
                null,
                Renderer.get().mutableMesh().emitter(),
                quads);
            if (quads.stream().anyMatch(q -> q.lightEmission() != 15))
              throw new AssertionError(
                  "Tagged material should render full-bright without a glowbrush");
          });
      world.getClientWorld().waitForChunksRender();
      context.runOnClient(NativeSeamRegression::verify);
      context.runOnClient(
          mc -> {
            for (int pair = 0; pair < 4; pair++) {
              BlockPos p = new BlockPos(24 + pair * 3, 3, 8);
              var be = (ShapeBlockEntity) mc.level.getBlockEntity(p);
              var mesh = ShapeGeometry.mesh(be.data(), be.getBlockState(), mc.level, p);
              var culling = new ShapeFaceCulling(mc.level, p);
              long hidden =
                  mesh.triangles().stream()
                      .filter(
                          t ->
                              culling.hidden(
                                  t,
                                  Direction.getApproximateNearest(
                                      (float) t.a().nx(), (float) t.a().ny(), (float) t.a().nz()),
                                  be.data().base()))
                      .count();
              if ((pair == 0 || pair == 3) ? hidden == 0 : hidden != 0)
                throw new AssertionError(
                    "Incorrect shared transparent face removal for case " + pair + ": " + hidden);
            }
            var emitter = Renderer.get().mutableMesh().emitter();
            List<Boolean> ambient = new ArrayList<>();
            emitter.pushTransform(
                q -> {
                  ambient.add(q.ambientOcclusion() == net.fabricmc.fabric.api.util.TriState.FALSE);
                  return true;
                });
            List<BakedQuad> quads = new ArrayList<>();
            var data =
                new ShapeData(
                    Shape.Slab.id,
                    MaterialTestBlocks.TRANSPARENT_CUBE.defaultBlockState(),
                    Optional.empty(),
                    0,
                    0);
            MeshRendering.emit(
                ShapeGeometry.mesh(data, ArchitectureCraft.SHAPE.defaultBlockState(), null, null),
                data,
                false,
                false,
                null,
                null,
                emitter,
                quads);
            emitter.popTransform();
            if (quads.isEmpty()
                || quads.stream().anyMatch(BakedQuad::shade)
                || ambient.isEmpty()
                || ambient.stream().anyMatch(v -> !v))
              throw new AssertionError(
                  "Cut surfaces must retain the source model's disabled shading and ambient"
                      + " occlusion");
          });
      context.takeScreenshot("architecturecraft-shapes");
      world
          .getServer()
          .runOnServer(
              server -> {
                var player = server.getPlayerList().getPlayers().getFirst();
                player.connection.teleport(0, 6, 10, 180, 20);
                player.openMenu(
                    (SawbenchBlockEntity) server.overworld().getBlockEntity(new BlockPos(0, 3, 8)));
              });
      context.waitForScreen(SawbenchScreen.class);
      click(context, 44 + 3 * 24 + 12, 23 + 24 + 12, 0);
      context.waitFor(mc -> ((SawbenchMenu) mc.player.containerMenu).selectedSlot(0) == 8);
      click(context, 176 + 10, 19 + 10 + 5, 0);
      context.waitFor(mc -> ((SawbenchMenu) mc.player.containerMenu).page() == 1);
      click(context, 44 + 24 + 12, 23 + 24 + 12, 0);
      context.waitFor(mc -> ((SawbenchMenu) mc.player.containerMenu).selectedSlot(1) == 6);
      click(context, 176 + 10, 19 + 5, 0);
      context.waitFor(mc -> ((SawbenchMenu) mc.player.containerMenu).page() == 0);
      context.runOnClient(
          mc -> {
            var menu = (SawbenchMenu) mc.player.containerMenu;
            if (menu.selectedSlot(0) != 8 || menu.selectedShape() != Shape.RoofOverhangOuterCorner)
              throw new AssertionError("Category switching lost selection");
          });
      context.takeScreenshot("architecturecraft-sawbench-reference-ui");
      world
          .getServer()
          .runOnServer(
              server ->
                  ((SawbenchBlockEntity) server.overworld().getBlockEntity(new BlockPos(0, 3, 8)))
                      .setItem(0, new ItemStack(Blocks.BRICKS, 32)));
      context.waitFor(mc -> mc.player.containerMenu.getSlot(1).getItem().getCount() == 3);
      context.runOnClient(
          mc -> {
            if (mc.player.containerMenu.getSlot(0).getItem().getCount() != 32)
              throw new AssertionError("Preview charged material");
          });
      click(context, 12 + 8, 57 + 8, 0);
      context.waitFor(
          mc ->
              mc.player.containerMenu.getCarried().getCount() == 3
                  && mc.player.containerMenu.getSlot(0).getItem().getCount() == 31);
      context.takeScreenshot("architecturecraft-sawbench-crafting");
      context.runOnClient(mc -> mc.screen.onClose());
      context.waitForScreen(null);
      // Place a preview anchor away from the catalog and aim at it within interaction range.
      var anchor = new BlockPos(16, 3, 8);
      world
          .getServer()
          .runOnServer(
              server -> {
                var player = server.getPlayerList().getPlayers().getFirst();
                server.overworld().setBlockAndUpdate(anchor, Blocks.STONE.defaultBlockState());
                player
                    .getInventory()
                    .setItem(
                        0,
                        new ShapeData(
                                Shape.RoofTile.id,
                                Blocks.BRICKS.defaultBlockState(),
                                Optional.empty(),
                                0,
                                0)
                            .stack(1));
                player.connection.teleport(16.5, 5, 11.5, 180, 41.13f);
              });
      context.waitFor(
          mc -> mc.hitResult instanceof BlockHitResult hit && hit.getBlockPos().equals(anchor));
      context.waitFor(mc -> preview.get() != null && preview.get().pos().equals(anchor.above()));
      var expected = preview.get();
      if (expected.quads().isEmpty()) throw new AssertionError("Placement preview is empty");
      // Capture an identical camera view without the preview, so validation can
      // compare the world pixels rather than just finding a nonempty snapshot.
      world
          .getServer()
          .runOnServer(
              server ->
                  server
                      .getPlayerList()
                      .getPlayers()
                      .getFirst()
                      .getInventory()
                      .setItem(0, ItemStack.EMPTY));
      context.waitFor(mc -> mc.player.getMainHandItem().isEmpty() && preview.get() == null);
      context.waitTicks(3);
      var previewOff = context.takeScreenshot("architecturecraft-placement-preview-off");
      world
          .getServer()
          .runOnServer(
              server ->
                  server
                      .getPlayerList()
                      .getPlayers()
                      .getFirst()
                      .getInventory()
                      .setItem(
                          0,
                          new ShapeData(
                                  Shape.RoofTile.id,
                                  Blocks.BRICKS.defaultBlockState(),
                                  Optional.empty(),
                                  0,
                                  0)
                              .stack(1)));
      context.waitFor(mc -> preview.get() != null && preview.get().pos().equals(anchor.above()));
      context.waitTicks(3);
      var previewOn = context.takeScreenshot("architecturecraft-placement-preview");
      assertPreviewDrawn(previewOff, previewOn);
      context.runOnClient(mc -> mc.options.hideGui = true);
      context.waitFor(mc -> preview.get() == null);
      context.runOnClient(mc -> mc.options.hideGui = false);
      context.waitFor(mc -> preview.get() != null && preview.get().pos().equals(anchor.above()));
      context.runOnClient(
          mc ->
              mc.gameMode.useItemOn(
                  mc.player, InteractionHand.MAIN_HAND, (BlockHitResult) mc.hitResult));
      context.waitFor(mc -> mc.level.getBlockEntity(anchor.above()) instanceof ShapeBlockEntity);
      context.runOnClient(
          mc -> {
            var be = (ShapeBlockEntity) mc.level.getBlockEntity(anchor.above());
            if (!be.getBlockState().equals(expected.plan().state())
                || !be.data().equals(expected.plan().data()))
              throw new AssertionError("Preview differs from actual placement");
          });
      context.takeScreenshot("architecturecraft-preview-placed");
      world
          .getServer()
          .runOnServer(
              server -> {
                var be = (ShapeBlockEntity) server.overworld().getBlockEntity(anchor.above());
                be.setData(
                    new ShapeData(
                        Shape.WindowFrame.id,
                        Blocks.BRICKS.defaultBlockState(),
                        Optional.empty(),
                        0,
                        0));
                server
                    .overworld()
                    .setBlockAndUpdate(anchor.above(), ArchitectureCraft.SHAPE.defaultBlockState());
              });
      context.waitFor(
          mc ->
              mc.level.getBlockEntity(anchor.above()) instanceof ShapeBlockEntity be
                  && be.data().shape() == Shape.WindowFrame);
      context.runOnClient(
          mc -> {
            var be = (ShapeBlockEntity) mc.level.getBlockEntity(anchor.above());
            var boxes =
                ShapeGeometry.collision(
                        be.data(), be.getBlockState(), mc.level, be.getBlockPos(), true)
                    .toAabbs();
            var box = boxes.stream().filter(b -> b.getYsize() < .2).findFirst().orElseThrow();
            var center =
                box.getCenter()
                    .add(be.getBlockPos().getX(), be.getBlockPos().getY(), be.getBlockPos().getZ());
            var hit =
                new BlockHitResult(
                    center.add(0, 0, box.getZsize() / 2), Direction.SOUTH, be.getBlockPos(), false);
            var outline = PlacementPreview.outline(be, center.add(0, 0, 3), hit);
            if (outline.toAabbs().size() != 1 || outline.bounds().getYsize() >= .2)
              throw new AssertionError(
                  "Window outline must highlight the selected frame, not the whole window");
          });
      world
          .getServer()
          .runOnServer(
              server -> {
                var level = server.overworld();
                level.setDayTime(6000);
                for (int x = 8; x < 14; x++) {
                  for (int y = 3; y < 7; y++) {
                    var p = new BlockPos(x, y, 24);
                    level.setBlockAndUpdate(
                        p,
                        ArchitectureCraft.SHAPE
                            .defaultBlockState()
                            .setValue(ShapeBlock.FACING, Direction.SOUTH));
                    ((ShapeBlockEntity) level.getBlockEntity(p))
                        .setData(
                            new ShapeData(
                                Shape.SlopeTileA1.id,
                                Blocks.STONE_BRICKS.defaultBlockState(),
                                Optional.empty(),
                                0,
                                0));
                  }
                  for (int z = 22; z < 29; z++)
                    level.setBlockAndUpdate(
                        new BlockPos(x, 2, z), Blocks.STONE_BRICKS.defaultBlockState());
                }
                var player = server.getPlayerList().getPlayers().getFirst();
                player.getInventory().setItem(0, ItemStack.EMPTY);
                player.connection.teleport(11, 5, 31, 180, 8);
              });
      context.waitFor(mc -> mc.player.getZ() > 30 && mc.level.getDayTime() >= 6000);
      world.getClientWorld().waitForChunksRender();
      context.waitTicks(40); // Allow the lightmap to settle after teleport/time changes.
      context.takeScreenshot("architecturecraft-slope-smooth-lighting-day");
      world.getServer().runOnServer(server -> server.overworld().setDayTime(18000));
      context.waitFor(mc -> mc.level.getDayTime() >= 18000);
      context.waitTicks(40);
      context.takeScreenshot("architecturecraft-slope-smooth-lighting-night");
    }
  }
}

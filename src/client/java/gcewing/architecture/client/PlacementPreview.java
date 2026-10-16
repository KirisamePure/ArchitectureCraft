package gcewing.architecture.client;

import gcewing.architecture.*;
import java.util.*;
import net.fabricmc.fabric.api.client.rendering.v1.RenderStateDataKey;
import net.fabricmc.fabric.api.client.rendering.v1.world.*;
import net.fabricmc.fabric.api.renderer.v1.Renderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.state.BlockOutlineRenderState;
import net.minecraft.client.renderer.texture.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.context.*;
import net.minecraft.world.phys.*;
import net.minecraft.world.phys.shapes.*;

/**
 * Extract immutable preview geometry before rendering; never insert a temporary block entity into
 * the world.
 */
public final class PlacementPreview {
  record Snapshot(BlockPos pos, List<BakedQuad> quads, int light, ShapePlacement.Plan plan) {}

  static final RenderStateDataKey<Snapshot> PREVIEW =
      RenderStateDataKey.create(() -> "ArchitectureCraft placement preview");

  private PlacementPreview() {}

  public static void register() {
    WorldRenderEvents.END_EXTRACTION.register(
        context -> {
          Minecraft mc = Minecraft.getInstance();
          context
              .worldState()
              .setData(
                  PREVIEW,
                  context.blockOutlines() && mc.hitResult instanceof BlockHitResult hit
                      ? extract(mc, hit)
                      : null);
        });
    WorldRenderEvents.BEFORE_TRANSLUCENT.register(
        context -> {
          Snapshot snapshot = context.worldState().getData(PREVIEW);
          if (snapshot == null) return;
          Vec3 camera = context.worldState().cameraRenderState.pos;
          var matrices = context.matrices();
          matrices.pushPose();
          matrices.translate(
              snapshot.pos.getX() - camera.x,
              snapshot.pos.getY() - camera.y,
              snapshot.pos.getZ() - camera.z);
          var buffer =
              context
                  .consumers()
                  .getBuffer(RenderType.entityTranslucent(TextureAtlas.LOCATION_BLOCKS));
          int light = snapshot.light;
          int[] lights = {light, light, light, light};
          float[] brightness = {1, 1, 1, 1};
          for (BakedQuad quad : snapshot.quads)
            buffer.putBulkData(
                matrices.last(),
                quad,
                brightness,
                1,
                1,
                1,
                .45f,
                lights,
                OverlayTexture.NO_OVERLAY,
                true);
          matrices.popPose();
        });
    WorldRenderEvents.AFTER_BLOCK_OUTLINE_EXTRACTION.register(
        (context, result) -> {
          var old = context.worldState().blockOutlineRenderState;
          if (old == null
              || !(result instanceof BlockHitResult hit)
              || !(context.world().getBlockEntity(hit.getBlockPos())
                  instanceof ShapeBlockEntity be)) return;
          VoxelShape shape = outline(be, context.camera().getPosition(), hit);
          context.worldState().blockOutlineRenderState =
              new BlockOutlineRenderState(
                  old.pos(), old.isTranslucent(), old.highContrast(), shape);
        });
  }

  static Snapshot extract(Minecraft mc, BlockHitResult hit) {
    if (mc.player == null || mc.level == null || hit.getType() != HitResult.Type.BLOCK) return null;
    InteractionHand hand =
        mc.player.getMainHandItem().getItem() instanceof ShapeItem
            ? InteractionHand.MAIN_HAND
            : InteractionHand.OFF_HAND;
    if (!(mc.player.getItemInHand(hand).getItem() instanceof ShapeItem)) return null;
    BlockPlaceContext placement = new BlockPlaceContext(new UseOnContext(mc.player, hand, hit));
    if (!placement.canPlace()
        || !mc.level.getWorldBorder().isWithinBounds(placement.getClickedPos())) return null;
    ShapePlacement.Plan plan = ShapePlacement.plan(placement);
    BlockPos pos = placement.getClickedPos();
    var mesh = ShapeGeometry.mesh(plan.data(), plan.state(), mc.level, pos);
    List<BakedQuad> quads = new ArrayList<>();
    MeshRendering.emit(
        mesh,
        plan.data(),
        false,
        plan.state().getValue(ShapeBlock.GLOWING),
        mc.level,
        pos,
        Renderer.get().mutableMesh().emitter(),
        quads);
    int light = LevelRenderer.getLightColor(mc.level, pos);
    light = Math.max(light & 65535, 80) | Math.max(light >>> 16, 80) << 16;
    return new Snapshot(pos, List.copyOf(quads), light, plan);
  }

  static VoxelShape outline(ShapeBlockEntity be, Vec3 start, BlockHitResult hit) {
    var shape =
        ShapeGeometry.collision(
            be.data(), be.getBlockState(), be.getLevel(), be.getBlockPos(), true);
    if (shape.isEmpty()) return shape;
    if (!be.data().shape().window()) {
      if (!be.data().shape().roof()) {
        var mesh =
            ShapeGeometry.mesh(be.data(), be.getBlockState(), be.getLevel(), be.getBlockPos());
        double[] min = {100, 100, 100}, max = {-100, -100, -100};
        for (var t : mesh.triangles())
          for (var v : List.of(t.a(), t.b(), t.c())) {
            double[] p = {v.x(), v.y(), v.z()};
            for (int i = 0; i < 3; i++) {
              min[i] = Math.min(min[i], p[i]);
              max[i] = Math.max(max[i], p[i]);
            }
          }
        return Shapes.box(min[0], min[1], min[2], max[0], max[1], max[2]);
      }
      return Shapes.create(shape.bounds());
    }
    BlockPos pos = be.getBlockPos();
    Vec3 localStart = start.subtract(pos.getX(), pos.getY(), pos.getZ());
    Vec3 localEnd =
        hit.getLocation()
            .subtract(pos.getX(), pos.getY(), pos.getZ())
            .add(
                hit.getDirection().getStepX() * -.01,
                hit.getDirection().getStepY() * -.01,
                hit.getDirection().getStepZ() * -.01);
    AABB closest = null;
    double distance = Double.POSITIVE_INFINITY;
    for (AABB box : shape.toAabbs()) {
      var intersection = box.clip(localStart, localEnd);
      if (intersection.isPresent() && intersection.get().distanceToSqr(localStart) < distance) {
        closest = box;
        distance = intersection.get().distanceToSqr(localStart);
      }
    }
    return closest == null ? shape : Shapes.create(closest);
  }
}

package gcewing.architecture.client;

import gcewing.architecture.*;
import gcewing.architecture.shape.Mesh;
import java.util.*;
import net.fabricmc.fabric.api.renderer.v1.mesh.QuadEmitter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.block.model.*;
import net.minecraft.client.renderer.texture.*;
import net.minecraft.core.*;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;

final class MeshRendering {
  private MeshRendering() {}

  static BlockStateModel materialModel(BlockState material) {
    return Minecraft.getInstance().getBlockRenderer().getBlockModel(material);
  }

  private record Surface(TextureAtlasSprite sprite, int tint, boolean shade, boolean ao) {}

  private static Surface surface(BlockState material, Direction face) {
    BlockStateModel model = materialModel(material);
    for (BlockModelPart part : model.collectParts(RandomSource.create(42))) {
      List<BakedQuad> quads = part.getQuads(face);
      if (!quads.isEmpty())
        return new Surface(
            quads.getFirst().sprite(),
            quads.getFirst().tintIndex(),
            quads.getFirst().shade(),
            part.useAmbientOcclusion());
      for (BakedQuad quad : part.getQuads(null))
        if (quad.direction() == face)
          return new Surface(
              quad.sprite(), quad.tintIndex(), quad.shade(), part.useAmbientOcclusion());
    }
    return new Surface(model.particleIcon(), -1, true, true);
  }

  /** Avoid cube-corner AO artifacts on oblique and smoothly curved triangle surfaces. */
  static boolean supportsSmoothLighting(Mesh.Triangle triangle) {
    var a = triangle.a();
    double ux = triangle.b().x() - a.x(),
        uy = triangle.b().y() - a.y(),
        uz = triangle.b().z() - a.z();
    double vx = triangle.c().x() - a.x(),
        vy = triangle.c().y() - a.y(),
        vz = triangle.c().z() - a.z();
    double nx = uy * vz - uz * vy, ny = uz * vx - ux * vz, nz = ux * vy - uy * vx;
    double length = Math.sqrt(nx * nx + ny * ny + nz * nz);
    if (length < 1e-12) return false;
    if (Math.max(Math.abs(nx), Math.max(Math.abs(ny), Math.abs(nz))) / length < .99999)
      return false;
    // A planar triangle can still belong to a surface with interpolated curved normals.
    for (var v : List.of(triangle.a(), triangle.b(), triangle.c())) {
      double normalLength = Math.sqrt(v.nx() * v.nx() + v.ny() * v.ny() + v.nz() * v.nz());
      if (normalLength < 1e-12
          || Math.max(Math.abs(v.nx()), Math.max(Math.abs(v.ny()), Math.abs(v.nz()))) / normalLength
              < .99999) return false;
    }
    return true;
  }

  static void emit(
      Mesh mesh,
      ShapeData data,
      boolean sawbench,
      boolean glow,
      BlockAndTintGetter world,
      BlockPos pos,
      QuadEmitter emitter,
      List<BakedQuad> baked) {
    Map<String, TextureAtlasSprite> sprites = new HashMap<>();
    Map<String, Surface> surfaces = new HashMap<>();
    ShapeFaceCulling culling =
        !sawbench && world != null && pos != null ? new ShapeFaceCulling(world, pos) : null;
    for (Mesh.Triangle triangle : mesh.triangles()) {
      Mesh.Vertex a = triangle.a();
      Direction face =
          Direction.getApproximateNearest((float) a.nx(), (float) a.ny(), (float) a.nz());
      BlockState material =
          triangle.material() >= 2 ? data.secondary().orElse(data.base()) : data.base();
      if (culling != null && culling.hidden(triangle, face, material)) continue;
      TextureAtlasSprite sprite;
      boolean shade = true, ao = true;
      int color = -1;
      if (sawbench) {
        String texture = "block/sawbench_" + (triangle.material() == 0 ? "wood" : "metal");
        sprite =
            sprites.computeIfAbsent(
                texture,
                k ->
                    Minecraft.getInstance()
                        .getAtlasManager()
                        .getAtlasOrThrow(net.minecraft.data.AtlasIds.BLOCKS)
                        .getSprite(ArchitectureCraft.id(k)));
      } else {
        String key = material.toString() + face;
        Surface surface = surfaces.computeIfAbsent(key, k -> surface(material, face));
        sprite = surface.sprite();
        shade = surface.shade();
        ao = surface.ao();
        if (surface.tint() >= 0) {
          int tint =
              Minecraft.getInstance()
                  .getBlockColors()
                  .getColor(material, world, pos, surface.tint());
          if (tint != -1) color = 0xff000000 | tint;
        }
      }
      emitter
          .nominalFace(face)
          .cullFace(null)
          .tintIndex(-1)
          .emissive(glow || material.is(ArchitectureCraft.EMISSIVE_MATERIALS))
          .diffuseShade(!glow && shade)
          .ambientOcclusion(
              net.fabricmc.fabric.api.util.TriState.of(
                  !glow && ao && supportsSmoothLighting(triangle)));
      emitter.renderLayer(sawbench ? null : ItemBlockRenderTypes.getChunkRenderType(material));
      Mesh.Vertex[] vertices = {triangle.a(), triangle.b(), triangle.c(), triangle.c()};
      for (int i = 0; i < 4; i++) {
        Mesh.Vertex v = vertices[i];
        double u = v.u(), vv = v.v();
        if ((triangle.material() & 1) != 0 && !sawbench) {
          switch (face) {
            case UP, DOWN -> {
              u = v.x();
              vv = v.z();
            }
            case NORTH -> {
              u = 1 - v.x();
              vv = 1 - v.y();
            }
            case SOUTH -> {
              u = v.x();
              vv = 1 - v.y();
            }
            case WEST -> {
              u = 1 - v.z();
              vv = 1 - v.y();
            }
            case EAST -> {
              u = v.z();
              vv = 1 - v.y();
            }
          }
        }
        emitter
            .pos(i, (float) v.x(), (float) v.y(), (float) v.z())
            .normal(i, (float) v.nx(), (float) v.ny(), (float) v.nz())
            .uv(i, (float) u, (float) vv)
            .color(i, color);
      }
      emitter.spriteBake(
          sprite, net.fabricmc.fabric.api.renderer.v1.mesh.MutableQuadView.BAKE_NORMALIZED);
      if (baked != null) baked.add(emitter.toBakedQuad(sprite));
      emitter.emit();
    }
  }
}

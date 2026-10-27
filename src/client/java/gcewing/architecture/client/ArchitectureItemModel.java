package gcewing.architecture.client;

import gcewing.architecture.*;
import gcewing.architecture.shape.*;
import java.util.*;
import net.fabricmc.fabric.api.renderer.v1.Renderer;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.block.model.*;
import net.minecraft.client.renderer.item.*;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.*;
import org.joml.Vector3f;

final class ArchitectureItemModel implements ItemModel {
  private record Key(ShapeData data, boolean glow) {}

  private record Surface(
      net.minecraft.world.level.block.state.BlockState material, List<BakedQuad> quads) {}

  private final String kind;
  // Bounded and tied to this reload's model instance; no stale sprites after resource reload.
  private final Map<Key, List<Surface>> cache =
      Collections.synchronizedMap(
          new LinkedHashMap<>(64, .75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<Key, List<Surface>> e) {
              return size() > 256;
            }
          });

  ArchitectureItemModel(String kind) {
    this.kind = kind;
  }

  @Override
  public void update(
      ItemStackRenderState state,
      ItemStack stack,
      ItemModelResolver resolver,
      ItemDisplayContext display,
      ClientLevel level,
      ItemOwner owner,
      int seed) {
    ShapeData data = ShapeData.fromStack(stack);
    boolean sawbench = kind.equals("sawbench");
    var properties = stack.get(net.minecraft.core.component.DataComponents.BLOCK_STATE);
    boolean glow =
        properties != null
            ? "true".equals(properties.properties().get("glowing"))
            : data.shape().glowing();
    List<Surface> surfaces =
        cache.computeIfAbsent(
            new Key(data, glow),
            key -> {
              Mesh mesh =
                  sawbench
                      ? Mesh.model("block/sawbench")
                      : kind.equals("cladding")
                          ? Mesh.model("shape/cladding")
                          : ShapeGeometry.mesh(
                              data, ArchitectureCraft.SHAPE.defaultBlockState(), null, null);
              if (!sawbench && !kind.equals("cladding"))
                mesh = mesh.transformed(Orientation.legacy(0, 3, 0));
              // A material boundary may be hidden. Group source triangles before
              // baking; emitted quad indices no longer correspond to mesh indices.
              Map<net.minecraft.world.level.block.state.BlockState, List<Mesh.Triangle>> grouped =
                  new LinkedHashMap<>();
              for (var triangle : mesh.triangles()) {
                var material =
                    triangle.material() >= 2 ? data.secondary().orElse(data.base()) : data.base();
                grouped.computeIfAbsent(material, ignored -> new ArrayList<>()).add(triangle);
              }
              List<Surface> bakedSurfaces = new ArrayList<>();
              for (var group : grouped.entrySet()) {
                List<BakedQuad> quads = new ArrayList<>();
                MeshRendering.emit(
                    new Mesh(List.copyOf(group.getValue()), List.of()),
                    data,
                    sawbench,
                    glow,
                    null,
                    null,
                    Renderer.get().mutableMesh().emitter(),
                    quads);
                if (!quads.isEmpty())
                  bakedSurfaces.add(new Surface(group.getKey(), List.copyOf(quads)));
              }
              return List.copyOf(bakedSurfaces);
            });
    state.appendModelIdentityElement(this);
    state.appendModelIdentityElement(data);
    state.appendModelIdentityElement(glow);
    ItemTransform transform =
        switch (display) {
          case GUI ->
              new ItemTransform(new Vector3f(30, 225, 0), new Vector3f(), new Vector3f(.625f));
          case GROUND ->
              new ItemTransform(new Vector3f(), new Vector3f(0, .1875f, 0), new Vector3f(.25f));
          case FIRST_PERSON_LEFT_HAND, FIRST_PERSON_RIGHT_HAND ->
              new ItemTransform(new Vector3f(0, 45, 0), new Vector3f(), new Vector3f(.4f));
          default ->
              new ItemTransform(
                  new Vector3f(75, 45, 0), new Vector3f(0, .15625f, 0), new Vector3f(.375f));
        };
    for (Surface surface : surfaces) {
      if (surface.quads().stream().anyMatch(q -> q.sprite().contents().isAnimated()))
        state.setAnimated();
      var layer = state.newLayer();
      layer.prepareQuadList().addAll(surface.quads());
      layer.setUsesBlockLight(true);
      layer.setParticleIcon(surface.quads().getFirst().sprite());
      layer.setRenderType(ItemBlockRenderTypes.getRenderType(surface.material()));
      layer.setExtents(() -> new Vector3f[] {new Vector3f(), new Vector3f(1, 1, 1)});
      layer.setTransform(transform);
    }
  }
}

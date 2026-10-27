package gcewing.architecture.shape;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class GeometryTest {
  @Test
  void originalCatalogIsCompleteAndStable() {
    assertEquals(106, Shape.values().length);
    assertEquals(106, Arrays.stream(Shape.values()).map(s -> s.id).distinct().count());
    assertEquals(Shape.RoofTile, Shape.byId(-1));
    assertEquals(Shape.DoubleRoofTile, Shape.byId(116));
    assertEquals(3, Shape.Stairs.materialUsed);
    assertEquals(4, Shape.Stairs.itemsProduced);
  }

  @ParameterizedTest
  @EnumSource(Shape.class)
  void allOriginalShapesHaveFiniteGeometryAndCollision(Shape shape) {
    Mesh mesh =
        shape.roof()
            ? RoofMesh.create(shape, (x, y, z) -> null)
            : shape.window()
                ? WindowMesh.create(shape, (x, y, z) -> false, true)
                : shape == Shape.DoubleRoofTile
                    ? DoubleRoofMesh.create()
                    : Mesh.model("shape/" + shape.model);
    assertFalse(mesh.triangles().isEmpty(), shape.name());
    assertFalse(mesh.boxes().isEmpty(), shape.name());
    for (Mesh.Triangle t : mesh.triangles())
      for (Mesh.Vertex v : List.of(t.a(), t.b(), t.c())) {
        assertTrue(
            Double.isFinite(v.x() + v.y() + v.z() + v.u() + v.v() + v.nx() + v.ny() + v.nz()),
            shape.name());
      }
    for (Mesh.Box b : mesh.boxes())
      assertTrue(b.x1() > b.x0() && b.y1() > b.y0() && b.z1() > b.z0(), shape.name());
  }

  @Test
  void orientationsAre24DistinctRigidRotations() {
    Set<String> positions = new HashSet<>();
    for (int side = 0; side < 6; side++)
      for (int turn = 0; turn < 4; turn++) {
        Orientation o = new Orientation(side, turn, 0);
        double[] v = o.vector(.1, .2, .3);
        assertEquals(.14, v[0] * v[0] + v[1] * v[1] + v[2] * v[2], 1e-10);
        positions.add(Arrays.toString(v));
      }
    assertEquals(24, positions.size());
  }

  @Test
  void roofNeighborsAffectSmartRidgeGeometry() {
    Mesh isolated = RoofMesh.create(Shape.RoofSmartRidge, (x, y, z) -> null);
    Mesh connected = RoofMesh.create(Shape.RoofSmartRidge, (x, y, z) -> Shape.RoofTile);
    assertNotEquals(isolated.triangles(), connected.triangles());
  }

  @Test
  void windowsJoinFramesAndOnlyAddGlassWhenGlazed() {
    Mesh framed = WindowMesh.create(Shape.WindowFrame, (x, y, z) -> false, false);
    Mesh joined = WindowMesh.create(Shape.WindowFrame, (x, y, z) -> x == 1, false);
    Mesh glazed = WindowMesh.create(Shape.WindowFrame, (x, y, z) -> false, true);
    assertNotEquals(framed.triangles(), joined.triangles());
    assertTrue(glazed.triangles().size() > framed.triangles().size());
  }

  @Test
  void sawbenchRetainsOriginalMesh() {
    assertTrue(Mesh.model("block/sawbench").triangles().size() > 12);
  }
}

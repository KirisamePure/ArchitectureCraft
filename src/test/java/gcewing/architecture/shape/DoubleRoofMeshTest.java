package gcewing.architecture.shape;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;
import org.junit.jupiter.api.Test;

class DoubleRoofMeshTest {
  @Test
  void bothMaterialHalvesAreClosedPrismsWithHalfBlockVolume() {
    Mesh mesh = DoubleRoofMesh.create();
    for (boolean secondary : new boolean[] {false, true}) {
      var triangles =
          mesh.triangles().stream().filter(t -> (t.material() >= 2) == secondary).toList();
      Map<String, Integer> edges = new HashMap<>();
      for (var t : triangles) {
        var points = List.of(t.a(), t.b(), t.c());
        for (int i = 0; i < 3; i++) {
          String a = point(points.get(i)), b = point(points.get((i + 1) % 3));
          edges.merge(a.compareTo(b) < 0 ? a + ":" + b : b + ":" + a, 1, Integer::sum);
        }
      }
      assertTrue(edges.values().stream().allMatch(n -> n == 2), "Every prism edge must be sealed");
      assertEquals(.5, volume(triangles), 1e-10);
      for (int side = 0; side < 6; side++)
        for (int turn = 0; turn < 4; turn++) {
          var rotated = new Mesh(triangles, List.of()).transformed(new Orientation(side, turn, 0));
          assertEquals(.5, volume(rotated.triangles()), 1e-10);
        }
    }
  }

  @Test
  void interfaceIsTheFullDiagonalWithOppositeOutwardNormals() {
    var caps =
        DoubleRoofMesh.create().triangles().stream().filter(DoubleRoofMesh::isInterface).toList();
    assertEquals(4, caps.size());
    for (var t : caps)
      for (var v : List.of(t.a(), t.b(), t.c())) {
        assertEquals(v.x(), v.y(), 1e-12);
        assertEquals(0, v.nx() + v.ny(), 1e-12);
        assertEquals(1, v.nx() * v.nx() + v.ny() * v.ny(), 1e-12);
        assertTrue(t.material() == 1 ? v.ny() > 0 : v.ny() < 0);
      }
  }

  private static String point(Mesh.Vertex v) {
    return v.x() + "," + v.y() + "," + v.z();
  }

  private static double volume(List<Mesh.Triangle> triangles) {
    double result = 0;
    for (var t : triangles) {
      var a = t.a();
      var b = t.b();
      var c = t.c();
      result +=
          a.x() * (b.y() * c.z() - b.z() * c.y())
              + a.y() * (b.z() * c.x() - b.x() * c.z())
              + a.z() * (b.x() * c.y() - b.y() * c.x());
    }
    return result / 6;
  }
}

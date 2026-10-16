package gcewing.architecture.shape;

import java.util.*;

/** Original ArchitectureCraft roof tessellation, independent of Minecraft rendering APIs. */
public final class RoofMesh {
  @FunctionalInterface
  public interface Neighbors {
    Shape get(int x, int y, int z);
  }

  private static final Shape[] ridgeShapes = {Shape.RoofRidge, Shape.RoofSmartRidge};
  private static final Shape[] ridgeOrSlopeShapes = {
    Shape.RoofRidge,
    Shape.RoofSmartRidge,
    Shape.RoofTile,
    Shape.RoofOuterCorner,
    Shape.RoofInnerCorner
  };
  private static final Shape[] valleyShapes = {Shape.RoofValley, Shape.RoofSmartValley};
  private static final Shape[] valleyOrSlopeShapes = {
    Shape.RoofValley, Shape.RoofSmartValley, Shape.RoofTile, Shape.RoofInnerCorner
  };
  private final Shape shape;
  private final Neighbors neighbors;
  private final List<Mesh.Triangle> triangles = new ArrayList<>();
  private final List<Mesh.Vertex> faceVertices = new ArrayList<>();
  private final boolean renderBase = true, renderSecondary = true;
  private int material = 1;
  private double nx, ny, nz;

  private RoofMesh(Shape shape, Neighbors neighbors) {
    this.shape = shape;
    this.neighbors = neighbors;
  }

  public static Mesh create(Shape shape, Neighbors neighbors) {
    RoofMesh builder = new RoofMesh(shape, neighbors);
    builder.render();
    return new Mesh(List.copyOf(builder.triangles), CollisionMask.boxes(shape));
  }

  public void render() {
    switch (shape) {
      case RoofTile, RoofTileSE:
        renderSlope();
        break;
      case SlopeTileA1, SlopeTileA1SE:
        renderSlopeA1();
        break;
      case SlopeTileA2, SlopeTileA2SE:
        renderSlopeA2();
        break;
      case SlopeTileB1, SlopeTileB1SE:
        renderSlopeB1();
        break;
      case SlopeTileB2, SlopeTileB2SE:
        renderSlopeB2();
        break;
      case SlopeTileB3, SlopeTileB3SE:
        renderSlopeB3();
        break;
      case SlopeTileC1, SlopeTileC1SE:
        renderSlopeC1();
        break;
      case SlopeTileC2, SlopeTileC2SE:
        renderSlopeC2();
        break;
      case SlopeTileC3, SlopeTileC3SE:
        renderSlopeC3();
        break;
      case SlopeTileC4, SlopeTileC4SE:
        renderSlopeC4();
        break;
      case RoofOuterCorner:
        renderOuterCorner();
        break;
      case RoofInnerCorner:
        renderInnerCorner();
        break;
      case RoofRidge:
        renderRidge();
        break;
      case RoofSmartRidge:
        renderSmartRidge();
        break;
      case RoofValley:
        renderValley();
        break;
      case RoofSmartValley:
        renderSmartValley();
        break;
    }
  }

  // -------------------------------------------------------------------------------------

  protected void renderSlope() {
    boolean valley = valleyAt(0, 0, 1);
    if (renderSecondary) {
      // Sloping face
      beginNegZSlope();
      if (valley) {
        beginTriangle();
        vertex(1, 1, 1, 0, 0);
        vertex(1, 0, 0, 0, 1);
        vertex(0.5, 0.5, 0.5, 0.5, 0.5);
        newTriangle();
        vertex(1, 0, 0, 0, 1);
        vertex(0, 0, 0, 1, 1);
        vertex(0.5, 0.5, 0.5, 0.5, 0.5);
        newTriangle();
        vertex(0, 0, 0, 1, 1);
        vertex(0, 1, 1, 1, 0);
        vertex(0.5, 0.5, 0.5, 0.5, 0.5);
        endFace();
        connectValleyBack();
      } else {
        beginQuad();
        vertex(1, 1, 1, 0, 0);
        vertex(1, 0, 0, 0, 1);
        vertex(0, 0, 0, 1, 1);
        vertex(0, 1, 1, 1, 0);
        endFace();
      }
    }
    // Other faces
    if (renderBase) {
      leftTriangle();
      rightTriangle();
      bottomQuad();
      if (!valley) backQuad();
    }
    if (renderSecondary) if (ridgeAt(0, 0, -1)) connectRidgeFront();
  }

  protected void renderSlopeA1() {
    if (renderSecondary) {
      renderVariableSlope(1.0, 0.5);
    }

    if (renderBase) {
      renderVariableFaceLeft(0, 0.5);
      renderVariableTriangleLeft(0.5, 0.5);

      renderVariableFaceRight(0, 0.5);
      renderVariableTriangleRight(0.5, 0.5);

      renderVariableFrontFace(0.5);

      bottomQuad();
      backQuad();
    }
  }

  protected void renderSlopeA2() {
    if (renderSecondary) {
      renderVariableSlope(0.5, 0);
    }

    if (renderBase) {
      renderVariableTriangleLeft(0, 0.5);

      renderVariableTriangleRight(0, 0.5);

      bottomQuad();
      renderVariableBackFace(0.5);
    }
  }

  protected void renderSlopeB1() {
    if (renderSecondary) {
      renderVariableSlope(1.0, 0.66666);
    }

    if (renderBase) {
      renderVariableFaceLeft(0, 0.66666);
      renderVariableTriangleLeft(0.66666, 0.33333);

      renderVariableFaceRight(0, 0.66666);
      renderVariableTriangleRight(0.66666, 0.33333);

      renderVariableFrontFace(0.66666);

      bottomQuad();
      backQuad();
    }
  }

  protected void renderSlopeB2() {
    if (renderSecondary) {
      renderVariableSlope(0.66666, 0.33333);
    }

    if (renderBase) {
      renderVariableFaceLeft(0, 0.33333);
      renderVariableTriangleLeft(0.33333, 0.33333);

      renderVariableFaceRight(0, 0.33333);
      renderVariableTriangleRight(0.33333, 0.33333);

      renderVariableFrontFace(0.33333);

      bottomQuad();
      renderVariableBackFace(0.66666);
    }
  }

  protected void renderSlopeB3() {
    if (renderSecondary) {
      renderVariableSlope(0.33333, 0);
    }

    if (renderBase) {
      renderVariableTriangleLeft(0, 0.33333);

      renderVariableTriangleRight(0, 0.33333);

      bottomQuad();
      renderVariableBackFace(0.33333);
    }
  }

  protected void renderSlopeC1() {
    if (renderSecondary) {
      renderVariableSlope(1, 0.75);
    }

    if (renderBase) {
      renderVariableFaceLeft(0, 0.75);
      renderVariableTriangleLeft(0.75, 0.25);

      renderVariableFaceRight(0, 0.75);
      renderVariableTriangleRight(0.75, 0.25);

      renderVariableFrontFace(0.75);

      bottomQuad();
      backQuad();
    }
  }

  protected void renderSlopeC2() {
    if (renderSecondary) {
      renderVariableSlope(0.75, 0.50);
    }

    if (renderBase) {
      renderVariableFaceLeft(0, 0.50);
      renderVariableTriangleLeft(0.50, 0.25);

      renderVariableFaceRight(0, 0.50);
      renderVariableTriangleRight(0.50, 0.25);

      renderVariableFrontFace(0.50);

      bottomQuad();
      renderVariableBackFace(0.75);
    }
  }

  protected void renderSlopeC3() {
    if (renderSecondary) {
      renderVariableSlope(0.50, 0.25);
    }

    if (renderBase) {
      renderVariableFaceLeft(0, 0.25);
      renderVariableTriangleLeft(0.25, 0.25);

      renderVariableFaceRight(0, 0.25);
      renderVariableTriangleRight(0.25, 0.25);

      renderVariableFrontFace(0.25);

      bottomQuad();
      renderVariableBackFace(0.50);
    }
  }

  protected void renderSlopeC4() {
    if (renderSecondary) {
      renderVariableSlope(0.25, 0);
    }

    if (renderBase) {
      renderVariableTriangleLeft(0, 0.25);

      renderVariableTriangleRight(0, 0.25);

      bottomQuad();
      renderVariableBackFace(0.25);
    }
  }

  // -------------------------------------------------------------------------------------

  protected void renderVariableSlope(double start, double end) {
    // Calculate normal
    double dy = end - start;
    double invLen = 1 / Math.sqrt(1 + dy * dy);
    material = 2;
    normal(0, invLen, invLen * dy);
    // Front slope
    beginQuad();
    vertex(1, start, 1, 0, 0);
    vertex(1, end, 0, 0, 1);
    vertex(0, end, 0, 1, 1);
    vertex(0, start, 1, 1, 0);
    endFace();
  }

  protected void renderVariableTriangleLeft(double offset, double height) {
    beginPosXFace();
    beginTriangle();
    vertex(1, offset + height, 1, 0, 0);
    vertex(1, offset, 1, 0, 1 - height);
    vertex(1, offset, 0, 1, 1 - height);
    endFace();
  }

  protected void renderVariableTriangleRight(double offset, double height) {
    beginNegXFace();
    beginTriangle();
    vertex(0, offset + height, 1, 1, 0);
    vertex(0, offset, 0, 0, 1 - height);
    vertex(0, offset, 1, 1, 1 - height);
    endFace();
  }

  protected void renderVariableFaceLeft(double offset, double height) {
    beginNegXFace();
    beginQuad();
    vertex(0, offset + height, 0, 0, 1 - height);
    vertex(0, offset, 0, 0, 1);
    vertex(0, offset, 1, 1, 1);
    vertex(0, offset + height, 1, 1, 1 - height);
    endFace();
  }

  protected void renderVariableFaceRight(double offset, double height) {
    beginPosXFace();
    beginQuad();
    vertex(1, offset + height, 1, 0, 1 - height);
    vertex(1, offset, 1, 0, 1);
    vertex(1, offset, 0, 1, 1);
    vertex(1, offset + height, 0, 1, 1 - height);
    endFace();
  }

  protected void renderVariableFrontFace(double height) {
    beginNegZFace();
    beginQuad();
    vertex(1, height, 0, 0, 1 - height);
    vertex(1, 0, 0, 0, 1);
    vertex(0, 0, 0, 1, 1);
    vertex(0, height, 0, 1, 1 - height);
    endFace();
  }

  protected void renderVariableBackFace(double height) {
    beginPosZFace();
    beginQuad();
    vertex(0, height, 1, 0, 1 - height);
    vertex(0, 0, 1, 0, 1);
    vertex(1, 0, 1, 1, 1);
    vertex(1, height, 1, 1, 1 - height);
    endFace();
  }

  // -------------------------------------------------------------------------------------
  protected void renderOuterCorner() {
    if (renderSecondary) {
      // Front slope
      beginNegZSlope();
      beginTriangle();
      vertex(0, 1, 1, 1, 0);
      vertex(1, 0, 0, 0, 1);
      vertex(0, 0, 0, 1, 1);
      endFace();
      // Left slope
      beginPosXSlope();
      beginTriangle();
      vertex(0, 1, 1, 0, 0);
      vertex(1, 0, 1, 0, 1);
      vertex(1, 0, 0, 1, 1);
      endFace();
    }
    if (renderBase) {
      // Back
      beginPosZFace();
      beginTriangle();
      vertex(0, 1, 1, 0, 0);
      vertex(0, 0, 1, 0, 1);
      vertex(1, 0, 1, 1, 1);
      endFace();
      // Other faces
      rightTriangle();
      bottomQuad();
    }
    if (renderSecondary) {
      if (ridgeAt(0, 0, -1)) connectRidgeFront();
      if (ridgeAt(1, 0, 0)) connectRidgeLeft();
    }
  }

  protected void renderInnerCorner() {
    if (renderSecondary) {
      // Left slope
      beginPosXSlope();
      beginTriangle();
      vertex(0, 1, 0, 1, 0);
      vertex(0.5, 0.5, 0.5, 0.5, 0.5);
      vertex(1, 0, 0, 1, 1);
      endFace();
      // Front slope
      beginNegZSlope();
      beginTriangle();
      vertex(1, 1, 1, 0, 0);
      vertex(1, 0, 0, 0, 1);
      vertex(0.5, 0.5, 0.5, 0.5, 0.5);
      endFace();
    }
    if (renderBase) {
      // Front triangle
      beginNegZFace();
      beginTriangle();
      vertex(0, 1, 0, 1, 0);
      vertex(1, 0, 0, 0, 1);
      vertex(0, 0, 0, 1, 1);
      endFace();
      // Other faces
      leftTriangle();
      bottomQuad();
    }
    if (valleyAt(0, 0, 1)) connectValleyBack();
    else terminateValleyBack();
    if (valleyAt(-1, 0, 0)) connectValleyRight();
    else terminateValleyRight();
  }

  protected void renderRidge() {
    if (renderSecondary) {
      // Front slope
      beginNegZSlope();
      beginQuad();
      vertex(1, 0.5, 0.5, 0, 0.5);
      vertex(1, 0, 0, 0, 1);
      vertex(0, 0, 0, 1, 1);
      vertex(0, 0.5, 0.5, 1, 0.5);
      endFace();
      // Other slops
      ridgeBackSlope();
      ridgeFront(false);
      ridgeBack(false);
    }
    if (renderBase) {
      ridgeLeftFace();
      ridgeRightFace();
      bottomQuad();
    }
  }

  protected void renderSmartRidge() {
    if (renderSecondary) {
      ridgeLeft();
      ridgeRight();
      ridgeBack(true);
      ridgeFront(true);
    }
    if (renderBase) bottomQuad();
  }

  protected void renderValley() {
    connectValleyLeft();
    connectValleyRight();
    smartValleyFront();
    smartValleyBack();
    if (renderBase) bottomQuad();
  }

  protected void renderSmartValley() {
    smartValleyLeft();
    smartValleyRight();
    smartValleyFront();
    smartValleyBack();
    if (renderBase) bottomQuad();
  }

  // -------------------------------------------------------------------------------------

  protected void smartValleyLeft() {
    if (valleyOrSlopeAt(1, 0, 0)) connectValleyLeft();
    else terminateValleyLeft();
  }

  protected void terminateValleyLeft() {
    if (renderSecondary) {
      beginNegXSlope();
      beginTriangle();
      vertex(1, 1, 0, 0, 0);
      vertex(0.5, 0.5, 0.5, 0.5, 0.5);
      vertex(1, 1, 1, 1, 0);
      endFace();
    }
    if (renderBase) leftQuad();
  }

  protected void smartValleyRight() {
    if (valleyOrSlopeAt(-1, 0, 0)) connectValleyRight();
    else terminateValleyRight();
  }

  protected void terminateValleyRight() {
    if (renderSecondary) {
      beginPosXSlope();
      beginTriangle();
      vertex(0, 1, 1, 0, 0);
      vertex(0.5, 0.5, 0.5, 0.5, 0.5);
      vertex(0, 1, 0, 1, 0);
      endFace();
    }
    if (renderBase) rightQuad();
  }

  protected void smartValleyFront() {
    if (valleyOrSlopeAt(0, 0, -1)) connectValleyFront();
    else terminateValleyFront();
  }

  protected void terminateValleyFront() {
    if (renderSecondary) {
      beginPosZSlope();
      beginTriangle();
      vertex(0, 1, 0, 0, 0);
      vertex(0.5, 0.5, 0.5, 0.5, 0.5);
      vertex(1, 1, 0, 1, 0);
      endFace();
    }
    if (renderBase) frontQuad();
  }

  protected void smartValleyBack() {
    if (valleyOrSlopeAt(0, 0, 1)) connectValleyBack();
    else terminateValleyBack();
  }

  protected void terminateValleyBack() {
    if (renderSecondary) {
      beginNegZSlope();
      beginTriangle();
      vertex(1, 1, 1, 0, 0);
      vertex(0.5, 0.5, 0.5, 0.5, 0.5);
      vertex(0, 1, 1, 1, 0);
      endFace();
    }
    if (renderBase) backQuad();
  }

  // -------------------------------------------------------------------------------------

  protected void leftQuad() {
    beginPosXFace();
    beginQuad();
    vertex(1, 1, 1, 0, 0);
    vertex(1, 0, 1, 0, 1);
    vertex(1, 0, 0, 1, 1);
    vertex(1, 1, 0, 1, 0);
    endFace();
  }

  protected void rightQuad() {
    beginNegXFace();
    beginQuad();
    vertex(0, 1, 0, 0, 0);
    vertex(0, 0, 0, 0, 1);
    vertex(0, 0, 1, 1, 1);
    vertex(0, 1, 1, 1, 0);
    endFace();
  }

  protected void frontQuad() {
    beginNegZFace();
    beginQuad();
    vertex(1, 1, 0, 0, 0);
    vertex(1, 0, 0, 0, 1);
    vertex(0, 0, 0, 1, 1);
    vertex(0, 1, 0, 1, 0);
    endFace();
  }

  protected void backQuad() {
    beginPosZFace();
    beginQuad();
    vertex(0, 1, 1, 0, 0);
    vertex(0, 0, 1, 0, 1);
    vertex(1, 0, 1, 1, 1);
    vertex(1, 1, 1, 1, 0);
    endFace();
  }

  protected void bottomQuad() {
    beginBottomFace();
    beginQuad();
    vertex(0, 0, 1, 0, 0);
    vertex(0, 0, 0, 0, 1);
    vertex(1, 0, 0, 1, 1);
    vertex(1, 0, 1, 1, 0);
    endFace();
  }

  protected void leftTriangle() {
    beginPosXFace();
    beginTriangle();
    vertex(1, 1, 1, 0, 0);
    vertex(1, 0, 1, 0, 1);
    vertex(1, 0, 0, 1, 1);
    // vertex(1, 1, 1, 0, 0);
    endFace();
  }

  protected void rightTriangle() {
    beginNegXFace();
    beginTriangle();
    vertex(0, 1, 1, 1, 0);
    vertex(0, 0, 0, 0, 1);
    vertex(0, 0, 1, 1, 1);
    endFace();
  }

  protected void ridgeLeftFace() {
    beginPosXFace();
    beginTriangle();
    vertex(1, 0.5, 0.5, 0.5, 0.5);
    vertex(1, 0, 1, 0, 1);
    vertex(1, 0, 0, 1, 1);
    endFace();
  }

  protected void ridgeRightFace() {
    beginNegXFace();
    beginTriangle();
    vertex(0, 0.5, 0.5, 0.5, 0.5);
    vertex(0, 0, 0, 0, 1);
    vertex(0, 0, 1, 1, 1);
    endFace();
  }

  protected void ridgeBackFace() {
    beginPosZFace();
    beginTriangle();
    vertex(0.5, 0.5, 1, 0.5, 0.5);
    vertex(0, 0, 1, 0, 1);
    vertex(1, 0, 1, 1, 1);
    endFace();
  }

  protected void ridgeFrontFace() {
    beginNegZFace();
    beginTriangle();
    vertex(0.5, 0.5, 0, 0.5, 0.5);
    vertex(1, 0, 0, 0, 1);
    vertex(0, 0, 0, 1, 1);
    endFace();
  }

  protected void ridgeFrontSlope() {
    beginNegZSlope();
    beginTriangle();
    vertex(0.5, 0.5, 0.5, 0.5, 0.5);
    vertex(1, 0, 0, 0, 1);
    vertex(0, 0, 0, 1, 1);
    endFace();
  }

  protected void ridgeBackSlope() {
    beginPosZSlope();
    beginQuad();
    vertex(0, 0.5, 0.5, 0, 0.5);
    vertex(0, 0, 1, 0, 1);
    vertex(1, 0, 1, 1, 1);
    vertex(1, 0.5, 0.5, 1, 0.5);
    endFace();
  }

  protected void ridgeLeft() {
    if (ridgeOrSlopeAt(1, 0, 0)) connectRidgeLeft();
    else {
      beginPosXSlope();
      beginTriangle();
      vertex(0.5, 0.5, 0.5, 0.5, 0.5);
      vertex(1, 0, 1, 0, 1);
      vertex(1, 0, 0, 1, 1);
      endFace();
    }
  }

  protected void connectRidgeLeft() {
    beginNegZSlope();
    beginTriangle();
    vertex(0.5, 0.5, 0.5, 0.5, 0.5);
    vertex(1, 0.5, 0.5, 0, 0.5);
    vertex(1, 0, 0, 0, 1);
    endFace();
    beginPosZSlope();
    beginTriangle();
    vertex(0.5, 0.5, 0.5, 0.5, 0.5);
    vertex(1, 0, 1, 1, 1);
    vertex(1, 0.5, 0.5, 1, 0.5);
    endFace();
  }

  protected void ridgeRight() {
    if (ridgeOrSlopeAt(-1, 0, 0)) connectRidgeRight();
    else {
      beginNegXSlope();
      beginTriangle();
      vertex(0.5, 0.5, 0.5, 0.5, 0.5);
      vertex(0, 0, 0, 0, 1);
      vertex(0, 0, 1, 1, 1);
      endFace();
    }
  }

  protected void connectRidgeRight() {
    beginNegZSlope();
    beginTriangle();
    vertex(0.5, 0.5, 0.5, 0.5, 0.5);
    vertex(0, 0, 0, 1, 1);
    vertex(0, 0.5, 0.5, 1, 0.5);
    endFace();
    beginPosZSlope();
    beginTriangle();
    vertex(0.5, 0.5, 0.5, 0.5, 0.5);
    vertex(0, 0.5, 0.5, 0, 0.5);
    vertex(0, 0, 1, 0, 1);
    endFace();
  }

  protected void ridgeFront(boolean fill) {
    if (ridgeOrSlopeAt(0, 0, -1)) connectRidgeFront();
    else if (fill) {
      beginNegZSlope();
      beginTriangle();
      vertex(0.5, 0.5, 0.5, 0.5, 0.5);
      vertex(1, 0, 0, 0, 1);
      vertex(0, 0, 0, 1, 1);
      endFace();
    }
  }

  protected void connectRidgeFront() {
    beginPosXSlope();
    beginTriangle();
    vertex(0.5, 0.5, 0.5, 0.5, 0.5);
    vertex(1, 0, 0, 1, 1);
    vertex(0.5, 0.5, 0, 1, 0.5);
    endFace();
    beginNegXSlope();
    beginTriangle();
    vertex(0.5, 0.5, 0.5, 0.5, 0.5);
    vertex(0.5, 0.5, 0, 0, 0.5);
    vertex(0, 0, 0, 0, 1);
    endFace();
  }

  protected void ridgeBack(boolean fill) {
    if (ridgeOrSlopeAt(0, 0, 1)) connectRidgeBack();
    else if (fill) {
      beginPosZSlope();
      beginTriangle();
      vertex(0.5, 0.5, 0.5, 0.5, 0.5);
      vertex(0, 0, 1, 0, 1);
      vertex(1, 0, 1, 1, 1);
      endFace();
    }
  }

  protected void connectRidgeBack() {
    beginPosXSlope();
    beginTriangle();
    vertex(0.5, 0.5, 0.5, 0.5, 0.5);
    vertex(0.5, 0.5, 1, 0, 0.5);
    vertex(1, 0, 1, 0, 1);
    endFace();
    beginNegXSlope();
    beginTriangle();
    vertex(0.5, 0.5, 0.5, 0.5, 0.5);
    vertex(0, 0, 1, 1, 1);
    vertex(0.5, 0.5, 1, 1, 0.5);
    endFace();
  }

  protected void connectValleyLeft() {
    beginPosZSlope();
    beginTriangle();
    vertex(0.5, 0.5, 0.5, 0.5, 0.5);
    vertex(1, 0.5, 0.5, 1, 0.5);
    vertex(1, 1, 0, 1, 0);
    endFace();
    beginNegZSlope();
    beginTriangle();
    vertex(0.5, 0.5, 0.5, 0.5, 0.5);
    vertex(1, 1, 1, 0, 0);
    vertex(1, 0.5, 0.5, 0, 0.5);
    endFace();
    valleyEndLeft();
  }

  protected void connectValleyRight() {
    beginPosZSlope();
    beginTriangle();
    vertex(0.5, 0.5, 0.5, 0.5, 0.5);
    vertex(0, 1, 0, 0, 0);
    vertex(0, 0.5, 0.5, 0, 0.5);
    endFace();
    beginNegZSlope();
    beginTriangle();
    vertex(0.5, 0.5, 0.5, 0.5, 0.5);
    vertex(0, 0.5, 0.5, 1, 0.5);
    vertex(0, 1, 1, 1, 0);
    endFace();
    valleyEndRight();
  }

  protected void connectValleyFront() {
    beginPosXSlope();
    beginTriangle();
    vertex(0, 1, 0, 1, 0);
    vertex(0.5, 0.5, 0.5, 0.5, 0.5);
    vertex(0.5, 0.5, 0, 1, 0.5);
    endFace();
    beginNegXSlope();
    beginTriangle();
    vertex(1, 1, 0, 0, 0);
    vertex(0.5, 0.5, 0, 0, 0.5);
    vertex(0.5, 0.5, 0.5, 0.5, 0.5);
    endFace();
    valleyEndFront();
  }

  protected void connectValleyBack() {
    beginPosXSlope();
    beginTriangle();
    vertex(0, 1, 1, 0, 0);
    vertex(0.5, 0.5, 1, 0, 0.5);
    vertex(0.5, 0.5, 0.5, 0.5, 0.5);
    endFace();
    beginNegXSlope();
    beginTriangle();
    vertex(1, 1, 1, 1, 0);
    vertex(0.5, 0.5, 0.5, 0.5, 0.5);
    vertex(0.5, 0.5, 1, 1, 0.5);
    endFace();
    valleyEndBack();
  }

  protected void valleyEndLeft() {
    beginPosXFace();
    beginTriangle();
    vertex(1, 1, 1, 0, 0);
    vertex(1, 0, 1, 0, 1);
    vertex(1, 0.5, 0.5, 0.5, 0.5);
    newTriangle();
    vertex(1, 0, 1, 0, 1);
    vertex(1, 0, 0, 1, 1);
    vertex(1, 0.5, 0.5, 0.5, 0.5);
    newTriangle();
    vertex(1, 0, 0, 1, 1);
    vertex(1, 1, 0, 1, 0);
    vertex(1, 0.5, 0.5, 0.5, 0.5);
    endFace();
  }

  protected void valleyEndRight() {
    beginNegXFace();
    beginTriangle();
    vertex(0, 0, 1, 1, 1);
    vertex(0, 1, 1, 1, 0);
    vertex(0, 0.5, 0.5, 0.5, 0.5);
    newTriangle();
    vertex(0, 0, 0, 0, 1);
    vertex(0, 0, 1, 1, 1);
    vertex(0, 0.5, 0.5, 0.5, 0.5);
    newTriangle();
    vertex(0, 1, 0, 0, 0);
    vertex(0, 0, 0, 0, 1);
    vertex(0, 0.5, 0.5, 0.5, 0.5);
    endFace();
  }

  protected void valleyEndFront() {
    beginNegZFace();
    beginTriangle();
    vertex(1, 1, 0, 0, 0);
    vertex(1, 0, 0, 0, 1);
    vertex(0.5, 0.5, 0, 0.5, 0.5);
    newTriangle();
    vertex(1, 0, 0, 0, 1);
    vertex(1, 0, 0, 1, 1);
    vertex(0.5, 0.5, 0, 0.5, 0.5);
    newTriangle();
    vertex(0, 0, 0, 1, 1);
    vertex(0, 1, 0, 1, 0);
    vertex(0.5, 0.5, 0, 0.5, 0.5);
    endFace();
  }

  protected void valleyEndBack() {
    beginPosZFace();
    beginTriangle();
    vertex(0, 1, 1, 0, 0);
    vertex(0, 0, 1, 0, 1);
    vertex(0.5, 0.5, 1, 0.5, 0.5);
    newTriangle();
    vertex(0, 0, 1, 0, 1);
    vertex(1, 0, 1, 1, 1);
    vertex(0.5, 0.5, 1, 0.5, 0.5);
    newTriangle();
    vertex(1, 0, 1, 1, 1);
    vertex(1, 1, 1, 1, 0);
    vertex(0.5, 0.5, 1, 0.5, 0.5);
    endFace();
  }

  // -------------------------------------------------------------------------------------

  protected boolean ridgeAt(int dx, int dy, int dz) {
    return hasNeighbour(dx, dy, dz, ridgeShapes);
  }

  protected boolean ridgeOrSlopeAt(int dx, int dy, int dz) {
    return hasNeighbour(dx, dy, dz, ridgeOrSlopeShapes);
  }

  protected boolean valleyAt(int dx, int dy, int dz) {
    return hasNeighbour(dx, dy, dz, valleyShapes);
  }

  protected boolean valleyOrSlopeAt(int dx, int dy, int dz) {
    return hasNeighbour(dx, dy, dz, valleyOrSlopeShapes);
  }

  private boolean hasNeighbour(int x, int y, int z, Shape[] shapes) {
    Shape other = neighbors.get(x, y, z);
    return other != null && Arrays.asList(shapes).contains(other);
  }

  private void normal(double x, double y, double z) {
    double l = Math.sqrt(x * x + y * y + z * z);
    nx = x / l;
    ny = y / l;
    nz = z / l;
  }

  private void beginTopFace() {
    material = 1;
    normal(0, 1, 0);
  }

  private void beginBottomFace() {
    material = 1;
    normal(0, -1, 0);
  }

  private void beginPosXFace() {
    material = 1;
    normal(1, 0, 0);
  }

  private void beginNegXFace() {
    material = 1;
    normal(-1, 0, 0);
  }

  private void beginPosZFace() {
    material = 1;
    normal(0, 0, 1);
  }

  private void beginNegZFace() {
    material = 1;
    normal(0, 0, -1);
  }

  private void beginPosXSlope() {
    material = 2;
    normal(1, 1, 0);
  }

  private void beginNegXSlope() {
    material = 2;
    normal(-1, 1, 0);
  }

  private void beginPosZSlope() {
    material = 2;
    normal(0, 1, 1);
  }

  private void beginNegZSlope() {
    material = 2;
    normal(0, 1, -1);
  }

  private void beginTriangle() {
    faceVertices.clear();
  }

  private void beginQuad() {
    faceVertices.clear();
  }

  private void newTriangle() {
    endFace();
    beginTriangle();
  }

  private void newQuad() {
    endFace();
    beginQuad();
  }

  private void vertex(double x, double y, double z, double u, double v) {
    faceVertices.add(new Mesh.Vertex(x, y, z, u, v, nx, ny, nz));
  }

  private void endFace() {
    for (int i = 1; i + 1 < faceVertices.size(); i++)
      triangles.add(
          new Mesh.Triangle(
              material, faceVertices.getFirst(), faceVertices.get(i), faceVertices.get(i + 1)));
    faceVertices.clear();
  }
}

# ArchitectureCraft behavior parity

The reference for this work is the original Forge source at commit `172a307fdf356ad7af4c1826b14c235455694592`, not the simplified first Fabric port. The original sawbench screenshot and original GUI textures are the visual reference. This checklist tracks restoration work; compilation and a nonempty mesh are not evidence of feature parity.

- [x] Restore eight original categories, original ordering, shared entries, and per-category selection memory.
- [x] Restore original sawbench texture, shape atlas, selection outline, labels, and slot coordinates.
- [x] Replace the invented Cut button with result previews and material charging on extraction.
- [x] Retain paid partial batches without charging twice; avoid charging on a failed shift transfer.
- [x] Restore original cladding yield and remove the invented extra cladding category/item conversion ratio.
- [x] Verify mouse, shift-click, partial extraction, inventory changes, persistence and multiple open menus against reference behavior.
- [x] Restore symmetry/hit-based placement, neighboring profile alignment, stair-mounted banisters and window placement.
- [x] Restore original mounting rotations, placement offsets and hammer behavior.
- [x] Restore chisel edge zones, secondary removal and fragile-block harvesting.
- [x] Restore original shape collision masks, supporting faces and window selection/collision differences.
- [x] Restore window frame compatibility, diagonal caps and glass boundary behavior.
- [x] Restore material-based mining speed, separate secondary drops, pick-block semantics and item tooltips.
- [x] Restore placement preview and contextual window frame outlines.
- [x] Provide native data-pack material acceptance and emissive-material overrides.
- [x] Run reference unit checks, server gameplay tests and actual client UI/preview tests; inspect reference UI screenshots.

Optional GregTech, Angelica, Hodgepodge, Postea, Waila and modpack integrations remain excluded, as requested. The surrounding item-list/cheat controls in the supplied screenshot belong to another mod; they are not part of ArchitectureCraft's sawbench. This port does not migrate old Minecraft world formats.

## Validation evidence

Validated with `./gradlew build runClientGameTest` on Java 21, Minecraft 1.21.10 and software OpenGL: 223 JUnit cases, 28 mod server GameTests plus the Fabric baseline, and the client suite passed.

- `ReferenceParityTest`: metadata for all 106 entries, exact page ordering, all 24 reference mounting matrices, collision masks/support, window compatibility and diagonal/glass flags.
- `ArchitectureGameTests`: 28 mod tests covering unpaid and paid output, vanilla inventory extraction paths, selection memory, persistence, multiple menus, automation, placement, collisions/support, mining, separate drops, tools, glazing, glow and native material tags. The server also runs Fabric's baseline test.
- `ArchitectureClientTest`: loads all catalog item models, checks material UVs and emissive tags, renders the catalog, clicks category/shape buttons and extracts output, then compares a networked placement with its rendered preview. Checks contextual window frame outlines and captures the restored UI and placement screenshots.

These checks exercise representative gameplay paths and reference data; they do not exhaust every material, world arrangement or resource pack. The original UI screenshot was compared with the actual 1.21.10 client capture. The port uses modern Minecraft's rendering, lighting, inventory and data-pack APIs, so pixel-identical world lighting is not expected.

Intentional changes: unpaid sawbench previews cannot be duplicated by breaking the bench; paid remainders are returned. Material acceptance and emissive overrides use native block tags in place of legacy configuration entries. Optional mod integrations and conversion of old world formats are outside this port's scope.

The material-acceptance regression also registers a transparent full cube in the test mod with no glass superclass and no acceptance tag. It verifies actual sawbench output, source material and extraction costs, alongside vanilla transparent cubes/slabs and rejection of containers and non-full shapes. Production acceptance uses the input item’s block-state component and full collision shape; opacity and vanilla glass classes are not requirements.

Transparent-surface rendering follows the source model's directional shading and ambient-occlusion flags and the source block's shade brightness. Boundary triangles fully covered by compatible neighboring rendered faces are removed according to the source block's `skipRendering` rule. Collision bounds are not used to infer face coverage. Client regressions cover matching glass slabs, a vanilla glass neighbor, a frame with holes, a different opaque material, and a custom unshaded/no-AO source model. Partially covered triangles and intersecting surfaces away from cell boundaries are retained; this is conservative shared-face removal, not a general constructive-solid-geometry union.

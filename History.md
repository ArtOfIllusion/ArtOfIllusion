Art of Illusion Version History

v3.0.3, Dec. 12, 2016

- Fix bug related to shaded mesh objects having wrong material properties calculated
- Fix twitchy smoothed Position/Rotation tracks
- Upgrade Jogl Libraries
- Move View controls to their own menu
- Performance enhancement for better multi-thread raytracing on many-core processers

v3.0.2, May 17, 2015

- Reduced memory use for raytracer.
- Bug fixes.

v3.0.1, Jan. 15, 2015

- Bug fixes and minor UI improvements.

v3.0, Sept. 23, 2013

- Animation can be previewed directly in the main window.
- Added support for Groovy scripting language.
- Refactored raytracer to be usable from scripts and plugins.
- Replaced Text script with a much better Text modelling tool.
- Changed the behavior of the Rotate View tool to be more intuitive.
- Lots of improvements to implicit objects.
- Render settings are saved as part of the scene.
- Added "Detach Points from Bone" command.
- Bug fixes and UI improvements.

v2.9.2, Oct. 21, 2012

- Optimizations to reduce memory use.
- Bug fixes.

v2.9.1, Feb. 19, 2012

- Textures and Materials window lets you reorder textures and materials.
- Color chooser window now displays gradient bars.
- Java Media Framework is no longer required to save Quicktime movies.
- A few new textures and materials in the library.
- A few bug fixes and minor UI improvements.

v2.9, Nov. 5, 2011

- Major UI changes to the windows for managing textures and materials,
assigning textures and materials to objects, and editing procedures.
- A library of textures and materials is now included with the application.
- The raytracer supports progressive rendering.
- Added "rendered" display mode.
- Can use SVG files as image maps.
- Added options for how many rays to use to sample gloss/translucency
and soft shadows.
- Some bug fixes.

v2.8.1, Jan. 3, 2010

- Lots of bug fixes.

v2.8, Dec. 12, 2009

- Added compound move/scale/rotate tool in the main scene window.
- Camera filters can be edited and reapplied after an image is rendered.
- Added depth of field filter.
- Noise reduction filter is now a standard camera filter.
- Cameras can be set to render images with a parallel projection.
- Lots of minor enhancements and bug fixes.

v2.7.2, April 19, 2009

- A few bug fixes.

v2.7.1, March 8, 2009

- Speed optimizations to boolean modeling.
- Speed optimizations to interactive rendering.
- OBJ export produces smaller files.
- Lots of bug fixes.

v2.7, Jan. 26, 2009

- Added procedural lights.
- Directional lights can cast soft shadows.
- The raytracer has an option to use less memory.
- Added commands to lock objects.
- A viewport can be bound to a light source.
- Added "Extract Selected Faces" command to the triangle mesh editor.
- Lots of improvements to theme handling architecture.
- Improved OBJ file import.
- There are now translations for Afrikaans, Chinese, Dutch, Finnish, and Vietnamese.
- Various user interface improvements.
- Lots of bug fixes.

v2.6.1, Aug. 3, 2008

- A few bug fixes.

v2.6, April 30, 2008

- The mesh editor can color the surface by parameters or bone influence.
- "Link to External Object" allows you to include children of the selected object.
- Pose tracks can be added to curve and tube objects.
- The procedure editor lets you specify what time the preview is displayed for.
- Animation tracks are automatically reapplied as the scene is edited.
- The color chooser lets you select what range is used for component values.
- Plugins can add arbitrary metadata to a scene.
- Created API for plugins to extend the raytracer by defining new object types.
- Upgraded to Buoy 1.9.
- Lots of bug fixes.

v2.5.1, Oct. 21, 2007

- Lots of bug fixes.

v2.5, Aug. 18, 2007

- Parallelized various operations, including the Raster renderer and additional
parts of the raytracer.
- Major overhaul of plugin handling with lots of new features (plugins can
define new plugin categories, import or extend other plugins, export methods,
define resources, provide new translations, customize object editing windows, etc.)
- Light sources can be marked as shadowless.
- Implemented reference images.
- Implemented "scale to object" option for textures and materials.
- Added support for UI themes.
- Upgraded to Buoy 1.8.
- Upgraded to JOGL JSR-231 1.1.0.
- Various bug fixes and UI improvements.

v2.4.1, Feb. 27, 2007

- Speed optimizations to the raytracer.
- The outline camera filter produces slightly smoother lines.
- Lots of bug fixes.

v2.4, Dec. 9, 2006

- Mesh editors allow the selection to be changed with any tool, not
just the "Select and Move" tool.
- Added a new compound move/scale/rotate tool to the mesh editors
(based on an implementation by Francois Guillet).
- The raytracer can render implicit surfaces.
- The raster renderer can render transparent textures and materials.
- The raster renderer can generate high dynamic range images.
- The Color Selector window now maintains a list of recent colors.
- The Image procedural module can now output HSV and HLS color components
as well as RGB.
- Replaced the Perlin Noise function used for procedures with Simplex Noise
(based on an implementation by Stefan Gustavson).
- Lots of internal changes to provide better support for the PolyMesh plugin.
- Upgraded to JOGL JSR-231 1.0.0.
- Lots of bug fixes.

v2.3.1, June 24, 2006

- Lots of bug fixes.

v2.3, May 20, 2006

- Created an architecture for configurable keyboard shortcuts.
- Implemented dockable panels in the main window.
- Added the Object Properties panel to the main window.
- Plugins can now add custom dockable panels to the main window.
- The editing window for boolean objects now provides most of the same
features as other editing windows (split view, multiple undo, grids, etc.).
- Transparent display mode is now available in the main window.
- The mouse scroll wheel can be used to zoom a view.
- Added a right-click context menu to views in the main window.
- There is an option to use a dark gray background instead of white in the views.
- Lots of improvements to the Scripts and Plugins Manager, including user
interface changes, filtering of scripts and plugins, and support for dependencies
between plugins.
- The raytracer now supports having the camera or a photon source inside an
object with a material.
- Upgraded to Buoy 1.7.
- Lots of bug fixes.

v2.2.1, Feb. 11, 2006

- Upgraded to Buoy 1.6.
- Lots of bug fixes.

v2.2, Nov. 6, 2005

- The Raytracer is now multithreaded, allowing it to take advantage of
multiple processors.
- Scattering materials can now be rendered with photon maps, allowing accurate
simulation of multiple scattering.
- Added Ambient Occlusion as a new global illumination method.
- When rendering global illumination, the raytracer can use more than one ray
to sample the environment.
- Various changes to the model for defining materials to make it more intuitive:
renamed "material color" to "emissive color", added "transparency" parameter,
changed various default values.
- Added "Project Control Mesh Onto Surface" feature to the triangle mesh editor.
- Texture previews can now be zoomed and panned using the standard keyboard
shortcuts.
- Added Exposure Correction camera filter.
- The Camera Filter window now allows you to configure the preview rendering options.
- The Camera Filter window now updates the preview automatically when any filter
changes.
- Dialogs for loading image files now show a preview of the image.
- Tolerant selection mode in the triangle mesh editor now accepts edges/faces
that overlap the selection region in any way, even if no vertex is inside
(implemented by Csaba Nagy).
- Many options in the mesh editor are now persistent between sessions.
- Upgraded to Buoy 1.5.
- Upgraded to Beanshell 2.0b4.
- Upgraded to JOGL 1.1.1.
- Lots of bug fixes.

v2.1, June 14, 2005

- OpenGL is now used for interactive rendering, allowing hardware acceleration
(requires Jogl to be installed).
- The Scale Object, Create Sphere, Create Cube, and Create Cylinder tools show
the objects being resized in real time, rather than just bounding boxes.
- The Preview Animation command can now use any of the standard display modes,
not just wireframe.
- When saving scenes, it now does a safe save (implemented by Nik Trevallyn-Jones).
- There is an option to keep a backup file when saving.
- When displaying a grid in parallel mode, the grid lines are numbered.
- The Lathe, Extrude, and Skin tools place the newly created object in the same
location as the objects it was created from.
- When saving a scene or image, it asks for confirmation before overwriting an
existing file.
- The Scripts and Plugins Manager is now much faster when scanning for new scripts
or plugins to download (written by Francois Guillet).
- Some minor optimizations to rendering speed.
- Upgraded to Buoy 1.4.
- Lots of bug fixes.

v2.0, March 7, 2005

- Added noise reduction filter to the raytracer.
- Mesh editing windows now offer a split view.
- Changing the selection is now undoable.
- Added new selection commands in mesh editing windows: invert selection, select
boundary of current selection, select edge loop, and select edge strip.
- When you select a face in the triangle mesh editor, it now hilights the entire
face, not just the outline.
- Saved scenes now use PNG compression for the image maps, which leads to smaller
scene files.
- Can use spacebar to toggle between tools.
- Added popup menu to texture previews to view the object from different sides,
and to select what object to show the texture on (sphere, cube, cylinder, or cone).
- Added HLS module in the procedure editor.
- When saving a movie as separate images, you can now number the frames starting
at any number.
- The object list now uses standard modifier keys (shift-click to select a range,
control-click or command-click to toggle selection).
- There is now a Swedish translation (written by Rasmus Anthin).
- Upgraded to Buoy 1.3.
- Bug fixes.

v1.9, Oct. 27, 2004

- Lots of optimizations to improve speed and memory use, especially in the raytracer.
- Added tooltips to all the toolbar icons.
- The template image can now be set separately for each view.
- When the grid is enabled on a perspective view, it now shows a ground plane.
- Added Show Coordinate Axes command.
- Added Open Recent command.
- The Create Cylinder tool now lets you specify the ratio of
top/bottom radius.
- Multiple levels of undo are now available.
- The mouse scroll wheel can be used to scroll the score.
- All of the deformation tools in the mesh editors now deform the surface in
real time as it is dragged.
- Rendered images can be saved in PNG format.
- The color model selected in the Color Chooser window is now remembered between
sessions.
- With the Move Object, Rotate Object, and Move Points tools, you can use
Alt+arrow keys to nudge objects by ten pixels at a time.
- Added Comment modules to the procedure editor.
- There is now a Portuguese translation (written by Marcos Sobrinho)
- Upgraded to Buoy 1.2.
- Lots of bug fixes.

v1.8, July 26, 2004

- The user interface has been completely rewritten to use Buoy
instead of AWT.
- Nearly all text in the program is now localized.
- Added Skeleton Shape tracks.
- Added Link to External Object command.
- Added the Script and Plugin Manager (written by Francois Guillet).
- The script editing window now provides smart indentation.
- Added a "Mask" output to Image modules in the procedure editor.
- Various bug fixes and minor UI improvements.

v1.7, Mar. 3, 2004

- Texture parameters can now be assigned per-face or per-face-vertex,
as well as per-object or per-vertex.
- UV mapping can now be done independently for each face.
- Added Hide Selection command in the triangle mesh editor.
- Added Textured display mode.
- Added "Use Gestures to Shape Mesh" option to Inverse Kinematics tracks.
- Texture previews in the procedure editor, texture mapping window,
and UV mapping window are now resizeable.
- Plugins can now define new procedure modules.
- OBJ importer automatically rescales objects to better fit scene.
- Camera filters can now depend on the depth map.
- Added Outline filter.
- There is now an Italian translation (written by Paolo Scarpa).
- Upgraded to Beanshell 1.3.0.
- Some optimizations to reduce memory use.
- Many bug fixes.

v1.6, Sept. 14, 2003

- Rewrote the inverse kinematics solver.  It is now much more stable,
and allows multiple joints to be pinned.
- Added inverse kinematics tracks as a new type of distortion track.
- The Bevel/Extrude function can now be applied to vertices and edges
as well as faces.
- Added Bevel/Extrude tool to give a more interactive interface for
this function.
- Added Create Vertex tool to the triangle mesh editor (partly written
by Michael Butscher).
- The "Close Selected Boundary" and "Join Selected Boundaries"
commands can now be applied to incomplete (partially selected) pieces
of boundaries.
- The Skeleton tool now provides Forward Kinematics handles for
adjusting each degree of freedom separately.
- Added Import Skeleton command.
- Added Set Parent Bone command.
- Improved the appearance of bones in the mesh editor.  They are now
three dimensional, and show through the surface when the Skeleton tool
is selected.
- The UV texture mapping window now has a Move View tool for scrolling
the texture (implemented by Michael Butscher).
- Improved antialiasing in the raytracer to reduce noise.
- There is now a Japanese translation (written by Tanaka Masahiko).
- There is now a French translation (written by Francois Guillet).
- Various bug fixes and other minor improvements.

v1.5, May 31, 2003

- Added photon mapping.  This can be used to render both caustics and
global illumination.  There are now three options for global
illumination: Monte Carlo (the old method), Photon Mapping, or Hybrid.
- Procedural textures can now depend on the angle at which the surface
is viewed.
- Cameras can have filters attached to them.  Current filters include
Brightness, Saturation, Tint, Blur, and Glow.  Filters can be animated
by adding a Pose track to the camera.
- There is now a Danish translation (written by Jan Rouvillain).
- A few bug fixes.

v1.4, Feb. 15, 2003

- The VRML and OBJ exporters can now export 2D textures as image maps.
- Redesigned the antialiasing algorithm for the raytracer.  It now
offers two levels of antialiasing, and produces better looking images
while needing fewer rays/pixel.
- Added "extra smoothing" options to the raytracer to reduce noise in
global illumination.
- Can load high dynamic range images in Radiance (.hdr) format for use
as texture and environment maps.
- Can save raytraced images as high dynamic range (.hdr) images.
- Pose tracks now have a "relative" mode allowing complex motions to
be built up from multiple tracks.
- Restructured the "Add Track" submenu to make it easier to add
different types of position and rotation tracks.
- Display mode can be set independently for each of the four views in
the main window.
- The "Set Smoothness" and "Edit Points" commands now update the display
in real time to give immediate feedback.
- The procedure editor now supports cut and paste.
- There is now a German translation (written by Martin Winkelbauer).
- Added "Render Immediately" command.
- Added "Optimize Mesh" command to the triangle mesh editor.
- Added "Display as Quads" command to the triangle mesh editor.
- Magnification can now be set to any value, rather than a fixed
list of values.  In parallel projection mode, control-drag with the
Move View tool zooms by changing the magnification.
- Object editors now gray out anything which is not selectable based on
your current tool and selection mode.
- Can save rendered images in BMP format (implemented by Michael Butscher).
- Converting a curve to a triangle mesh now produces a much better
triangulation of the curve.
- Minor user interface improvements too numerous to list (some
implemented by Michael Butscher).
- Various bug fixes.

v1.3, Nov. 24, 2002

- Added Beanshell scripting (requires Java 1.2 or later).
- Added UV mapping.
- There is now a "Shininess" texture property for setting the strength of
specular highlights independent of specular reflections.
- The raytracer now offers Russian Roulette sampling as an advanced option.
- Implemented Ken Perlin's "improved" noise function.
- The .obj importer can now import textures from .mtl files.
- Added POV-Ray export (implemented by Norbert Krieg).
- Lots of bug fixes.

v1.2, Aug. 9, 2002

- Added procedural distortions (Bend, Twist, Scale, Shatter, and Custom).
- Added Tube objects.
- Position and Rotation tracks can apply to any joint in the skeleton of
an object.
- Animations can be saved as Quicktime movies (implemented by Ken McNeill,
requires Java Media Framework).
- Added "Bind to Parent Skeleton" command.
- Added a Preferences window for setting global options.
- Added a freehand selection mode in object editors.
- Object editors now let you independently hide or show the control mesh,
object surface, and skeleton.
- You can set a template image to use as the background in editing windows.
- Started the (very long) process of internationalizing all text in the
program, so it can be translated into other languages.  Spanish translation
provide by Julio Sangrador Paton.
- Bug fixes and speed improvements.

v1.1, May 5, 2002

- The raytracer now supports global illumination.
- Can now import and export Wavefront .obj files.
- Revised the inverse kinematics algorithm so that bones now move more
easily, and meshes deform more smoothly around joints.
- Added grids to the object editors.
- The object editors now allow you to view other objects in the scene,
and to work in either local or scene coordinate systems.
- Added procedural position and rotation tracks.
- Textures may now be applied to only the front or back surface of
a mesh.
- Added contextual popup menus on the item list in the main window, and
the list of tracks in the score.
- Added Frame Scene With Camera and Frame Selection With Camera commands.
- Added a preview to the Bevel/Extrude window in the triangle mesh editor.
- Added a 1D random noise module to the procedure editor.
- Revised the interpolating subdivision algorithm for triangle meshes so
that changing the smoothness of vertices now works in a more useful and
intuitive way.
- Lots of miscellaneous user interface improvements, including new keyboard
equivalents for lots of commands.
- Lots of bug fixes.

v1.0, Feb. 10, 2002

- Added Pose tracks, which allow you to animate the shape and other
internal parameters of objects.
- Adding a Pose track to a Spline Mesh or Triangle Mesh will convert it
into an Actor.  This allows you to create predefined gestures for the
object, which can be blended in arbitrary combinations and automatically
morphed between.
- Every Spline Mesh and Triangle Mesh now has a skeleton.  This can be
used as a tool for shaping the mesh (with forward and inverse kinematics),
and also allows morphing between gestures to happen in a more natural way.
- Added an Array tool (written by Rick van der Meiden) for creating evenly
spaced copies of objects.
- Position, Rotation, and Constraint tracks can now be relative to any
object (or, if it is a mesh, to any bone in its skeleton), not just to the
immediate parent object.
- Texture parameters can now be specified on a per-object basis, as well
as per-vertex.
- Added Texture Parameter tracks which allow you to animate the values
of per-object texture parameters.
- Texture mapping coordinates can now be bound to the vertices of a mesh,
so that the texture will distort with the mesh.
- The Move, Rotate, and Resize tools, as well as the Transform Object
command, offer new options to specify how they interact with parent-child
hierarchies and to set the center for rotation/scaling.
- The file format is now finalized.  This means that files created with this
version are guaranteed to be openable by later versions.
- Many, many bug fixes and other minor improvements.

v0.9, Aug. 25, 2001

Where do I begin?  This version introduces animation, which touches (or
will touch) almost every part of the program in one way or another.
- Each object may have any number of animation tracks, which are applied
according to their individual weight curves.  Current track types include 
Position, Rotation (XYZ and quaternion), Visibility, and Constraint.
- The Score provides a user interface for viewing and editing tracks and
keyframes.
- A Preview Animation command allows you to quickly render and view
wireframe tests of animation sequences.
- Animations can be rendered to a series of image files.  Multiple
subframes per image can be used to create motion blur.
- Various bulk keyframe editing commands allow you to simultaneously 
modify large numbers of keyframes at once.
- A Set Path from Curve command allows you to create animation paths
from curve objects.
- Any object can now be made a child of any other object by dragging it
within the Object List, which is now a hierarchical display.
- I have eliminated the Group and Ungroup commands.  They caused problems
for animation, and the parent-child relationships described above offer
a more powerful way of doing the same things.
- There are several new modules in the procedural texture and material
editor.  The most important of these is the Expression module (written
by David Turner), which allows you to enter arbitrary mathematical
expressions to be evaluated.  Others are Min, Max, Lighter, and Darker
(the last two also by David Turner).  In addition, the Cells module now
offers a choice of three different distance metrics.
- Added a "tolerant selection mode" in the triangle mesh editor.
- The Textures and Materials dialogs now allow you to import textures
and materials from files.
- Added Null objects.
- Thanks to Carmen DiMichele for the beautiful new icons!
- Bug fixes and miscellaneous user interface improvements beyond all
reckoning.

v0.8, April 5, 2001

- Added Raster renderer.  Features include: Gouraud, Phong, and Hybrid
shading methods; antialiasing; fog; bump mapping; displacement mapping;
and environment mapping.
- Added Lathe tool.
- Added Extrude tool.
- Added Skin tool.
- Added Boolean Modelling tool.
- Added lots of new commands to the triangle mesh and/or spline mesh
editors: Clear, Select Boundary, Extend Selection, Close Selected Boundary,
Join Selected Boundaries, Extract Selected Curve, and Invert Surface
Normals.
- All editing tools in the object editors now respect the mesh tension.
- Added a tool for moving vertices inward or outward along the local
surface normal.
- Any camera in a scene can now be specified as the viewpoint in a scene
window.  (Removed the "View Through Camera" command, which was no longer
needed.)
- In the "Align Objects" command, can now specify the precise coordinates
to align to.
- Lots of bug fixes and user interface improvements.

v0.7, Jan. 15, 2001

- Added a graphical editor for creating procedural textures and materials.
- Added displacement mapping.
- Can now save rendered images as TIFF files with transparency information.
- Assorted bug fixes, etc.

v0.6, Oct. 5, 2000

- Split the old "Material" class into two new classes: Textures, which
describe surface properties, and Materials, which describe bulk properties.
- Any Texture can now be used as an environment map.
- Added layered textures.
- Added per-vertex texture parameters.
- Completely rewrote routines for raytracing Materials to add support for
nonuniform Materials, Materials with internal scattering, and Materials
which cast shadows.
- Added "Ambient" and "Decay Rate" parameters for point lights and spot
lights.
- Implemented plugin API for procedural Textures and Materials.
- The usual crop of bug fixes, etc.

v0.5, July 15, 2000

- Added interactive rendering (wireframe, flat shaded, smooth shaded, 
and transparent views).
- Replaced the old Line and Spline objects with a new Curve object.
- Added Spline Mesh objects.
- The VRML exporter now saves explicit normals for IndexedFaceSets.
- A few bug fixes and user interface improvements.

v0.4, May 3, 2000

- Rewrote the triangle mesh smoothing code to use subdivision surfaces.
This also involved many changes to the triangle mesh editor.
- Can now load and save scenes.  However, the file format is NOT final.
Later versions will NOT be able to open files created with this version.
- Material previews can now be rotated by dragging the image.
- Added a "solid" checkbox to the Set Material dialog.
- A few bug fixes.

v0.3, Feb. 18, 2000

- Added image mapped materials.
- Added bump-mapping to the raytracer.
- Rewrote the adaptive distribution raytracing code.  It now does a much
better job of figuring out when to send more rays, and can use up to 256
rays/pixel.
- Added spotlights.
- Added soft shadows to the raytracer.
- A huge number of bug fixes, optimizations, and user interface improvements.

v0.2, Dec. 23, 1999

- The raytracer can now directly render spheres, ellipsoids, and cylinders
without having to break them into triangles.
- The raytracer now supports antialiasing, depth of field, and
gloss/translucency effects.
- Added "Render Preview" command to the triangle mesh editor.
- Added automatic preview feature to the material editor.
- Rendered images can now be saved as JPEG files.
- Added a tool for creating circular and elliptical spline curves.
- There is now a "specular color" material property.
- Can now export scenes to VRML files.
- Many bug fixes and user interface improvements.

v0.1, Oct. 29, 1999

The first public release.  After six months gestation, a new program has
come into the world!

April 21, 1999

The very first line of code was written (in Vec3.java).

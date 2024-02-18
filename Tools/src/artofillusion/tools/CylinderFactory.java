package artofillusion.tools;

import artofillusion.object.Cylinder;
import artofillusion.object.Object3D;
import artofillusion.ui.Translate;

import java.util.Optional;

public class CylinderFactory implements PrimitiveFactory {
    @Override
    public String getCategory() {
        return "Geometry";
    }

    @Override
    public String getName() {
        return Translate.text("menu.cylinder");
    }

    @Override
    public Optional<Object3D> create() {
        return Optional.of(new Cylinder(1.0, 0.5, 0.5, 1.0));
    }
}

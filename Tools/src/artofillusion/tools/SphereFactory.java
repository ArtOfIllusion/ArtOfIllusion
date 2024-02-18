package artofillusion.tools;

import artofillusion.object.Object3D;
import artofillusion.object.Sphere;
import artofillusion.ui.Translate;

import java.util.Optional;

public class SphereFactory implements PrimitiveFactory {
    @Override
    public String getName() {
        return Translate.text("menu.sphere");
    }
    @Override
    public String getCategory() {
        return "Geometry";
    }

    @Override
    public Optional<Object3D> create() {
        return Optional.of(new Sphere(0.5, 0.5, 0.5));
    }
}

package artofillusion.tools;

import artofillusion.object.Cube;
import artofillusion.object.Object3D;
import artofillusion.ui.Translate;

import java.util.Optional;

public class CubeFactory implements PrimitiveFactory {

    @Override
    public String getName() {
        return Translate.text("menu.cube");
    }
    @Override
    public String getCategory() {
        return "Geometry";
    }

    @Override
    public Optional<Object3D> create() {
        return Optional.of(new Cube(1.0, 1.0, 1.0));
    }
}

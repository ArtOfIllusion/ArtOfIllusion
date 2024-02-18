package artofillusion.tools;

import artofillusion.math.RGBColor;
import artofillusion.object.Object3D;
import artofillusion.object.SpotLight;
import artofillusion.ui.Translate;

import java.util.Optional;

public class SpotLightFactory implements PrimitiveFactory {
    @Override
    public String getName() {
        return Translate.text("menu.spotLight");
    }

    @Override
    public String getCategory() {
        return "Lights";
    }

    @Override
    public Optional<Object3D> create() {
        return Optional.of(new SpotLight(new RGBColor(1.0f, 1.0f, 1.0f), 1.0f, 20.0, 0.0, 0.1));
    }
}

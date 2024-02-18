package artofillusion.tools;

import artofillusion.math.RGBColor;
import artofillusion.object.DirectionalLight;
import artofillusion.object.Object3D;
import artofillusion.ui.Translate;

import java.util.Optional;

public class DirectionalLightFactory implements PrimitiveFactory {
    @Override
    public String getName() {
        return Translate.text("menu.directionalLight");
    }

    @Override
    public String getCategory() {
        return "Lights";
    }

    @Override
    public Optional<Object3D> create() {
        return Optional.of(new DirectionalLight(new RGBColor(1.0f, 1.0f, 1.0f), 1.0f));
    }
}

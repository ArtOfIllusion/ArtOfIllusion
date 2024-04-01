package artofillusion.tools;

import artofillusion.object.Object3D;
import artofillusion.object.SceneCamera;
import artofillusion.ui.Translate;

import java.util.Optional;

public class CameraFactory implements PrimitiveFactory {

    @Override
    public String getName() {
        return Translate.text("menu.camera");
    }
    @Override
    public String getCategory() {
        return "Cameras";
    }

    @Override
    public Optional<Object3D> create() {
        return Optional.of(new SceneCamera());
    }
}

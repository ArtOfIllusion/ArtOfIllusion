package artofillusion.tools;

import artofillusion.object.Object3D;
import artofillusion.script.ScriptRunner;
import artofillusion.script.ScriptedObject;

import java.util.Optional;

public class ScriptedObjectProvider implements PrimitiveFactory {
    @Override
    public String getCategory() {
        return "Scripting";
    }

    @Override
    public String getName() {
        return "Scripted Object";
    }

    @Override
    public Optional<Object3D> create() {
        ScriptedObject obj = new ScriptedObject("", ScriptRunner.Language.GROOVY.name);
        return Optional.of(obj);
    }
}

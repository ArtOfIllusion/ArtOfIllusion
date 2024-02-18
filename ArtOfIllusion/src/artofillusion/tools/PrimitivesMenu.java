package artofillusion.tools;

import artofillusion.LayoutWindow;
import artofillusion.PluginRegistry;
import artofillusion.UndoRecord;
import artofillusion.math.CoordinateSystem;
import artofillusion.math.Vec3;
import artofillusion.object.Object3D;
import artofillusion.object.ObjectInfo;
import artofillusion.ui.Translate;
import buoy.widget.BMenu;
import buoy.widget.BMenuItem;
import buoy.widget.Widget;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public final class PrimitivesMenu extends BMenu {

    private LayoutWindow layout;
    public PrimitivesMenu(LayoutWindow layout) {

        super(Translate.text("menu.createPrimitive"));
        this.layout = layout;


        Map<String, List<PrimitiveFactory>> providers = PluginRegistry.getPlugins(PrimitiveFactory.class).
                stream().collect(Collectors.groupingBy(PrimitiveFactory::getCategory));

        providers.forEach((category, items) -> {
            items.forEach(item -> {
                BMenuItem menuItem = new BMenuItem();
                menuItem.getComponent().setAction(new PrimitiveAction(item));
                menuItem.setText(item.getName());
                this.add(menuItem);
            });
            if(!items.isEmpty()) this.addSeparator();
        });

        this.remove((Widget)this.getChild(this.getChildCount()-1));
    }

    private class PrimitiveAction extends AbstractAction {

        private final PrimitiveFactory provider;
        public PrimitiveAction(PrimitiveFactory provider) {
            super();
            this.provider = provider;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            provider.create().ifPresent(this::addToScene);
        }

        private void addToScene(Object3D obj) {
            int counter = 0;
            String name;
            while (layout.getScene().getObject(name = provider.getObjectName() + " " + counter) != null) {
                counter++;
            }
            CoordinateSystem coordinates = new CoordinateSystem(new Vec3(), Vec3.vz(), Vec3.vy());
            ObjectInfo info = new ObjectInfo(obj, coordinates, name);

            UndoRecord undo = new UndoRecord(layout, false);
            int sel[] = layout.getSelectedIndices();
            layout.addObject(info, undo);
            undo.addCommand(UndoRecord.SET_SCENE_SELECTION, sel);
            layout.setSelection(layout.getScene().getNumObjects()-1);
            layout.setUndoRecord(undo);
            layout.updateImage();
        }

    }
}

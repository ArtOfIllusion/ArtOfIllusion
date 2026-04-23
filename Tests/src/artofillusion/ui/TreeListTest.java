package artofillusion.ui;

import artofillusion.object.NullObject;
import org.junit.Assert;
import org.junit.Test;
import java.util.ArrayList;



public class TreeListTest {
    private class DummyTreeElement extends TreeElement {

        private Object item;
        public  DummyTreeElement(Object item) {
            this.item = item;
            this.children = new ArrayList<>();
        }

        @Override
        public String getLabel() {
            return null;
        }

        @Override
        public boolean canAcceptAsParent(TreeElement el) {
            return false;
        }

        @Override
        public void addChild(TreeElement el, int position) {
            this.children.add(position, el);
        }

        @Override
        public void removeChild(Object obj) {

        }

        @Override
        public Object getObject() {
            return item;
        }

        @Override
        public boolean isGray() {
            return false;
        }
    }

    @Test
    public void getElementsFromEmptyList() {

        TreeList treeList = new TreeList(null);
        TreeElement[] elements = treeList.getElements();
        Assert.assertEquals(0, elements.length);

    }

    @Test
    public void getSingleItem() {
        TreeList treeList = new TreeList(null);
        treeList.setUpdateEnabled(false);

        TreeElement el = new DummyTreeElement(new NullObject());

        treeList.addElement(el, 0);
        TreeElement[] elements = treeList.getElements();
        Assert.assertEquals(1, elements.length);
        Assert.assertEquals(el, elements[0]);
    }

    @Test
    public void getDoubleItemOneLevel() {
        TreeList treeList = new TreeList(null);
        treeList.setUpdateEnabled(false);

        TreeElement el = new DummyTreeElement(new NullObject());

        treeList.addElement(el);
        treeList.addElement(el);

        TreeElement[] elements = treeList.getElements();
        Assert.assertEquals(2, elements.length);
        Assert.assertEquals(el, elements[0]);
        Assert.assertEquals(el, elements[1]);
    }

    @Test
    public void getDoubleItemTwoLevels() {
        TreeList treeList = new TreeList(null);
        treeList.setUpdateEnabled(false);

        TreeElement el0 = new DummyTreeElement(new NullObject());
        TreeElement el2 = new DummyTreeElement(new NullObject());

        el0.addChild(el2, 0);

        treeList.addElement(el0);
        TreeElement[] elements = treeList.getElements();
        Assert.assertEquals(2, elements.length);
        Assert.assertEquals(el0, elements[0]);
        Assert.assertEquals(el2, elements[1]);
    }


    @Test
    public void findNonExistedItemInEmptyTree() {
        TreeList treeList = new TreeList(null);
        treeList.setUpdateEnabled(false);

        Assert.assertNull(treeList.findElement(new NullObject()));
    }

    @Test
    public void findNonExistedItemInTreeOnelevel() {
        TreeList treeList = new TreeList(null);
        treeList.setUpdateEnabled(false);

        TreeElement el0 = new DummyTreeElement(new NullObject());
        treeList.addElement(el0);
        Assert.assertNull(treeList.findElement(new NullObject()));

    }

    @Test
    public void findExistedItemInTreeOnelevel() {
        TreeList treeList = new TreeList(null);
        treeList.setUpdateEnabled(false);
        NullObject target = new NullObject();
        TreeElement el0 = new DummyTreeElement(target);
        treeList.addElement(el0);

        TreeElement result = treeList.findElement(target);
        Assert.assertEquals(el0, result);


    }


    @Test
    public void findNonExistedItemInTreeTwoLevels() {
        TreeList treeList = new TreeList(null);
        treeList.setUpdateEnabled(false);

        TreeElement el0 = new DummyTreeElement(new NullObject());
        TreeElement el2 = new DummyTreeElement(new NullObject());
        el0.addChild(el2, 0);

        treeList.addElement(el0);
        Assert.assertNull(treeList.findElement(new NullObject()));

    }

    @Test
    public void findExistedItemInTreeTwoLevels() {
        TreeList treeList = new TreeList(null);
        treeList.setUpdateEnabled(false);

        TreeElement el0 = new DummyTreeElement(new NullObject());

        NullObject target = new NullObject();


        TreeElement el2 = new DummyTreeElement(target);
        el0.addChild(el2, 0);

        treeList.addElement(el0);
        TreeElement result = treeList.findElement(target);
        Assert.assertEquals(el2, result);

    }

    @Test
    public void findSelectedItemsInEmptyTree() {
        TreeList treeList = new TreeList(null);
        treeList.setUpdateEnabled(false);

        Object[] selected = treeList.getSelectedObjects();
        Assert.assertNotNull(selected);
        Assert.assertEquals(0, selected.length);
    }

    @Test
    public void findSelectedItemsInOneLevelTree() {
        TreeList treeList = new TreeList(null);
        treeList.setUpdateEnabled(false);

        NullObject target = new NullObject();
        TreeElement el0 = new DummyTreeElement(target);

        treeList.addElement(el0);
        treeList.setSelected(el0, true);


        Object[] selected = treeList.getSelectedObjects();
        Assert.assertNotNull(selected);
        Assert.assertEquals(1, selected.length);
    }

    @Test
    public void findSelectedItemsInTwoLevelsTree() {
        TreeList treeList = new TreeList(null);
        treeList.setUpdateEnabled(false);


        TreeElement el0 = new DummyTreeElement( new NullObject());
        TreeElement el1 = new DummyTreeElement( new NullObject());

        el0.addChild(el1, 0);

        treeList.addElement(el0);
        treeList.setSelected(el0, true);
        treeList.setSelected(el1, true);

        Object[] selected = treeList.getSelectedObjects();
        Assert.assertNotNull(selected);
        Assert.assertEquals(2, selected.length);
    }
}
package com.zfile.manager.model.decorator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

import com.zfile.manager.model.FileItem;
import com.zfile.manager.model.FileType;

import org.junit.Before;
import org.junit.Test;

public class DecoratorChainTest {

    private FileItem item;
    private BaseFileItem base;

    @Before
    public void setUp() {
        item = new FileItem(
                "photo.jpg", "/sdcard/photo.jpg",
                1024L, System.currentTimeMillis(),
                false, false, true, true,
                FileType.IMAGE, "image/jpeg");
        base = new BaseFileItem(item);
    }

    @Test
    public void baseFileItem_returnsNeutralDefaults() {
        assertEquals("photo.jpg", base.getName());
        assertEquals("/sdcard/photo.jpg", base.getPath());
        assertEquals(0, base.getIconResource());
        assertEquals(0, base.getTintColor());
        assertEquals("", base.getTag());
        assertSame(item, base.getFileItem());
    }

    @Test
    public void iconDecorator_overridesOnlyIcon() {
        IconDecorator decorated = new IconDecorator(base, 0xABCD);
        assertEquals(0xABCD, decorated.getIconResource());
        assertEquals(0, decorated.getTintColor());
        assertEquals("", decorated.getTag());
        assertEquals("photo.jpg", decorated.getName());
        assertSame(item, decorated.getFileItem());
    }

    @Test
    public void colorDecorator_overridesOnlyTint() {
        ColorDecorator decorated = new ColorDecorator(base, 0xFF112233);
        assertEquals(0xFF112233, decorated.getTintColor());
        assertEquals(0, decorated.getIconResource());
    }

    @Test
    public void systemTagDecorator_addsBracketedMarker() {
        SystemTagDecorator decorated = new SystemTagDecorator(base, "HIDDEN");
        assertEquals("[HIDDEN]", decorated.getTag());
    }

    @Test
    public void chainedDecorators_stackAttributes() {
        FileItemComponent chain =
                new SystemTagDecorator(
                        new ColorDecorator(
                                new IconDecorator(base, 42),
                                0xFF00FF00),
                        "SYSTEM");

        assertEquals(42, chain.getIconResource());
        assertEquals(0xFF00FF00, chain.getTintColor());
        assertEquals("[SYSTEM]", chain.getTag());
        assertEquals("photo.jpg", chain.getName());
        assertSame(item, chain.getFileItem());
    }

    @Test
    public void stackedSystemTags_appendBracketed() {
        FileItemComponent chain =
                new SystemTagDecorator(
                        new SystemTagDecorator(base, "HIDDEN"),
                        "SYSTEM");
        assertEquals("[HIDDEN] [SYSTEM]", chain.getTag());
    }
}

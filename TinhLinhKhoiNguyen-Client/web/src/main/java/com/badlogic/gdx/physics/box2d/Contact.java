package com.badlogic.gdx.physics.box2d;

import com.badlogic.gdx.physics.box2d.cpp.b2Contact;
import com.badlogic.gdx.physics.box2d.cpp.b2Fixture;
import com.badlogic.gdx.physics.box2d.cpp.b2Manifold;
import org.teavm.jso.JSBody;

public class Contact {

    protected b2Contact b2Contact;

    protected World world;

    protected final WorldManifold worldManifold = new WorldManifold();

    protected Contact(World world, long addr) {
        this.world = world;
        b2Contact = new b2Contact();
        b2Contact.setPointer(addr);
    }

    @JSBody(params = {"contactAddr"}, script =
        "var contact = Box2D.wrapPointer(contactAddr, Box2D.b2Contact);\n" +
        "if (!contact) return false;\n" +
        "if (!Box2D._sharedWorldManifold) Box2D._sharedWorldManifold = new Box2D.b2WorldManifold();\n" +
        "contact.GetWorldManifold(Box2D._sharedWorldManifold);\n" +
        "return true;"
    )
    private static native boolean nativeUpdateWorldManifold(int contactAddr);

    @JSBody(params = {}, script =
        "var wm = Box2D._sharedWorldManifold;\n" +
        "if (!wm) return 0;\n" +
        "var n = wm.get_normal();\n" +
        "return n ? n.get_x() : 0;\n"
    )
    private static native float nativeGetNormalX();

    @JSBody(params = {}, script =
        "var wm = Box2D._sharedWorldManifold;\n" +
        "if (!wm) return 0;\n" +
        "var n = wm.get_normal();\n" +
        "return n ? n.get_y() : 0;\n"
    )
    private static native float nativeGetNormalY();

    @JSBody(params = {"index"}, script =
        "var wm = Box2D._sharedWorldManifold;\n" +
        "if (!wm) return 0;\n" +
        "var p = wm.get_points(index);\n" +
        "return p ? p.get_x() : 0;\n"
    )
    private static native float nativeGetPointX(int index);

    @JSBody(params = {"index"}, script =
        "var wm = Box2D._sharedWorldManifold;\n" +
        "if (!wm) return 0;\n" +
        "var p = wm.get_points(index);\n" +
        "return p ? p.get_y() : 0;\n"
    )
    private static native float nativeGetPointY(int index);

    @JSBody(params = {"index"}, script =
        "var wm = Box2D._sharedWorldManifold;\n" +
        "if (!wm) return 0;\n" +
        "return wm ? wm.get_separations(index) : 0;\n"
    )
    private static native float nativeGetSeparation(int index);

    @JSBody(params = {"contactAddr"}, script =
        "var contact = Box2D.wrapPointer(contactAddr, Box2D.b2Contact);\n" +
        "if (!contact) return 0;\n" +
        "var m = contact.GetManifold();\n" +
        "if (!m) return 0;\n" +
        "return typeof m.get_pointCount === 'function' ? m.get_pointCount() : (m.pointCount || 0);\n"
    )
    private static native int nativeGetContactPointCount(int contactAddr);

    public WorldManifold getWorldManifold() {
        if (b2Contact != null) {
            int addr = (int) b2Contact.getCPointer();
            if (nativeUpdateWorldManifold(addr)) {
                int count = nativeGetContactPointCount(addr);
                float nx = nativeGetNormalX();
                float ny = nativeGetNormalY();
                float p0x = nativeGetPointX(0);
                float p0y = nativeGetPointY(0);
                float p1x = nativeGetPointX(1);
                float p1y = nativeGetPointY(1);
                float s0 = nativeGetSeparation(0);
                float s1 = nativeGetSeparation(1);
                worldManifold.setValues(count, nx, ny, p0x, p0y, p1x, p1y, s0, s1);
            }
        }
        return worldManifold;
    }

    public Fixture getFixtureA() {
        if (b2Contact == null) return null;
        b2Fixture b2Fixture = b2Contact.GetFixtureA();
        if (b2Fixture == null) return null;
        int addr = (int) b2Fixture.getCPointer();
        Fixture fixture = world.fixtures.get(addr);
        if (fixture == null) {
            fixture = world.freeFixtures.obtain();
            fixture.reset(addr);
        }
        return fixture;
    }

    public Fixture getFixtureB() {
        if (b2Contact == null) return null;
        b2Fixture b2Fixture = b2Contact.GetFixtureB();
        if (b2Fixture == null) return null;
        int addr = (int) b2Fixture.getCPointer();
        Fixture fixture = world.fixtures.get(addr);
        if (fixture == null) {
            fixture = world.freeFixtures.obtain();
            fixture.reset(addr);
        }
        return fixture;
    }

    @JSBody(params = {"contactAddr", "flag"}, script =
        "var contact = Box2D.wrapPointer(contactAddr, Box2D.b2Contact);\n" +
        "if (contact && typeof contact.SetEnabled === 'function') {\n" +
        "    contact.SetEnabled(flag);\n" +
        "}"
    )
    private static native void nativeSetEnabled(int contactAddr, boolean flag);

    public void setEnabled(boolean flag) {
        if (b2Contact != null) {
            nativeSetEnabled((int) b2Contact.getCPointer(), flag);
        }
    }

    @JSBody(params = {"contactAddr"}, script =
        "var contact = Box2D.wrapPointer(contactAddr, Box2D.b2Contact);\n" +
        "if (contact && typeof contact.IsEnabled === 'function') {\n" +
        "    return contact.IsEnabled();\n" +
        "}\n" +
        "return true;"
    )
    private static native boolean nativeIsEnabled(int contactAddr);

    public boolean isEnabled() {
        if (b2Contact != null) {
            return nativeIsEnabled((int) b2Contact.getCPointer());
        }
        return true;
    }

    @JSBody(params = {"contactAddr"}, script =
        "var contact = Box2D.wrapPointer(contactAddr, Box2D.b2Contact);\n" +
        "if (contact && typeof contact.IsTouching === 'function') {\n" +
        "    return contact.IsTouching();\n" +
        "}\n" +
        "return false;"
    )
    private static native boolean nativeIsTouching(int contactAddr);

    public boolean isTouching() {
        if (b2Contact != null) {
            return nativeIsTouching((int) b2Contact.getCPointer());
        }
        return false;
    }
}

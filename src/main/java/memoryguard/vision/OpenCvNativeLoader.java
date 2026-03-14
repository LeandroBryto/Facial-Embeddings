package memoryguard.vision;

import org.opencv.core.Core;

import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicBoolean;

public final class OpenCvNativeLoader {

    private static final AtomicBoolean LOADED = new AtomicBoolean(false);

    private OpenCvNativeLoader() {
    }

    public static void load() {
        if (LOADED.get()) {
            return;
        }

        synchronized (OpenCvNativeLoader.class) {
            if (LOADED.get()) {
                return;
            }

            if (tryLoadViaNuPattern()) {
                LOADED.set(true);
                return;
            }

            System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
            LOADED.set(true);
        }
    }

    private static boolean tryLoadViaNuPattern() {
        try {
            Class<?> openCvClass = Class.forName("nu.pattern.OpenCV");
            Method loadLocally = openCvClass.getMethod("loadLocally");
            loadLocally.invoke(null);
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }
}


package org.avvento.apps.telefyna.listen;

import org.avvento.apps.telefyna.Monitor;

public class TelefynaUnCaughtExceptionHandler implements Thread.UncaughtExceptionHandler {
    public static String CRASH = "crash";
    public static String EXCEPTION = "exception";

    @Override
    public void uncaughtException(Thread thread, Throwable ex) {
        Monitor.instance.restartApp();
    }
}
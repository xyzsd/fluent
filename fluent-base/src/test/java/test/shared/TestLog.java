package test.shared;

/// Used to gate test logging output
///
/// Set environmental variable 'fluent.tests.verbose' to true to enable verbose logging.
///
public final class TestLog {
    private static final boolean VERBOSE = Boolean.getBoolean("fluent.tests.verbose");

    private TestLog() {}


    public static void println(Object message) {
        if (VERBOSE) {
            System.out.println(message);
        }
    }

    public static void printf(String format, Object... args) {
        if (VERBOSE) {
            System.out.printf(format, args);
        }
    }

    public static boolean isVerbose() {
        return VERBOSE;
    }

    public static void errPrintf(String format, Object... args) {
        if (VERBOSE) {
            System.err.printf(format, args);
        }
    }

    public static void errPrintln(Object message) {
        if (VERBOSE) {
            System.err.println(message);
        }
    }
}

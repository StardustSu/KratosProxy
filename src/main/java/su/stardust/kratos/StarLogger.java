package su.stardust.kratos;

public class StarLogger {

    private static final String ANSI_RESET = "\u001B[0m";
    private static final String ANSI_BLACK = "\u001B[30m";
    private static final String ANSI_RED = "\u001B[31m";
    private static final String ANSI_GREEN = "\u001B[32m";
    private static final String ANSI_YELLOW = "\u001B[33m";
    private static final String ANSI_BLUE = "\u001B[34m";
    private static final String ANSI_PURPLE = "\u001B[35m";
    private static final String ANSI_CYAN = "\u001B[36m";
    private static final String ANSI_WHITE = "\u001B[37m";

    public static void log(String msg) {
        Kratos.getLogger().info(msg);
        Kratos.logVerbose("&7" + msg);
    }

    public static void info(String msg) {
        Kratos.getLogger().info(ANSI_CYAN + "{}" + ANSI_RESET, msg);
        Kratos.logVerbose("&b&o" + msg);
    }

    public static void success(String msg) {
        Kratos.getLogger().info(ANSI_GREEN + "{}" + ANSI_RESET, msg);
        Kratos.logVerbose("&a&o" + msg);
    }

    public static void warn(String msg) {
        Kratos.getLogger().warn(msg);
        Kratos.logVerbose("&e&o" + msg);
    }

    public static void error(String msg) {
        Kratos.getLogger().error(msg);
        Kratos.logVerbose("&c&o" + msg);
    }

    public static void fatal(String msg) {
        Kratos.getLogger().error(ANSI_RED + "{}" + ANSI_RESET, msg);
        Kratos.logVerbose("&4&o" + msg);
    }

    public static void debug(String msg) {
        Kratos.getLogger().info(ANSI_PURPLE + "{}" + ANSI_RESET, msg);
        Kratos.logVerbose("&d&o" + msg);
    }

}

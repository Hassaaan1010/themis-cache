package common;


public class LogUtil {
    /* Example Use
     * ErrorHelper.log(
        "Error while encoding ResponseData",
        "ResponseData", msg,
        "OutputByteBuf", out,
        "Charset", charset
    );*/

    public static void log(String message, Object... vars) {
        StringBuilder sb = new StringBuilder();
        sb.append("[DEBUG] ").append(message).append("\n");

        // vars = name1, value1, name2, value2, ...
        for (int i = 0; i < vars.length; i += 2) {
            String name = String.valueOf(vars[i]);
            Object value = (i + 1 < vars.length) ? vars[i + 1] : "MISSING";
            sb.append("  ").append(name).append(": ");
            sb.append(value != null ? value.toString() : "null").append("\n");
        }

        System.out.println(sb.toString());
    }
}

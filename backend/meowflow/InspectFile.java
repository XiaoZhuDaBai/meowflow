import java.nio.file.*;
import java.util.*;

public class InspectFile {
    public static void main(String[] args) throws Exception {
        Path p = Paths.get("d:\\Code\\喵流\\backend\\meowflow\\meowflow-template\\src\\main\\java\\com\\meowflow\\template\\catalog\\BuiltinTemplateCatalog.java");
        List<String> lines = Files.readAllLines(p);
        // Print lines 215-235 with char code at column 17
        for (int i = 214; i < 235 && i < lines.size(); i++) {
            String line = lines.get(i);
            String snippet = line.length() > 80 ? line.substring(0, 80) : line;
            // Find column 17 (1-indexed), so char at index 16
            String col17Info = "N/A";
            if (line.length() > 16) {
                char ch = line.charAt(16);
                col17Info = "0x" + Integer.toHexString(ch) + " ('" + ch + "')";
            }
            System.out.println("L" + (i+1) + " col17=" + col17Info + " | " + snippet);
        }
        // Count triple-quotes in the entire file
        String content = String.join("\n", lines);
        System.out.println("--- triple-quote ---");
        int count = 0; int idx = 0;
        while ((idx = content.indexOf("\"\"\"", idx)) != -1) { count++; idx++; }
        System.out.println("Total \"\"\" occurrences: " + count);
    }
}

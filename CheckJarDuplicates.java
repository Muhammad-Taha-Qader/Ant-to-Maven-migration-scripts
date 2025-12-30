import java.nio.file.*;
import java.util.*;

public class CheckJarDuplicates {
    public static void main(String[] args) throws Exception {
        Map<String, List<String>> map = new HashMap<>();

        for (String line : Files.readAllLines(Paths.get("jars.txt"))) {
            String base = line.replaceAll("-\\d.*\\.jar$", "");
            map.computeIfAbsent(base, k -> new ArrayList<>()).add(line);
        }

        map.forEach((k, v) -> {
            if (v.size() > 1) {
                System.out.println("DUPLICATE FOUND: " + k);
                v.forEach(j -> System.out.println("  " + j));
                System.out.println("-------------------------");
            }
        });
    }
}

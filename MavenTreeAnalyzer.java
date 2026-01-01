import java.io.*;
import java.util.*;
import java.util.regex.*;

public class MavenTreeAnalyzer {

    private static final Pattern DEP_PATTERN =
            Pattern.compile("([\\w\\.-]+):([\\w\\.-]+):jar:([\\w\\.-]+):\\w+");

    public static void main(String[] args) throws IOException {

        if (args.length != 1) {
            System.out.println("Usage: java MavenTreeAnalyzer <tree.txt>");
            return;
        }

        File treeFile = new File(args[0]);

        List<String> lines = readAllLines(treeFile);

        // ===== PASS 1: collect all MAIN deps =====
        Set<String> mainGA = new HashSet<>();
        Set<String> mainArtifacts = new HashSet<>();

        for (String line : lines) {
            if (!line.contains("+- ")) continue;

            Matcher m = DEP_PATTERN.matcher(line);
            if (!m.find()) continue;

            String groupId = m.group(1);
            String artifactId = m.group(2);

            mainGA.add(groupId + ":" + artifactId);
            mainArtifacts.add(artifactId);
        }

        // ===== PASS 2: evaluate transitive deps =====
        Map<String, Set<String>> reportGA = new LinkedHashMap<>();
        Map<String, Set<String>> reportArtifactOnly = new LinkedHashMap<>();

        String currentMainGA = null;

        for (String line : lines) {
            Matcher m = DEP_PATTERN.matcher(line);
            if (!m.find()) continue;

            String groupId = m.group(1);
            String artifactId = m.group(2);
            String ga = groupId + ":" + artifactId;

            // MAIN
            if (line.contains("+- ")) {
                currentMainGA = ga;
                continue;
            }

            // TRANSITIVE
            if (currentMainGA == null) continue;

            if (line.contains("\\- ") || line.contains("|  +-")) {

                // Report 1: groupId + artifactId
                if (!mainGA.contains(ga)) {
                    reportGA
                            .computeIfAbsent(currentMainGA, k -> new LinkedHashSet<>())
                            .add(ga);
                }

                // Report 2: artifactId ONLY
                if (!mainArtifacts.contains(artifactId)) {
                    reportArtifactOnly
                            .computeIfAbsent(currentMainGA, k -> new LinkedHashSet<>())
                            .add(ga);
                }
            }
        }

        printAssumptions();
        printReport(
                "REPORT 1: Missing transitive deps (groupId + artifactId comparison)",
                reportGA
        );
        printReport(
                "REPORT 2: Missing transitive deps (artifactId-only comparison, group ignored)",
                reportArtifactOnly
        );
    }

    private static List<String> readAllLines(File file) throws IOException {
        List<String> lines = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                lines.add(line);
            }
        }
        return lines;
    }

    private static void printAssumptions() {
        System.out.println("==================================================");
        System.out.println("Key Assumptions (important & realistic)");
        System.out.println("--------------------------------------------------");
        System.out.println("- The tree is generated with default Maven formatting");
        System.out.println("- Lines starting with '+-' are main dependencies");
        System.out.println("- Lines starting with '|  \\\\' or '|  +-' are transitive dependencies");
        System.out.println("- Report 1 conflict check uses groupId + artifactId");
        System.out.println("- Report 2 conflict check uses artifactId only (groupId ignored)");
        System.out.println("- Version differences are ignored intentionally");
        System.out.println("==================================================\n");
    }

    private static void printReport(String title, Map<String, Set<String>> report) {
        System.out.println(title);
        System.out.println("--------------------------------------------------");

        boolean found = false;

        for (Map.Entry<String, Set<String>> e : report.entrySet()) {
            if (e.getValue().isEmpty()) continue;

            found = true;
            System.out.println(e.getKey());
            for (String dep : e.getValue()) {
                System.out.println("  -> " + dep);
            }
            System.out.println();
        }

        if (!found) {
            System.out.println("No entries found.\n");
        }
    }
}

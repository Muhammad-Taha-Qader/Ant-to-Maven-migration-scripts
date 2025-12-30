import java.io.*;
import java.nio.file.*;
import java.util.regex.*;
 
public class JarVersionSplitter {
 
    // Regex to detect version like: -1.2.3, -2.0, -3.1.Final, etc.
    private static final Pattern VERSION_PATTERN =
            Pattern.compile(".*-\\d+(\\.\\d+)+([.-][A-Za-z0-9]+)*\\.jar$");
 
    public static void main(String[] args) {
 
        Path inputFile = Paths.get("jars.txt");
        Path withVersionFile = Paths.get("jars-with-version.txt");
        Path withoutVersionFile = Paths.get("jars-without-version.txt");
 
        int totalCount = 0;
        int withVersionCount = 0;
        int withoutVersionCount = 0;
 
        try (
            BufferedReader reader = Files.newBufferedReader(inputFile);
            BufferedWriter withVersionWriter = Files.newBufferedWriter(withVersionFile);
            BufferedWriter withoutVersionWriter = Files.newBufferedWriter(withoutVersionFile)
        ) {
 
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) {
                    continue;
                }
 
                totalCount++;
 
                if (VERSION_PATTERN.matcher(line).matches()) {
                    withVersionWriter.write(line);
                    withVersionWriter.newLine();
                    withVersionCount++;
                } else {
                    withoutVersionWriter.write(line);
                    withoutVersionWriter.newLine();
                    withoutVersionCount++;
                }
            }
 
            // Console summary
            System.out.println("===== JAR Split Summary =====");
            System.out.println("Total jar names read     : " + totalCount);
            System.out.println("Jars WITH version        : " + withVersionCount);
            System.out.println("Jars WITHOUT version     : " + withoutVersionCount);
            System.out.println("================================");
 
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
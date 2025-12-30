import java.io.*;
import java.net.URI;
import java.net.http.*;
import java.nio.file.*;
import java.util.regex.*;

import org.json.*;

public class MavenDependencyGenerator {

    private static final Pattern JAR_PATTERN =
            Pattern.compile("(.+)-(\\d+.*)\\.jar");

    private static final HttpClient CLIENT = HttpClient.newHttpClient();

    public static void main(String[] args) throws Exception {

        Path input = Paths.get("jars-with-version.txt");
        Path output = Paths.get("generated-dependencies.xml");
        Path unresolved = Paths.get("unresolved-jars.txt");

        int total = 0, resolved = 0, failed = 0;

        try (
            BufferedReader reader = Files.newBufferedReader(input);
            BufferedWriter out = Files.newBufferedWriter(output);
            BufferedWriter fail = Files.newBufferedWriter(unresolved)
        ) {

            String jar;
            while ((jar = reader.readLine()) != null) {
                jar = jar.trim();
                if (jar.isEmpty()) continue;

                total++;

                Matcher m = JAR_PATTERN.matcher(jar);
                if (!m.matches()) {
                    fail.write(jar);
                    fail.newLine();
                    failed++;
                    continue;
                }

                String artifactId = m.group(1);
                String version = m.group(2);

                String query = String.format(
                        "https://search.maven.org/solrsearch/select?q=a:%s+AND+v:%s&rows=1&wt=json",
                        artifactId, version
                );

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(query))
                        .GET()
                        .build();

                HttpResponse<String> response =
                        CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

                JSONObject json = new JSONObject(response.body());
                JSONArray docs = json
                        .getJSONObject("response")
                        .getJSONArray("docs");

                if (docs.isEmpty()) {
                    fail.write(jar);
                    fail.newLine();
                    failed++;
                    continue;
                }

                JSONObject doc = docs.getJSONObject(0);
                String groupId = doc.getString("g");

                out.write("<!-- " + jar + " -->\n");
                out.write("<dependency>\n");
                out.write("    <groupId>" + groupId + "</groupId>\n");
                out.write("    <artifactId>" + artifactId + "</artifactId>\n");
                out.write("    <version>" + version + "</version>\n");
                out.write("</dependency>\n\n");

                resolved++;
            }
        }

        System.out.println("===== Maven Dependency Generation Summary =====");
        System.out.println("Total jars processed : " + total);
        System.out.println("Resolved successfully: " + resolved);
        System.out.println("Failed / unresolved  : " + failed);
        System.out.println("Output file          : generated-dependencies.xml");
        System.out.println("Unresolved jars file : unresolved-jars.txt");
        System.out.println("================================================");
    }
}

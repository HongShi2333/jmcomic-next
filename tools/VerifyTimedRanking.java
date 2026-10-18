import com.par9uet.jm.network.TimedComicWebSearchKt;
import okhttp3.HttpUrl;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.HexFormat;
import java.util.stream.Collectors;

/**
 * Read-only live check against the already compiled production HTML parser.
 * stdin: label<TAB>url<TAB>base64(query)<TAB>base64(html), one response per line.
 * Does not fetch images, access accounts, or rebuild the application.
 */
class VerifyTimedRanking {
    public static void main(String[] args) throws Exception {
        var input = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));
        String line;
        while ((line = input.readLine()) != null) {
            if (line.isBlank()) continue;
            var fields = line.split("\\t", 4);
            if (fields.length != 4) throw new IllegalArgumentException("Expected four fields");
            var query = new String(Base64.getDecoder().decode(fields[2]), StandardCharsets.UTF_8);
            var html = new String(Base64.getDecoder().decode(fields[3]), StandardCharsets.UTF_8);
            var result = TimedComicWebSearchKt.parseTimedComicSearchPage(
                html, HttpUrl.Companion.get(fields[1]), query
            );
            var ids = result.getContent().stream().map(item -> item.getId()).collect(Collectors.joining(","));
            var digest = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(ids.getBytes(StandardCharsets.UTF_8)));
            System.out.printf("%s total=%s count=%d first=%s sha256=%s%n", fields[0], result.getTotal(),
                result.getContent().size(),
                result.getContent().stream().limit(5).map(item -> item.getId()).collect(Collectors.joining(",")),
                digest);
        }
    }
}

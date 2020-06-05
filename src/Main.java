import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Main {
    public static String escapeSpecialCharacters(String data) {
        String escapedData = data.replaceAll("\\R", " ");
        if (data.contains(",") || data.contains("\"") || data.contains("'")) {
            data = data.replace("\"", "\"\"");
            escapedData = "\"" + data + "\"";
        }
        return escapedData;
    }

    public static String convertToCSV(String[] data) {
        return Stream.of(data)
                .map(Main::escapeSpecialCharacters)
                .collect(Collectors.joining(","));
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        List<String[]> dataLines = new ArrayList<>();
        dataLines.add(new String[]{"unused", "unused", "topic", "songFragment"});
        for (int i = 1; i <= 100; i++) {
            for (int topic = 0; topic < 2; topic++) {
                URL url = new URL("https://teksciory.interia.pl/szukaj");
                HttpURLConnection con = (HttpURLConnection) url.openConnection();
                con.setRequestMethod("GET");

                Map<String, String> parameters = new HashMap<>();
                parameters.put("page", Integer.toString(i));
                if (topic == 0)
                    parameters.put("q", "pieniądze");
                else
                    parameters.put("q", "miłość");
                parameters.put("t", "lyric");
                parameters.put("sort", "score");
                parameters.put("dr", "all");

                con.setDoOutput(true);
                DataOutputStream out = new DataOutputStream(con.getOutputStream());
                out.writeBytes(ParameterStringBuilder.getParamsString(parameters));
                out.flush();
                out.close();

                con.setConnectTimeout(5000);
                con.setReadTimeout(5000);

                int status = con.getResponseCode();

                BufferedReader in = new BufferedReader(
                        new InputStreamReader(con.getInputStream()));
                String inputLine;
                StringBuffer content = new StringBuffer();
                while ((inputLine = in.readLine()) != null) {
                    content.append(inputLine).append(" ");
                }
                in.close();

                con.disconnect();

                Pattern pattern = Pattern.compile("<ul>(.*?)</ul>");
                Matcher matcher = pattern.matcher(content);

                String responseRecords = "";
                if (matcher.find())
                    responseRecords = matcher.group(1);
                pattern = Pattern.compile("<li>(.*?)</li>");
                matcher = pattern.matcher(responseRecords);
//            System.out.println(responseRecords);
                Pattern songTextPattern = Pattern.compile("<div>\"(.*?)\"</div>");
                Matcher songTextMatcher;
                while (matcher.find()) {
                    String responseRecord = matcher.group(1);
//                    System.out.println(responseRecord);
                    songTextMatcher = songTextPattern.matcher(responseRecord);
                    if (songTextMatcher.find()) {
                        if (topic == 0)
                            dataLines.add(new String[]{"", "", "money", songTextMatcher.group(1)});
                        else
                            dataLines.add(new String[]{"", "", "love", songTextMatcher.group(1)});
                    }
                }
                Thread.sleep(2000);
            }
            System.out.println(i + "%");
        }
        File csvOutputFile = new File("selected_songs2.csv");
        try (PrintWriter pw = new PrintWriter(csvOutputFile)) {
            dataLines.stream()
                    .map(Main::convertToCSV)
                    .forEach(pw::println);
        }
    }
}

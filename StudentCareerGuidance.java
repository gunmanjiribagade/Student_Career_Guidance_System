import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.file.Files;
import java.nio.file.Paths;

/*
 * Student Career Guidance System - Rule-Based Expert System
 * Backend : Core Java (rules + small web server built into the JDK)
 * Frontend: index.html + style.css (HTML and CSS only)
 */
public class StudentCareerGuidance {

    public static void main(String[] args) throws IOException {
        // Start a simple web server on port 8080
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
        server.createContext("/", new PageHandler());        // shows the form and CSS
        server.createContext("/result", new ResultHandler()); // gives the career result
        server.start();

        System.out.println("Server started!");
        System.out.println("Open this link in your browser: http://localhost:8080");
        System.out.println("Press Ctrl + C to stop the server.");
    }

    // ---------- Handler 1: sends index.html and style.css ----------
    static class PageHandler implements HttpHandler {
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();

            if (path.equals("/") || path.equals("/index.html")) {
                sendFile(exchange, "index.html", "text/html");
            } else if (path.equals("/style.css")) {
                sendFile(exchange, "style.css", "text/css");
            } else {
                sendText(exchange, 404, "Page not found");
            }
        }
    }

    // ---------- Handler 2: reads the form, applies the rules ----------
    static class ResultHandler implements HttpHandler {
        public void handle(HttpExchange exchange) throws IOException {
            String query = exchange.getRequestURI().getRawQuery();

            // Read the values sent by the form
            String name = getValue(query, "name");
            int interest = toChoice(getValue(query, "interest"));
            int skill = toChoice(getValue(query, "skill"));
            int strength = toChoice(getValue(query, "strength"));

            // Basic input validation
            if (name.isEmpty() || !name.matches("[a-zA-Z ]+")
                    || interest == 0 || skill == 0 || strength == 0) {
                sendText(exchange, 200, makePage("Invalid Input",
                        "<p class=\"error\">Please enter a valid name (letters and spaces only) "
                        + "and answer all 3 questions.</p>"
                        + "<a class=\"back\" href=\"/\">Go back</a>"));
                return;
            }

            // IF-THEN RULES
            // 1 = Software Developer, 2 = Web Developer, 3 = Data Analyst,
            // 4 = Cybersecurity Analyst, 5 = Network Engineer
            int career;
            String ruleUsed;

            if (interest == skill) {
                career = interest;
                ruleUsed = "Rule 1: Your interest and your technical skill point to the same career.";
            } else if (skill == strength) {
                career = skill;
                ruleUsed = "Rule 2: Your technical skill and your strength point to the same career.";
            } else if (interest == strength) {
                career = interest;
                ruleUsed = "Rule 3: Your interest and your strength point to the same career.";
            } else {
                career = skill;
                ruleUsed = "Rule 4: Your answers are mixed, so your technical skill decides.";
            }

            // Build the result page
            String body = "<h2>Result for " + name + "</h2>"
                    + "<p class=\"career\">" + getCareerName(career) + "</p>"
                    + "<h3>Why this career?</h3>"
                    + "<ul>"
                    + "<li>Interest: " + getInterestName(interest) + "</li>"
                    + "<li>Skill: " + getSkillName(skill) + "</li>"
                    + "<li>Strength: " + getStrengthName(strength) + "</li>"
                    + "<li>" + ruleUsed + "</li>"
                    + "<li>" + getReason(career) + "</li>"
                    + "</ul>"
                    + "<h3>Alternative careers</h3>"
                    + "<p>" + getAlternatives(career) + "</p>"
                    + "<a class=\"back\" href=\"/\">Try again</a>";

            sendText(exchange, 200, makePage("Your Career Result", body));
        }
    }

    // ---------- Helper methods ----------

    // Wraps the content in a full HTML page that uses style.css
    static String makePage(String title, String body) {
        return "<!DOCTYPE html><html lang=\"en\"><head><meta charset=\"UTF-8\">"
                + "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">"
                + "<title>" + title + "</title>"
                + "<link rel=\"stylesheet\" href=\"/style.css\"></head><body>"
                + "<header><h1>Student Career Guidance System</h1></header>"
                + "<main><section>" + body + "</section></main>"
                + "</body></html>";
    }

    // Gets one value from the query, for example name=Rahul&skill=2
    static String getValue(String query, String key) throws IOException {
        if (query == null) {
            return "";
        }
        String[] pairs = query.split("&");
        for (String pair : pairs) {
            String[] parts = pair.split("=", 2);
            if (parts.length == 2 && parts[0].equals(key)) {
                return URLDecoder.decode(parts[1], "UTF-8").trim();
            }
        }
        return "";
    }

    // Converts text to a number from 1 to 5, or 0 if it is not valid
    static int toChoice(String text) {
        try {
            int number = Integer.parseInt(text);
            if (number >= 1 && number <= 5) {
                return number;
            }
        } catch (NumberFormatException e) {
            // not a number
        }
        return 0;
    }

    // Sends a file (index.html or style.css) to the browser
    static void sendFile(HttpExchange exchange, String fileName, String type) throws IOException {
        if (!Files.exists(Paths.get(fileName))) {
            sendText(exchange, 404, fileName + " not found. Run the program from the project folder.");
            return;
        }
        byte[] data = Files.readAllBytes(Paths.get(fileName));
        exchange.getResponseHeaders().set("Content-Type", type + "; charset=UTF-8");
        exchange.sendResponseHeaders(200, data.length);
        OutputStream out = exchange.getResponseBody();
        out.write(data);
        out.close();
    }

    // Sends text or HTML to the browser
    static void sendText(HttpExchange exchange, int code, String text) throws IOException {
        byte[] data = text.getBytes("UTF-8");
        exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
        exchange.sendResponseHeaders(code, data.length);
        OutputStream out = exchange.getResponseBody();
        out.write(data);
        out.close();
    }

    // ---------- switch methods ----------

    static String getCareerName(int career) {
        switch (career) {
            case 1: return "Software Developer";
            case 2: return "Web Developer";
            case 3: return "Data Analyst";
            case 4: return "Cybersecurity Analyst";
            default: return "Network Engineer";
        }
    }

    static String getInterestName(int interest) {
        switch (interest) {
            case 1: return "Writing programs and building software";
            case 2: return "Designing websites";
            case 3: return "Working with data and numbers";
            case 4: return "Protecting systems from hackers";
            default: return "Computer networks and the internet";
        }
    }

    static String getSkillName(int skill) {
        switch (skill) {
            case 1: return "Programming (Java / C / Python)";
            case 2: return "HTML, CSS and JavaScript";
            case 3: return "Excel, SQL and statistics";
            case 4: return "Linux and security tools";
            default: return "Networking basics";
        }
    }

    static String getStrengthName(int strength) {
        switch (strength) {
            case 1: return "Logical thinking";
            case 2: return "Creativity";
            case 3: return "Analysing information";
            case 4: return "Attention to detail and curiosity";
            default: return "Patience in fixing problems";
        }
    }

    static String getReason(int career) {
        switch (career) {
            case 1: return "Software Developers write programs, so logic and coding skills are very important.";
            case 2: return "Web Developers build websites, which needs both technical and creative skills.";
            case 3: return "Data Analysts study data to find useful patterns and need strong analytical skills.";
            case 4: return "Cybersecurity Analysts protect systems and need curiosity and attention to detail.";
            default: return "Network Engineers manage networks and need patience to find and fix problems.";
        }
    }

    static String getAlternatives(int career) {
        switch (career) {
            case 1: return "Web Developer, Data Analyst";
            case 2: return "Software Developer, Data Analyst";
            case 3: return "Software Developer, Cybersecurity Analyst";
            case 4: return "Network Engineer, Software Developer";
            default: return "Cybersecurity Analyst, Software Developer";
        }
    }
}

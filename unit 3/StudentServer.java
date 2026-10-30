import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class StudentServer {

    public static void main(String[] args) throws Exception {

        HttpServer server = HttpServer.create(
            new InetSocketAddress(8080), 0
        );

        server.createContext("/", StudentServer::handleFiles);

        server.createContext("/students", StudentServer::handleStudents);

        server.start();

        System.out.println("Server started at http://localhost:8080");
    }

    static void handleFiles(HttpExchange exchange) throws IOException {

        String path = exchange.getRequestURI().getPath();

        if (path.equals("/")) {
            path = "/index.html";
        }

        Path file = Path.of("web" + path);

        if (!Files.exists(file)) {
            sendResponse(exchange, 404, "404 Not Found");
            return;
        }

        String contentType = "text/html";

        if (path.endsWith(".css")) {
            contentType = "text/css";
        }

        if (path.endsWith(".js")) {
            contentType = "application/javascript";
        }

        byte[] content = Files.readAllBytes(file);

        exchange.getResponseHeaders()
            .set("Content-Type", contentType);

        exchange.sendResponseHeaders(200, content.length);

        OutputStream os = exchange.getResponseBody();

        os.write(content);

        os.close();
    }

    static void handleStudents(HttpExchange exchange) throws IOException {

        String method = exchange.getRequestMethod();

        try {

            if (method.equals("GET")) {

                fetchStudents(exchange);

            } else if (method.equals("POST")) {

                insertStudent(exchange);

            } else if (method.equals("DELETE")) {

                deleteStudent(exchange);

            } else {

                sendResponse(exchange, 405, "Method not allowed");
            }

        } catch (Exception e) {

            e.printStackTrace();

            sendResponse(exchange, 500, e.getMessage());
        }
    }

    static void fetchStudents(HttpExchange exchange) throws Exception {

        Connection con = DBConnect.getConnection();

        String sql = "SELECT * FROM student";

        PreparedStatement ps = con.prepareStatement(sql);

        ResultSet rs = ps.executeQuery();

        StringBuilder json = new StringBuilder("[");

        while (rs.next()) {

            json.append("{")
                .append("\"id\":").append(rs.getInt("id")).append(",")
                .append("\"name\":\"")
                .append(rs.getString("name"))
                .append("\",")
                .append("\"email\":\"")
                .append(rs.getString("email"))
                .append("\"")
                .append("},");
        }

        if (json.length() > 1) {
            json.deleteCharAt(json.length() - 1);
        }

        json.append("]");

        con.close();

        exchange.getResponseHeaders()
            .set("Content-Type", "application/json");

        sendResponse(exchange, 200, json.toString());
    }

    static void insertStudent(HttpExchange exchange) throws Exception {

        String body = new String(
            exchange.getRequestBody().readAllBytes(),
            StandardCharsets.UTF_8
        );

        String[] data = body.split("&");

        String name = data[0].split("=")[1];
        String email = data[1].split("=")[1];

        Connection con = DBConnect.getConnection();

        String sql =
            "INSERT INTO student (name, email) VALUES (?, ?)";

        PreparedStatement ps = con.prepareStatement(sql);

        ps.setString(1, name);
        ps.setString(2, email);

        ps.executeUpdate();

        con.close();

        sendResponse(
            exchange,
            200,
            "Student inserted successfully"
        );
    }

    static void deleteStudent(HttpExchange exchange) throws Exception {

        String query =
            exchange.getRequestURI().getQuery();

        String id = query.split("=")[1];

        Connection con = DBConnect.getConnection();

        String sql =
            "DELETE FROM student WHERE id = ?";

        PreparedStatement ps =
            con.prepareStatement(sql);

        ps.setInt(
            1,
            Integer.parseInt(id)
        );

        int rows = ps.executeUpdate();

        con.close();

        if (rows > 0) {

            sendResponse(
                exchange,
                200,
                "Student deleted successfully"
            );

        } else {

            sendResponse(
                exchange,
                404,
                "Student not found"
            );
        }
    }

    static void sendResponse(
        HttpExchange exchange,
        int status,
        String response
    ) throws IOException {

        byte[] bytes =
            response.getBytes(StandardCharsets.UTF_8);

        exchange.sendResponseHeaders(
            status,
            bytes.length
        );

        OutputStream os =
            exchange.getResponseBody();

        os.write(bytes);

        os.close();
    }
}
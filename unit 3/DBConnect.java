import java.sql.Connection;
import java.sql.DriverManager;

public class DBConnect {

    public static Connection getConnection() throws Exception {

        Class.forName("com.mysql.cj.jdbc.Driver");

        String url = "jdbc:mysql://localhost:3306/college";
        String username = "root";
        String password = "1234567890";

        return DriverManager.getConnection(url, username, password);
    }

    public static void main(String[] args) {

        try {

            Connection con = getConnection();

            System.out.println("Database connected successfully!");

            con.close();

        } catch (Exception e) {

            System.out.println(e);

        }
    }
}
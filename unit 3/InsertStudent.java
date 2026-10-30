import java.sql.Connection;
import java.sql.PreparedStatement;

public class InsertStudent {

    public static void main(String[] args) {

        try {
            Connection con = DBConnect.getConnection();

            String sql = "INSERT INTO student (name, email) VALUES (?, ?)";

            PreparedStatement ps = con.prepareStatement(sql);

            ps.setString(1, "Aryan");
            ps.setString(2, "aryan@gmail.com");

            int rows = ps.executeUpdate();

            if (rows > 0) {
                System.out.println("Student inserted successfully!");
            }

            con.close();

        } catch (Exception e) {
            System.out.println(e);
        }
    }
}
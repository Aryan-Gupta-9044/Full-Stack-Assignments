import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class FetchStudents {

    public static void main(String[] args) {

        try {
            Connection con = DBConnect.getConnection();

            String sql = "SELECT * FROM student";

            PreparedStatement ps = con.prepareStatement(sql);

            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                System.out.println(
                    rs.getInt("id") + " " +
                    rs.getString("name") + " " +
                    rs.getString("email")
                );
            }

            con.close();

        } catch (Exception e) {
            System.out.println(e);
        }
    }
}
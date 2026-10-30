import java.sql.Connection;
import java.sql.PreparedStatement;

public class DeleteStudent {

    public static void main(String[] args) {

        try {
            Connection con = DBConnect.getConnection();

            String sql = "DELETE FROM student WHERE id = ?";

            PreparedStatement ps = con.prepareStatement(sql);

            ps.setInt(1, 1);

            int rows = ps.executeUpdate();

            if (rows > 0) {
                System.out.println("Student deleted successfully!");
            } else {
                System.out.println("Student not found!");
            }

            con.close();

        } catch (Exception e) {
            System.out.println(e);
        }
    }
}
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class TestConnection {
    public static void main(String[] args) {
        String url = "jdbc:postgresql://localhost:5432/supplysight";
        String user = "supplysight";
        String password = "supplysight_secret_new";

        if (args.length > 0) url = args[0];
        if (args.length > 1) user = args[1];
        if (args.length > 2) password = args[2];

        System.out.println("Testing connection to: " + url);
        System.out.println("User: " + user);
        
        try (Connection conn = DriverManager.getConnection(url, user, password)) {
            System.out.println("SUCCESS: Connection established!");
        } catch (SQLException e) {
            System.out.println("FAILURE: " + e.getMessage());
            e.printStackTrace();
        }
    }
}

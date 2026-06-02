package db;

import config.Constantes;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class ConexionBD {
    private static Connection conexion = null;

    public static Connection getConexion() throws SQLException {
        // Si no existe la conexión o se cerró, la crea una sola vez
        if (conexion == null || conexion.isClosed()) {
            String url = Constantes.ORACLE_URL;
            String user = Constantes.USERNAME;
            String password = Constantes.PASSWORD;
            conexion = DriverManager.getConnection(url, user, password);
        }
        return conexion;
    }

    public static void cerrarConexion() {
        if (conexion != null) {
            try {
                conexion.close();
            } catch (SQLException e) {
                System.err.println("Error al cerrar la conexión global: " + e.getMessage());
            }
        }
    }
}
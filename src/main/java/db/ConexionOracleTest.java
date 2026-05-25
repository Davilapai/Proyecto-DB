package db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import config.Constantes;

public class ConexionOracleTest
{
    public static void main(String[] args) 
    {
        String SQL = "select book_id, title, author_last_name, author_first_name, rating from books";
        try 
        {
            // Contiene la referencia a la base de datos Oracle
            Connection conex = DriverManager.getConnection
            (
                Constantes.ORACLE_URL,
                Constantes.USERNAME,
                Constantes.PASSWORD
            );

            System.out.println("Conectado a Oracle");

            PreparedStatement ps = conex.prepareStatement(SQL);
            ResultSet rs = ps.executeQuery();

            boolean hayDatos = false;
            while (rs.next()) 
            {
                hayDatos = true;
                System.out.println("BOOK_ID: " + rs.getInt("BOOK_ID"));
                System.out.println("TITLE: " + rs.getString("TITLE"));
                System.out.println("AUTHOR_LAST_NAME: " + rs.getString("AUTHOR_LAST_NAME"));
                System.out.println("AUTHOR_FIRST_NAME: " + rs.getString("AUTHOR_FIRST_NAME"));
                System.out.println("RATING: " + rs.getBigDecimal("RATING"));
                System.out.println("-------------------");
            }

            if (!hayDatos) {
                System.out.println("La consulta se ejecutó, pero no devolvió resultados.");
                System.out.println("Asegúrate de haber ejecutado 'COMMIT;' en tu cliente SQL después de hacer los INSERT.");
            }

            // Importante para cerrar la conexión
            conex.close();

        } 
        catch (Exception e) 
        {
            e.printStackTrace();
        }
    }
}
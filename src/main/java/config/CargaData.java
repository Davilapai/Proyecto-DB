package config;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

import oracle.jdbc.Const;

public class CargaData 
{
    public static void cargarData() 
    {
        String url = Constantes.ORACLE_URL;
        String user = Constantes.USERNAME;
        String password = Constantes.PASSWORD;

        try
        (
            // Contiene la referencia a la base de datos Oracle
            Connection conn = DriverManager.getConnection(url, user, password);
            Statement stmt = conn.createStatement();
        )
        {

            // Opcional: manejar commits manualmente
            conn.setAutoCommit(false);

            InputStream input = CargaData.class
                    .getClassLoader()
                    .getResourceAsStream("data.sql");

            if (input == null) 
            {
                throw new RuntimeException("No se encontro data.sql");
            }

            String sql = new String(input.readAllBytes());  

            // Quitar comentarios simples --
            StringBuilder limpio = new StringBuilder();

            for (String linea : sql.split("\n")) 
            {

                linea = linea.trim();

                if (!linea.startsWith("--")) 
                {
                    limpio.append(linea).append("\n");
                }
            }

            // Separar instrucciones
            String[] sentencias = limpio.toString().split(";");

            for (String sentencia : sentencias) 
            {

                sentencia = sentencia.trim();

                if (sentencia.isEmpty()) 
                {
                    continue;
                }

                try 
                {

                    stmt.execute(sentencia);

                } 
                catch (Exception e) 
                {

                    // Ignorar DROP TABLE si la tabla no existe
                    if (!sentencia.toUpperCase().startsWith("DROP TABLE")) 
                    {
                        System.out.println("Error ejecutando:");
                        System.out.println(sentencia);

                        e.printStackTrace();

                        conn.rollback();
                        return;
                    }
                }
            }

            conn.commit();

        } 
        catch (Exception e) 
        {
            e.printStackTrace();
        }
        System.out.println("Ya maricon");
    }
}

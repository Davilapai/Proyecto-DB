package config;

import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import org.apache.ibatis.jdbc.ScriptRunner;

public class CargaData 
{
    public static void cargarData(Connection conn) 
    {
        try
        {
            ScriptRunner runner = new ScriptRunner(conn);

            // CONFIGURACIÓN CRÍTICA PARA TRIGGERS Y BLOQUES PL/SQL
            runner.setDelimiter("/"); // Define la barra como el separador maestro
            runner.setFullLineDelimiter(true); // Indica que el '/' debe estar solo en su propia línea

            runner.setStopOnError(true); 
            runner.setLogWriter(null); 
            runner.setErrorLogWriter(new java.io.PrintWriter(System.err)); 

            Reader reader = Files.newBufferedReader(Paths.get("src/main/java/config/scripts/data.sql"));

            runner.runScript(reader);

        } 
        catch (Exception e) 
        {
            System.err.println("Error critico durante la ejecucion del script:");
            e.printStackTrace();
        }
    }
}
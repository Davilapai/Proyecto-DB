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

            conex.close();

        } 
        catch (Exception e) 
        {
            e.printStackTrace();
        }
    }
}
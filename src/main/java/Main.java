import java.sql.Connection;
import java.util.Scanner;

import db.ConexionBD;
import db.ClienteDAO;

public class Main {
    static Scanner sc = new Scanner(System.in);
    public static void main(String[] args) 
    {   
        Connection conn;
        try
        {
           conn = ConexionBD.getConexion();
        }
        catch(Exception e)
        {
            System.out.println(e);
            return;
        }
        
        ClienteDAO.verificarInstalacion(conn);
        System.out.println("Bienvenido a FanExperience");
        System.out.println("Elige el modo de ingreso para el cliente");
        System.out.println("\t 1. Ingresar con número de documento");
        System.out.println("\t 2. Ingresar con codigo de reserva");
        System.out.print("Su opcion: ");
        int opcion = 0;

        while (true)
        {
            try
            {
                opcion = sc.nextInt();
                if(opcion != 1 && opcion != 2)
                {
                    throw new IllegalArgumentException("Opcion no valida");
                }
                sc.nextLine();
                break;
            }
            catch (Exception e) 
            {
                System.out.print("Opcion invalida, ingrese nuevamente: ");
                sc.nextLine(); 
            }  
        }

        int lim = (opcion == 1) ? 10 : 6;
        String acceso = (opcion == 1) ? "documento de identidad" : "codigo de reserva";
  
        System.out.print("Ingrese el " + acceso + ": ");
        String verificarS = "";
        long codigoRes;

        while (true) 
        {
            try
            {
                verificarS = sc.nextLine();
                
                if(verificarS.length() != lim)
                {
                    throw new Exception("Longitud incorrecta para " + acceso);
                }
                
                if (!verificarS.matches("[0-9]+")) 
                {
                    throw new IllegalArgumentException("El " + acceso + " no puede contener letras ni caracteres especiales.");
                }

                codigoRes = Long.parseLong(verificarS);

                break;
            }
            catch (Exception e) 
            {   
                System.out.println(e.getMessage());
                System.out.print("Ingrese nuevamente: ");
            } 
        }

        boolean encontrado = ClienteDAO.ConsultaIngreso(lim, conn, verificarS);

        if(!encontrado && opcion == 2)
        {
            System.out.println("El codigo de reserva ingresado no existe");
        }
        else if (!encontrado && opcion == 1)
        {
            System.out.println("No hay una persona registrada con esa cedula");
            System.out.println("¿Desea agregarla al sistema? (s/n)");
            System.out.print("Su opcion: ");

            String sn = sc.nextLine().toLowerCase().trim();

            if (sn.equals("si") || sn.equals("s") || sn.equals("sí"))
            { 
                ClienteDAO.AgregarPersona(conn, sc, codigoRes);
            }
            else 
            {
                System.out.println("Regresando al menu de inicio...");
            }
        }
        else 
        {
            System.out.println("Ingreso exitoso. ¡Bienvenido!");
            // Continuar al flujo del cliente existente...
        }
    }
}
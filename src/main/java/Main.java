import java.util.Scanner;

import config.CargaData;

public class Main {
    static Scanner sc = new Scanner(System.in);
    public static void main(String[] args) {
            System.out.println("Bienvenido a FanExperience");
            System.out.println("Elige el modo de ingreso para el cliente");
            System.out.println("\t 1. Ingresar con número de documento");
            System.out.println("\t 2. Ingresar con codigo de reserva");
            System.out.print("Su opcion: ");

            int opcion = 0;
            opcion = sc.nextInt();

            CargaData.cargarData();
    }
}
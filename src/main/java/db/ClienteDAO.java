package db;

import java.io.File;
import config.CargaData;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.sql.Statement;

public class ClienteDAO 
{
    public static void verificarInstalacion(Connection conn) 
    {
        File flag = new File("src\\main\\java\\config\\scripts\\configurado.flag");

        if (!flag.exists()) 
        {
            try 
            {
                CargaData.cargarData(conn);
                flag.createNewFile();
                System.out.println("Base de datos inicializada.");
            } 
            catch (Exception e) 
            {
                System.out.println("Error al inicializar la base de datos.");
                e.printStackTrace();
            }
        }
        else
        {
            System.out.println("La base de datos ya se ha inicializado");
        }
    }

    public static boolean ConsultaIngreso(int longitud, Connection conn, String value) 
    {
        String tabla = (longitud == 10) ? "cliente" : "inscripcion";
        String columna = (longitud == 10) ? "numero_documento" : "id";

        String sql = "SELECT 1 FROM " + tabla + " WHERE " + columna + " = ? AND ROWNUM <= 1";

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) 
        {
            pstmt.setString(1, value);

            try (ResultSet rs = pstmt.executeQuery()) 
            {
                return rs.next();
            }
        } 
        catch (Exception e) 
        {
            System.err.println("Error al ejecutar la consulta de ingreso:");
            e.printStackTrace();
            return false;
        }
    }


    public static void AgregarPersona(Connection conn, Scanner sc, long numeroDoc)
    {
        // 1. CAPTURA DE DATOS PERSONALES CON VALIDACIÓN
        System.out.println("\n--- REGISTRO DE NUEVO CLIENTE ---");
        String tipoDoc;
        while (true) {
            System.out.print("Tipo de documento (CC, TI, CE, PA): ");
            tipoDoc = sc.nextLine().toUpperCase().trim();
            if (tipoDoc.equals("CC") || tipoDoc.equals("TI") || tipoDoc.equals("CE") || tipoDoc.equals("PA")) {
                break;
            }
            System.out.println("Tipo invalido. Intente de nuevo.");
        }

        System.out.print("Nombre completo: ");
        String nombreComp = sc.nextLine().trim();

        System.out.print("Correo electrónico: ");
        String email = sc.nextLine().trim();

        String telefono;
        while (true) {
            System.out.print("Teléfono: ");
            telefono = sc.nextLine().trim();
            if (telefono.matches("[0-9]+")) {
                break;
            }
            System.out.println("Error: El telefono solo puede contener numeros. Intente nuevamente.");
        }

        // 2. SELECCIÓN Y FILTRO DE EXPERIENCIAS
        System.out.println("\n¿Desea buscar experiencias filtradas por alguna seleccion nacional? (s/n)");
        System.out.print("Su opcion: ");
        String filtroSn = sc.nextLine().toLowerCase().trim();
        String seleccionBuscada = "";
        if (filtroSn.equals("s") || filtroSn.equals("si")) {
            System.out.print("Escriba el nombre de la seleccion (ej. Mexico, Colombia): ");
            seleccionBuscada = sc.nextLine().trim();
        }

        // 3. MOSTRAR CATÁLOGO DE EXPERIENCIAS
        String sqlBuscarExp = "SELECT e.id, e.descripcion, e.precio_base, e.cupos_disponibles, s1.nombre AS loc, s2.nombre AS vis " +
                            "FROM experiencia e " +
                            "JOIN partido p ON e.id_partido = p.id " +
                            "JOIN seleccion s1 ON p.id_seleccion_local = s1.id " +
                            "JOIN seleccion s2 ON p.id_seleccion_visitante = s2.id " +
                            "WHERE e.estado = 'DISPONIBLE'";
        
        if (!seleccionBuscada.isEmpty()) {
            sqlBuscarExp += " AND (UPPER(s1.nombre) LIKE ? OR UPPER(s2.nombre) LIKE ?)";
        }

        try (PreparedStatement pstmtExp = conn.prepareStatement(sqlBuscarExp)) {
            if (!seleccionBuscada.isEmpty()) {
                String busq = "%" + seleccionBuscada.toUpperCase() + "%";
                pstmtExp.setString(1, busq);
                pstmtExp.setString(2, busq);
            }

            try (ResultSet rsExp = pstmtExp.executeQuery()) {
                System.out.println("\n--- EXPERIENCIAS DISPONIBLES MUNDIAL 2026 ---");
                System.out.printf("%-5s %-35s %-15s %-10s %-25s\n", "ID", "Descripcion", "Precio Base", "Cupos", "Partido");
                while (rsExp.next()) {
                    System.out.printf("%-5d %-35s $%-14.2f %-10d %-25s\n",
                            rsExp.getInt("id"), rsExp.getString("descripcion"),
                            rsExp.getDouble("precio_base"), rsExp.getInt("cupos_disponibles"),
                            rsExp.getString("loc") + " vs " + rsExp.getString("vis"));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error al listar experiencias: " + e.getMessage());
            return;
        }

        System.out.print("\nIngrese el ID de la experiencia que desea reservar: ");
        int idExpElegida = sc.nextInt();
        sc.nextLine(); // Limpiar buffer

        // 4. MOSTRAR Y SELECCIONAR SERVICIOS OPCIONALES
        List<Integer> serviciosDisponiblesIds = new ArrayList<>();
        String sqlServicios = "SELECT es.id, s.nombre, s.costo FROM experiencia_servicio es " +
                            "JOIN servicio s ON es.id_servicio = s.id WHERE es.id_experiencia = ?";
        
        try (PreparedStatement pstmtServ = conn.prepareStatement(sqlServicios)) {
            pstmtServ.setInt(1, idExpElegida);
            try (ResultSet rsServ = pstmtServ.executeQuery()) {
                System.out.println("\n--- SERVICIOS ADICIONALES DISPONIBLES ---");
                while (rsServ.next()) {
                    int idExpServ = rsServ.getInt("id");
                    serviciosDisponiblesIds.add(idExpServ);
                    System.out.printf("ID: %d | %-25s | Costo: $%.2f\n", idExpServ, rsServ.getString("nombre"), rsServ.getDouble("costo"));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error al listar servicios: " + e.getMessage());
        }

        List<Integer> serviciosElegidosIds = new ArrayList<>();
        while (true) {
            System.out.print("Ingrese el ID del servicio que quiera (Marque 0 para no agregar otro mas): ");
            try {
                int idServicioElegido = sc.nextInt();
                sc.nextLine(); 

                if (idServicioElegido == 0) {
                    break;
                }

                if (serviciosDisponiblesIds.contains(idServicioElegido)) {
                    if (!serviciosElegidosIds.contains(idServicioElegido)) {
                        serviciosElegidosIds.add(idServicioElegido);
                        System.out.println("Servicio ID " + idServicioElegido + " agregado a la lista.");
                    } else {
                        System.out.println("Este servicio ya fue seleccionado anteriormente.");
                    }
                } else {
                    System.out.println("ID de servicio no valido para esta experiencia. Intente de nuevo.");
                }
            } catch (Exception e) {
                System.out.println("Entrada invalida. Por favor ingrese un numero valido.");
                sc.nextLine(); 
            }
        }

        // =================================================================
        // NUEVA SECCIÓN: CÁLCULO ANTICIPADO DEL TOTAL LIQUIDADO EN JAVA
        // =================================================================
        double totalLiquidado = 0;

        // Obtener precio base de la experiencia
        String sqlPrecioBase = "SELECT precio_base FROM experiencia WHERE id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sqlPrecioBase)) {
            pstmt.setInt(1, idExpElegida);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    totalLiquidado = rs.getDouble("precio_base");
                }
            }
        } catch (SQLException e) {
            System.err.println("Error al calcular el precio base: " + e.getMessage());
        }

        // Sumar el costo de los servicios seleccionados con sus respectivos impuestos (Misma logica del Trigger)
        String sqlCostoServicioConImpuesto = 
            "SELECT s.costo, NVL(SUM(imp.porcentaje), 0) AS porcentaje " +
            "FROM experiencia_servicio es " +
            "JOIN servicio s ON es.id_servicio = s.id " +
            "LEFT JOIN servicio_impuesto si ON si.id_servicio = s.id " +
            "LEFT JOIN impuesto imp ON imp.id = si.id_impuesto " +
            "WHERE es.id = ? " +
            "GROUP BY s.costo";

        for (int idExpServ : serviciosElegidosIds) {
            try (PreparedStatement pstmt = conn.prepareStatement(sqlCostoServicioConImpuesto)) {
                pstmt.setInt(1, idExpServ);
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        double costo = rs.getDouble("costo");
                        double porcentajeImpuesto = rs.getDouble("porcentaje");
                        totalLiquidado += costo * (1 + porcentajeImpuesto / 100);
                    }
                }
            } catch (SQLException e) {
                System.err.println("Error al calcular costo de servicios: " + e.getMessage());
            }
        }

        // 5. CAPTURA DEL MODO DE PAGO
        System.out.println("\nSeleccione el metodo de pago:");
        System.out.println("1. TARJETA CREDITO | 2. TARJETA DEBITO | 3. EFECTIVO | 4. BILLETERA DIGITAL");
        System.out.print("Su opcion (1-4): ");
        int opPago = sc.nextInt();
        sc.nextLine(); 

        String metodoPagoText = "EFECTIVO";
        if (opPago == 1) metodoPagoText = "TARJETA CREDITO";
        else if (opPago == 2) metodoPagoText = "TARJETA DEBITO";
        else if (opPago == 4) metodoPagoText = "BILLETERA DIGITAL";

        // =================================================================
        // NUEVA SECCIÓN: VALIDACIÓN ESTRICTA DEL ABONO
        // =================================================================
        System.out.printf("\nEl valor total de su inscripcion (con impuestos) es de: $%.2f\n", totalLiquidado);
        
        double abono = 0;
        while (true) {
            System.out.print("Ingrese el valor a abonar como pago inicial: $");
            try {
                abono = sc.nextDouble();
                sc.nextLine(); 

                if (abono <= 0) {
                    System.out.println("Error: El pago inicial debe ser obligatorio (mayor a $0.00).");
                    continue;
                }
                if (abono > totalLiquidado) {
                    System.out.printf("Error: El abono no puede ser superior al valor total liquidado ($%.2f).\n", totalLiquidado);
                    continue;
                }
                break; 
            } catch (Exception e) {
                System.out.println("Entrada invalida. Por favor introduzca un monto numerico.");
                sc.nextLine(); 
            }
        }

        // 6. EJECUCIÓN DE TRANSACCIÓN EN LA BASE DE DATOS
        String sqlCliente = "INSERT INTO cliente (tipo_documento, numero_documento, nombre_completo, correo, telefono) VALUES (?, ?, ?, ?, ?)";
        String sqlHistorial = "INSERT INTO historial_cliente (id_cliente, id_inscripcion, descripcion) VALUES (?, null, 'Registro inicial del cliente.')";
        String sqlInscripcion = "INSERT INTO inscripcion (id_cliente, id_experiencia, precio_total, saldo_pendiente, fecha) VALUES (?, ?, 0, 0, CURRENT_TIMESTAMP)";
        String sqlInsServicio = "INSERT INTO inscripcion_servicio (id_inscripcion, id_exp_servicio, cantidad, costo_unitario) " +
                                "VALUES (?, ?, 1, (SELECT s.costo FROM experiencia_servicio es JOIN servicio s ON es.id_servicio = s.id WHERE es.id = ?))";
        String sqlPago = "INSERT INTO pago (id_inscripcion, metodo, referencia, valor_pagado, fecha) VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP)";

        try {
            conn.setAutoCommit(false); 

            int idClienteGenerado = -1;
            int idInscripcionGenerada = -1;

            // A. Insertar Cliente
            try (PreparedStatement pstmtC = conn.prepareStatement(sqlCliente, new String[]{"ID"})) {
                pstmtC.setString(1, tipoDoc);
                pstmtC.setLong(2, numeroDoc);
                pstmtC.setString(3, nombreComp);
                pstmtC.setString(4, email);
                pstmtC.setString(5, telefono);
                pstmtC.executeUpdate();
                try (ResultSet rs = pstmtC.getGeneratedKeys()) {
                    if (rs.next()) idClienteGenerado = rs.getInt(1);
                }
            }

            // B. Insertar Historial Auditoría
            try (PreparedStatement pstmtH = conn.prepareStatement(sqlHistorial)) {
                pstmtH.setInt(1, idClienteGenerado);
                pstmtH.executeUpdate();
            }

            
            try (PreparedStatement pstmtI = conn.prepareStatement(sqlInscripcion, new String[]{"ID"})) {
                pstmtI.setInt(1, idClienteGenerado);
                pstmtI.setInt(2, idExpElegida);
                pstmtI.executeUpdate(); 
                try (ResultSet rs = pstmtI.getGeneratedKeys()) {
                    if (rs.next()) idInscripcionGenerada = rs.getInt(1);
                }
            }

            // D. Insertar Servicios Seleccionados
            try (PreparedStatement pstmtIS = conn.prepareStatement(sqlInsServicio)) {
                for (int idExpServ : serviciosElegidosIds) {
                    pstmtIS.setInt(1, idInscripcionGenerada);
                    pstmtIS.setInt(2, idExpServ);
                    pstmtIS.setInt(3, idExpServ);
                    pstmtIS.addBatch();
                }
                pstmtIS.executeBatch();
            }

            // E. Insertar Pago Inicial
            try (PreparedStatement pstmtP = conn.prepareStatement(sqlPago)) {
                pstmtP.setInt(1, idInscripcionGenerada);
                pstmtP.setString(2, metodoPagoText);
                pstmtP.setString(3, "REF-" + System.currentTimeMillis() / 1000);
                pstmtP.setDouble(4, abono);
                pstmtP.executeUpdate();
            }

            conn.commit(); 

            // 7. GENERACIÓN DEL COMPROBANTE DE INSCRIPCIÓN FINALES
            String sqlComprobante = "SELECT i.id, c.nombre_completo, e.descripcion, i.precio_total, i.saldo_pendiente " +
                                    "FROM inscripcion i JOIN cliente c ON i.id_cliente = c.id " +
                                    "JOIN experiencia e ON i.id_experiencia = e.id WHERE i.id = ?";
            
            try (PreparedStatement pstmtComp = conn.prepareStatement(sqlComprobante)) {
                pstmtComp.setInt(1, idInscripcionGenerada);
                try (ResultSet rsComp = pstmtComp.executeQuery()) {
                    if (rsComp.next()) {
                        System.out.println("\n=================================================");
                        System.out.println("       COMPROBANTE DE INSCRIPCION OFICIAL        ");
                        System.out.println("=================================================");
                        System.out.println("NUMERO DE RESERVA (AUTOMATICO): " + rsComp.getInt("id"));
                        System.out.println("TITULAR: " + rsComp.getString("nombre_completo"));
                        System.out.println("EXPERIENCIA: " + rsComp.getString("descripcion"));
                        System.out.println("-------------------------------------------------");
                        System.out.printf("VALOR TOTAL LIQUIDADO (CON IVA): $%.2f\n", rsComp.getDouble("precio_total"));
                        System.out.printf("SALDO PENDIENTE POST-PAGO:       $%.2f\n", rsComp.getDouble("saldo_pendiente"));
                        System.out.println("=================================================\n");
                    }
                }
            }

        } catch (SQLException e) {
            try { conn.rollback(); } catch (SQLException ex) { ex.printStackTrace(); }
            System.err.println("\nNo se pudo completar la reserva. Verifique si la experiencia cuenta con cupos disponibles.");
            e.printStackTrace();
        } finally {
            try { conn.setAutoCommit(true); } catch (SQLException e) { e.printStackTrace(); }
        }
    }
}
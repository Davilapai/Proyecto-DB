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

public class ClienteDAO
{
    // =====================================================================
    // INICIALIZACIÓN Y CONSULTA DE ACCESO
    // =====================================================================

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
        String tabla   = (longitud == 10) ? "cliente"          : "inscripcion";
        String columna = (longitud == 10) ? "numero_documento"  : "id";

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

    // =====================================================================
    // HISTORIA DE USUARIO 1 — REGISTRO NUEVO CLIENTE
    // =====================================================================

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

        System.out.print("Correo electronico: ");
        String email = sc.nextLine().trim();

        String telefono;
        while (true) {
            System.out.print("Telefono: ");
            telefono = sc.nextLine().trim();
            if (telefono.matches("[0-9]+")) {
                break;
            }
            System.out.println("Error: El telefono solo puede contener numeros. Intente nuevamente.");
        }

        // 2. INSERCION DEL CLIENTE Y REGISTRO INICIAL EN HISTORIAL
        String sqlCliente   = "INSERT INTO cliente (tipo_documento, numero_documento, nombre_completo, correo, telefono) VALUES (?, ?, ?, ?, ?)";
        String sqlHistorial = "INSERT INTO historial_cliente (id_cliente, id_inscripcion, descripcion) VALUES (?, null, 'Registro inicial del cliente.')";
        int idClienteGenerado = -1;

        try {
            conn.setAutoCommit(false);

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

            try (PreparedStatement pstmtH = conn.prepareStatement(sqlHistorial)) {
                pstmtH.setInt(1, idClienteGenerado);
                pstmtH.executeUpdate();
            }

            conn.commit();
            System.out.println("Cliente registrado con exito.");

        } catch (SQLException e) {
            try { conn.rollback(); } catch (SQLException ex) { ex.printStackTrace(); }
            System.err.println("No se pudo registrar el cliente: " + e.getMessage());
            return;
        } finally {
            try { conn.setAutoCommit(true); } catch (SQLException e) { e.printStackTrace(); }
        }

        // 3. FLUJO DE INSCRIPCION A EXPERIENCIA
        inscribirCliente(conn, sc, idClienteGenerado);
    }

    // =====================================================================
    // HISTORIA DE USUARIO 1 — CLIENTE EXISTENTE (INGRESO POR DOCUMENTO)
    // =====================================================================

    public static void MenuCliente(Connection conn, Scanner sc, String numeroDoc)
    {
        // 1. OBTENER Y MOSTRAR DATOS DEL CLIENTE
        String sqlCliente = "SELECT id, nombre_completo, correo, telefono FROM cliente WHERE numero_documento = ?";
        int idCliente = -1;

        try (PreparedStatement ps = conn.prepareStatement(sqlCliente)) {
            ps.setString(1, numeroDoc);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    idCliente = rs.getInt("id");
                    System.out.println("\n=========================================");
                    System.out.println("         BIENVENIDO DE NUEVO             ");
                    System.out.println("=========================================");
                    System.out.println("Nombre:   " + rs.getString("nombre_completo"));
                    System.out.println("Correo:   " + rs.getString("correo"));
                    System.out.println("Telefono: " + rs.getString("telefono"));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error al buscar cliente: " + e.getMessage());
            return;
        }

        // 2. MOSTRAR HISTORIAL DE INSCRIPCIONES
        System.out.println("\n--- HISTORIAL DE INSCRIPCIONES ---");
        String sqlHist =
            "SELECT i.id, e.descripcion, i.precio_total, i.saldo_pendiente " +
            "FROM inscripcion i JOIN experiencia e ON i.id_experiencia = e.id " +
            "WHERE i.id_cliente = ? ORDER BY i.fecha DESC";
        try (PreparedStatement ps = conn.prepareStatement(sqlHist)) {
            ps.setInt(1, idCliente);
            try (ResultSet rs = ps.executeQuery()) {
                boolean hayInscripciones = false;
                while (rs.next()) {
                    hayInscripciones = true;
                    System.out.printf("  Reserva #%06d | %-30s | Total: $%8.2f | Saldo: $%8.2f%n",
                        rs.getInt("id"), rs.getString("descripcion"),
                        rs.getDouble("precio_total"), rs.getDouble("saldo_pendiente"));
                }
                if (!hayInscripciones) System.out.println("  Sin inscripciones previas.");
            }
        } catch (SQLException e) {
            System.err.println("Error al mostrar historial: " + e.getMessage());
        }

        // 3. OPCION PARA NUEVA INSCRIPCION
        System.out.println("\nDesea inscribirse a una nueva experiencia? (s/n)");
        System.out.print("Su opcion: ");
        String sn = sc.nextLine().toLowerCase().trim();
        if (sn.equals("s") || sn.equals("si")) {
            inscribirCliente(conn, sc, idCliente);
        } else {
            System.out.println("Hasta pronto.");
        }
    }

    // =====================================================================
    // HISTORIA DE USUARIO 2 — GESTION POR CODIGO DE RESERVA
    // =====================================================================

    public static void MenuReserva(Connection conn, Scanner sc, String codigoReserva)
    {
        // 1. CARGAR DATOS COMPLETOS DE LA RESERVA
        String sqlReserva =
            "SELECT c.id AS id_cliente, c.nombre_completo, c.correo, c.telefono, " +
            "       e.id AS id_experiencia, e.descripcion AS experiencia, e.precio_base, " +
            "       i.id AS id_inscripcion, i.precio_total, i.saldo_pendiente " +
            "FROM inscripcion i " +
            "JOIN cliente c ON i.id_cliente = c.id " +
            "JOIN experiencia e ON i.id_experiencia = e.id " +
            "WHERE i.id = ?";

        int idCliente = -1, idInscripcion = -1, idExperiencia = -1;

        try (PreparedStatement ps = conn.prepareStatement(sqlReserva)) {
            ps.setString(1, codigoReserva);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    idCliente     = rs.getInt("id_cliente");
                    idInscripcion = rs.getInt("id_inscripcion");
                    idExperiencia = rs.getInt("id_experiencia");

                    System.out.println("\n=========================================");
                    System.out.println("         DATOS DEL CLIENTE               ");
                    System.out.println("=========================================");
                    System.out.println("Nombre:          " + rs.getString("nombre_completo"));
                    System.out.println("Correo:          " + rs.getString("correo"));
                    System.out.println("Telefono:        " + rs.getString("telefono"));
                    System.out.println("\n--- EXPERIENCIA ADQUIRIDA ---");
                    System.out.println("Experiencia:     " + rs.getString("experiencia"));
                    System.out.printf("Precio base:     $%.2f%n", rs.getDouble("precio_base"));
                    System.out.printf("Total actual:    $%.2f%n", rs.getDouble("precio_total"));
                    System.out.printf("Saldo pendiente: $%.2f%n", rs.getDouble("saldo_pendiente"));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error al cargar datos de la reserva: " + e.getMessage());
            return;
        }

        if (idInscripcion == -1) {
            System.out.println("Reserva no encontrada.");
            return;
        }

        // 2. MOSTRAR SERVICIOS ADICIONALES CONTRATADOS
        mostrarServiciosInscripcion(conn, idInscripcion);

        // 3. MOSTRAR HISTORIAL DE EXPERIENCIAS ANTERIORES DEL CLIENTE
        System.out.println("\n--- HISTORIAL DE EXPERIENCIAS ANTERIORES ---");
        String sqlHistorial =
            "SELECT h.descripcion, TO_CHAR(h.fecha, 'YYYY-MM-DD HH24:MI') AS fecha_fmt " +
            "FROM historial_cliente h " +
            "WHERE h.id_cliente = ? ORDER BY h.fecha DESC";
        try (PreparedStatement ps = conn.prepareStatement(sqlHistorial)) {
            ps.setInt(1, idCliente);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    System.out.printf("  [%s] %s%n",
                        rs.getString("fecha_fmt"), rs.getString("descripcion"));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error al mostrar historial: " + e.getMessage());
        }

        // 4. MENU DE MODIFICACIONES
        System.out.println("\n=========================================");
        System.out.println("         MODIFICACIONES DISPONIBLES      ");
        System.out.println("=========================================");

        boolean continuar = true;
        while (continuar) {
            System.out.println("\n1. Agregar acceso VIP adicional");
            System.out.println("2. Cambiar alimentacion estandar por premium");
            System.out.println("3. Registrar novedad del cliente");
            System.out.println("4. Ver resumen actualizado (Ventana 1)");
            System.out.println("5. Ver detalle de pagos e impuestos (Ventana 2)");
            System.out.println("0. Salir");
            System.out.print("Su opcion: ");

            try {
                int op = sc.nextInt();
                sc.nextLine();

                switch (op) {
                    case 1:
                        agregarAccesoVIP(conn, idInscripcion, idExperiencia);
                        break;
                    case 2:
                        cambiarAlimentacion(conn, idInscripcion, idExperiencia);
                        break;
                    case 3:
                        registrarNovedad(conn, sc, idCliente, idInscripcion);
                        break;
                    case 4:
                        mostrarResumenActualizado(conn, idInscripcion);
                        break;
                    case 5:
                        mostrarDetallePagos(conn, idInscripcion);
                        break;
                    case 0:
                        continuar = false;
                        break;
                    default:
                        System.out.println("Opcion no valida.");
                }
            } catch (SQLException e) {
                System.err.println("Error al procesar la operacion: " + e.getMessage());
            } catch (Exception e) {
                System.out.println("Entrada invalida. Por favor ingrese un numero.");
                sc.nextLine();
            }
        }
    }

    // =====================================================================
    // METODOS PRIVADOS DE SOPORTE
    // =====================================================================

    /**
     * Flujo completo de inscripcion a una experiencia para un cliente ya registrado.
     * Reutilizado por AgregarPersona (cliente nuevo) y MenuCliente (cliente existente).
     */
    private static void inscribirCliente(Connection conn, Scanner sc, int idCliente)
    {
        // FILTRO OPCIONAL POR SELECCION NACIONAL
        System.out.println("\nDesea buscar experiencias filtradas por alguna seleccion nacional? (s/n)");
        System.out.print("Su opcion: ");
        String filtroSn = sc.nextLine().toLowerCase().trim();
        String seleccionBuscada = "";
        if (filtroSn.equals("s") || filtroSn.equals("si")) {
            System.out.print("Escriba el nombre de la seleccion (ej. Mexico, Colombia): ");
            seleccionBuscada = sc.nextLine().trim();
        }

        // MOSTRAR CATALOGO DE EXPERIENCIAS DISPONIBLES
        String sqlBuscarExp =
            "SELECT e.id, e.descripcion, e.precio_base, e.cupos_disponibles, " +
            "       s1.nombre AS loc, s2.nombre AS vis " +
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
                System.out.printf("%-5s %-35s %-15s %-10s %-25s%n",
                    "ID", "Descripcion", "Precio Base", "Cupos", "Partido");
                while (rsExp.next()) {
                    System.out.printf("%-5d %-35s $%-14.2f %-10d %-25s%n",
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
        sc.nextLine();

        // MOSTRAR Y SELECCIONAR SERVICIOS OPCIONALES
        List<Integer> serviciosDisponiblesIds = new ArrayList<>();
        String sqlServicios =
            "SELECT es.id, s.nombre, s.costo FROM experiencia_servicio es " +
            "JOIN servicio s ON es.id_servicio = s.id WHERE es.id_experiencia = ?";

        try (PreparedStatement pstmtServ = conn.prepareStatement(sqlServicios)) {
            pstmtServ.setInt(1, idExpElegida);
            try (ResultSet rsServ = pstmtServ.executeQuery()) {
                System.out.println("\n--- SERVICIOS ADICIONALES DISPONIBLES ---");
                while (rsServ.next()) {
                    int idExpServ = rsServ.getInt("id");
                    serviciosDisponiblesIds.add(idExpServ);
                    System.out.printf("ID: %d | %-35s | Costo: $%.2f%n",
                        idExpServ, rsServ.getString("nombre"), rsServ.getDouble("costo"));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error al listar servicios: " + e.getMessage());
        }

        List<Integer> serviciosElegidosIds = new ArrayList<>();
        while (true) {
            System.out.print("Ingrese el ID del servicio que quiera (0 para terminar): ");
            try {
                int idServicioElegido = sc.nextInt();
                sc.nextLine();
                if (idServicioElegido == 0) break;
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

        // CALCULO ANTICIPADO DEL TOTAL (precio_base + servicios con impuestos)
        double totalLiquidado = 0;

        String sqlPrecioBase = "SELECT precio_base FROM experiencia WHERE id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sqlPrecioBase)) {
            pstmt.setInt(1, idExpElegida);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) totalLiquidado = rs.getDouble("precio_base");
            }
        } catch (SQLException e) {
            System.err.println("Error al calcular el precio base: " + e.getMessage());
        }

        String sqlCostoConImpuesto =
            "SELECT s.costo, NVL(SUM(imp.porcentaje), 0) AS porcentaje " +
            "FROM experiencia_servicio es " +
            "JOIN servicio s ON es.id_servicio = s.id " +
            "LEFT JOIN servicio_impuesto si ON si.id_servicio = s.id " +
            "LEFT JOIN impuesto imp ON imp.id = si.id_impuesto " +
            "WHERE es.id = ? GROUP BY s.costo";

        for (int idExpServ : serviciosElegidosIds) {
            try (PreparedStatement pstmt = conn.prepareStatement(sqlCostoConImpuesto)) {
                pstmt.setInt(1, idExpServ);
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        double costo = rs.getDouble("costo");
                        double pct   = rs.getDouble("porcentaje");
                        totalLiquidado += costo * (1 + pct / 100);
                    }
                }
            } catch (SQLException e) {
                System.err.println("Error al calcular costo de servicios: " + e.getMessage());
            }
        }

        // SELECCION DEL METODO DE PAGO
        System.out.println("\nSeleccione el metodo de pago:");
        System.out.println("1. TARJETA CREDITO | 2. TARJETA DEBITO | 3. EFECTIVO | 4. BILLETERA DIGITAL");
        System.out.print("Su opcion (1-4): ");
        int opPago = sc.nextInt();
        sc.nextLine();

        String metodoPago = "EFECTIVO";
        if (opPago == 1) metodoPago = "TARJETA CREDITO";
        else if (opPago == 2) metodoPago = "TARJETA DEBITO";
        else if (opPago == 4) metodoPago = "BILLETERA DIGITAL";

        // VALIDACION DEL ABONO INICIAL
        System.out.printf("%nEl valor total de su inscripcion (con impuestos) es de: $%.2f%n", totalLiquidado);
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
                    System.out.printf("Error: El abono no puede ser superior al valor total ($%.2f).%n", totalLiquidado);
                    continue;
                }
                break;
            } catch (Exception e) {
                System.out.println("Entrada invalida. Por favor introduzca un monto numerico.");
                sc.nextLine();
            }
        }

        // EJECUCION DE TRANSACCION EN LA BASE DE DATOS
        // El trigger trg_act_cupos_disp inicializa precio_total con precio_base
        // El trigger trg_calcular_precio_total acumula los costos de servicios
        // El trigger trg_saldo_pendiente descuenta el pago del saldo
        String sqlInscripcion =
            "INSERT INTO inscripcion (id_cliente, id_experiencia, precio_total, saldo_pendiente, fecha) " +
            "VALUES (?, ?, 0, 0, CURRENT_TIMESTAMP)";
        String sqlInsServicio =
            "INSERT INTO inscripcion_servicio (id_inscripcion, id_exp_servicio, cantidad, costo_unitario) " +
            "VALUES (?, ?, 1, (SELECT s.costo FROM experiencia_servicio es JOIN servicio s ON es.id_servicio = s.id WHERE es.id = ?))";
        String sqlPago =
            "INSERT INTO pago (id_inscripcion, metodo, referencia, valor_pagado, fecha) " +
            "VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP)";

        int idInscripcionGenerada = -1;

        try {
            conn.setAutoCommit(false);

            try (PreparedStatement pstmtI = conn.prepareStatement(sqlInscripcion, new String[]{"ID"})) {
                pstmtI.setInt(1, idCliente);
                pstmtI.setInt(2, idExpElegida);
                pstmtI.executeUpdate();
                try (ResultSet rs = pstmtI.getGeneratedKeys()) {
                    if (rs.next()) idInscripcionGenerada = rs.getInt(1);
                }
            }

            try (PreparedStatement pstmtIS = conn.prepareStatement(sqlInsServicio)) {
                for (int idExpServ : serviciosElegidosIds) {
                    pstmtIS.setInt(1, idInscripcionGenerada);
                    pstmtIS.setInt(2, idExpServ);
                    pstmtIS.setInt(3, idExpServ);
                    pstmtIS.addBatch();
                }
                pstmtIS.executeBatch();
            }

            try (PreparedStatement pstmtP = conn.prepareStatement(sqlPago)) {
                pstmtP.setInt(1, idInscripcionGenerada);
                pstmtP.setString(2, metodoPago);
                pstmtP.setString(3, "REF-" + System.currentTimeMillis() / 1000);
                pstmtP.setDouble(4, abono);
                pstmtP.executeUpdate();
            }

            conn.commit();

            // GENERACION DEL COMPROBANTE DE INSCRIPCION
            String sqlComprobante =
                "SELECT i.id, c.nombre_completo, e.descripcion, i.precio_total, i.saldo_pendiente " +
                "FROM inscripcion i JOIN cliente c ON i.id_cliente = c.id " +
                "JOIN experiencia e ON i.id_experiencia = e.id WHERE i.id = ?";

            try (PreparedStatement pstmtComp = conn.prepareStatement(sqlComprobante)) {
                pstmtComp.setInt(1, idInscripcionGenerada);
                try (ResultSet rsComp = pstmtComp.executeQuery()) {
                    if (rsComp.next()) {
                        System.out.println("\n=================================================");
                        System.out.println("       COMPROBANTE DE INSCRIPCION OFICIAL        ");
                        System.out.println("=================================================");
                        System.out.printf("NUMERO DE RESERVA (AUTOMATICO): %06d%n", rsComp.getInt("id"));
                        System.out.println("TITULAR:    " + rsComp.getString("nombre_completo"));
                        System.out.println("EXPERIENCIA:" + rsComp.getString("descripcion"));
                        System.out.println("-------------------------------------------------");
                        System.out.printf("VALOR TOTAL LIQUIDADO (CON IVA): $%.2f%n", rsComp.getDouble("precio_total"));
                        System.out.printf("SALDO PENDIENTE POST-PAGO:       $%.2f%n", rsComp.getDouble("saldo_pendiente"));
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

    /** Imprime los servicios contratados en una inscripcion con su desglose de impuestos. */
    private static void mostrarServiciosInscripcion(Connection conn, int idInscripcion)
    {
        System.out.println("\n--- SERVICIOS ADICIONALES CONTRATADOS ---");
        String sql =
            "SELECT s.nombre, ins.cantidad, ins.costo_unitario, " +
            "       NVL(SUM(imp.porcentaje), 0) AS pct_impuesto " +
            "FROM inscripcion_servicio ins " +
            "JOIN experiencia_servicio es ON es.id = ins.id_exp_servicio " +
            "JOIN servicio s ON s.id = es.id_servicio " +
            "LEFT JOIN servicio_impuesto si ON si.id_servicio = s.id " +
            "LEFT JOIN impuesto imp ON imp.id = si.id_impuesto " +
            "WHERE ins.id_inscripcion = ? " +
            "GROUP BY s.nombre, ins.cantidad, ins.costo_unitario";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idInscripcion);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    double costo = rs.getDouble("costo_unitario");
                    double pct   = rs.getDouble("pct_impuesto");
                    System.out.printf("  %-35s | Cant: %d | Costo: $%.2f | Imp: %.1f%% | Total: $%.2f%n",
                        rs.getString("nombre"), rs.getInt("cantidad"),
                        costo, pct, costo * (1 + pct / 100));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error al mostrar servicios: " + e.getMessage());
        }
    }

    /**
     * HU2: Agrega el servicio de Acceso VIP Adicional a la inscripcion.
     * El trigger trg_calcular_precio_total actualiza precio_total y saldo_pendiente automaticamente.
     */
    private static void agregarAccesoVIP(Connection conn, int idInscripcion, int idExperiencia) throws SQLException
    {
        // Buscar el exp_serv de "Acceso VIP Adicional" en esta experiencia
        String sqlBuscar =
            "SELECT es.id, s.costo FROM experiencia_servicio es " +
            "JOIN servicio s ON s.id = es.id_servicio " +
            "WHERE es.id_experiencia = ? AND UPPER(s.nombre) LIKE '%ACCESO VIP%'";

        int expServId = -1;
        double costo  = 0;

        try (PreparedStatement ps = conn.prepareStatement(sqlBuscar)) {
            ps.setInt(1, idExperiencia);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    expServId = rs.getInt("id");
                    costo     = rs.getDouble("costo");
                } else {
                    System.out.println("No hay acceso VIP disponible para esta experiencia.");
                    return;
                }
            }
        }

        // Verificar que no haya sido agregado previamente
        String sqlCheck = "SELECT 1 FROM inscripcion_servicio WHERE id_inscripcion = ? AND id_exp_servicio = ?";
        try (PreparedStatement ps = conn.prepareStatement(sqlCheck)) {
            ps.setInt(1, idInscripcion);
            ps.setInt(2, expServId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    System.out.println("El acceso VIP ya fue agregado a esta inscripcion.");
                    return;
                }
            }
        }

        // Insertar — el trigger trg_calcular_precio_total actualiza los totales
        String sqlInsert =
            "INSERT INTO inscripcion_servicio (id_inscripcion, id_exp_servicio, cantidad, costo_unitario) VALUES (?, ?, 1, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sqlInsert)) {
            ps.setInt(1, idInscripcion);
            ps.setInt(2, expServId);
            ps.setDouble(3, costo);
            ps.executeUpdate();
        }
        System.out.println("Acceso VIP adicional agregado correctamente. Los totales han sido actualizados.");
    }

    /**
     * HU2: Cambia el servicio de Alimentacion Normal por Alimentacion Premium.
     * DELETE + INSERT porque el PK de inscripcion_servicio incluye id_exp_servicio.
     * trg_calcular_precio_total maneja el ajuste de precios en ambas operaciones.
     */
    private static void cambiarAlimentacion(Connection conn, int idInscripcion, int idExperiencia) throws SQLException
    {
        // Buscar Alimentacion Normal en la inscripcion actual
        String sqlNormal =
            "SELECT ins.id_exp_servicio FROM inscripcion_servicio ins " +
            "JOIN experiencia_servicio es ON es.id = ins.id_exp_servicio " +
            "JOIN servicio s ON s.id = es.id_servicio " +
            "WHERE ins.id_inscripcion = ? AND UPPER(s.nombre) LIKE '%ALIMENTACI%NORMAL%'";

        int expServNormalId = -1;
        try (PreparedStatement ps = conn.prepareStatement(sqlNormal)) {
            ps.setInt(1, idInscripcion);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    expServNormalId = rs.getInt("id_exp_servicio");
                } else {
                    System.out.println("No se encontro el servicio de alimentacion estandar en esta inscripcion.");
                    return;
                }
            }
        }

        // Buscar Alimentacion Premium disponible en la experiencia
        String sqlPremium =
            "SELECT es.id, s.costo FROM experiencia_servicio es " +
            "JOIN servicio s ON s.id = es.id_servicio " +
            "WHERE es.id_experiencia = ? AND UPPER(s.nombre) LIKE '%ALIMENTACI%PREMIUM%'";

        int expServPremiumId = -1;
        double costoPremium  = 0;
        try (PreparedStatement ps = conn.prepareStatement(sqlPremium)) {
            ps.setInt(1, idExperiencia);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    expServPremiumId = rs.getInt("id");
                    costoPremium     = rs.getDouble("costo");
                } else {
                    System.out.println("No hay servicio de alimentacion premium para esta experiencia.");
                    return;
                }
            }
        }

        // Obtener id_cliente para el registro manual de historial
        int idCliente = -1;
        String sqlCliente = "SELECT id_cliente FROM inscripcion WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sqlCliente)) {
            ps.setInt(1, idInscripcion);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) idCliente = rs.getInt("id_cliente");
            }
        }

        // DELETE Alimentacion Normal: trg_calcular_precio_total descuenta el costo
        String sqlDelete = "DELETE FROM inscripcion_servicio WHERE id_inscripcion = ? AND id_exp_servicio = ?";
        try (PreparedStatement ps = conn.prepareStatement(sqlDelete)) {
            ps.setInt(1, idInscripcion);
            ps.setInt(2, expServNormalId);
            ps.executeUpdate();
        }

        // INSERT Alimentacion Premium: trg_calcular_precio_total suma el nuevo costo
        String sqlInsert =
            "INSERT INTO inscripcion_servicio (id_inscripcion, id_exp_servicio, cantidad, costo_unitario) VALUES (?, ?, 1, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sqlInsert)) {
            ps.setInt(1, idInscripcion);
            ps.setInt(2, expServPremiumId);
            ps.setDouble(3, costoPremium);
            ps.executeUpdate();
        }

        // Registro manual en historial (trg_historial_modificacion solo actua en UPDATE,
        // aqui se usa DELETE+INSERT porque id_exp_servicio es parte de la PK)
        if (idCliente != -1) {
            String sqlHist =
                "INSERT INTO historial_cliente (id_cliente, id_inscripcion, descripcion) VALUES (?, ?, ?)";
            try (PreparedStatement ps = conn.prepareStatement(sqlHist)) {
                ps.setInt(1, idCliente);
                ps.setInt(2, idInscripcion);
                ps.setString(3, "Cambio de alimentacion estandar a premium en inscripcion #" + idInscripcion);
                ps.executeUpdate();
            }
        }

        System.out.println("Servicio de alimentacion cambiado a premium. Los totales han sido actualizados.");
    }

    /** HU2: Registra una novedad libre en el historial del cliente. */
    private static void registrarNovedad(Connection conn, Scanner sc, int idCliente, int idInscripcion)
    {
        System.out.print("\nDescripcion de la novedad: ");
        String descripcion = sc.nextLine().trim();
        if (descripcion.isEmpty()) {
            System.out.println("Descripcion vacia. No se registro la novedad.");
            return;
        }

        String sql = "INSERT INTO historial_cliente (id_cliente, id_inscripcion, descripcion) VALUES (?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idCliente);
            ps.setInt(2, idInscripcion);
            ps.setString(3, "NOVEDAD: " + descripcion);
            ps.executeUpdate();
            System.out.println("Novedad registrada correctamente en el historial del cliente.");
        } catch (SQLException e) {
            System.err.println("Error al registrar la novedad: " + e.getMessage());
        }
    }

    /**
     * HU2 — Ventana 1: Resumen actualizado de la experiencia.
     * Muestra precio base, desglose de servicios con impuestos, total y saldo.
     */
    private static void mostrarResumenActualizado(Connection conn, int idInscripcion)
    {
        System.out.println("\n");
        System.out.println("+=======================================================+");
        System.out.println("|   VENTANA 1: RESUMEN ACTUALIZADO DE EXPERIENCIA      |");
        System.out.println("+=======================================================+");

        String sqlResumen =
            "SELECT c.nombre_completo, e.descripcion, e.precio_base, " +
            "       i.precio_total, i.saldo_pendiente " +
            "FROM inscripcion i " +
            "JOIN cliente c ON i.id_cliente = c.id " +
            "JOIN experiencia e ON i.id_experiencia = e.id " +
            "WHERE i.id = ?";

        try (PreparedStatement ps = conn.prepareStatement(sqlResumen)) {
            ps.setInt(1, idInscripcion);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    System.out.printf("  RESERVA #:    %06d%n", idInscripcion);
                    System.out.println("  CLIENTE:      " + rs.getString("nombre_completo"));
                    System.out.println("  EXPERIENCIA:  " + rs.getString("descripcion"));
                    System.out.printf("  PRECIO BASE:  $%.2f%n", rs.getDouble("precio_base"));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error al cargar resumen: " + e.getMessage());
        }

        System.out.println("\n  SERVICIOS ADICIONALES:");
        double subtotalServicios = 0, totalImpuestos = 0;
        String sqlServ =
            "SELECT s.nombre, ins.cantidad, ins.costo_unitario, " +
            "       NVL(SUM(imp.porcentaje), 0) AS pct " +
            "FROM inscripcion_servicio ins " +
            "JOIN experiencia_servicio es ON es.id = ins.id_exp_servicio " +
            "JOIN servicio s ON s.id = es.id_servicio " +
            "LEFT JOIN servicio_impuesto si ON si.id_servicio = s.id " +
            "LEFT JOIN impuesto imp ON imp.id = si.id_impuesto " +
            "WHERE ins.id_inscripcion = ? GROUP BY s.nombre, ins.cantidad, ins.costo_unitario";

        try (PreparedStatement ps = conn.prepareStatement(sqlServ)) {
            ps.setInt(1, idInscripcion);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int    cant  = rs.getInt("cantidad");
                    double costo = rs.getDouble("costo_unitario");
                    double pct   = rs.getDouble("pct");
                    double imp   = costo * cant * pct / 100;
                    double total = costo * cant + imp;
                    subtotalServicios += costo * cant;
                    totalImpuestos    += imp;
                    System.out.printf("    %-33s x%d  $%7.2f  +%5.1f%%  = $%7.2f%n",
                        rs.getString("nombre"), cant, costo, pct, total);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error al listar servicios: " + e.getMessage());
        }

        System.out.println("  -------------------------------------------------------");
        System.out.printf("  SUBTOTAL SERVICIOS: $%.2f%n", subtotalServicios);
        System.out.printf("  TOTAL IMPUESTOS:    $%.2f%n", totalImpuestos);

        String sqlTotales = "SELECT precio_total, saldo_pendiente FROM inscripcion WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sqlTotales)) {
            ps.setInt(1, idInscripcion);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    System.out.printf("  PRECIO TOTAL:       $%.2f%n", rs.getDouble("precio_total"));
                    System.out.printf("  SALDO PENDIENTE:    $%.2f%n", rs.getDouble("saldo_pendiente"));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error al cargar totales: " + e.getMessage());
        }

        System.out.println("+=======================================================+");
        System.out.println("|                   FIN VENTANA 1                      |");
        System.out.println("+=======================================================+\n");
    }

    /**
     * HU2 — Ventana 2: Detalle de pagos e impuestos (para impresion en pantalla).
     * Muestra el desglose de impuestos por servicio y el historial de pagos.
     */
    private static void mostrarDetallePagos(Connection conn, int idInscripcion)
    {
        System.out.println("\n");
        System.out.println("+=======================================================+");
        System.out.println("| VENTANA 2: DETALLE DE PAGOS E IMPUESTOS (IMPRESION)  |");
        System.out.println("+=======================================================+");
        System.out.printf("  RESERVA #: %06d%n%n", idInscripcion);

        // Desglose de impuestos por servicio
        System.out.println("  DESGLOSE DE IMPUESTOS POR SERVICIO:");
        String sqlImp =
            "SELECT s.nombre AS servicio, t.tipo_impuesto, t.porcentaje, " +
            "       ins.costo_unitario * t.porcentaje / 100 AS valor_impuesto " +
            "FROM inscripcion_servicio ins " +
            "JOIN experiencia_servicio es ON es.id = ins.id_exp_servicio " +
            "JOIN servicio s ON s.id = es.id_servicio " +
            "JOIN servicio_impuesto si ON si.id_servicio = s.id " +
            "JOIN impuesto t ON t.id = si.id_impuesto " +
            "WHERE ins.id_inscripcion = ? " +
            "ORDER BY s.nombre, t.tipo_impuesto";

        double totalImpuestos = 0;
        try (PreparedStatement ps = conn.prepareStatement(sqlImp)) {
            ps.setInt(1, idInscripcion);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    double val = rs.getDouble("valor_impuesto");
                    totalImpuestos += val;
                    System.out.printf("    %-33s | %-10s | %5.1f%% | $%.2f%n",
                        rs.getString("servicio"), rs.getString("tipo_impuesto"),
                        rs.getDouble("porcentaje"), val);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error al mostrar impuestos: " + e.getMessage());
        }
        System.out.printf("  TOTAL IMPUESTOS: $%.2f%n%n", totalImpuestos);

        // Historial de pagos realizados
        System.out.println("  PAGOS REALIZADOS:");
        String sqlPagos =
            "SELECT TO_CHAR(p.fecha, 'YYYY-MM-DD HH24:MI') AS fecha_pago, " +
            "       p.metodo, p.referencia, p.valor_pagado " +
            "FROM pago p WHERE p.id_inscripcion = ? ORDER BY p.fecha";

        double totalPagado = 0;
        try (PreparedStatement ps = conn.prepareStatement(sqlPagos)) {
            ps.setInt(1, idInscripcion);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    double val = rs.getDouble("valor_pagado");
                    totalPagado += val;
                    System.out.printf("    [%s] %-20s Ref: %-15s $%.2f%n",
                        rs.getString("fecha_pago"), rs.getString("metodo"),
                        rs.getString("referencia"), val);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error al mostrar pagos: " + e.getMessage());
        }

        System.out.println("  -------------------------------------------------------");
        System.out.printf("  TOTAL PAGADO:       $%.2f%n", totalPagado);

        String sqlSaldo = "SELECT saldo_pendiente FROM inscripcion WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sqlSaldo)) {
            ps.setInt(1, idInscripcion);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) System.out.printf("  SALDO PENDIENTE:    $%.2f%n", rs.getDouble("saldo_pendiente"));
            }
        } catch (SQLException e) {
            System.err.println("Error al cargar saldo: " + e.getMessage());
        }

        System.out.println("+=======================================================+");
        System.out.println("|                   FIN VENTANA 2                      |");
        System.out.println("+=======================================================+\n");
    }
}
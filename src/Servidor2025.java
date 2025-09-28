import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

public class Servidor2025 {

    private static final String ARCHIVO_USUARIOS = "usuarios.txt";
    private static final String ARCHIVO_MENSAJES = "mensajes.txt";
    private static final String ARCHIVO_BLOQUEOS = "bloqueos.txt";
    private static final String ARCHIVO_SOLICITUDES = "solicitudes.txt";
    private static final String ARCHIVO_DECISIONES = "decisiones_descarga.txt";
    private static final String DIRECTORIO_RAIZ = "servidor_archivos";
    private static final String CLAVE_LISTAR = "LISTAR_ARCHIVOS";


    private static final Map<String, Map<String, String>> PERMISOS_CONCEDIDOS = new ConcurrentHashMap<>();

    public static void main(String[] args) {
        new File(DIRECTORIO_RAIZ).mkdirs();

        try {
            ServerSocket serverSocket = new ServerSocket(8081);
            System.out.println("Servidor listo. Esperando cliente...");

            while (true) {
                Socket cliente = serverSocket.accept();
                System.out.println("Cliente conectado: " + cliente.getInetAddress());

                new Thread(() -> manejarCliente(cliente)).start();
            }
        } catch (IOException e) {
            System.out.println("Ocurrió un error en el servidor: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void manejarCliente(Socket cliente) {
        String usuarioAutenticado = null;

        try (
                PrintWriter escritor = new PrintWriter(cliente.getOutputStream(), true);
                BufferedReader lector = new BufferedReader(new InputStreamReader(cliente.getInputStream()))
        ) {
            escritor.println("Bienvenido. ¿Deseas [1] Iniciar sesión, [2] Registrarte ");
            String opcion = lector.readLine();

            if ("1".equals(opcion)) {
                escritor.println("Usuario:");
                String usuario = lector.readLine();

                if (verificarUsuarioExiste(usuario)) {
                    escritor.println("Contraseña:");
                    String contrasena = lector.readLine();
                    if (verificarCredenciales(usuario, contrasena)) {
                        escritor.println("Autenticación exitosa");
                        usuarioAutenticado = usuario;
                    } else {
                        escritor.println("Contraseña incorrecta");
                    }
                } else {
                    escritor.println("El usuario no existe");
                }
            } else if ("2".equals(opcion)) {
                escritor.println("Usuario:");
                String usuario = lector.readLine();
                escritor.println("Contraseña:");
                String contrasena = lector.readLine();
                if (!esContrasenaValida(contrasena)) {
                    escritor.println("Contraseña no válida. Debe tener al menos 8 caracteres y no puede estar vacía.");
                } else {
                    if (registrarUsuario(usuario, contrasena)) {
                        escritor.println("Usuario registrado exitosamente");
                        usuarioAutenticado = usuario;
                    } else {
                        escritor.println("El usuario ya existe");
                    }
                }
            } else {
                escritor.println("Opción no válida");
            }


            if (usuarioAutenticado == null) {
                cliente.close();
                return;
            }

            // --- FLUJO: Notificar decisiones pendientes al Usuario A ---
            mostrarDecisionesPendientes(usuarioAutenticado, escritor);

            // Manejar y mostrar solicitudes pendientes (del usuario actual, como dueño del archivo)
            manejarSolicitudesPendientes(usuarioAutenticado, lector, escritor);

            escritor.println("Menú: [1] Jugar | [2] Enviar mensaje | [3] Leer mensajes | [4] Cerrar sesión | [5] Eliminar mensaje recibido | [6] Eliminar mensaje enviado | [7] Ver usuarios | [8] Bloquear/Desbloquear | [9] Listar archivos | [10] Crear archivo | [11] Descargar archivo | [12] Transferir archivo entre usuarios");

            String opcionMenu;
            while ((opcionMenu = lector.readLine()) != null) {
                System.out.println("Opción recibida del cliente '" + usuarioAutenticado + "': '" + opcionMenu + "'");

                switch (opcionMenu) {
                    case "1":
                        jugarAdivinarNumero(lector, escritor);
                        break;
                    case "2":
                        enviarMensaje(usuarioAutenticado, lector, escritor);
                        break;
                    case "3":
                        manejarLecturaMensajes(usuarioAutenticado, lector, escritor);
                        break;
                    case "4":
                        System.out.println("Cliente " + usuarioAutenticado + " ha cerrado sesión.");
                        cliente.close();
                        return;
                    case "5":
                        eliminarMensajes(usuarioAutenticado, lector, escritor, "recibidos");
                        break;
                    case "6":
                        eliminarMensajes(usuarioAutenticado, lector, escritor, "enviados");
                        break;
                    case "7":
                        mostrarUsuariosRegistrados(escritor);
                        break;
                    case "8":
                        bloquearDesbloquearUsuario(usuarioAutenticado, lector, escritor);
                        break;
                    case "9":
                        listarArchivosDeUsuario(usuarioAutenticado, lector, escritor); // Pasa el solicitante
                        break;
                    case "10":
                        crearArchivoRemoto(usuarioAutenticado, lector, escritor);
                        break;
                    case "11":
                        descargarArchivo(usuarioAutenticado, lector, escritor);
                        break;
                    case "12":
                        transferirArchivoInterno(usuarioAutenticado, lector, escritor);
                        break;
                    default:
                        escritor.println("Opción de menú no válida.");
                        break;
                }
                escritor.println("Menú: [1] Jugar | [2] Enviar mensaje | [3] Leer mensajes | [4] Cerrar sesión | [5] Eliminar mensaje recibido | [6] Eliminar mensaje enviado | [7] Ver usuarios | [8] Bloquear/Desbloquear | [9] Listar archivos | [10] Crear archivo | [11] Descargar archivo | [12] Transferir archivo entre usuarios");
            }

        } catch (IOException e) {
            System.out.println("Error con el cliente: " + e.getMessage());
        } finally {
            if (usuarioAutenticado != null) {
                PERMISOS_CONCEDIDOS.remove(usuarioAutenticado);
            }

            try {
                if (cliente != null && !cliente.isClosed()) {
                    cliente.close();
                }
            } catch (IOException e) {
                System.out.println("Error al cerrar el socket del cliente: " + e.getMessage());
            }
        }
    }

    // --- MÉTODO: Muestra y transfiere decisiones pendientes a memoria ---
    private static void mostrarDecisionesPendientes(String usuarioAutenticado, PrintWriter escritor) throws IOException {
        File archivoDecisiones = new File(ARCHIVO_DECISIONES);
        if (!archivoDecisiones.exists()) return;

        List<String> lineasRestantes = new ArrayList<>();
        List<String> notificaciones = new ArrayList<>();

        // 1. Leer el archivo y recopilar notificaciones para el usuario
        try (BufferedReader reader = new BufferedReader(new FileReader(archivoDecisiones))) {
            String linea;
            while ((linea = reader.readLine()) != null) {
                String[] partes = linea.split(":");
                if (partes.length == 4 && partes[0].equals(usuarioAutenticado)) {
                    notificaciones.add(linea);
                } else {
                    lineasRestantes.add(linea);
                }
            }
        }

        if (notificaciones.isEmpty()) {
            return; // No hay nada que notificar
        }

        // 2. Mostrar las notificaciones al cliente y transferir permisos
        escritor.println("\n--- 🔔 NOTIFICACIONES PENDIENTES DE DESCARGA ---");
        for (String notificacion : notificaciones) {
            String[] partes = notificacion.split(":");
            String propietario = partes[1];
            String clave = partes[2]; // archivo.txt o LISTAR_ARCHIVOS
            String decision = partes[3];

            String accion = clave.equals(CLAVE_LISTAR) ? "LISTAR sus archivos" : "DESCARGAR el archivo: " + clave;
            String opcionMenu = clave.equals(CLAVE_LISTAR) ? "[9]" : "[11]";

            if ("ACEPTADA".equals(decision)) {
                // Añadir el permiso al mapa temporal en memoria
                PERMISOS_CONCEDIDOS.computeIfAbsent(propietario, k -> new ConcurrentHashMap<>())
                        .put(usuarioAutenticado, clave);

                escritor.println("✅ El usuario " + propietario + " ACEPTÓ tu solicitud para " + accion + ".");
                escritor.println("   (Usa la opción " + opcionMenu + " para iniciar la acción.)");
            } else if ("DENEGADA".equals(decision)) {
                escritor.println("❌ El usuario " + propietario + " DENEGÓ tu solicitud para " + accion + ".");
            }
        }
        escritor.println("----------------------------------------------");

        // 3. Reescribir el archivo sin las notificaciones mostradas (limpieza)
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(archivoDecisiones))) {
            for (String linea : lineasRestantes) {
                writer.write(linea);
                writer.newLine();
            }
        }
    }


    private static void manejarSolicitudesPendientes(String propietario, BufferedReader lector, PrintWriter escritor) throws IOException {
        List<String> solicitudesActuales = new ArrayList<>();
        List<String> lineasRestantes = new ArrayList<>();
        boolean tieneSolicitudes = false;

        // 1. Cargar todas las solicitudes
        File archivo = new File(ARCHIVO_SOLICITUDES);
        if (archivo.exists()) {
            try (BufferedReader reader = new BufferedReader(new FileReader(archivo))) {
                String linea;
                while ((linea = reader.readLine()) != null) {
                    String[] partes = linea.split(":");
                    if (partes.length == 3 && partes[0].equals(propietario)) {
                        solicitudesActuales.add(linea);
                        tieneSolicitudes = true;
                    } else {
                        lineasRestantes.add(linea);
                    }
                }
            }
        }

        if (!tieneSolicitudes) {
            escritor.println("INFO: No tienes solicitudes de descarga pendientes.");
            return;
        }

        escritor.println("--- ¡ATENCIÓN! SOLICITUDES DE DESCARGA PENDIENTES ---");
        escritor.println("Se encontraron " + solicitudesActuales.size() + " solicitudes. Presiona [Enter] para revisar.");

        lector.readLine();

        for (String solicitud : solicitudesActuales) {
            String[] partes = solicitud.split(":");
            String solicitante = partes[1];
            String claveSolicitada = partes[2]; // archivo.txt o LISTAR_ARCHIVOS

            String mensajeSolicitud;
            if (claveSolicitada.equals(CLAVE_LISTAR)) {
                mensajeSolicitud = "El usuario '" + solicitante + "' solicita PERMISO para LISTAR tus archivos.";
            } else {
                mensajeSolicitud = "El usuario '" + solicitante + "' solicita descargar tu archivo: " + claveSolicitada;
            }

            escritor.println(mensajeSolicitud); // Muestra el mensaje ajustado
            escritor.println("¿Permitir descarga/listado? [1] Sí / [2] No / [3] Dejar Pendiente");
            String opcion = lector.readLine();

            if ("1".equals(opcion)) {
                PERMISOS_CONCEDIDOS.computeIfAbsent(propietario, k -> new ConcurrentHashMap<>())
                        .put(solicitante, claveSolicitada);

                // Registra la decisión PERMANENTE
                almacenarDecision(solicitante, propietario, claveSolicitada, "ACEPTADA");

                escritor.println("Permiso concedido a " + solicitante + ". Se le notificará su decisión.");
            } else if ("2".equals(opcion)) {
                // Registra la decisión PERMANENTE
                almacenarDecision(solicitante, propietario, claveSolicitada, "DENEGADA");

                escritor.println("Permiso denegado a " + solicitante + ". Se le notificará su decisión.");
            } else {
                escritor.println("Solicitud de " + solicitante + " marcada como pendiente.");
                lineasRestantes.add(solicitud); // Vuelve a guardar la solicitud
            }
        }

        // 3. Reescribir el archivo solo con las denegadas/pendientes
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(archivo))) {
            for (String linea : lineasRestantes) {
                writer.write(linea);
                writer.newLine();
            }
        }

        escritor.println("--- Fin de solicitudes ---");
    }

    private static synchronized boolean almacenarNuevaSolicitud(String nuevaSolicitud) throws IOException {
        List<String> lineas = new ArrayList<>();
        boolean existe = false;

        if (new File(ARCHIVO_SOLICITUDES).exists()) {
            try (BufferedReader reader = new BufferedReader(new FileReader(ARCHIVO_SOLICITUDES))) {
                String linea;
                while ((linea = reader.readLine()) != null) {
                    if (linea.equals(nuevaSolicitud)) {
                        existe = true;
                    }
                    lineas.add(linea);
                }
            }
        }

        if (existe) return false;

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(ARCHIVO_SOLICITUDES, true))) {
            writer.write(nuevaSolicitud);
            writer.newLine();
        }
        return true;
    }

    // --- MÉTODOS AUXILIARES DE DECISIONES ---

    private static synchronized void almacenarDecision(String solicitante, String propietario, String clave, String decision) throws IOException {
        String nuevaDecision = solicitante + ":" + propietario + ":" + clave + ":" + decision;

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(ARCHIVO_DECISIONES, true))) {
            writer.write(nuevaDecision);
            writer.newLine();
        }
    }

    private static synchronized void eliminarDecision(String solicitante, String propietario, String clave) throws IOException {
        File archivoDecisiones = new File(ARCHIVO_DECISIONES);
        if (!archivoDecisiones.exists()) return;

        List<String> lineasRestantes = new ArrayList<>();
        String decisionBuscadaBase = solicitante + ":" + propietario + ":" + clave;

        try (BufferedReader reader = new BufferedReader(new FileReader(archivoDecisiones))) {
            String linea;
            while ((linea = reader.readLine()) != null) {
                if (!linea.startsWith(decisionBuscadaBase)) {
                    lineasRestantes.add(linea);
                }
            }
        }

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(archivoDecisiones))) {
            for (String linea : lineasRestantes) {
                writer.write(linea);
                writer.newLine();
            }
        }
    }

    private static String buscarDecisionPersistente(String solicitante, String propietario, String clave) throws IOException {
        File archivoDecisiones = new File(ARCHIVO_DECISIONES);
        if (!archivoDecisiones.exists()) return null;

        try (BufferedReader reader = new BufferedReader(new FileReader(archivoDecisiones))) {
            String linea;
            while ((linea = reader.readLine()) != null) {
                String[] partes = linea.split(":");
                // Formato: SOLICITANTE:PROPIETARIO:CLAVE:DECISION
                if (partes.length == 4 && partes[0].equals(solicitante) && partes[1].equals(propietario) && partes[2].equals(clave)) {
                    return partes[3];
                }
            }
        }
        return null;
    }

    // --- MÉTODOS DE LÓGICA DE NEGOCIO ---

    private static boolean esContrasenaValida(String contrasena) {
        return contrasena != null && !contrasena.trim().isEmpty() && contrasena.length() >= 8;
    }

    private static void jugarAdivinarNumero(BufferedReader lector, PrintWriter escritor) throws IOException {
        escritor.println("¡Bienvenido al juego de adivinar! Elige un número del 1 al 10. Solo tienes 3 intentos.");
        Random random = new Random();
        int numeroSecreto = random.nextInt(10) + 1;
        int intentosTotales = 3;

        for (int intento = 1; intento <= intentosTotales; intento++) {
            String entrada = lector.readLine();
            if (entrada == null) break;

            int numeroIngresado;
            try {
                numeroIngresado = Integer.parseInt(entrada);
            } catch (NumberFormatException e) {
                int intentosRestantes = intentosTotales - (intento - 1);
                escritor.println("Eso no es un número válido. Te quedan " + (intentosRestantes) + " intentos.");
                intento--;
                continue;
            }

            if (numeroIngresado < 1 || numeroIngresado > 10) {
                int intentosRestantes = intentosTotales - (intento - 1);
                escritor.println("Número fuera de rango (1-10). Te quedan " + (intentosRestantes) + " intentos.");
                intento--;
                continue;
            }

            if (numeroIngresado == numeroSecreto) {
                escritor.println("¡Genial! Adivinaste el número correcto. FIN_JUEGO");
                return;
            } else if (intento < intentosTotales) {
                int intentosRestantes = intentosTotales - intento;
                if (numeroIngresado < numeroSecreto) {
                    escritor.println("El número secreto es más alto. Te quedan " + intentosRestantes + " intentos.");
                } else {
                    escritor.println("El número secreto es más bajo. Te quedan " + intentosRestantes + " intentos.");
                }
            }
        }
        escritor.println("No lograste adivinar el número. Era: " + numeroSecreto + ". Mejor suerte la próxima vez. FIN_JUEGO");
    }

    private static void enviarMensaje(String remitente, BufferedReader lector, PrintWriter escritor) throws IOException {
        escritor.println("¿Para quién es el mensaje? Escribe '[V]' para volver.");
        String destinatario = lector.readLine();
        if (destinatario == null || destinatario.equalsIgnoreCase("V")) {
            escritor.println("Volviendo al menú principal.");
            return;
        }

        if (destinatario.equals(remitente)) {
            escritor.println("Error: No puedes enviarte un mensaje a ti mismo.");
            return;
        }
        if (!verificarUsuarioExiste(destinatario)) {
            escritor.println("Error: El usuario '" + destinatario + "' no existe.");
            return;
        }

        // Nueva lógica para un bloqueo bidireccional
        if (estaBloqueado(remitente, destinatario) || estaBloqueado(destinatario, remitente)) {
            escritor.println("Error: No puedes enviar un mensaje a este usuario debido a una restricción de bloqueo.");
            return;
        }

        escritor.println("Escribe tu mensaje:");
        String mensaje = lector.readLine();
        if (mensaje == null || mensaje.equalsIgnoreCase("V")) {
            escritor.println("Volviendo al menú principal.");
            return;
        }
        if (mensaje.trim().isEmpty()) {
            escritor.println("Error: No se puede enviar un mensaje vacío.");
            return;
        }

        synchronized (Servidor2025.class) {
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(ARCHIVO_MENSAJES, true))) {
                writer.write(destinatario + ":" + remitente + ":" + mensaje);
                writer.newLine();
                escritor.println("Mensaje enviado exitosamente a " + destinatario);
            } catch (IOException e) {
                escritor.println("Error al guardar el mensaje.");
                e.printStackTrace();
            }
        }
    }

    private static void manejarLecturaMensajes(String usuario, BufferedReader lector, PrintWriter escritor) throws IOException {
        escritor.println("Deseas ver [1] Todos tus mensajes o [2] Mensajes de un usuario específico?");
        String opcionLectura = lector.readLine();

        if ("1".equals(opcionLectura)) {
            leerMensajesDesdeArchivo(usuario, null, lector, escritor);
        } else if ("2".equals(opcionLectura)) {
            escritor.println("Escribe el nombre del usuario del que quieres leer los mensajes:");
            String filtroUsuario = lector.readLine();
            if (filtroUsuario == null) return;
            leerMensajesDesdeArchivo(usuario, filtroUsuario, lector, escritor);
        } else {
            escritor.println("Opción de lectura no válida.");
        }
    }

    private static void leerMensajesDesdeArchivo(String usuario, String filtroUsuario, BufferedReader lector, PrintWriter escritor) throws IOException {
        List<String> mensajesRecibidos = new ArrayList<>();
        File archivo = new File(ARCHIVO_MENSAJES);

        if (archivo.exists()) {
            try (BufferedReader reader = new BufferedReader(new FileReader(archivo))) {
                String linea;
                while ((linea = reader.readLine()) != null) {
                    String[] partes = linea.split(":", 3);
                    if (partes.length == 3 && partes[0].equals(usuario) && (filtroUsuario == null || partes[1].equals(filtroUsuario))) {
                        mensajesRecibidos.add(linea);
                    }
                }
            }
        }

        if (mensajesRecibidos.isEmpty()) {
            escritor.println("No tienes mensajes.");
            escritor.println("FIN_PAGINA");
            return;
        }

        int mensajesPorPagina = 10;
        int totalPaginas = (int) Math.ceil((double) mensajesRecibidos.size() / mensajesPorPagina);
        int paginaActual = 1;

        while (true) {
            escritor.println("--- Mensajes (Página " + paginaActual + " de " + totalPaginas + ") ---");
            int inicio = (paginaActual - 1) * mensajesPorPagina;
            int fin = Math.min(inicio + mensajesPorPagina, mensajesRecibidos.size());

            for (int i = inicio; i < fin; i++) {
                String[] partes = mensajesRecibidos.get(i).split(":", 3);
                escritor.println("De [" + partes[1] + "]: " + partes[2]);
            }

            if (totalPaginas > 1) {
                if (paginaActual < totalPaginas) {
                    escritor.println("[N] Siguiente página");
                }
                if (paginaActual > 1) {
                    escritor.println("[A] Anterior página");
                }
            }
            escritor.println("[V] Volver al menú principal");
            escritor.println("FIN_PAGINA");

            String opcionCliente = lector.readLine();
            if (opcionCliente == null) break;

            if (opcionCliente.equalsIgnoreCase("N") && paginaActual < totalPaginas) {
                paginaActual++;
            } else if (opcionCliente.equalsIgnoreCase("A") && paginaActual > 1) {
                paginaActual--;
            } else if (opcionCliente.equalsIgnoreCase("V")) {
                break;
            } else {
                escritor.println("Opción no válida.");
            }
        }
    }

    private static void eliminarMensajes(String usuario, BufferedReader lector, PrintWriter escritor, String tipo) throws IOException {
        File archivo = new File(ARCHIVO_MENSAJES);
        if (!archivo.exists()) {
            escritor.println("No tienes mensajes para eliminar.");
            escritor.println("FIN_PAGINA");
            return;
        }

        List<String> todasLasLineas = new ArrayList<>();
        List<String> mensajesDeUsuario = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(archivo))) {
            String linea;
            while ((linea = reader.readLine()) != null) {
                todasLasLineas.add(linea);
                String[] partes = linea.split(":", 3);
                if (partes.length == 3) {
                    if (tipo.equals("recibidos") && partes[0].equals(usuario)) {
                        mensajesDeUsuario.add(linea);
                    } else if (tipo.equals("enviados") && partes[1].equals(usuario)) {
                        mensajesDeUsuario.add(linea);
                    }
                }
            }
        }

        if (mensajesDeUsuario.isEmpty()) {
            escritor.println("No tienes mensajes " + tipo + ".");
            escritor.println("FIN_PAGINA");
            return;
        }

        int mensajesPorPagina = 10;
        int totalPaginas = (int) Math.ceil((double) mensajesDeUsuario.size() / mensajesPorPagina);
        int paginaActual = 1;

        while (true) {
            escritor.println("--- Mensajes " + tipo + " (Página " + paginaActual + " de " + totalPaginas + ") ---");
            int inicio = (paginaActual - 1) * mensajesPorPagina;
            int fin = Math.min(inicio + mensajesPorPagina, mensajesDeUsuario.size());

            for (int i = inicio; i < fin; i++) {
                String[] partes = mensajesDeUsuario.get(i).split(":", 3);
                String deOpara = tipo.equals("recibidos") ? "De [" + partes[1] + "]" : "Para [" + partes[0] + "]";
                escritor.println("[" + (i + 1) + "] " + deOpara + ": " + partes[2]);
            }

            if (totalPaginas > 1) {
                if (paginaActual < totalPaginas) {
                    escritor.println("[N] Siguiente página");
                }
                if (paginaActual > 1) {
                    escritor.println("[A] Anterior página");
                }
            }
            escritor.println("Escribe el número del mensaje a eliminar o [V] para volver:");
            escritor.println("FIN_PAGINA");

            String opcionCliente = lector.readLine();
            if (opcionCliente == null) return;

            if (opcionCliente.equalsIgnoreCase("N") && paginaActual < totalPaginas) {
                paginaActual++;
            } else if (opcionCliente.equalsIgnoreCase("A") && paginaActual > 1) {
                paginaActual--;
            } else if (opcionCliente.equalsIgnoreCase("V")) {
                break;
            } else {
                try {
                    int seleccion = Integer.parseInt(opcionCliente);
                    if (seleccion >= 1 && seleccion <= mensajesDeUsuario.size()) {
                        String mensajeAEliminar = mensajesDeUsuario.get(seleccion - 1);
                        todasLasLineas.remove(mensajeAEliminar);
                        reescribirArchivo(todasLasLineas);
                        escritor.println("Mensaje eliminado exitosamente.");
                    } else {
                        escritor.println("Selección fuera de rango.");
                    }
                } catch (NumberFormatException e) {
                    escritor.println("Entrada inválida.");
                }
                break;
            }
        }
    }

    private static void reescribirArchivo(List<String> lineas) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(ARCHIVO_MENSAJES))) {
            for (String linea : lineas) {
                writer.write(linea);
                writer.newLine();
            }
        }
    }

    private static boolean verificarUsuarioExiste(String usuario) throws IOException {
        File archivo = new File(ARCHIVO_USUARIOS);
        if (!archivo.exists()) return false;

        try (BufferedReader lector = new BufferedReader(new FileReader(archivo))) {
            String linea;
            while ((linea = lector.readLine()) != null) {
                String[] partes = linea.split(":");
                if (partes.length > 0 && partes[0].equals(usuario)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean verificarCredenciales(String usuario, String contrasena) throws IOException {
        File archivo = new File(ARCHIVO_USUARIOS);
        if (!archivo.exists()) return false;

        try (BufferedReader lector = new BufferedReader(new FileReader(archivo))) {
            String linea;
            while ((linea = lector.readLine()) != null) {
                String[] partes = linea.split(":");
                if (partes.length == 2 && partes[0].equals(usuario) && partes[1].equals(contrasena)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean registrarUsuario(String usuario, String contrasena) throws IOException {
        synchronized (Servidor2025.class) {
            if (verificarUsuarioExiste(usuario)) {
                return false;
            }
            try (BufferedWriter escritor = new BufferedWriter(new FileWriter(ARCHIVO_USUARIOS, true))) {
                escritor.write(usuario + ":" + contrasena);
                escritor.newLine();
                // Crea la carpeta de archivos del usuario
                new File(DIRECTORIO_RAIZ + File.separator + usuario).mkdirs();
                return true;
            }
        }
    }

    private static void mostrarUsuariosRegistrados(PrintWriter escritor) {
        File archivo = new File(ARCHIVO_USUARIOS);
        escritor.println("--- Usuarios Registrados ---");
        if (!archivo.exists()) {
            escritor.println("No hay usuarios registrados.");
        } else {
            try (BufferedReader lector = new BufferedReader(new FileReader(archivo))) {
                String linea;
                while ((linea = lector.readLine()) != null) {
                    String[] partes = linea.split(":");
                    if (partes.length >= 1) {
                        escritor.println("- " + partes[0]);
                    }
                }
            } catch (IOException e) {
                escritor.println("Error al leer usuarios.");
            }
        }
        escritor.println("FIN_USUARIOS");
    }

    private static void bloquearDesbloquearUsuario(String usuario, BufferedReader lector, PrintWriter escritor) throws IOException {
        escritor.println("Escribe el usuario que quieres bloquear o desbloquear, o [V] para volver.");
        String usuarioATocar = lector.readLine();
        if (usuarioATocar == null || usuarioATocar.equalsIgnoreCase("V")) {
            escritor.println("Volviendo al menú principal.");
            return;
        }

        if (usuarioATocar.equals(usuario)) {
            escritor.println("Error: No puedes bloquearte o desbloquearte a ti mismo.");
            return;
        }

        if (!verificarUsuarioExiste(usuarioATocar)) {
            escritor.println("Error: El usuario '" + usuarioATocar + "' no existe.");
            return;
        }

        if (estaBloqueado(usuario, usuarioATocar)) {
            escritor.println("El usuario '" + usuarioATocar + "' está bloqueado. ¿Quieres [1] Desbloquearlo o [2] Volver?");
            String opcion = lector.readLine();
            if ("1".equals(opcion)) {
                gestionarBloqueosArchivo(usuario, usuarioATocar, false);
                escritor.println("Usuario '" + usuarioATocar + "' desbloqueado exitosamente.");
            } else {
                escritor.println("Volviendo al menú principal.");
            }
        } else {
            escritor.println("El usuario '" + usuarioATocar + "' no está bloqueado. ¿Quieres [1] Bloquearlo o [2] Volver?");
            String opcion = lector.readLine();
            if ("1".equals(opcion)) {
                gestionarBloqueosArchivo(usuario, usuarioATocar, true);
                escritor.println("Usuario '" + usuarioATocar + "' bloqueado exitosamente.");
            } else {
                escritor.println("Volviendo al menú principal.");
            }
        }
    }

    private static boolean estaBloqueado(String usuario, String bloqueado) throws IOException {
        File archivo = new File(ARCHIVO_BLOQUEOS);
        if (!archivo.exists()) return false;

        try (BufferedReader lector = new BufferedReader(new FileReader(archivo))) {
            String linea;
            while ((linea = lector.readLine()) != null) {
                String[] partes = linea.split(":");
                if (partes.length > 1 && partes[0].equals(usuario)) {
                    String[] bloqueados = partes[1].split(",");
                    for (String b : bloqueados) {
                        if (b.equals(bloqueado)) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    private static void gestionarBloqueosArchivo(String usuario, String usuarioCambio, boolean bloquear) throws IOException {
        synchronized (Servidor2025.class) {
            List<String> lineas = new ArrayList<>();
            File archivo = new File(ARCHIVO_BLOQUEOS);
            if (archivo.exists()) {
                try (BufferedReader lector = new BufferedReader(new FileReader(archivo))) {
                    String linea;
                    while ((linea = lector.readLine()) != null) {
                        lineas.add(linea);
                    }
                }
            }

            int indiceUsuario = -1;
            for (int i = 0; i < lineas.size(); i++) {
                if (lineas.get(i).startsWith(usuario + ":")) {
                    indiceUsuario = i;
                    break;
                }
            }

            if (bloquear) {
                if (indiceUsuario != -1) {
                    String lineaActual = lineas.get(indiceUsuario);
                    if (!lineaActual.contains(usuarioCambio)) {
                        lineas.set(indiceUsuario, lineaActual + "," + usuarioCambio);
                    }
                } else {
                    lineas.add(usuario + ":" + usuarioCambio);
                }
            } else { // Desbloquear
                if (indiceUsuario != -1) {
                    String lineaActual = lineas.get(indiceUsuario);
                    String[] bloqueados = lineaActual.split(":")[1].split(",");
                    List<String> listaBloqueados = new ArrayList<>(Arrays.asList(bloqueados));
                    listaBloqueados.remove(usuarioCambio);
                    String nuevaLinea = usuario + ":" + String.join(",", listaBloqueados);
                    if (listaBloqueados.isEmpty()) {
                        lineas.remove(indiceUsuario);
                    } else {
                        lineas.set(indiceUsuario, nuevaLinea);
                    }
                }
            }

            try (BufferedWriter escritor = new BufferedWriter(new FileWriter(archivo))) {
                for (String linea : lineas) {
                    escritor.write(linea);
                    escritor.newLine();
                }
            }
        }
    }

    private static void listarArchivosDeUsuario(String solicitante, BufferedReader lector, PrintWriter escritor) throws IOException {
        escritor.println("Escribe el nombre del usuario del que quieres ver los archivos:");
        String usuarioObjetivo = lector.readLine();

        // Lógica para el caso de Listar los propios archivos
        if (solicitante.equalsIgnoreCase(usuarioObjetivo)) {
            realizarListado(usuarioObjetivo, escritor);
            return;
        }

        if (!verificarUsuarioExiste(usuarioObjetivo)) {
            escritor.println("Error: El usuario '" + usuarioObjetivo + "' no existe.");
            escritor.println("FIN_LISTA_ARCHIVOS");
            return;
        }

        // --------------------------------------------------------------------
        // 1. Verificar Permiso en Memoria o Persistente
        // --------------------------------------------------------------------
        boolean permisoConcedidoMemoria = PERMISOS_CONCEDIDOS.getOrDefault(usuarioObjetivo, Collections.emptyMap())
                .getOrDefault(solicitante, "").equals(CLAVE_LISTAR);

        String decisionPersistente = buscarDecisionPersistente(solicitante, usuarioObjetivo, CLAVE_LISTAR);

        if (permisoConcedidoMemoria || "ACEPTADA".equals(decisionPersistente)) {

            if ("ACEPTADA".equals(decisionPersistente)) {
                // Si la decisión estaba persistente, la notificamos (resguardo) y la eliminamos.
                escritor.println("NOTIFICACIÓN: El usuario " + usuarioObjetivo + " ha ACEPTADO tu solicitud para listar sus archivos.");
                eliminarDecision(solicitante, usuarioObjetivo, CLAVE_LISTAR);
            } else {
                escritor.println("Permiso concedido para listar archivos.");
            }

            // 2. Realizar el listado
            realizarListado(usuarioObjetivo, escritor);

            // 3. Eliminar permiso de memoria después de usarlo (es de un solo uso)
            PERMISOS_CONCEDIDOS.getOrDefault(usuarioObjetivo, Collections.emptyMap()).remove(solicitante);

        } else if ("DENEGADA".equals(decisionPersistente)) {
            // Notificar denegación y limpiar persistencia
            escritor.println("NOTIFICACIÓN: El usuario " + usuarioObjetivo + " ha DENEGADO tu solicitud para listar sus archivos.");
            eliminarDecision(solicitante, usuarioObjetivo, CLAVE_LISTAR);
            escritor.println("FIN_LISTA_ARCHIVOS");

        } else {
            // 3. Crear Nueva Solicitud
            String nuevaSolicitud = usuarioObjetivo + ":" + solicitante + ":" + CLAVE_LISTAR;
            if (almacenarNuevaSolicitud(nuevaSolicitud)) {
                escritor.println("Permiso no concedido. Se ha registrado una solicitud a '" + usuarioObjetivo + "'. Deberá esperar a que el usuario se conecte para responder.");
            } else {
                escritor.println("Permiso no concedido. Ya existe una solicitud pendiente.");
            }
            escritor.println("FIN_LISTA_ARCHIVOS");
        }
    }

    private static void realizarListado(String usuarioObjetivo, PrintWriter escritor) {
        File directorioUsuario = new File(DIRECTORIO_RAIZ + File.separator + usuarioObjetivo);

        if (!directorioUsuario.exists() || !directorioUsuario.isDirectory()) {
            escritor.println("Error: El usuario no tiene una carpeta de archivos.");
            escritor.println("FIN_LISTA_ARCHIVOS");
            return;
        }

        File[] archivos = directorioUsuario.listFiles((dir, name) -> name.toLowerCase().endsWith(".txt"));

        if (archivos == null || archivos.length == 0) {
            escritor.println("El usuario '" + usuarioObjetivo + "' no tiene archivos de texto.");
            escritor.println("FIN_LISTA_ARCHIVOS");
            return;
        }

        escritor.println("--- Archivos de '" + usuarioObjetivo + "' ---");
        for (File archivo : archivos) {
            escritor.println(archivo.getName());
        }
        escritor.println("FIN_LISTA_ARCHIVOS");
    }

    private static void crearArchivoRemoto(String usuario, BufferedReader lector, PrintWriter escritor) throws IOException {
        escritor.println("Escribe el nombre del archivo de texto (ej: archivo.txt):");
        String nombreArchivo = lector.readLine();

        if (!nombreArchivo.toLowerCase().endsWith(".txt")) {
            escritor.println("Error: El nombre del archivo debe terminar con .txt");
            return;
        }

        File archivoDestino = new File(DIRECTORIO_RAIZ + File.separator + usuario + File.separator + nombreArchivo);

        if (archivoDestino.exists()) {
            escritor.println("Error: Ya existe un archivo con ese nombre en tu carpeta.");
            return;
        }

        escritor.println("Escribe el contenido del archivo. Escribe 'FIN_CONTENIDO' en una nueva linea para terminar.");

        try (BufferedWriter escritorArchivo = new BufferedWriter(new FileWriter(archivoDestino))) {
            String linea;
            while ((linea = lector.readLine()) != null && !linea.equals("FIN_CONTENIDO")) {
                escritorArchivo.write(linea);
                escritorArchivo.newLine();
            }
            escritor.println("Archivo '" + nombreArchivo + "' creado exitosamente.");
        } catch (IOException e) {
            escritor.println("Error al crear el archivo en el servidor.");
            e.printStackTrace();
        }
    }

    private static void descargarArchivo(String solicitante, BufferedReader lector, PrintWriter escritor) throws IOException {
        escritor.println("Escribe el nombre del usuario de donde quieres descargar el archivo:");
        String usuarioObjetivo = lector.readLine();
        escritor.println("Escribe el nombre del archivo de texto (ej: archivo.txt):");
        String nombreArchivo = lector.readLine();

        File archivoFuente = new File(DIRECTORIO_RAIZ + File.separator + usuarioObjetivo + File.separator + nombreArchivo);

        if (!archivoFuente.exists() || !archivoFuente.isFile() || !nombreArchivo.toLowerCase().endsWith(".txt")) {
            escritor.println("Error: El archivo no existe o no es un archivo de texto.");
            escritor.println("FIN_DESCARGA_ARCHIVO");
            return;
        }

        // --- LÓGICA: Buscar decisiones en el ARCHIVO_DECISIONES ---
        String decisionPersistente = buscarDecisionPersistente(solicitante, usuarioObjetivo, nombreArchivo);

        // Lógica principal basada en la persistencia o permisos en memoria
        boolean permisoConcedidoMemoria = PERMISOS_CONCEDIDOS.getOrDefault(usuarioObjetivo, Collections.emptyMap())
                .getOrDefault(solicitante, "").equals(nombreArchivo);

        if (permisoConcedidoMemoria || "ACEPTADA".equals(decisionPersistente)) {

            if ("ACEPTADA".equals(decisionPersistente)) {
                escritor.println("NOTIFICACIÓN: El usuario " + usuarioObjetivo + " ha ACEPTADO tu solicitud.");
                eliminarDecision(solicitante, usuarioObjetivo, nombreArchivo); // Limpieza
            } else {
                escritor.println("Permiso concedido para la descarga.");
            }

            escritor.println("¿Deseas [1] Descargar ahora o [2] Descargar después?");
            String opcionCliente = lector.readLine();

            if ("1".equals(opcionCliente)) {

                try (BufferedReader lectorArchivo = new BufferedReader(new FileReader(archivoFuente))) {
                    String linea;
                    while ((linea = lectorArchivo.readLine()) != null) {
                        escritor.println(linea);
                    }
                    escritor.println("FIN_DESCARGA_ARCHIVO");

                    // Si el permiso estaba en memoria, también lo eliminamos (es de un solo uso)
                    PERMISOS_CONCEDIDOS.getOrDefault(usuarioObjetivo, Collections.emptyMap()).remove(solicitante);

                } catch (IOException e) {
                    escritor.println("Error al leer el archivo en el servidor.");
                    escritor.println("FIN_DESCARGA_ARCHIVO");
                }
                return;

            } else if ("2".equals(opcionCliente)) {
                // Si elige descargar después.
                escritor.println("Descarga pospuesta. Puedes iniciarla nuevamente con la opción 11.");
                escritor.println("FIN_DESCARGA_ARCHIVO");
                return;
            } else {
                escritor.println("Opción no válida. Volviendo al menú.");
                escritor.println("FIN_DESCARGA_ARCHIVO");
                return;
            }

        } else if ("DENEGADA".equals(decisionPersistente)) {
            // Si la decisión fue DENEGADA previamente, lo notificamos y eliminamos el registro.
            escritor.println("NOTIFICACIÓN: El usuario " + usuarioObjetivo + " ha DENEGADO tu solicitud.");
            eliminarDecision(solicitante, usuarioObjetivo, nombreArchivo);
            escritor.println("FIN_DESCARGA_ARCHIVO");
            return;

        } else {
            // No hay permiso ni decisión previa. Generar nueva solicitud.
            String nuevaSolicitud = usuarioObjetivo + ":" + solicitante + ":" + nombreArchivo;
            if (almacenarNuevaSolicitud(nuevaSolicitud)) {
                escritor.println("Permiso no concedido. Se ha registrado una solicitud a '" + usuarioObjetivo + "'. Deberá esperar a que el usuario se conecte para responder.");
            } else {
                escritor.println("Permiso no concedido. Ya existe una solicitud pendiente.");
            }
            escritor.println("FIN_DESCARGA_ARCHIVO");
            return;
        }
    }

    private static void transferirArchivoInterno(String remitente, BufferedReader lector, PrintWriter escritor) throws IOException {
        escritor.println("Escribe el nombre del usuario al que quieres transferir el archivo:");
        String usuarioDestino = lector.readLine();
        escritor.println("Escribe el nombre del archivo de tu carpeta que quieres transferir:");
        String nombreArchivo = lector.readLine();

        if (usuarioDestino.equals(remitente)) {
            escritor.println("Error: No puedes transferir un archivo a tu propia carpeta.");
            return;
        }

        File archivoFuente = new File(DIRECTORIO_RAIZ + File.separator + remitente + File.separator + nombreArchivo);
        File archivoDestino = new File(DIRECTORIO_RAIZ + File.separator + usuarioDestino + File.separator + nombreArchivo);

        if (!archivoFuente.exists() || !archivoFuente.isFile() || !nombreArchivo.toLowerCase().endsWith(".txt")) {
            escritor.println("Error: El archivo no existe o no es un archivo de texto en tu carpeta.");
            return;
        }

        if (archivoDestino.exists()) {
            escritor.println("Error: El usuario de destino ya tiene un archivo con ese nombre.");
            return;
        }

        try (
                InputStream in = new FileInputStream(archivoFuente);
                OutputStream out = new FileOutputStream(archivoDestino)
        ) {
            byte[] buffer = new byte[1024];
            int length;
            while ((length = in.read(buffer)) > 0) {
                out.write(buffer, 0, length);
            }
            escritor.println("Archivo '" + nombreArchivo + "' transferido exitosamente a la carpeta de '" + usuarioDestino + "'.");
        } catch (IOException e) {
            escritor.println("Error al transferir el archivo.");
            e.printStackTrace();
        }
    }
}
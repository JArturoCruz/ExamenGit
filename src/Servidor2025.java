import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class Servidor2025 {

    private static final int PUERTO = 8081;
    private static final String DIRECTORIO_RAIZ = "servidor_archivos";

    private static final String ARCHIVO_USUARIOS = "usuarios.txt";
    private static final String ARCHIVO_MENSAJES = "mensajes.txt";
    private static final String ARCHIVO_BLOQUEOS = "bloqueos.txt";
    private static final String ARCHIVO_SOLICITUDES = "solicitudes.txt";
    private static final String ARCHIVO_DECISIONES = "decisiones_descarga.txt";

    private static final String CLAVE_LISTAR = "LISTAR_ARCHIVOS";

    private static final Map<String, Map<String, String>> PERMISOS_CONCEDIDOS = new ConcurrentHashMap<>();

    public static void main(String[] args) {
        new File(DIRECTORIO_RAIZ).mkdirs();

        try (ServerSocket serverSocket = new ServerSocket(PUERTO)) {
            System.out.println("Servidor listo en el puerto " + PUERTO + ". Esperando clientes...");

            while (true) {
                Socket clienteSocket = serverSocket.accept();
                System.out.println("Cliente conectado: " + clienteSocket.getInetAddress());
                new Thread(() -> manejarCliente(clienteSocket)).start();
            }
        } catch (IOException e) {
            System.err.println("Ocurrió un error crítico en el servidor: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void manejarCliente(Socket cliente) {
        String usuarioAutenticado = null;
        try (
                PrintWriter escritor = new PrintWriter(cliente.getOutputStream(), true);
                BufferedReader lector = new BufferedReader(new InputStreamReader(cliente.getInputStream()))
        ) {
            usuarioAutenticado = procesoDeAutenticacion(lector, escritor);

            if (usuarioAutenticado == null) {
                cliente.close();
                return;
            }

            mostrarDecisionesPendientes(usuarioAutenticado, escritor);
            manejarSolicitudesPendientes(usuarioAutenticado, lector, escritor);
            buclePrincipalDeMenu(usuarioAutenticado, lector, escritor, cliente);

        } catch (IOException e) {
            System.err.println("Error de comunicación con el cliente '" + (usuarioAutenticado != null ? usuarioAutenticado : "desconocido") + "': " + e.getMessage());
        } finally {
            if (usuarioAutenticado != null) {
                PERMISOS_CONCEDIDOS.remove(usuarioAutenticado);
            }
            try {
                if (!cliente.isClosed()) {
                    cliente.close();
                }
            } catch (IOException e) {
                System.err.println("Error al cerrar el socket del cliente: " + e.getMessage());
            }
        }
    }

    private static String procesoDeAutenticacion(BufferedReader lector, PrintWriter escritor) throws IOException {
        escritor.println("Bienvenido. ¿Deseas [1] Iniciar sesión, [2] Registrarte?");
        String opcion = lector.readLine();
        String usuario = null;

        if ("1".equals(opcion)) {
            escritor.println("Usuario:");
            String user = lector.readLine();
            escritor.println("Contraseña:");
            String pass = lector.readLine();
            if (verificarCredenciales(user, pass)) {
                escritor.println("Autenticación exitosa");
                usuario = user;
            } else {
                escritor.println("Error: Usuario o contraseña incorrectos.");
            }
        } else if ("2".equals(opcion)) {
            escritor.println("Usuario:");
            String user = lector.readLine();
            escritor.println("Contraseña:");
            String pass = lector.readLine();
            if (!esContrasenaValida(pass)) {
                escritor.println("Contraseña no válida. Debe tener al menos 8 caracteres.");
            } else if (registrarUsuario(user, pass)) {
                escritor.println("Usuario registrado exitosamente");
                usuario = user;
            } else {
                escritor.println("Error: El usuario ya existe.");
            }
        } else {
            escritor.println("Opción no válida.");
        }
        return usuario;
    }

    private static void buclePrincipalDeMenu(String usuario, BufferedReader lector, PrintWriter escritor, Socket cliente) throws IOException {
        String opcionMenu;
        do {
            escritor.println("Menú: [1] Jugar | [2] Enviar mensaje | [3] Leer mensajes | [4] Cerrar sesión | [5] Eliminar mensaje | [6] Ver usuarios | [7] Bloquear/Desbloquear | [8] Archivos");
            opcionMenu = lector.readLine();
            if (opcionMenu == null) break;

            System.out.println("Opción de '" + usuario + "': '" + opcionMenu + "'");

            switch (opcionMenu) {
                case "1": jugarAdivinarNumero(lector, escritor); break;
                case "2": enviarMensaje(usuario, lector, escritor); break;
                case "3": manejarLecturaMensajes(usuario, lector, escritor); break;
                case "4":
                    System.out.println("Cliente " + usuario + " ha cerrado sesión.");
                    cliente.close();
                    return;
                case "5": eliminarMensajes(usuario, lector, escritor); break;
                case "6": mostrarUsuariosRegistrados(escritor); break;
                case "7": bloquearDesbloquearUsuario(usuario, lector, escritor); break;
                case "8": menuDeArchivos(usuario, lector, escritor); break;
                default: escritor.println("Opción de menú no válida."); break;
            }
        } while (opcionMenu != null && !"4".equals(opcionMenu));
    }

    private static void menuDeArchivos(String usuario, BufferedReader lector, PrintWriter escritor) throws IOException {
        escritor.println("Menú Archivos: [1] Listar | [2] Crear | [3] Descargar | [4] Transferir | [V] Volver");
        String opcion = lector.readLine();
        switch (opcion) {
            case "1": listarArchivosDeUsuario(usuario, lector, escritor); break;
            case "2": crearArchivoRemoto(usuario, lector, escritor); break;
            case "3": descargarArchivo(usuario, lector, escritor); break;
            case "4": transferirArchivoInterno(usuario, lector, escritor); break;
            case "V": break;
            default: escritor.println("Opción de archivos no válida."); break;
        }
    }

    private static void jugarAdivinarNumero(BufferedReader lector, PrintWriter escritor) throws IOException {
        escritor.println("¡Bienvenido al juego de adivinar! Elige un número del 1 al 10. Solo tienes 3 intentos.");
        Random random = new Random();
        int numeroSecreto = random.nextInt(10) + 1;
        int intentosTotales = 3;
        for (int intento = 1; intento <= intentosTotales; intento++) {
            String entrada = lector.readLine();
            if (entrada == null) break;
            try {
                int numeroIngresado = Integer.parseInt(entrada);
                if (numeroIngresado < 1 || numeroIngresado > 10) {
                    escritor.println("Número fuera de rango (1-10). Te quedan " + (intentosTotales - intento + 1) + " intentos.");
                    intento--;
                    continue;
                }
                if (numeroIngresado == numeroSecreto) {
                    escritor.println("¡Genial! Adivinaste el número correcto. FIN_JUEGO");
                    return;
                } else if (intento < intentosTotales) {
                    String pista = numeroIngresado < numeroSecreto ? "más alto" : "más bajo";
                    escritor.println("El número secreto es " + pista + ". Te quedan " + (intentosTotales - intento) + " intentos.");
                }
            } catch (NumberFormatException e) {
                escritor.println("Eso no es un número válido. Te quedan " + (intentosTotales - intento + 1) + " intentos.");
                intento--;
            }
        }
        escritor.println("No lograste adivinar. Era: " + numeroSecreto + ". FIN_JUEGO");
    }

    private static void enviarMensaje(String remitente, BufferedReader lector, PrintWriter escritor) throws IOException {
        escritor.println("¿Para quién es el mensaje? Escribe '[V]' para volver.");
        String destinatario = lector.readLine();
        if (destinatario == null || destinatario.equalsIgnoreCase("V")) return;

        if (destinatario.equals(remitente)) {
            escritor.println("Error: No puedes enviarte un mensaje a ti mismo.");
            return;
        }
        if (!verificarUsuarioExiste(destinatario)) {
            escritor.println("Error: El usuario '" + destinatario + "' no existe.");
            return;
        }
        if (estaBloqueado(remitente, destinatario) || estaBloqueado(destinatario, remitente)) {
            escritor.println("Error: No puedes enviar un mensaje a este usuario (bloqueo).");
            return;
        }
        escritor.println("Escribe tu mensaje:");
        String mensaje = lector.readLine();
        if (mensaje == null || mensaje.trim().isEmpty()) {
            escritor.println("Error: No se puede enviar un mensaje vacío.");
            return;
        }
        synchronized (Servidor2025.class) {
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(ARCHIVO_MENSAJES, true))) {
                writer.write(destinatario + ":" + remitente + ":" + mensaje);
                writer.newLine();
                escritor.println("Mensaje enviado exitosamente a " + destinatario);
            }
        }
    }

    private static void manejarLecturaMensajes(String usuario, BufferedReader lector, PrintWriter escritor) throws IOException {
        escritor.println("Deseas ver [1] Todos tus mensajes o [2] Mensajes de un usuario específico?");
        String opcionLectura = lector.readLine();
        if ("1".equals(opcionLectura)) {
            leerMensajesPaginados(usuario, null, lector, escritor);
        } else if ("2".equals(opcionLectura)) {
            escritor.println("Escribe el nombre del usuario:");
            String filtroUsuario = lector.readLine();
            if (filtroUsuario != null) {
                leerMensajesPaginados(usuario, filtroUsuario, lector, escritor);
            }
        } else {
            escritor.println("Opción no válida.");
        }
    }

    private static void leerMensajesPaginados(String usuario, String filtroUsuario, BufferedReader lector, PrintWriter escritor) throws IOException {
        List<String> mensajesRecibidos = new ArrayList<>();
        synchronized (Servidor2025.class) {
            File archivo = new File(ARCHIVO_MENSAJES);
            if (archivo.exists()) {
                try (BufferedReader reader = new BufferedReader(new FileReader(archivo))) {
                    String linea;
                    while ((linea = reader.readLine()) != null) {
                        String[] partes = linea.split(":", 3);
                        if (partes.length == 3 && partes[0].equals(usuario) && (filtroUsuario == null || partes[1].equals(filtroUsuario))) {
                            mensajesRecibidos.add("De [" + partes[1] + "]: " + partes[2]);
                        }
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
            mensajesRecibidos.stream()
                    .skip((long)(paginaActual - 1) * mensajesPorPagina)
                    .limit(mensajesPorPagina)
                    .forEach(escritor::println);

            if (paginaActual < totalPaginas) escritor.println("[N] Siguiente");
            if (paginaActual > 1) escritor.println("[A] Anterior");
            escritor.println("[V] Volver");
            escritor.println("FIN_PAGINA");

            String opcion = lector.readLine();
            if (opcion == null || opcion.equalsIgnoreCase("V")) break;
            if (opcion.equalsIgnoreCase("N") && paginaActual < totalPaginas) paginaActual++;
            else if (opcion.equalsIgnoreCase("A") && paginaActual > 1) paginaActual--;
        }
    }

    private static void eliminarMensajes(String usuario, BufferedReader lector, PrintWriter escritor) throws IOException {
        escritor.println("¿Qué mensajes deseas eliminar? [1] Recibidos | [2] Enviados");
        String tipo = lector.readLine();
        String tipoStr = "1".equals(tipo) ? "recibidos" : ("2".equals(tipo) ? "enviados" : null);

        if (tipoStr == null) {
            escritor.println("Opción no válida.");
            return;
        }

        List<String> mensajesDeUsuario = new ArrayList<>();
        List<String> todasLasLineas = new ArrayList<>();
        synchronized (Servidor2025.class) {
            File archivo = new File(ARCHIVO_MENSAJES);
            if (archivo.exists()) {
                try (BufferedReader reader = new BufferedReader(new FileReader(archivo))) {
                    reader.lines().forEach(linea -> {
                        todasLasLineas.add(linea);
                        String[] partes = linea.split(":", 3);
                        if (partes.length == 3) {
                            boolean esRecibido = "recibidos".equals(tipoStr) && partes[0].equals(usuario);
                            boolean esEnviado = "enviados".equals(tipoStr) && partes[1].equals(usuario);
                            if (esRecibido || esEnviado) {
                                mensajesDeUsuario.add(linea);
                            }
                        }
                    });
                }
            }
        }

        if (mensajesDeUsuario.isEmpty()) {
            escritor.println("No tienes mensajes " + tipoStr + " para eliminar.");
            return;
        }

        escritor.println("--- Selecciona el mensaje a eliminar ---");
        for (int i = 0; i < mensajesDeUsuario.size(); i++) {
            String[] partes = mensajesDeUsuario.get(i).split(":", 3);
            String info = "recibidos".equals(tipoStr) ? "De [" + partes[1] + "]" : "Para [" + partes[0] + "]";
            escritor.println("[" + (i + 1) + "] " + info + ": " + partes[2]);
        }
        escritor.println("Escribe el número del mensaje o [V] para cancelar:");

        String seleccionStr = lector.readLine();
        try {
            int seleccion = Integer.parseInt(seleccionStr);
            if (seleccion >= 1 && seleccion <= mensajesDeUsuario.size()) {
                String mensajeAEliminar = mensajesDeUsuario.get(seleccion - 1);
                todasLasLineas.remove(mensajeAEliminar);
                reescribirArchivo(ARCHIVO_MENSAJES, todasLasLineas);
                escritor.println("Mensaje eliminado.");
            } else {
                escritor.println("Selección fuera de rango.");
            }
        } catch (NumberFormatException e) {
            escritor.println("Selección no válida.");
        }
    }

    private static void mostrarUsuariosRegistrados(PrintWriter escritor) {
        escritor.println("--- Usuarios Registrados ---");
        try (BufferedReader br = new BufferedReader(new FileReader(ARCHIVO_USUARIOS))) {
            br.lines().map(line -> line.split(":")[0]).forEach(user -> escritor.println("- " + user));
        } catch (IOException e) {
            escritor.println("Error al leer la lista de usuarios.");
        }
        escritor.println("FIN_COMANDO");
    }

    private static void bloquearDesbloquearUsuario(String usuario, BufferedReader lector, PrintWriter escritor) throws IOException {
        escritor.println("Escribe el usuario que quieres bloquear o desbloquear:");
        String usuarioATocar = lector.readLine();
        if (usuarioATocar == null || usuarioATocar.equals(usuario) || !verificarUsuarioExiste(usuarioATocar)) {
            escritor.println("Error: Usuario no válido o no puedes seleccionarte a ti mismo.");
            return;
        }

        if (estaBloqueado(usuario, usuarioATocar)) {
            escritor.println("El usuario está bloqueado. ¿Desbloquear? [1] Sí");
            if ("1".equals(lector.readLine())) {
                gestionarBloqueosArchivo(usuario, usuarioATocar, false);
                escritor.println("Usuario desbloqueado.");
            }
        } else {
            escritor.println("El usuario no está bloqueado. ¿Bloquear? [1] Sí");
            if ("1".equals(lector.readLine())) {
                gestionarBloqueosArchivo(usuario, usuarioATocar, true);
                escritor.println("Usuario bloqueado.");
            }
        }
    }

    private static void listarArchivosDeUsuario(String solicitante, BufferedReader lector, PrintWriter escritor) throws IOException {
        escritor.println("¿De qué usuario quieres ver los archivos?");
        String propietario = lector.readLine();
        if (propietario == null || !verificarUsuarioExiste(propietario)) {
            escritor.println("Error: Usuario no existe.");
            return;
        }

        if (solicitante.equals(propietario)) {
            realizarListado(propietario, escritor);
            return;
        }

        boolean tienePermiso = PERMISOS_CONCEDIDOS.getOrDefault(propietario, Collections.emptyMap())
                .getOrDefault(solicitante, "").equals(CLAVE_LISTAR);

        if (tienePermiso) {
            realizarListado(propietario, escritor);
            PERMISOS_CONCEDIDOS.get(propietario).remove(solicitante); // Permiso de un solo uso
        } else {
            solicitarPermiso(solicitante, propietario, CLAVE_LISTAR, escritor);
        }
    }

    private static void crearArchivoRemoto(String usuario, BufferedReader lector, PrintWriter escritor) throws IOException {
        escritor.println("Nombre del archivo (debe terminar en .txt):");
        String nombreArchivo = lector.readLine();
        if (nombreArchivo == null || !nombreArchivo.toLowerCase().endsWith(".txt")) {
            escritor.println("Error: Nombre de archivo no válido.");
            return;
        }

        File archivo = new File(DIRECTORIO_RAIZ + File.separator + usuario, nombreArchivo);
        if (archivo.exists()) {
            escritor.println("Error: El archivo ya existe.");
            return;
        }

        escritor.println("Escribe el contenido. Termina con 'FIN_CONTENIDO' en una nueva línea.");
        try (BufferedWriter escritorArchivo = new BufferedWriter(new FileWriter(archivo))) {
            String linea;
            while ((linea = lector.readLine()) != null && !linea.equals("FIN_CONTENIDO")) {
                escritorArchivo.write(linea);
                escritorArchivo.newLine();
            }
            escritor.println("Archivo creado exitosamente.");
        }
    }

    private static void descargarArchivo(String solicitante, BufferedReader lector, PrintWriter escritor) throws IOException {
        escritor.println("Usuario propietario del archivo:");
        String propietario = lector.readLine();
        escritor.println("Nombre del archivo a descargar:");
        String nombreArchivo = lector.readLine();

        if (propietario == null || nombreArchivo == null || !verificarUsuarioExiste(propietario)) {
            escritor.println("Error: Datos inválidos.");
            return;
        }

        File archivo = new File(DIRECTORIO_RAIZ + File.separator + propietario, nombreArchivo);
        if (!archivo.exists()) {
            escritor.println("Error: El archivo no existe.");
            return;
        }

        boolean tienePermiso = PERMISOS_CONCEDIDOS.getOrDefault(propietario, Collections.emptyMap())
                .getOrDefault(solicitante, "").equals(nombreArchivo);

        if (tienePermiso) {
            escritor.println("PERMISO_OK"); // Señal para el cliente
            escritor.println("Enviando contenido de " + nombreArchivo + "...");
            try (BufferedReader lectorArchivo = new BufferedReader(new FileReader(archivo))) {
                lectorArchivo.lines().forEach(escritor::println);
            }
            escritor.println("FIN_DESCARGA_ARCHIVO");
            PERMISOS_CONCEDIDOS.get(propietario).remove(solicitante);
        } else {
            solicitarPermiso(solicitante, propietario, nombreArchivo, escritor);
        }
    }

    private static void transferirArchivoInterno(String remitente, BufferedReader lector, PrintWriter escritor) throws IOException {
        escritor.println("Usuario destino:");
        String destino = lector.readLine();
        escritor.println("Nombre del archivo a transferir:");
        String nombreArchivo = lector.readLine();

        if(destino == null || nombreArchivo == null || !verificarUsuarioExiste(destino) || destino.equals(remitente)){
            escritor.println("Error: Datos de transferencia no válidos.");
            return;
        }

        File origen = new File(DIRECTORIO_RAIZ + File.separator + remitente, nombreArchivo);
        File archivoDestino = new File(DIRECTORIO_RAIZ + File.separator + destino, nombreArchivo);

        if(!origen.exists()){
            escritor.println("Error: No tienes un archivo con ese nombre.");
            return;
        }
        if(archivoDestino.exists()){
            escritor.println("Error: El usuario destino ya tiene un archivo con ese nombre.");
            return;
        }

        try (InputStream in = new FileInputStream(origen); OutputStream out = new FileOutputStream(archivoDestino)) {
            in.transferTo(out);
            escritor.println("Archivo transferido a " + destino + " exitosamente.");
        }
    }

    private static void solicitarPermiso(String solicitante, String propietario, String clave, PrintWriter escritor) throws IOException {
        String nuevaSolicitud = propietario + ":" + solicitante + ":" + clave;
        if (almacenarNuevaSolicitud(nuevaSolicitud)) {
            escritor.println("Permiso no concedido. Se ha enviado una solicitud a '" + propietario + "'.");
        } else {
            escritor.println("Ya existe una solicitud pendiente para esto.");
        }
        escritor.println("FIN_COMANDO");
    }

    private static void mostrarDecisionesPendientes(String usuario, PrintWriter escritor) throws IOException {
        List<String> lineasRestantes = new ArrayList<>();
        List<String> notificaciones = new ArrayList<>();

        synchronized (Servidor2025.class) {
            File archivo = new File(ARCHIVO_DECISIONES);
            if (!archivo.exists()) return;
            try (BufferedReader reader = new BufferedReader(new FileReader(archivo))) {
                reader.lines().forEach(linea -> {
                    if (linea.startsWith(usuario + ":")) {
                        notificaciones.add(linea);
                    } else {
                        lineasRestantes.add(linea);
                    }
                });
            }
            reescribirArchivo(ARCHIVO_DECISIONES, lineasRestantes);
        }

        if (!notificaciones.isEmpty()) {
            escritor.println("\n--- Notificaciones Pendientes ---");
            for (String notificacion : notificaciones) {
                String[] partes = notificacion.split(":");
                String propietario = partes[1], clave = partes[2], decision = partes[3];
                String accion = CLAVE_LISTAR.equals(clave) ? "listar sus archivos" : "descargar el archivo " + clave;

                escritor.println("El usuario " + propietario + " " + decision.toLowerCase() + " tu solicitud para " + accion);
                if ("ACEPTADA".equals(decision)) {
                    PERMISOS_CONCEDIDOS.computeIfAbsent(propietario, k -> new ConcurrentHashMap<>()).put(usuario, clave);
                }
            }
            escritor.println("---------------------------------");
        }
    }

    private static void manejarSolicitudesPendientes(String propietario, BufferedReader lector, PrintWriter escritor) throws IOException {
        List<String> solicitudesPendientes = new ArrayList<>();
        List<String> lineasRestantes = new ArrayList<>();

        synchronized (Servidor2025.class) {
            File archivo = new File(ARCHIVO_SOLICITUDES);
            if (!archivo.exists()) return;
            try (BufferedReader reader = new BufferedReader(new FileReader(archivo))) {
                reader.lines().forEach(linea -> {
                    if (linea.startsWith(propietario + ":")) {
                        solicitudesPendientes.add(linea);
                    } else {
                        lineasRestantes.add(linea);
                    }
                });
            }
        }

        if (solicitudesPendientes.isEmpty()) return;

        escritor.println("--- ¡ATENCIÓN! Tienes solicitudes de permisos pendientes ---");
        for (String solicitud : solicitudesPendientes) {
            String[] partes = solicitud.split(":");
            String solicitante = partes[1], clave = partes[2];
            String accion = CLAVE_LISTAR.equals(clave) ? "listar tus archivos" : "descargar tu archivo " + clave;

            escritor.println("El usuario '" + solicitante + "' solicita permiso para " + accion + ".");
            escritor.println("¿Aceptar? [1] Sí / [2] No / [3] Dejar Pendiente");
            String opcion = lector.readLine();

            if ("1".equals(opcion)) {
                PERMISOS_CONCEDIDOS.computeIfAbsent(propietario, k -> new ConcurrentHashMap<>()).put(solicitante, clave);
                almacenarDecision(solicitante, propietario, clave, "ACEPTADA");
                escritor.println("Permiso concedido.");
            } else if ("2".equals(opcion)) {
                almacenarDecision(solicitante, propietario, clave, "DENEGADA");
                escritor.println("Permiso denegado.");
            } else {
                lineasRestantes.add(solicitud);
                escritor.println("Decisión pospuesta.");
            }
        }
        reescribirArchivo(ARCHIVO_SOLICITUDES, lineasRestantes);
        escritor.println("--- Fin de solicitudes ---");
    }

    private static boolean esContrasenaValida(String contrasena) {
        return contrasena != null && !contrasena.trim().isEmpty() && contrasena.length() >= 8;
    }

    private static synchronized boolean verificarUsuarioExiste(String usuario) throws IOException {
        if (usuario == null) return false;
        try (BufferedReader lector = new BufferedReader(new FileReader(ARCHIVO_USUARIOS))) {
            return lector.lines().anyMatch(linea -> linea.startsWith(usuario + ":"));
        } catch (FileNotFoundException e) {
            return false;
        }
    }

    private static synchronized boolean verificarCredenciales(String usuario, String contrasena) throws IOException {
        if (usuario == null || contrasena == null) return false;
        try (BufferedReader lector = new BufferedReader(new FileReader(ARCHIVO_USUARIOS))) {
            return lector.lines().anyMatch(linea -> linea.equals(usuario + ":" + contrasena));
        } catch (FileNotFoundException e) {
            return false;
        }
    }

    private static synchronized boolean registrarUsuario(String usuario, String contrasena) throws IOException {
        if (verificarUsuarioExiste(usuario)) return false;
        try (BufferedWriter escritor = new BufferedWriter(new FileWriter(ARCHIVO_USUARIOS, true))) {
            escritor.write(usuario + ":" + contrasena);
            escritor.newLine();
            new File(DIRECTORIO_RAIZ, usuario).mkdirs();
            return true;
        }
    }

    private static synchronized boolean estaBloqueado(String usuario, String bloqueado) throws IOException {
        try (BufferedReader lector = new BufferedReader(new FileReader(ARCHIVO_BLOQUEOS))) {
            return lector.lines().anyMatch(linea -> {
                String[] partes = linea.split(":");
                if (partes.length > 1 && partes[0].equals(usuario)) {
                    return Arrays.asList(partes[1].split(",")).contains(bloqueado);
                }
                return false;
            });
        } catch (FileNotFoundException e) {
            return false;
        }
    }

    private static synchronized void gestionarBloqueosArchivo(String usuario, String usuarioCambio, boolean bloquear) throws IOException {
        File archivo = new File(ARCHIVO_BLOQUEOS);
        List<String> lineas = new ArrayList<>();
        boolean usuarioEncontrado = false;
        if(archivo.exists()){
            try (BufferedReader lector = new BufferedReader(new FileReader(archivo))) {
                String linea;
                while((linea = lector.readLine()) != null) {
                    if (linea.startsWith(usuario + ":")) {
                        usuarioEncontrado = true;
                        String[] partes = linea.split(":");
                        Set<String> bloqueados = new HashSet<>(partes.length > 1 ? Arrays.asList(partes[1].split(",")) : Collections.emptySet());
                        if (bloquear) {
                            bloqueados.add(usuarioCambio);
                        } else {
                            bloqueados.remove(usuarioCambio);
                        }
                        if (!bloqueados.isEmpty()) {
                            lineas.add(usuario + ":" + String.join(",", bloqueados));
                        }
                    } else {
                        lineas.add(linea);
                    }
                }
            }
        }
        if (bloquear && !usuarioEncontrado) {
            lineas.add(usuario + ":" + usuarioCambio);
        }
        reescribirArchivo(ARCHIVO_BLOQUEOS, lineas);
    }

    private static synchronized void reescribirArchivo(String nombreArchivo, List<String> lineas) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(nombreArchivo))) {
            for (String linea : lineas) {
                writer.write(linea);
                writer.newLine();
            }
        }
    }

    private static synchronized boolean almacenarNuevaSolicitud(String nuevaSolicitud) throws IOException {
        File archivo = new File(ARCHIVO_SOLICITUDES);
        if (archivo.exists()) {
            try (BufferedReader reader = new BufferedReader(new FileReader(archivo))) {
                if (reader.lines().anyMatch(linea -> linea.equals(nuevaSolicitud))) {
                    return false; // Ya existe
                }
            }
        }
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(archivo, true))) {
            writer.write(nuevaSolicitud);
            writer.newLine();
        }
        return true;
    }

    private static synchronized void almacenarDecision(String sol, String prop, String clave, String dec) throws IOException {
        String nuevaDecision = sol + ":" + prop + ":" + clave + ":" + dec;
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(ARCHIVO_DECISIONES, true))) {
            writer.write(nuevaDecision);
            writer.newLine();
        }
    }

    private static void realizarListado(String usuario, PrintWriter escritor) {
        escritor.println("--- Archivos de '" + usuario + "' ---");
        File dirUsuario = new File(DIRECTORIO_RAIZ, usuario);
        File[] archivos = dirUsuario.listFiles((dir, name) -> name.toLowerCase().endsWith(".txt"));
        if (archivos != null && archivos.length > 0) {
            for (File archivo : archivos) {
                escritor.println(archivo.getName());
            }
        } else {
            escritor.println("(No tiene archivos de texto)");
        }
        escritor.println("FIN_COMANDO");
    }
}

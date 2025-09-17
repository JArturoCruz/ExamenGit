import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;

public class Servidor2025 {

    private static final String ARCHIVO_USUARIOS = "usuarios.txt";
    private static final String ARCHIVO_MENSAJES = "mensajes.txt";

    public static void main(String[] args) {
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
        try (
                PrintWriter escritor = new PrintWriter(cliente.getOutputStream(), true);
                BufferedReader lector = new BufferedReader(new InputStreamReader(cliente.getInputStream()))
        ) {
            escritor.println("Bienvenido. ¿Deseas [1] Iniciar sesión, [2] Registrarte ");
            String opcion = lector.readLine();

            String usuarioAutenticado = null;
            escritor.println("Usuario:");
            String usuario = lector.readLine();
            escritor.println("Contraseña:");
            String contrasena = lector.readLine();

            if ("1".equals(opcion)) {
                if (verificarCredenciales(usuario, contrasena)) {
                    escritor.println("Autenticación exitosa");
                    usuarioAutenticado = usuario;
                } else {
                    escritor.println("Credenciales inválidas");
                }
            } else if ("2".equals(opcion)) {
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

            escritor.println("Menú: [1] Jugar | [2] Enviar mensaje | [3] Leer mensajes | [4] Cerrar sesión | [5] Eliminar mensaje recibido | [6] Eliminar mensaje enviado");

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
                        leerMensajes(usuarioAutenticado, escritor);
                        break;
                    case "4":
                        System.out.println("Cliente " + usuarioAutenticado + " ha cerrado sesión.");
                        cliente.close();
                        return;
                    case "5":
                        eliminarMensajeRecibido(usuarioAutenticado, lector, escritor);
                        break;
                    case "6":
                        eliminarMensajeEnviado(usuarioAutenticado, lector, escritor);
                        break;
                    default:
                        escritor.println("Opción de menú no válida.");
                        break;
                }

                escritor.println("Menú: [1] Jugar | [2] Enviar mensaje | [3] Leer mensajes | [4] Cerrar sesión | [5] Eliminar mensaje recibido | [6] Eliminar mensaje enviado");
            }

        } catch (IOException e) {
            System.out.println("Error con el cliente: " + e.getMessage());
        } finally {
            try {
                if (cliente != null && !cliente.isClosed()) {
                    cliente.close();
                }
            } catch (IOException e) {
                System.out.println("Error al cerrar el socket del cliente: " + e.getMessage());
            }
        }
    }

    private static boolean esContrasenaValida(String contrasena) {
        return contrasena != null && !contrasena.trim().isEmpty() && contrasena.length() >= 8;
    }

    private static void jugarAdivinarNumero(BufferedReader lector, PrintWriter escritor) throws IOException {
        escritor.println("¡Bienvenido al juego de adivinar! Elige un número del 1 al 10. Solo tienes 3 intentos.");

        Random random = new Random();
        int numeroSecreto = random.nextInt(10) + 1;

        for (int intento = 1; intento <= 3; intento++) {
            String entrada = lector.readLine();
            if (entrada == null) break;

            int numeroIngresado;
            try {
                numeroIngresado = Integer.parseInt(entrada);
            } catch (NumberFormatException e) {
                escritor.println("Eso no es un número válido. Intenta de nuevo.");
                intento--;
                continue;
            }

            if (numeroIngresado < 1 || numeroIngresado > 10) {
                escritor.println("Número fuera de rango (1-10). Prueba otra vez.");
                intento--;
                continue;
            }

            if (numeroIngresado == numeroSecreto) {
                escritor.println("¡Genial! Adivinaste el número correcto. FIN_JUEGO");
                return;
            } else if (numeroIngresado < numeroSecreto) {
                escritor.println("El número secreto es más alto.");
            } else {
                escritor.println("El número secreto es más bajo.");
            }
        }
        escritor.println("No lograste adivinar el número. Era: " + numeroSecreto + ". Mejor suerte la próxima vez. FIN_JUEGO");
    }

    private static void enviarMensaje(String remitente, BufferedReader lector, PrintWriter escritor) throws IOException {
        escritor.println("¿Para quién es el mensaje? (nombre de usuario)");
        String destinatario = lector.readLine();
        if (destinatario == null ) return;

        if (destinatario.equals(remitente)) {
            escritor.println("Error: No puedes enviarte un mensaje a ti mismo.");
            return;
        }
        if (!verificarUsuarioExiste(destinatario)) {
            escritor.println("Error: El usuario '" + destinatario + "' no existe.");
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
            } catch (IOException e) {
                escritor.println("Error al guardar el mensaje.");
                e.printStackTrace();
            }
        }
    }

    private static void leerMensajes(String usuario, PrintWriter escritor) {
        escritor.println("--- Tus mensajes ---");
        File archivo = new File(ARCHIVO_MENSAJES);
        if (!archivo.exists()) {
            escritor.println("No tienes mensajes.");
        } else {
            try (BufferedReader reader = new BufferedReader(new FileReader(archivo))) {
                String linea;
                int contador = 0;
                while ((linea = reader.readLine()) != null) {
                    String[] partes = linea.split(":", 3);
                    if (partes.length == 3 && partes[0].equals(usuario)) {
                        escritor.println("De [" + partes[1] + "]: " + partes[2]);
                        contador++;
                    }
                }
                if (contador == 0) {
                    escritor.println("No tienes mensajes nuevos.");
                }
            } catch (IOException e) {
                escritor.println("Error al leer los mensajes.");
            }
        }
        escritor.println("--- Fin de los mensajes ---");
        escritor.println("FIN_MENSAJES");
    }

    private static void eliminarMensajeRecibido(String usuario, BufferedReader lector, PrintWriter escritor) throws IOException {
        File archivo = new File(ARCHIVO_MENSAJES);
        if (!archivo.exists()) {
            escritor.println("No tienes mensajes para eliminar.");
            return;
        }

        List<String> todasLasLineas = new ArrayList<>();
        List<String> mensajesRecibidos = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(archivo))) {
            String linea;
            while ((linea = reader.readLine()) != null) {
                todasLasLineas.add(linea);
                String[] partes = linea.split(":", 3);
                if (partes.length == 3 && partes[0].equals(usuario)) {
                    mensajesRecibidos.add(linea);
                }
            }
        }

        if (mensajesRecibidos.isEmpty()) {
            escritor.println("No tienes mensajes recibidos.");
            return;
        }

        escritor.println("--- Mensajes recibidos ---");
        for (int i = 0; i < mensajesRecibidos.size(); i++) {
            String[] partes = mensajesRecibidos.get(i).split(":", 3);
            escritor.println("[" + (i + 1) + "] De [" + partes[1] + "]: " + partes[2]);
        }
        escritor.println("Escribe el número del mensaje que deseas eliminar:");
        int seleccion;
        try {
            seleccion = Integer.parseInt(lector.readLine());
        } catch (NumberFormatException e) {
            escritor.println("Entrada inválida.");
            return;
        }

        if (seleccion < 1 || seleccion > mensajesRecibidos.size()) {
            escritor.println("Selección fuera de rango.");
            return;
        }

        String mensajeAEliminar = mensajesRecibidos.get(seleccion - 1);
        todasLasLineas.remove(mensajeAEliminar);

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(ARCHIVO_MENSAJES))) {
            for (String linea : todasLasLineas) {
                writer.write(linea);
                writer.newLine();
            }
        }

        escritor.println("Mensaje eliminado exitosamente.");
    }

    private static void eliminarMensajeEnviado(String usuario, BufferedReader lector, PrintWriter escritor) throws IOException {
        File archivo = new File(ARCHIVO_MENSAJES);
        if (!archivo.exists()) {
            escritor.println("No tienes mensajes para eliminar.");
            return;
        }

        List<String> todasLasLineas = new ArrayList<>();
        List<String> mensajesEnviados = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(archivo))) {
            String linea;
            while ((linea = reader.readLine()) != null) {
                todasLasLineas.add(linea);
                String[] partes = linea.split(":", 3);
                if (partes.length == 3 && partes[1].equals(usuario)) {
                    mensajesEnviados.add(linea);
                }
            }
        }

        if (mensajesEnviados.isEmpty()) {
            escritor.println("No tienes mensajes enviados.");
            return;
        }

        escritor.println("--- Mensajes enviados ---");
        for (int i = 0; i < mensajesEnviados.size(); i++) {
            String[] partes = mensajesEnviados.get(i).split(":", 3);
            escritor.println("[" + (i + 1) + "] Para [" + partes[0] + "]: " + partes[2]);
        }
        escritor.println("Escribe el número del mensaje que deseas eliminar:");
        int seleccion;
        try {
            seleccion = Integer.parseInt(lector.readLine());
        } catch (NumberFormatException e) {
            escritor.println("Entrada inválida.");
            return;
        }

        if (seleccion < 1 || seleccion > mensajesEnviados.size()) {
            escritor.println("Selección fuera de rango.");
            return;
        }

        String mensajeAEliminar = mensajesEnviados.get(seleccion - 1);
        todasLasLineas.remove(mensajeAEliminar);

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(ARCHIVO_MENSAJES))) {
            for (String linea : todasLasLineas) {
                writer.write(linea);
                writer.newLine();
            }
        }

        escritor.println("Mensaje eliminado exitosamente.");
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
}

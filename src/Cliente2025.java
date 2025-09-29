import java.io.*;
import java.net.Socket;

public class Cliente2025 {

    private static final String HOST = "localhost";
    private static final int PUERTO = 8081;
    private static final String DIRECTORIO_CLIENTE = "cliente_archivos";
    private static String usuarioAutenticado;

    public static void main(String[] args) {
        new File(DIRECTORIO_CLIENTE).mkdirs();

        try (
                Socket socket = new Socket(HOST, PUERTO);
                PrintWriter escritor = new PrintWriter(socket.getOutputStream(), true);
                BufferedReader lectorServidor = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                BufferedReader teclado = new BufferedReader(new InputStreamReader(System.in))
        ) {
            System.out.println("Conectado al servidor en " + HOST + ":" + PUERTO);
            if (!manejarAutenticacion(lectorServidor, teclado, escritor)) {
                System.out.println("No se pudo iniciar sesión. Adiós.");
                return;
            }
            manejarNotificacionesIniciales(lectorServidor, teclado, escritor);
            manejarMenuPrincipal(lectorServidor, teclado, escritor);

        } catch (IOException e) {
            System.err.println("Error en el cliente: " + e.getMessage());
        } finally {
            System.out.println("Conexión cerrada.");
        }
    }

    private static boolean manejarAutenticacion(BufferedReader lectorServidor, BufferedReader teclado, PrintWriter escritor) throws IOException {
        System.out.println("Servidor: " + lectorServidor.readLine()); // Lee bienvenida

        String opcionLogin;
        while (true) {
            System.out.print("Elige una opción: ");
            opcionLogin = teclado.readLine();
            if ("1".equals(opcionLogin) || "2".equals(opcionLogin)) {
                break;
            } else {
                System.out.println("Opción no válida. Por favor, elige [1] o [2].");
            }
        }
        if ("1".equals(opcionLogin)) {
            System.out.println("\n-> Iniciando sesión...");
        } else {
            System.out.println("\n-> Registrando usuario...");
        }
        escritor.println(opcionLogin);

        System.out.println("Servidor: " + lectorServidor.readLine());
        String usuario = teclado.readLine();
        escritor.println(usuario);

        System.out.println("Servidor: " + lectorServidor.readLine());
        String contrasena = teclado.readLine();
        escritor.println(contrasena);

        String respuestaAuth = lectorServidor.readLine();
        System.out.println("Servidor: " + respuestaAuth);

        if (respuestaAuth.contains("exitosa")) {
            usuarioAutenticado = usuario;
            new File(DIRECTORIO_CLIENTE, usuarioAutenticado).mkdirs();
            return true;
        }
        return false;
    }
    private static void manejarNotificacionesIniciales(BufferedReader lectorServidor, BufferedReader teclado, PrintWriter escritor) throws IOException {
        String linea;
        while ((linea = lectorServidor.readLine()) != null) {
            if (linea.startsWith("Menú:")) {
                System.out.println("\n" + linea);
                break;
            }
            System.out.println(linea);
            if (linea.contains("¿Aceptar?")) {
                System.out.print("Tu decisión: ");
                String decision = teclado.readLine();
                escritor.println(decision);
            }
        }
    }

    private static void manejarMenuPrincipal(BufferedReader lectorServidor, BufferedReader teclado, PrintWriter escritor) throws IOException {
        String opcionMenu;
        while (true) {
            System.out.print(">> ");
            opcionMenu = teclado.readLine();
            escritor.println(opcionMenu);

            if ("4".equals(opcionMenu)) {
                System.out.println("Desconectando del servidor...");
                break;
            }

            switch (opcionMenu) {
                case "1": manejarJuego(lectorServidor, teclado, escritor); break;
                case "2": manejarEnvioMensaje(lectorServidor, teclado, escritor); break;
                case "3": manejarLecturaMensajes(lectorServidor, teclado, escritor); break;
                case "5": manejarEliminacionMensajes(lectorServidor, teclado, escritor); break;
                case "6": manejarVerUsuarios(lectorServidor); break;
                case "7": manejarBloqueo(lectorServidor, teclado, escritor); break;
                case "8": manejarMenuArchivos(lectorServidor, teclado, escritor); break;
                default:
                    System.out.println("Servidor: " + lectorServidor.readLine());
                    break;
            }
            System.out.println("\nServidor: " + lectorServidor.readLine());
        }
    }

    private static void manejarJuego(BufferedReader lectorServidor, BufferedReader teclado, PrintWriter escritor) throws IOException {
        System.out.println("Servidor: " + lectorServidor.readLine());
        while (true) {
            System.out.print("Ingresa tu intento: ");
            escritor.println(teclado.readLine());
            String respuesta = lectorServidor.readLine();
            System.out.println("Servidor: " + respuesta);
            if (respuesta.contains("FIN_JUEGO")) {
                break;
            }
        }
    }

    private static void manejarEnvioMensaje(BufferedReader lectorServidor, BufferedReader teclado, PrintWriter escritor) throws IOException {
        System.out.println("Servidor: " + lectorServidor.readLine());
        String destinatario = teclado.readLine();
        escritor.println(destinatario);

        if (destinatario != null && destinatario.equalsIgnoreCase("V")) {
            return;
        }

        String respuestaServidor = lectorServidor.readLine();
        System.out.println("Servidor: " + respuestaServidor);
        if (!respuestaServidor.startsWith("Error:")) {
            System.out.print("Mensaje: ");
            escritor.println(teclado.readLine());
            System.out.println("Servidor: " + lectorServidor.readLine());
        }
    }

    private static void manejarLecturaMensajes(BufferedReader lectorServidor, BufferedReader teclado, PrintWriter escritor) throws IOException {
        System.out.println("Servidor: " + lectorServidor.readLine());
        escritor.println(teclado.readLine());

        String respuesta = lectorServidor.readLine();
        if (respuesta.contains("Escribe el nombre")) {
            System.out.println("Servidor: " + respuesta);
            escritor.println(teclado.readLine());
        } else {
            System.out.println(respuesta);
        }

        while (true) {
            String linea = lectorServidor.readLine();
            if (linea == null || linea.equals("FIN_PAGINA")) {
                System.out.print("Elige una opción ([N] Siguiente, [A] Anterior, [V] Volver): ");
                String eleccion = teclado.readLine();
                escritor.println(eleccion);
                if (eleccion.equalsIgnoreCase("V")) {
                    break;
                }
            } else {
                System.out.println(linea);
            }
        }
    }

    private static void manejarEliminacionMensajes(BufferedReader lectorServidor, BufferedReader teclado, PrintWriter escritor) throws IOException {
        System.out.println("Servidor: " + lectorServidor.readLine());
        escritor.println(teclado.readLine());

        String linea;
        while(!(linea = lectorServidor.readLine()).contains("Escribe el número")){
            System.out.println(linea);
            if (linea.contains("No tienes mensajes")) return;
        }
        System.out.println(linea);

        escritor.println(teclado.readLine());
        System.out.println("Servidor: " + lectorServidor.readLine());
    }

    private static void manejarVerUsuarios(BufferedReader lectorServidor) throws IOException {
        String linea;
        while (!(linea = lectorServidor.readLine()).equals("FIN_COMANDO")) {
            System.out.println(linea);
        }
    }

    private static void manejarBloqueo(BufferedReader lectorServidor, BufferedReader teclado, PrintWriter escritor) throws IOException {
        System.out.println("Servidor: " + lectorServidor.readLine());
        escritor.println(teclado.readLine());

        String respuesta = lectorServidor.readLine();
        System.out.println("Servidor: " + respuesta);

        if (respuesta.contains("¿Desbloquear?") || respuesta.contains("¿Bloquear?")) {
            System.out.print(">> ");
            escritor.println(teclado.readLine());
            System.out.println("Servidor: " + lectorServidor.readLine());
        }
    }

    private static void manejarMenuArchivos(BufferedReader lectorServidor, BufferedReader teclado, PrintWriter escritor) throws IOException {
        System.out.println("Servidor: " + lectorServidor.readLine());
        System.out.print("Opción de archivos >> ");
        String opcionArchivos = teclado.readLine();
        escritor.println(opcionArchivos);

        switch(opcionArchivos) {
            case "1": // Listar
                System.out.println("Servidor: " + lectorServidor.readLine());
                escritor.println(teclado.readLine());
                leerHastaFinComando(lectorServidor);
                break;
            case "2": // Crear
                manejarCreacionArchivo(lectorServidor, teclado, escritor);
                break;
            case "3": // Descargar
                manejarDescargaArchivo(lectorServidor, teclado, escritor);
                break;
            case "4": // Transferir
                System.out.println("Servidor: " + lectorServidor.readLine());
                escritor.println(teclado.readLine());
                System.out.println("Servidor: " + lectorServidor.readLine());
                escritor.println(teclado.readLine());
                System.out.println("Servidor: " + lectorServidor.readLine());
                break;
            default:
                System.out.println("Servidor: " + lectorServidor.readLine());
                break;
        }
    }

    private static void manejarCreacionArchivo(BufferedReader lectorServidor, BufferedReader teclado, PrintWriter escritor) throws IOException {
        System.out.println("Servidor: " + lectorServidor.readLine());
        escritor.println(teclado.readLine());

        String respuestaServidor = lectorServidor.readLine();
        System.out.println("Servidor: " + respuestaServidor);

        if (respuestaServidor.startsWith("Escribe el contenido")) {
            System.out.println("Escribe tu contenido y termina con 'FIN_CONTENIDO' en una nueva línea.");
            String linea;
            while (!(linea = teclado.readLine()).equals("FIN_CONTENIDO")) {
                escritor.println(linea);
            }
            escritor.println("FIN_CONTENIDO");
            System.out.println("Servidor: " + lectorServidor.readLine());
        }
    }

    private static void manejarDescargaArchivo(BufferedReader lectorServidor, BufferedReader teclado, PrintWriter escritor) throws IOException {
        System.out.println("Servidor: " + lectorServidor.readLine());
        escritor.println(teclado.readLine());
        System.out.println("Servidor: " + lectorServidor.readLine());
        String nombreArchivo = teclado.readLine();
        escritor.println(nombreArchivo);

        String respuesta = lectorServidor.readLine();
        if ("PERMISO_OK".equals(respuesta)) {
            String rutaCompleta = DIRECTORIO_CLIENTE + File.separator + usuarioAutenticado + File.separator + "descargado_" + nombreArchivo;
            System.out.println(lectorServidor.readLine());
            try (FileWriter escritorArchivo = new FileWriter(rutaCompleta)) {
                String linea;
                while (!(linea = lectorServidor.readLine()).equals("FIN_DESCARGA_ARCHIVO")) {
                    escritorArchivo.write(linea + System.lineSeparator());
                }
                System.out.println("Archivo descargado y guardado en: " + rutaCompleta);
            } catch (IOException e) {
                System.err.println("Error al guardar el archivo descargado: " + e.getMessage());
            }
        } else {
            System.out.println("Servidor: " + respuesta);
            leerHastaFinComando(lectorServidor);
        }
    }

    private static void leerHastaFinComando(BufferedReader lectorServidor) throws IOException {
        String linea;
        while (!(linea = lectorServidor.readLine()).equals("FIN_COMANDO")) {
            System.out.println("Servidor: " + linea);
        }
    }
}
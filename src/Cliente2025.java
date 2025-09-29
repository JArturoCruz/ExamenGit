import java.io.*;
import java.net.Socket;

public class Cliente2025 {


    private static final String HOST = "localhost";
    private static final int PUERTO = 8081;
    private static final String DIRECTORIO_CLIENTE = "cliente_archivos";
    private static String usuarioAutenticado;

    public static void main(String[] args) {
        new File(DIRECTORIO_CLIENTE).mkdirs(); // Asegura que el directorio del cliente exista.

        try (
                Socket socket = new Socket(HOST, PUERTO);
                PrintWriter escritor = new PrintWriter(socket.getOutputStream(), true);
                BufferedReader lectorServidor = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                BufferedReader teclado = new BufferedReader(new InputStreamReader(System.in))
        ) {
            System.out.println("Conectado al servidor en " + HOST + ":" + PUERTO);

            // 1. Proceso de Autenticación
            if (!manejarAutenticacion(lectorServidor, teclado, escritor)) {
                System.out.println("No se pudo iniciar sesión. Adiós.");
                return;
            }

            // 2. Manejo de notificaciones iniciales del servidor
            manejarNotificacionesIniciales(lectorServidor, teclado, escritor);

            // 3. Bucle del menú principal
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
        do {
            System.out.print("Elige una opción: ");
            opcionLogin = teclado.readLine();
        } while (!"1".equals(opcionLogin) && !"2".equals(opcionLogin));
        escritor.println(opcionLogin);

        System.out.println("Servidor: " + lectorServidor.readLine()); // Pide usuario
        String usuario = teclado.readLine();
        escritor.println(usuario);

        System.out.println("Servidor: " + lectorServidor.readLine()); // Pide contraseña
        String contrasena = teclado.readLine();
        escritor.println(contrasena);

        String respuestaAuth = lectorServidor.readLine();
        System.out.println("Servidor: " + respuestaAuth);

        if (respuestaAuth.contains("exitosa")) {
            usuarioAutenticado = usuario;
            new File(DIRECTORIO_CLIENTE, usuarioAutenticado).mkdirs(); // Crea dir del cliente
            return true;
        }
        return false;
    }

    private static void manejarNotificacionesIniciales(BufferedReader lectorServidor, BufferedReader teclado, PrintWriter escritor) throws IOException {
        String linea;
        while ((linea = lectorServidor.readLine()) != null) {
            // Si la línea contiene el inicio del menú, terminamos de leer notificaciones.
            if (linea.startsWith("Menú:")) {
                System.out.println("\n" + linea); // Imprime el menú
                break;
            }
            System.out.println(linea);
            // Si el servidor nos pide una decisión sobre una solicitud pendiente.
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
                    // El servidor enviará un mensaje de "opción no válida"
                    System.out.println("Servidor: " + lectorServidor.readLine());
                    break;
            }
            // Lee el siguiente menú que envía el servidor al finalizar una acción
            System.out.println("\nServidor: " + lectorServidor.readLine());
        }
    }

    private static void manejarJuego(BufferedReader lectorServidor, BufferedReader teclado, PrintWriter escritor) throws IOException {
        System.out.println("Servidor: " + lectorServidor.readLine()); // Lee bienvenida del juego
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
        System.out.println("Servidor: " + lectorServidor.readLine()); // Pide destinatario
        String destinatario = teclado.readLine();
        escritor.println(destinatario); // Envía destinatario

        // Si el usuario decide volver, el servidor no enviará nada más.
        if (destinatario != null && destinatario.equalsIgnoreCase("V")) {
            return;
        }

        String respuestaServidor = lectorServidor.readLine();
        System.out.println("Servidor: " + respuestaServidor);

        // Si la respuesta NO es un error, procedemos a pedir y enviar el mensaje.
        if (!respuestaServidor.startsWith("Error:")) {
            System.out.print("Mensaje: ");
            escritor.println(teclado.readLine()); // Envía el mensaje
            System.out.println("Servidor: " + lectorServidor.readLine()); // Lee la confirmación final
        }
        // Si fue un error, la función termina y el bucle principal leerá el siguiente menú.
    }

    private static void manejarLecturaMensajes(BufferedReader lectorServidor, BufferedReader teclado, PrintWriter escritor) throws IOException {
        System.out.println("Servidor: " + lectorServidor.readLine()); // Pide tipo de lectura
        escritor.println(teclado.readLine()); // Envía opción 1 o 2

        String respuesta = lectorServidor.readLine();
        if (respuesta.contains("Escribe el nombre")) { // Si se eligió la opción 2
            System.out.println("Servidor: " + respuesta);
            escritor.println(teclado.readLine()); // Envía el nombre de usuario a filtrar
        } else {
            System.out.println(respuesta); // Muestra la primera línea de mensajes o "No tienes mensajes"
        }

        // Bucle para leer todas las páginas
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
        System.out.println("Servidor: " + lectorServidor.readLine()); // Pide tipo de mensajes a eliminar
        escritor.println(teclado.readLine()); // Envía opción 1 o 2

        // El servidor enviará la lista de mensajes o un mensaje de que no hay
        String linea;
        while(!(linea = lectorServidor.readLine()).contains("Escribe el número")){
            System.out.println(linea);
            if (linea.contains("No tienes mensajes")) return;
        }
        System.out.println(linea); // Imprime el prompt para seleccionar el número

        escritor.println(teclado.readLine()); // Envía el número seleccionado
        System.out.println("Servidor: " + lectorServidor.readLine()); // Lee confirmación/error
    }

    private static void manejarVerUsuarios(BufferedReader lectorServidor) throws IOException {
        String linea;
        while (!(linea = lectorServidor.readLine()).equals("FIN_COMANDO")) {
            System.out.println(linea);
        }
    }

    private static void manejarBloqueo(BufferedReader lectorServidor, BufferedReader teclado, PrintWriter escritor) throws IOException {
        System.out.println("Servidor: " + lectorServidor.readLine()); // Pide usuario
        escritor.println(teclado.readLine()); // Envía usuario

        String respuesta = lectorServidor.readLine();
        System.out.println("Servidor: " + respuesta);
        if (respuesta.contains("¿Desbloquear?") || respuesta.contains("¿Bloquear?")) {
            escritor.println(teclado.readLine()); // Envía opción 1 o 2
            System.out.println("Servidor: " + lectorServidor.readLine()); // Lee confirmación
        }
    }

    private static void manejarMenuArchivos(BufferedReader lectorServidor, BufferedReader teclado, PrintWriter escritor) throws IOException {
        System.out.println("Servidor: " + lectorServidor.readLine()); // Muestra el menú de archivos
        System.out.print("Opción de archivos >> ");
        String opcionArchivos = teclado.readLine();
        escritor.println(opcionArchivos);

        switch(opcionArchivos) {
            case "1": // Listar
                System.out.println("Servidor: " + lectorServidor.readLine()); // Pide usuario
                escritor.println(teclado.readLine()); // Envía usuario
                leerHastaFinComando(lectorServidor);
                break;
            case "2": // Crear
                manejarCreacionArchivo(lectorServidor, teclado, escritor);
                break;
            case "3": // Descargar
                manejarDescargaArchivo(lectorServidor, teclado, escritor);
                break;
            case "4": // Transferir
                System.out.println("Servidor: " + lectorServidor.readLine()); // Pide usuario destino
                escritor.println(teclado.readLine());
                System.out.println("Servidor: " + lectorServidor.readLine()); // Pide nombre de archivo
                escritor.println(teclado.readLine());
                System.out.println("Servidor: " + lectorServidor.readLine()); // Muestra resultado
                break;
            default:
                System.out.println("Servidor: " + lectorServidor.readLine()); // Lee error
                break;
        }
    }

    private static void manejarCreacionArchivo(BufferedReader lectorServidor, BufferedReader teclado, PrintWriter escritor) throws IOException {
        System.out.println("Servidor: " + lectorServidor.readLine()); // Pide nombre de archivo
        escritor.println(teclado.readLine()); // Envía nombre

        System.out.println("Servidor: " + lectorServidor.readLine()); // Pide contenido

        StringBuilder contenido = new StringBuilder();
        String linea;
        while (!(linea = teclado.readLine()).equals("FIN_CONTENIDO")) {
            escritor.println(linea);
        }
        escritor.println("FIN_CONTENIDO"); // Envía señal de fin

        System.out.println("Servidor: " + lectorServidor.readLine()); // Muestra confirmación
    }

    private static void manejarDescargaArchivo(BufferedReader lectorServidor, BufferedReader teclado, PrintWriter escritor) throws IOException {
        System.out.println("Servidor: " + lectorServidor.readLine()); // Pide propietario
        escritor.println(teclado.readLine());
        System.out.println("Servidor: " + lectorServidor.readLine()); // Pide nombre archivo
        String nombreArchivo = teclado.readLine();
        escritor.println(nombreArchivo);

        String respuesta = lectorServidor.readLine();
        if ("PERMISO_OK".equals(respuesta)) {
            String rutaCompleta = DIRECTORIO_CLIENTE + File.separator + usuarioAutenticado + File.separator + "descargado_" + nombreArchivo;
            System.out.println(lectorServidor.readLine()); // Lee "Enviando contenido..."
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
            // Muestra el mensaje de solicitud enviada o error
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


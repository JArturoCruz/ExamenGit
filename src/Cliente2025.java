import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class Cliente2025 {
    public static void main(String[] args) {
        try (
                Socket socket = new Socket("localhost", 8081);
                PrintWriter escritor = new PrintWriter(socket.getOutputStream(), true);
                BufferedReader lectorServidor = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                BufferedReader teclado = new BufferedReader(new InputStreamReader(System.in))
        ) {
            // --- FASE DE AUTENTICACIÓN ---
            System.out.println("Servidor: " + lectorServidor.readLine()); // Mensaje de bienvenida
            String opcionLogin = teclado.readLine();
            escritor.println(opcionLogin);

            // Si la opción es '3', solo leemos la lista de usuarios
            if ("3".equals(opcionLogin)) {
                String linea;
                while (!(linea = lectorServidor.readLine()).equals("FIN_USUARIOS")) {
                    System.out.println(linea);
                }
                return; // Termina el programa
            }

            System.out.println("Servidor: " + lectorServidor.readLine()); // Pide Usuario
            String usuario = teclado.readLine();
            escritor.println(usuario);

            System.out.println("Servidor: " + lectorServidor.readLine()); // Pide Contraseña
            String contrasena = teclado.readLine();
            escritor.println(contrasena);

            String respuestaAuth = lectorServidor.readLine();
            System.out.println("Servidor: " + respuestaAuth);

            // Si la autenticación falla, el programa termina
            if (!respuestaAuth.contains("Autenticación exitosa") && !respuestaAuth.contains("registrado exitosamente")) {
                System.out.println("No se pudo iniciar sesión. Adiós.");
                return;
            }

            // ---- NUEVO: BUCLE DE MENÚ PRINCIPAL ----
            while (true) {
                mostrarMenu();
                String opcionMenu = teclado.readLine();
                escritor.println(opcionMenu);

                if ("1".equals(opcionMenu)) { // JUGAR
                    System.out.println(lectorServidor.readLine()); // Lee "¡Vamos a jugar!..."
                    jugar(lectorServidor, teclado, escritor);

                } else if ("2".equals(opcionMenu)) { // ENVIAR MENSAJE
                    // Lee pregunta de destinatario
                    System.out.println("Servidor: " + lectorServidor.readLine());
                    String destinatario = teclado.readLine();
                    escritor.println(destinatario);

                    // Lee la respuesta del servidor (si pide mensaje o si hay error)
                    String respuestaDestinatario = lectorServidor.readLine();
                    System.out.println("Servidor: " + respuestaDestinatario);

                    // Solo si no hubo error, pedimos el mensaje
                    if (!respuestaDestinatario.startsWith("Error:")) {
                        String mensaje = teclado.readLine();
                        escritor.println(mensaje);
                        // Imprime la confirmación final
                        System.out.println("Servidor: " + lectorServidor.readLine());
                    }

                } else if ("3".equals(opcionMenu)) { // LEER MENSAJES
                    String linea;
                    // Leemos líneas hasta que el servidor envíe la señal de fin
                    while (!(linea = lectorServidor.readLine()).equals("FIN_MENSAJES")) {
                        System.out.println(linea);
                    }

                } else if ("4".equals(opcionMenu)) { // SALIR
                    System.out.println("Desconectando del servidor...");
                    break; // Rompe el bucle while y cierra el programa

                } else {
                    System.out.println("Opción no válida. Inténtalo de nuevo.");
                }
            }

        } catch (IOException e) {
            System.out.println("Ocurrió un error en el cliente: " + e.getMessage());
        }
    }

    private static void mostrarMenu() {
        System.out.println("\n----- MENÚ PRINCIPAL -----");
        System.out.println("Elige una opción:");
        System.out.println("[1] Jugar a adivinar el número");
        System.out.println("[2] Enviar un mensaje a otro usuario");
        System.out.println("[3] Leer mis mensajes");
        System.out.println("[4] Salir");
        System.out.print("> ");
    }

    private static void jugar(BufferedReader lectorServidor, BufferedReader teclado, PrintWriter escritor) throws IOException {
        while (true) {
            System.out.print("Ingresa tu intento (1-10): ");
            String intento = teclado.readLine();
            escritor.println(intento);

            String respuesta = lectorServidor.readLine();
            System.out.println("Servidor: " + respuesta);

            // Si el juego termina (por ganar, perder o error), salimos de la función
            if (respuesta.contains("Felicidades") || respuesta.contains("Se acabaron") || respuesta.equals("FIN_JUEGO")) {
                break;
            }
        }
    }
}
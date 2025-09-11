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

            System.out.println("Servidor: " + lectorServidor.readLine()); // Mensaje de bienvenida
            String opcionLogin = teclado.readLine();
            escritor.println(opcionLogin);

            System.out.println("Servidor: " + lectorServidor.readLine());
            String usuario = teclado.readLine();
            escritor.println(usuario);

            System.out.println("Servidor: " + lectorServidor.readLine());
            String contrasena = teclado.readLine();
            escritor.println(contrasena);

            String respuestaAuth = lectorServidor.readLine();
            System.out.println("Servidor: " + respuestaAuth);

            // Si la autenticación falla, el programa termina
            if (!respuestaAuth.contains("Autenticación exitosa") && !respuestaAuth.contains("registrado exitosamente")) {
                System.out.println("No se pudo iniciar sesión. Adiós.");
                return;
            }

            String opcionMenu;
            while (true) {
                mostrarMenu();
                opcionMenu = teclado.readLine();

                // Validación del input del menú
                if (!opcionMenu.matches("[1-4]")) {
                    System.out.println("Opción no válida. Inténtalo de nuevo.");
                    continue;
                }

                escritor.println(opcionMenu);

                if ("1".equals(opcionMenu)) {
                    System.out.println(lectorServidor.readLine());
                    jugar(lectorServidor, teclado, escritor);
                    socket.close();
                    System.exit(0);
                } else if ("2".equals(opcionMenu)) { // Enviar mensaje

                    System.out.println("Servidor: " + lectorServidor.readLine());
                    String destinatario = teclado.readLine();
                    escritor.println(destinatario);

                    String respuestaDestinatario = lectorServidor.readLine();
                    System.out.println("Servidor: " + respuestaDestinatario);

                    if (!respuestaDestinatario.startsWith("Error:")) {
                        String mensaje = teclado.readLine();
                        escritor.println(mensaje);

                        System.out.println("Servidor: " + lectorServidor.readLine());
                    }

                } else if ("3".equals(opcionMenu)) { // Leer mensajes
                    String linea;
                    while (!(linea = lectorServidor.readLine()).equals("FIN_MENSAJES")) {
                        System.out.println(linea);
                    }

                } else if ("4".equals(opcionMenu)) { // Salir
                    System.out.println("Desconectando del servidor...");
                    break;
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
        int contador = 0;
        while (contador <= 2) {
            System.out.print("Ingresa tu intento (1-10): ");
            String intento = teclado.readLine();
            try {
                int numero = Integer.parseInt(intento);
                escritor.println(intento);
                String respuesta = lectorServidor.readLine();
                System.out.println("Servidor: " + respuesta);
                contador = contador + 1;
                if (respuesta.contains("FIN_JUEGO")) {
                    break;
                }
            } catch (NumberFormatException e) {
                System.out.println("Entrada inválida. Por favor, ingresa un número.");
            }
        }

    }
}

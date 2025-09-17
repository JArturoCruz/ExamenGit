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

            System.out.println("Servidor: " + lectorServidor.readLine()); // Bienvenida
            String opcionLogin = teclado.readLine();
            escritor.println(opcionLogin);

            System.out.println("Servidor: " + lectorServidor.readLine()); // Usuario
            String usuario = teclado.readLine();
            escritor.println(usuario);

            System.out.println("Servidor: " + lectorServidor.readLine()); // Contraseña
            String contrasena = teclado.readLine();
            escritor.println(contrasena);

            String respuestaAuth = lectorServidor.readLine();
            System.out.println("Servidor: " + respuestaAuth);

            if (!respuestaAuth.contains("Autenticación exitosa") && !respuestaAuth.contains("registrado exitosamente")) {
                System.out.println("No se pudo iniciar sesión. Adiós.");
                return;
            }

            // Mostrar menú inicial del servidor
            System.out.println("Servidor: " + lectorServidor.readLine());

            String opcionMenu;
            while (true) {
                opcionMenu = teclado.readLine();
                if (!opcionMenu.matches("[1-6]")) {
                    System.out.println("Opción no válida. Inténtalo de nuevo.");
                    continue;
                }

                escritor.println(opcionMenu);

                switch (opcionMenu) {
                    case "1": // Juego
                        System.out.println("Servidor: " + lectorServidor.readLine());
                        jugar(lectorServidor, teclado, escritor);
                        break;

                    case "2": // Enviar mensaje
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
                        break;

                    case "3": // Leer mensajes
                        String linea;
                        while (!(linea = lectorServidor.readLine()).equals("FIN_MENSAJES")) {
                            System.out.println(linea);
                        }
                        break;

                    case "4": // Cerrar sesión
                        System.out.println("Desconectando del servidor...");
                        return;

                    case "5": // Eliminar mensaje recibido
                    case "6": // Eliminar mensaje enviado
                        while (true) {
                            linea = lectorServidor.readLine();
                            if (linea == null
                                    || linea.equals("Mensaje eliminado exitosamente.")
                                    || linea.contains("No tienes")
                                    || linea.equals("Escribe el número del mensaje que deseas eliminar:")) {
                                System.out.println(linea);
                                break;
                            }
                            System.out.println(linea);
                        }

                        if ("Escribe el número del mensaje que deseas eliminar:".equals(linea)) {
                            String seleccion = teclado.readLine();
                            System.out.println("Enviando selección: " + seleccion); // depuración
                            escritor.println(seleccion);
                            System.out.println("Servidor: " + lectorServidor.readLine());
                        }
                        break;

                }

                // Mostrar menú actualizado enviado por el servidor
                String lineaMenu;
                while ((lineaMenu = lectorServidor.readLine()) != null) {
                    if (lineaMenu.startsWith("Menú:")) {
                        System.out.println("\n" + lineaMenu);
                        break;
                    }
                }
            }

        } catch (IOException e) {
            System.out.println("Ocurrió un error en el cliente: " + e.getMessage());
        }
    }

    private static void jugar(BufferedReader lectorServidor, BufferedReader teclado, PrintWriter escritor) throws IOException {
        while (true) {
            System.out.print("Ingresa tu intento (1-10): ");
            String intento = teclado.readLine();
            try {
                Integer.parseInt(intento);
                escritor.println(intento);
                String respuesta = lectorServidor.readLine();
                System.out.println("Servidor: " + respuesta);
                if (respuesta.contains("FIN_JUEGO")) {
                    break;
                }
            } catch (NumberFormatException e) {
                System.out.println("Entrada inválida. Por favor, ingresa un número.");
            }
        }
    }
}

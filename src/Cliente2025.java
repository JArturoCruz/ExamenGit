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
                        while (true) {
                            String linea = lectorServidor.readLine();
                            if (linea == null) break;

                            System.out.println(linea);

                            if (linea.equals("FIN_PAGINA")) {
                                System.out.print("Elige una opción: ");
                                String eleccion = teclado.readLine();
                                escritor.println(eleccion);
                                if (eleccion.equalsIgnoreCase("V")) {
                                    break;
                                }
                            } else if (linea.contains("No tienes mensajes.")) {
                                // En este caso, no hay paginación y el servidor nos dice que terminemos.
                                break;
                            }
                        }
                        break;

                    case "4": // Cerrar sesión
                        System.out.println("Desconectando del servidor...");
                        return;

                    case "5": // Eliminar mensaje recibido
                    case "6": // Eliminar mensaje enviado
                        while (true) {
                            String linea = lectorServidor.readLine();
                            if (linea == null
                                    || linea.equals("Mensaje eliminado exitosamente.")
                                    || linea.contains("No tienes")
                                    || linea.equals("Escribe el número del mensaje que deseas eliminar:")
                                    || linea.equals("Selección fuera de rango.")
                                    || linea.equals("Entrada inválida.")) {
                                System.out.println(linea);
                                break;
                            }
                            System.out.println(linea);
                        }

                        String ultimaLinea = lectorServidor.readLine();
                        System.out.println(ultimaLinea);
                        if ("Escribe el número del mensaje que deseas eliminar:".equals(ultimaLinea)) {
                            String seleccion = teclado.readLine();
                            escritor.println(seleccion);
                            System.out.println("Servidor: " + lectorServidor.readLine());
                        }
                        break;

                }

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
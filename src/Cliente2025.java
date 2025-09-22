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

            System.out.println("Servidor: " + lectorServidor.readLine());

            String opcionLogin;
            while (true) {
                System.out.print("Elige una opción: ");
                opcionLogin = teclado.readLine();
                if ("1".equals(opcionLogin) || "2".equals(opcionLogin)) {
                    escritor.println(opcionLogin);
                    break;
                } else {
                    System.out.println("Opción no válida. Por favor, elige [1] o [2].");
                }
            }

            System.out.println("Servidor: " + lectorServidor.readLine());
            String usuario = teclado.readLine();
            escritor.println(usuario);

            System.out.println("Servidor: " + lectorServidor.readLine());
            String contrasena = teclado.readLine();
            escritor.println(contrasena);

            String respuestaAuth = lectorServidor.readLine();
            System.out.println("Servidor: " + respuestaAuth);

            if (!respuestaAuth.contains("Autenticación exitosa") && !respuestaAuth.contains("registrado exitosamente")) {
                System.out.println("No se pudo iniciar sesión. Adiós.");
                return;
            }

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
                    case "1":
                        System.out.println("Servidor: " + lectorServidor.readLine());
                        jugar(lectorServidor, teclado, escritor);
                        break;

                    case "2":
                        // Lee el mensaje del servidor para el destinatario
                        String promptDestinatario = lectorServidor.readLine();
                        System.out.println("Servidor: " + promptDestinatario);
                        String destinatario = teclado.readLine();
                        escritor.println(destinatario);

                        // Si el usuario decidió volver, el servidor envía un mensaje de confirmación y volvemos al menú.
                        if (destinatario.equalsIgnoreCase("V")) {
                            System.out.println("Servidor: " + lectorServidor.readLine()); // Lee el mensaje "Volviendo..."
                            break;
                        }

                        String respuestaDestinatario = lectorServidor.readLine();
                        System.out.println("Servidor: " + respuestaDestinatario);

                        // Si el usuario existe, pide el mensaje.
                        if (!respuestaDestinatario.startsWith("Error:")) {
                            String mensaje = teclado.readLine();
                            escritor.println(mensaje);
                            // Si el usuario decidió volver, el servidor envía un mensaje de confirmación y volvemos al menú.
                            if (mensaje.equalsIgnoreCase("V")) {
                                System.out.println("Servidor: " + lectorServidor.readLine()); // Lee el mensaje "Volviendo..."
                                break;
                            }
                            System.out.println("Servidor: " + lectorServidor.readLine());
                        }
                        break;

                    case "3":
                        while (true) {
                            String linea = lectorServidor.readLine();
                            if (linea == null || linea.equals("FIN_PAGINA")) {
                                if (linea == null) break;
                                System.out.print("Elige una opción: ");
                                String eleccion = teclado.readLine();
                                escritor.println(eleccion);
                                if (eleccion.equalsIgnoreCase("V")) {
                                    break;
                                }
                            } else if (linea.contains("No tienes mensajes.")) {
                                System.out.println(linea);
                                break;
                            } else {
                                System.out.println(linea);
                            }
                        }
                        break;

                    case "4":
                        System.out.println("Desconectando del servidor...");
                        return;

                    case "5":
                    case "6":
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
                                } else {
                                    String respuestaServidor = lectorServidor.readLine();
                                    System.out.println("Servidor: " + respuestaServidor);
                                    if (!respuestaServidor.contains("Página")) {
                                        break;
                                    }
                                }
                            } else if (linea.contains("No tienes mensajes")) {
                                break;
                            }
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

            escritor.println(intento);

            String respuesta = lectorServidor.readLine();
            System.out.println("Servidor: " + respuesta);

            if (respuesta.contains("FIN_JUEGO")) {
                break;
            }
        }
    }
}
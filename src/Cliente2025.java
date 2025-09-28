import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.io.File;
import java.io.FileReader;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.util.regex.Pattern;

public class Cliente2025 {

    private static final String DIRECTORIO_CLIENTE = "cliente_archivos";
    private static String usuarioAutenticado;

    public static void main(String[] args) {
        // Crea el directorio raíz para los archivos del cliente si no existe
        new File(DIRECTORIO_CLIENTE).mkdirs();

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

            usuarioAutenticado = usuario;
            new File(DIRECTORIO_CLIENTE + File.separator + usuarioAutenticado).mkdirs();

            String mensajeInicial = lectorServidor.readLine();
            if (mensajeInicial.contains("SOLICITUDES DE DESCARGA PENDIENTES") || mensajeInicial.startsWith("INFO:")) {
                System.out.println("\n" + mensajeInicial);

                if(mensajeInicial.contains("SOLICITUDES DE DESCARGA PENDIENTES")) {
                    System.out.println("(Presiona Enter para revisar las solicitudes)");


                    teclado.readLine();
                    escritor.println("ENTER_PRESIONADO");

                    while (true) {
                        String linea = lectorServidor.readLine();
                        if (linea == null || linea.startsWith("--- Fin de solicitudes ---")) break;

                        System.out.println(linea);

                        if (linea.contains("¿Permitir descarga?")) {
                            String opcion = teclado.readLine();
                            escritor.println(opcion);
                            System.out.println("Servidor: " + lectorServidor.readLine()); // Lee el mensaje de confirmación/denegación
                        }
                    }
                }
            } else {
                System.out.println(mensajeInicial);
            }

            System.out.println("Servidor: " + lectorServidor.readLine());
            String opcionMenu;
            while (true) {
                opcionMenu = teclado.readLine();
                if (!Pattern.matches("1|2|3|4|5|6|7|8|9|10|11|12", opcionMenu)) {
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
                        // Nueva lógica para leer mensajes con opciones de filtrado
                        String opcionLectura = lectorServidor.readLine();
                        System.out.println("Servidor: " + opcionLectura);
                        String eleccionLectura = teclado.readLine();
                        escritor.println(eleccionLectura);

                        if ("2".equals(eleccionLectura)) {
                            String promptUsuario = lectorServidor.readLine();
                            System.out.println("Servidor: " + promptUsuario);
                            String usuarioFiltro = teclado.readLine();
                            escritor.println(usuarioFiltro);
                        }

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
                    case "7":
                        while (true) {
                            String linea = lectorServidor.readLine();
                            if (linea == null || linea.equals("FIN_USUARIOS")) {
                                break;
                            }
                            System.out.println("Servidor: " + linea);
                        }
                        break;
                    case "8":
                        // Lee y muestra la solicitud del servidor (e.g., "Escribe el usuario a...")
                        String promptBloqueo = lectorServidor.readLine();
                        System.out.println("Servidor: " + promptBloqueo);
                        String usuarioBloquear = teclado.readLine();
                        escritor.println(usuarioBloquear);
                        // Lee y muestra la respuesta del servidor (éxito, error, o siguiente prompt)
                        String respuestaBloqueo = lectorServidor.readLine();
                        System.out.println("Servidor: " + respuestaBloqueo);

                        if (respuestaBloqueo.contains("¿Quieres")) {
                            String opcion = teclado.readLine();
                            escritor.println(opcion);
                            String resultado = lectorServidor.readLine();
                            System.out.println("Servidor: " + resultado);
                        }
                        break;
                    case "9":
                        System.out.println("Servidor: " + lectorServidor.readLine());
                        String usuarioListar = teclado.readLine();
                        escritor.println(usuarioListar);

                        while (true) {
                            String linea = lectorServidor.readLine();
                            if (linea == null || linea.equals("FIN_LISTA_ARCHIVOS")) {
                                break;
                            }
                            System.out.println("Servidor: " + linea);
                        }
                        break;
                    case "10":
                        System.out.println("Servidor: " + lectorServidor.readLine());
                        String nombreArchivoCrear = teclado.readLine();
                        escritor.println(nombreArchivoCrear);

                        String respuestaCreacion = lectorServidor.readLine();
                        System.out.println("Servidor: " + respuestaCreacion);

                        if (respuestaCreacion.startsWith("Escribe el contenido")) {
                            String linea;
                            while ((linea = teclado.readLine()) != null) {
                                if (linea.equals("FIN_CONTENIDO")) {
                                    escritor.println(linea);
                                    break;
                                }
                                escritor.println(linea);
                            }
                            String respuestaFinal = lectorServidor.readLine();
                            System.out.println("Servidor: " + respuestaFinal);
                        }
                        break;
                    case "11":
                        System.out.println("Servidor: " + lectorServidor.readLine()); // Pide el usuario de origen
                        String usuarioOrigen = teclado.readLine();
                        escritor.println(usuarioOrigen);

                        System.out.println("Servidor: " + lectorServidor.readLine()); // Pide el nombre del archivo
                        String archivoADescargar = teclado.readLine();
                        escritor.println(archivoADescargar);

                        // Recibe la respuesta del servidor (Permiso, Error, o primera línea de contenido)
                        String primeraRespuesta = lectorServidor.readLine();
                        System.out.println("Servidor: " + primeraRespuesta);

                        if (primeraRespuesta.contains("Permiso no concedido") || primeraRespuesta.startsWith("Error:")) {
                            // Si el permiso no fue concedido o hubo un error, espera el FIN_DESCARGA_ARCHIVO del servidor
                            while (true) {
                                String linea = lectorServidor.readLine();
                                if (linea == null || linea.equals("FIN_DESCARGA_ARCHIVO")) break;
                                System.out.println("Servidor: " + linea);
                            }
                            break;
                        }

                        // Procede con la descarga si el permiso está concedido (la primera respuesta ya es contenido)
                        String rutaCompletaDescarga = DIRECTORIO_CLIENTE + File.separator + usuarioAutenticado + File.separator + "descargado_" + archivoADescargar;

                        try (FileWriter escritorArchivo = new FileWriter(rutaCompletaDescarga)) {
                            escritorArchivo.write(primeraRespuesta + System.lineSeparator()); // Escribe la primera línea de contenido

                            while (true) {
                                String linea = lectorServidor.readLine();
                                if (linea == null || linea.equals("FIN_DESCARGA_ARCHIVO")) {
                                    if (linea != null && linea.startsWith("Error:")) {
                                        System.out.println("Servidor: " + linea);
                                    } else {
                                        System.out.println("Archivo '" + archivoADescargar + "' descargado y guardado en '" + rutaCompletaDescarga + "'.");
                                        File archivoDescargado = new File(rutaCompletaDescarga);
                                        if (archivoDescargado.length() == 0) {
                                            System.out.println("[Advertencia]: El archivo descargado está vacío. Es posible que el archivo en el servidor estuviera vacío.");
                                        }
                                    }
                                    break;
                                }
                                escritorArchivo.write(linea + System.lineSeparator());
                            }
                        } catch (IOException e) {
                            System.out.println("Error al guardar el archivo descargado.");
                        }
                        break;
                    case "12":
                        System.out.println("Servidor: " + lectorServidor.readLine());
                        String usuarioDestinoInterno = teclado.readLine();
                        escritor.println(usuarioDestinoInterno);

                        System.out.println("Servidor: " + lectorServidor.readLine());
                        String nombreArchivoInterno = teclado.readLine();
                        escritor.println(nombreArchivoInterno);

                        String respuestaInterna = lectorServidor.readLine();
                        System.out.println("Servidor: " + respuestaInterna);
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
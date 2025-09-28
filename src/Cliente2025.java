import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.io.File;
import java.io.FileReader;
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

            // --- LÓGICA CORREGIDA PARA LEER NOTIFICACIONES Y MENSAJE INICIAL ---
            String mensajeInicial = null;
            String lineaLeida;

            // Leemos línea por línea hasta encontrar el mensaje inicial de INFO o SOLICITUDES.
            while ((lineaLeida = lectorServidor.readLine()) != null) {
                // Si encontramos el inicio del mensaje de Solicitudes/Info (que debe venir después de las notificaciones)
                if (lineaLeida.contains("SOLICITUDES DE DESCARGA PENDIENTES") || lineaLeida.startsWith("INFO:")) {
                    mensajeInicial = lineaLeida;
                    break;
                }
                // Si la línea no es el mensaje de INFO/SOLICITUDES, es parte de la notificación (o un salto de línea).
                System.out.println(lineaLeida);
            }

            if (mensajeInicial == null) {
                System.out.println("Error: No se recibió el mensaje inicial del servidor.");
                return;
            }
            // --- FIN DE LÓGICA CORREGIDA ---


            if (mensajeInicial.contains("SOLICITUDES DE DESCARGA PENDIENTES") || mensajeInicial.startsWith("INFO:")) {
                System.out.println("\n" + mensajeInicial);

                if(mensajeInicial.contains("SOLICITUDES DE DESCARGA PENDIENTES")) {
                    System.out.println("(Presiona Enter para revisar las solicitudes)");


                    teclado.readLine();
                    escritor.println("ENTER_PRESIONADO");

                    while (true) {
                        String lineaSol = lectorServidor.readLine();
                        if (lineaSol == null || lineaSol.startsWith("--- Fin de solicitudes ---")) break;

                        System.out.println(lineaSol);

                        if (lineaSol.contains("¿Permitir descarga?")) {
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
                            String lineaMensaje = lectorServidor.readLine();
                            if (lineaMensaje == null || lineaMensaje.equals("FIN_PAGINA")) {
                                if (lineaMensaje == null) break;
                                System.out.print("Elige una opción: ");
                                String eleccion = teclado.readLine();
                                escritor.println(eleccion);
                                if (eleccion.equalsIgnoreCase("V")) {
                                    break;
                                }
                            } else if (lineaMensaje.contains("No tienes mensajes.")) {
                                System.out.println(lineaMensaje);
                                break;
                            } else {
                                System.out.println(lineaMensaje);
                            }
                        }
                        break;
                    case "4":
                        System.out.println("Desconectando del servidor...");
                        return;

                    case "5":
                    case "6":
                        while (true) {
                            String lineaMsjDel = lectorServidor.readLine();
                            if (lineaMsjDel == null) break;

                            System.out.println(lineaMsjDel);

                            if (lineaMsjDel.equals("FIN_PAGINA")) {
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
                            } else if (lineaMsjDel.contains("No tienes mensajes")) {
                                break;
                            }
                        }
                        break;
                    case "7":
                        while (true) {
                            String lineaUsers = lectorServidor.readLine();
                            if (lineaUsers == null || lineaUsers.equals("FIN_USUARIOS")) {
                                break;
                            }
                            System.out.println("Servidor: " + lineaUsers);
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
                            String lineaLista = lectorServidor.readLine();
                            if (lineaLista == null || lineaLista.equals("FIN_LISTA_ARCHIVOS")) {
                                break;
                            }
                            System.out.println("Servidor: " + lineaLista);
                        }
                        break;
                    case "10":
                        System.out.println("Servidor: " + lectorServidor.readLine());
                        String nombreArchivoCrear = teclado.readLine();
                        escritor.println(nombreArchivoCrear);

                        String respuestaCreacion = lectorServidor.readLine();
                        System.out.println("Servidor: " + respuestaCreacion);

                        if (respuestaCreacion.startsWith("Escribe el contenido")) {
                            String lineaContenido;
                            while ((lineaContenido = teclado.readLine()) != null) {
                                if (lineaContenido.equals("FIN_CONTENIDO")) {
                                    escritor.println(lineaContenido);
                                    break;
                                }
                                escritor.println(lineaContenido);
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

                        // Recibe la primera respuesta del servidor (Permiso, Error, NOTIFICACIÓN)
                        String primeraRespuesta = lectorServidor.readLine();
                        System.out.println("Servidor: " + primeraRespuesta);

                        // Si la primera respuesta es una notificación de DECISIÓN o una solicitud de OPCIÓN (1 o 2),
                        // el cliente debe leer la siguiente línea para la opción de descarga/posponer.
                        String opcionDescarga = null;
                        if (primeraRespuesta.contains("NOTIFICACIÓN:") || primeraRespuesta.contains("Permiso concedido para la descarga.")) {
                            String promptOpcion = lectorServidor.readLine();
                            System.out.println("Servidor: " + promptOpcion);
                            opcionDescarga = teclado.readLine();
                            escritor.println(opcionDescarga);
                        }

                        // Si es un error, denegación o posposición, el servidor enviará FIN_DESCARGA_ARCHIVO o la confirmación de pospuesta.
                        if (primeraRespuesta.contains("Permiso no concedido") || primeraRespuesta.startsWith("Error:") ||
                                (opcionDescarga != null && opcionDescarga.equals("2")) || primeraRespuesta.contains("DENEGADO tu solicitud")) {
                            // Espera el mensaje final del servidor (FIN_DESCARGA_ARCHIVO o la confirmación de la pospuesta)
                            while (true) {
                                String lineaFinal = lectorServidor.readLine();
                                if (lineaFinal == null || lineaFinal.equals("FIN_DESCARGA_ARCHIVO")) break;
                                System.out.println("Servidor: " + lineaFinal);
                            }
                            break;
                        }

                        // Si la opción de descarga es '1', la siguiente línea que el servidor envía es la primera línea del archivo.
                        String primeraLineaContenido = (opcionDescarga != null && opcionDescarga.equals("1")) ? lectorServidor.readLine() : primeraRespuesta;

                        // Procede con la descarga si el permiso está concedido (la primera respuesta ya es contenido)
                        String rutaCompletaDescarga = DIRECTORIO_CLIENTE + File.separator + usuarioAutenticado + File.separator + "descargado_" + archivoADescargar;

                        try (FileWriter escritorArchivo = new FileWriter(rutaCompletaDescarga)) {
                            escritorArchivo.write(primeraLineaContenido + System.lineSeparator()); // Escribe la primera línea de contenido

                            while (true) {
                                String lineaDescarga = lectorServidor.readLine();
                                if (lineaDescarga == null || lineaDescarga.equals("FIN_DESCARGA_ARCHIVO")) {
                                    if (lineaDescarga != null && lineaDescarga.startsWith("Error:")) {
                                        System.out.println("Servidor: " + lineaDescarga);
                                    } else {
                                        System.out.println("Archivo '" + archivoADescargar + "' descargado y guardado en '" + rutaCompletaDescarga + "'.");
                                        File archivoDescargado = new File(rutaCompletaDescarga);
                                        // Leer el archivo descargado para verificar si está vacío (manejo de caso extremo)
                                        try(BufferedReader br = new BufferedReader(new FileReader(archivoDescargado))) {
                                            if (br.readLine() == null) {
                                                System.out.println("[Advertencia]: El archivo descargado está vacío. Es posible que el archivo en el servidor estuviera vacío.");
                                            }
                                        } catch (IOException e) {
                                            // Ignorar, solo verificamos el contenido.
                                        }
                                    }
                                    break;
                                }
                                escritorArchivo.write(lineaDescarga + System.lineSeparator());
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
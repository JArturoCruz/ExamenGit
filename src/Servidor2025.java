import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;

public class Servidor2025 {

    private static final String ARCHIVO_USUARIOS = "usuarios.txt";

    public static void main(String[] args) {
        try {
            ServerSocket serverSocket = new ServerSocket(8081);
            System.out.println("Servidor listo. Esperando cliente...");

            while (true) {
                Socket cliente = serverSocket.accept();
                System.out.println("Cliente conectado.");

                new Thread(() -> manejarCliente(cliente)).start();
            }
        } catch (IOException e) {
            System.out.println("Ocurrió un error en el servidor: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void manejarCliente(Socket cliente) {
        try {
            PrintWriter escritor = new PrintWriter(cliente.getOutputStream(), true);
            BufferedReader lector = new BufferedReader(new InputStreamReader(cliente.getInputStream()));

            escritor.println("¿Deseas [1] Iniciar sesión o [2] Registrarte?");
            String opcion = lector.readLine();

            escritor.println("Usuario:");
            String usuario = lector.readLine();
            escritor.println("Contraseña:");
            String contrasena = lector.readLine();

            boolean autenticado = false;

            synchronized (Servidor2025.class) {
                if ("1".equals(opcion)) {
                    autenticado = verificarCredenciales(usuario, contrasena);
                    escritor.println(autenticado ? "Autenticación exitosa" : "Credenciales inválidas");
                } else if ("2".equals(opcion)) {
                    if (registrarUsuario(usuario, contrasena)) {
                        escritor.println("Usuario registrado exitosamente");
                        autenticado = true;
                    } else {
                        escritor.println("El usuario ya existe");
                    }
                } else {
                    escritor.println("Opción no válida");
                }
            }

            if (!autenticado) {
                cliente.close();
                return;
            }

            // Juego
            Random random = new Random();
            int numeroSecreto = random.nextInt(10) + 1;
            int intentos = 0;
            boolean adivinado = false;
            String entrada;

            while (intentos < 3 && (entrada = lector.readLine()) != null) {
                int intentoCliente;
                try {
                    intentoCliente = Integer.parseInt(entrada);
                    if (intentoCliente < 1 || intentoCliente > 10) {
                        escritor.println("Número fuera de rango. Ingresa un número entre 1 y 10.");
                        continue;
                    }
                    intentos++;
                    if (intentoCliente == numeroSecreto) {
                        escritor.println("¡Felicidades! Has adivinado el número.");
                        adivinado = true;
                        break;
                    } else if (intentoCliente < numeroSecreto) {
                        escritor.println("El número es mayor.");
                    } else {
                        escritor.println("El número es menor.");
                    }
                } catch (NumberFormatException e) {
                    escritor.println("Entrada inválida. Por favor, ingresa un número entre 1 y 10.");
                }
            }

            if (!adivinado) {
                escritor.println("Se acabaron los intentos. El número era: " + numeroSecreto);
            }

            cliente.close();
        } catch (IOException e) {
            System.out.println("Error con el cliente: " + e.getMessage());
        }
    }

    private static boolean verificarCredenciales(String usuario, String contrasena) throws IOException {
        File archivo = new File(ARCHIVO_USUARIOS);
        if (!archivo.exists()) return false;

        BufferedReader lector = new BufferedReader(new FileReader(archivo));
        String linea;
        while ((linea = lector.readLine()) != null) {
            String[] partes = linea.split(":");
            if (partes.length == 2 && partes[0].equals(usuario) && partes[1].equals(contrasena)) {
                lector.close();
                return true;
            }
        }
        lector.close();
        return false;
    }

    private static boolean registrarUsuario(String usuario, String contrasena) throws IOException {
        File archivo = new File(ARCHIVO_USUARIOS);
        if (!archivo.exists()) archivo.createNewFile();

        BufferedReader lector = new BufferedReader(new FileReader(archivo));
        String linea;
        while ((linea = lector.readLine()) != null) {
            String[] partes = linea.split(":");
            if (partes.length > 0 && partes[0].equals(usuario)) {
                lector.close();
                return false; // Usuario ya existe
            }
        }
        lector.close();

        BufferedWriter escritor = new BufferedWriter(new FileWriter(archivo, true));
        escritor.write(usuario + ":" + contrasena);
        escritor.newLine();
        escritor.close();
        return true;
    }
}

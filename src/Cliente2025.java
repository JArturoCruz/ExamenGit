import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class Cliente2025 {
    public static void main(String[] args) {
        try {
            Socket socket = new Socket("localhost", 8081);
            PrintWriter escritor = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader lector = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            BufferedReader teclado = new BufferedReader(new InputStreamReader(System.in));

            System.out.println(lector.readLine()); // ¿Deseas [1] Iniciar sesión o [2] Registrarte?
            String opcion = teclado.readLine();
            escritor.println(opcion);

            System.out.print(lector.readLine()); // Usuario:
            String usuario = teclado.readLine();
            escritor.println(usuario);

            System.out.print(lector.readLine()); // Contraseña:
            String contrasena = teclado.readLine();
            escritor.println(contrasena);

            String respuesta = lector.readLine();
            System.out.println("Servidor: " + respuesta);

            if (!respuesta.contains("Autenticación exitosa") && !respuesta.contains("registrado")) {
                System.out.println("No puedes continuar sin iniciar sesión.");
                socket.close();
                return;
            }

            int intentos = 0;

            while (intentos < 3) {
                System.out.print("Adivina el número (1-10): ");
                String entrada = teclado.readLine();
                escritor.println(entrada);

                respuesta = lector.readLine();
                if (respuesta == null) break;
                System.out.println("Servidor: " + respuesta);

                boolean entradaInvalida = respuesta.contains("Entrada inválida")
                        || respuesta.contains("Número fuera de rango");
                if (!entradaInvalida) {
                    intentos++;
                }
                if (respuesta.contains("Felicidades") || respuesta.contains("Se acabaron")) {
                    break;
                }
            }

            socket.close();
        } catch (IOException e) {
            System.out.println("Ocurrió un error en cliente: " + e.getMessage());
        }
    }
}

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class Cliente2025 {
    public static void main(String[] args) {
        try {
            Socket salida = new Socket("localhost", 8081);
            PrintWriter escritor = new PrintWriter(salida.getOutputStream(), true);
            BufferedReader lector = new BufferedReader(new InputStreamReader(salida.getInputStream()));
            BufferedReader teclado = new BufferedReader(new InputStreamReader(System.in));

            int intentos = 0;
            String respuesta;

            while (intentos < 3) {
                System.out.print("Adivina el número (1-10): ");
                String entrada = teclado.readLine();
                escritor.println(entrada);

                respuesta = lector.readLine();
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

            salida.close();

        } catch (IOException e) {
            System.out.println("Ocurrió un error en cliente: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
 
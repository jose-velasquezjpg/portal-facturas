package pe.edu.utp.portalfacturas.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/** Herramienta local para generar un hash BCrypt antes de registrar un usuario de laboratorio. */
public final class GenerarHashBCrypt {
    private GenerarHashBCrypt() { }

    public static void main(String[] args) {
        if (args.length != 1 || args[0] == null || args[0].isBlank()) {
            System.err.println("Uso: indicar una contraseña de laboratorio como único argumento.");
            System.err.println("Ejemplo: mvn -q -Dexec.mainClass=pe.edu.utp.portalfacturas.util.GenerarHashBCrypt -Dexec.args=\"ClaveLocalSegura123!\" exec:java");
            System.exit(2);
        }
        System.out.println(new BCryptPasswordEncoder().encode(args[0]));
    }
}

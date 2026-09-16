import java.io.File;
import java.security.KeyStore;

/**
 * Comprueba que el keystore de subida, su alias y sus dos contrasenas son
 * correctos, usando la misma API que AGP emplea al firmar el AAB.
 *
 * Existe porque keytool no sirve para esto: cuando la contrasena de clave
 * falla, reintenta con la del almacen y da un falso OK. KeyStore.getKey no
 * hace ese fallback, asi que reproduce exactamente el fallo que daria
 * bundleRelease, pero en segundos en lugar de al final de la compilacion.
 *
 * Lee RELEASE_KEYSTORE_PATH, RELEASE_STORE_PASSWORD, RELEASE_KEY_ALIAS y
 * RELEASE_KEY_PASSWORD del entorno. Sale con 0 si todo es valido y con 1,
 * describiendo cual de los cuatro falla, en caso contrario.
 */
public class ValidateKeystore {

    public static void main(String[] args) {
        String path = env("RELEASE_KEYSTORE_PATH");
        String storePassword = env("RELEASE_STORE_PASSWORD");
        String alias = env("RELEASE_KEY_ALIAS");
        String keyPassword = env("RELEASE_KEY_PASSWORD");

        KeyStore keyStore;
        try {
            keyStore = KeyStore.getInstance(new File(path), storePassword.toCharArray());
        } catch (Exception e) {
            fail("RELEASE_STORE_PASSWORD incorrecta, o el keystore esta corrupto"
                    + " (revisa el base64 de RELEASE_KEYSTORE): " + e.getMessage());
            return;
        }

        try {
            if (!keyStore.containsAlias(alias)) {
                fail("El alias de RELEASE_KEY_ALIAS no existe en el keystore.");
                return;
            }
            keyStore.getKey(alias, keyPassword.toCharArray());
        } catch (Exception e) {
            fail("RELEASE_KEY_PASSWORD incorrecta: el keystore se abre y el alias existe,"
                    + " pero la clave privada no se descifra (" + e.getMessage() + ")");
            return;
        }

        System.out.println("Keystore, alias y contrasenas correctos.");
    }

    /**
     * Lee una variable de entorno obligatoria.
     *
     * @param name nombre de la variable.
     * @return su valor, o cadena vacia si no esta definida.
     */
    private static String env(String name) {
        String value = System.getenv(name);
        return value == null ? "" : value;
    }

    /**
     * Anota el error en el resumen de GitHub Actions y aborta el job.
     *
     * @param message explicacion de cual de los cuatro datos de firma falla.
     */
    private static void fail(String message) {
        System.out.println("::error::" + message);
        System.exit(1);
    }
}

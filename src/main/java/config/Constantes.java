package config;
import io.github.cdimascio.dotenv.Dotenv;

public class Constantes {
    private static final Dotenv dotenv = Dotenv.load();

    public static final String USERNAME = dotenv.get("DB_USER");
    public static final String PASSWORD = dotenv.get("DB_PASSWORD");
    public static final String ORACLE_URL = dotenv.get("DB_URL");
}
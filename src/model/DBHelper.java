package model;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DBHelper {
    private final String url;
    private final String user;
    private final String password;

    public DBHelper(String host, int port, String dbName, String user, String password) {
        this.url = "jdbc:postgresql://" + host + ":" + port + "/" + dbName;
        this.user = user;
        this.password = password;
    }

    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(url, user, password);
    }

    public void initDatabase() throws SQLException {
        try (Connection c = getConnection(); Statement st = c.createStatement()) {
            // Table situation : états uniques du plateau encodés en base 3 hex
            st.execute(
                "CREATE TABLE IF NOT EXISTS situation (" +
                "  id            SERIAL PRIMARY KEY," +
                "  base3_hex     TEXT NOT NULL UNIQUE," +  // forme canonique = min(hex, sym_hex)
                "  sym_base3_hex TEXT NOT NULL," +          // symétrique horizontal
                "  nb_parties    INTEGER DEFAULT 1," +      // mutualisé : nb de parties y menant
                "  move_number   INTEGER," +                // profondeur du coup
                "  resultat      INTEGER" +                 // NULL=en cours, 0=nul, 1=j1, 2=j2
                ")"
            );
            st.execute("CREATE INDEX IF NOT EXISTS idx_situation_base3 ON situation(base3_hex)");
            st.execute("CREATE INDEX IF NOT EXISTS idx_situation_sym   ON situation(sym_base3_hex)");

            // Table partie : progression (séquence de coups menant à une situation)
            st.execute(
                "CREATE TABLE IF NOT EXISTS partie (" +
                "  id           SERIAL PRIMARY KEY," +
                "  situation_id INTEGER REFERENCES situation(id) ON DELETE SET NULL," +
                "  sequence     TEXT NOT NULL," +    // ex: '4534621' (colonnes 1-based)
                "  nb_coups     INTEGER," +
                "  winner       INTEGER DEFAULT 0," + // 0=en cours, 1=j1, 2=j2
                "  mode         VARCHAR(20) CHECK (mode IN " +
                "      ('HUMAN_VS_HUMAN','HUMAN_VS_AI','AI_VS_AI'))," +
                "  created_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                ")"
            );
            st.execute("CREATE INDEX IF NOT EXISTS idx_partie_situation ON partie(situation_id)");
        }
    }
}

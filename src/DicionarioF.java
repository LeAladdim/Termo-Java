import javax.swing.JOptionPane;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

public class DicionarioF implements FonteDicionario {
    private final String caminhoFicheiro;

    public DicionarioF(String caminhoFicheiro) {
        this.caminhoFicheiro = caminhoFicheiro;
    }

    @Override
    public void carregar(List<String> respostas, Map<String, String> dicionario) {
        InputStream is = getClass().getResourceAsStream(caminhoFicheiro);

        if (is == null) {
            JOptionPane.showMessageDialog(null,
                    "Ficheiro '" + caminhoFicheiro + "' não encontrado!\n\n" +
                            "Certifique-se de que o ficheiro está na raiz da pasta de recursos.",
                    "Erro - Dicionário Ausente",
                    JOptionPane.ERROR_MESSAGE);
            System.exit(0);
        }

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            String linha;
            while ((linha = reader.readLine()) != null) {
                String w = linha.trim();
                if (w.isEmpty()) continue;

                String n = Dicionario.normalizar(w);
                if (n.matches("[A-Z]{5}") && dicionario.putIfAbsent(n, w.toLowerCase()) == null) {
                    respostas.add(w);
                }
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Falha ao ler o ficheiro: " + e.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }

        if (respostas.isEmpty()) {
            JOptionPane.showMessageDialog(null, "Nenhuma palavra válida de 5 letras foi encontrada.", "Erro", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }
    }
}
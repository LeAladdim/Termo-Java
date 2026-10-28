import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

public class Dicionario {
    private final Map<String, String> mapaPalavras = new HashMap<>();
    private final List<String> respostas = new ArrayList<>();

    public Dicionario(FonteDicionario fonte) {
        fonte.carregar(respostas, mapaPalavras);
    }

    public static String normalizar(String s) {
        return Normalizer.normalize(s, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toUpperCase(Locale.ROOT);
    }

    public String obterPalavraAleatoria(Random rnd) {
        return respostas.get(rnd.nextInt(respostas.size()));
    }

    public boolean contem(String palavraNormalizada) {
        return mapaPalavras.containsKey(palavraNormalizada);
    }

    public String obterDisplayCorreto(String palavraNormalizada) {
        return mapaPalavras.getOrDefault(palavraNormalizada, palavraNormalizada.toLowerCase()).toUpperCase(Locale.ROOT);
    }
}
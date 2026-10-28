import java.util.Arrays;
import java.util.Random;

public class TermoJ {
    public static final int TAMANHO = 5;
    public static final int TENTATIVAS = 6;

    private final Dicionario dicionario;
    private String resposta;
    private String respostaNormalizada;
    private int linhaAtual;
    private int colunaAtual;
    private final char[] tentativaAtual = new char[TAMANHO];
    private boolean terminado;
    private boolean estrito = false;

    public TermoJ(Dicionario dicionario) {
        this.dicionario = dicionario;
    }

    public void iniciarNovoJogo(Random rnd) {
        this.resposta = dicionario.obterPalavraAleatoria(rnd);
        this.respostaNormalizada = Dicionario.normalizar(this.resposta);
        this.linhaAtual = 0;
        this.colunaAtual = 0;
        this.terminado = false;
        Arrays.fill(this.tentativaAtual, '\0');
    }

    public void apagarLetraCursor() {
        tentativaAtual[colunaAtual] = '\0';
    }

    public void retrocederEApagar() {
        if (tentativaAtual[colunaAtual] != '\0') {
            apagarLetraCursor();
        } else if (colunaAtual > 0) {
            colunaAtual--;
            apagarLetraCursor();
        }
    }

    public void digitarLetra(char c) {
        if (terminado) return;
        tentativaAtual[colunaAtual] = c;
        int proximo = proximoVazio(colunaAtual);
        if (proximo >= 0) colunaAtual = proximo;
    }

    public void moverCursor(int d) {
        if (terminado) return;
        colunaAtual = Math.max(0, Math.min(TAMANHO - 1, colunaAtual + d));
    }

    private int proximoVazio(int inicio) {
        for (int k = 1; k < TAMANHO; k++) {
            int i = (inicio + k) % TAMANHO;
            if (tentativaAtual[i] == '\0') return i;
        }
        return -1;
    }

    public boolean tentativaIncompleta() {
        for (char c : tentativaAtual) {
            if (c == '\0') return true;
        }
        return false;
    }

    public String obterTentativaString() {
        return new String(tentativaAtual);
    }

    public boolean isTentativaValida(String palpite) {
        return !estrito || dicionario.contem(palpite);
    }

    public EstadoLetra[] avaliar(String palpite) {
        EstadoLetra[] resultado = new EstadoLetra[TAMANHO];
        int[] contagem = new int[26];

        for (int i = 0; i < TAMANHO; i++) {
            if (palpite.charAt(i) == respostaNormalizada.charAt(i)) {
                resultado[i] = EstadoLetra.CERTA;
            } else {
                contagem[respostaNormalizada.charAt(i) - 'A']++;
            }
        }

        for (int i = 0; i < TAMANHO; i++) {
            if (resultado[i] != null) continue;
            int k = palpite.charAt(i) - 'A';
            if (contagem[k] > 0) {
                resultado[i] = EstadoLetra.LUGAR;
                contagem[k]--;
            } else {
                resultado[i] = EstadoLetra.ERRADA;
            }
        }
        return resultado;
    }

    public void avancarLinha() {
        linhaAtual++;
        colunaAtual = 0;
        Arrays.fill(tentativaAtual, '\0');
    }

    public int getLinhaAtual() { return linhaAtual; }
    public int getColunaAtual() { return colunaAtual; }
    public void setColunaAtual(int c) { this.colunaAtual = c; }
    public char[] getPalavraAtual() { return tentativaAtual; }
    public boolean isTerminado() { return terminado; }
    public void setTerminado(boolean terminado) { this.terminado = terminado; }
    public String getRespostaOriginal() { return resposta; }
    public Dicionario getDicionario() { return dicionario; }
}
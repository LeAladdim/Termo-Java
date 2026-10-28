import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

public class UI extends JFrame {
    private static final Color BG = new Color(0x3b3336);
    private static final Color TECLA_COR = new Color(0x5e5156);
    private static final Color ATIVO = new Color(0xe8d9de);

    private final TermoJ jogo;
    private final Design[][] grelha = new Design[TermoJ.TENTATIVAS][TermoJ.TAMANHO];
    private final Map<Character, JLabel> botoesTeclado = new HashMap<>();
    private final Map<Character, EstadoLetra> estadoTeclado = new HashMap<>();
    private final JLabel msg = new JLabel(" ", SwingConstants.CENTER);

    private boolean ocupado = false;
    private final Random rnd = new Random();

    public UI(TermoJ jogo) {
        super("Termo");
        this.jogo = jogo;

        setDefaultCloseOperation(EXIT_ON_CLOSE);
        getContentPane().setBackground(BG);
        setLayout(new BorderLayout(0, 10));

        construirCabecalho();
        construirGrelha();
        construirTeclado();
        configurarEventosTeclado();

        novoJogo();
        pack();
        setMinimumSize(getSize());
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setResizable(true);
        setLocationRelativeTo(null);
    }

    private void construirCabecalho() {
        JPanel topo = new JPanel(new GridLayout(2, 1));
        topo.setBackground(BG);
        topo.setBorder(BorderFactory.createEmptyBorder(14, 10, 0, 10));

        JLabel titulo = new JLabel("TERMO", SwingConstants.CENTER);
        titulo.setFont(new Font("SansSerif", Font.BOLD, 34));
        titulo.setForeground(Color.WHITE);

        msg.setFont(new Font("SansSerif", Font.PLAIN, 14));
        msg.setForeground(ATIVO);

        topo.add(titulo);
        topo.add(msg);
        add(topo, BorderLayout.NORTH);
    }

    private void construirGrelha() {
        JPanel painelGrelha = new JPanel(new GridLayout(TermoJ.TENTATIVAS, TermoJ.TAMANHO, 6, 6));
        painelGrelha.setBackground(BG);
        painelGrelha.setBorder(BorderFactory.createEmptyBorder(0, 24, 0, 24));

        for (int r = 0; r < TermoJ.TENTATIVAS; r++) {
            for (int c = 0; c < TermoJ.TAMANHO; c++) {
                final int linha = r;
                final int coluna = c;
                Design t = new Design();
                t.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                t.addMouseListener(new MouseAdapter() {
                    @Override
                    public void mousePressed(MouseEvent e) {
                        if (!ocupado && !jogo.isTerminado() && linha == jogo.getLinhaAtual()) {
                            jogo.setColunaAtual(coluna);
                            atualizarGrelha();
                        }
                    }
                });
                grelha[r][c] = t;
                painelGrelha.add(t);
            }
        }

        JPanel container = new JPanel(new GridBagLayout());
        container.setBackground(BG);
        container.add(painelGrelha);
        add(container, BorderLayout.CENTER);
    }

    private void construirTeclado() {
        JPanel kb = new JPanel();
        kb.setLayout(new BoxLayout(kb, BoxLayout.Y_AXIS));
        kb.setBackground(BG);
        kb.setBorder(BorderFactory.createEmptyBorder(0, 8, 14, 8));

        String[] linhas = { "QWERTYUIOP", "ASDFGHJKL", "ZXCVBNM" };
        for (int i = 0; i < linhas.length; i++) {
            JPanel p = new JPanel(new FlowLayout(FlowLayout.CENTER, 4, 3));
            p.setBackground(BG);

            if (i == 2) p.add(criarBotaoTecla("ENTER", 70, this::processarEnter));

            for (char c : linhas[i].toCharArray()) {
                final char cc = c;
                JLabel b = criarBotaoTecla(String.valueOf(c), 42, () -> processarLetra(cc));
                botoesTeclado.put(c, b);
                p.add(b);
            }

            if (i == 2) p.add(criarBotaoTecla("\u232B", 70, this::processarApagar));
            kb.add(p);
        }
        add(kb, BorderLayout.SOUTH);
    }

    private JLabel criarBotaoTecla(String texto, int w, Runnable acao) {
        JLabel b = new JLabel(texto, SwingConstants.CENTER);
        b.setPreferredSize(new Dimension(w, 50));
        b.setFont(new Font("SansSerif", Font.BOLD, texto.length() > 1 ? 12 : 16));
        b.setForeground(Color.WHITE);
        b.setBackground(TECLA_COR);
        b.setOpaque(true);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) { acao.run(); }
        });
        return b;
    }

    private void configurarEventosTeclado() {
        KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(e -> {
            if (e.getID() == KeyEvent.KEY_PRESSED) {
                switch (e.getKeyCode()) {
                    case KeyEvent.VK_ENTER: processarEnter(); break;
                    case KeyEvent.VK_BACK_SPACE: processarApagar(); break;
                    case KeyEvent.VK_DELETE: processarDelete(); break;
                    case KeyEvent.VK_LEFT: processarSeta(-1); break;
                    case KeyEvent.VK_RIGHT: processarSeta(1); break;
                }
            } else if (e.getID() == KeyEvent.KEY_TYPED) {
                String n = Dicionario.normalizar(String.valueOf(e.getKeyChar()));
                if (n.length() == 1 && n.charAt(0) >= 'A' && n.charAt(0) <= 'Z') {
                    processarLetra(n.charAt(0));
                }
            }
            return false;
        });
    }

    private void novoJogo() {
        jogo.iniciarNovoJogo(rnd);
        ocupado = false;
        estadoTeclado.clear();

        for (Design[] linha : grelha) {
            for (Design t : linha) {
                t.atualizar(' ', EstadoLetra.VAZIA, false);
            }
        }
        atualizarCoresTeclado();
        atualizarGrelha();
        msg.setText("Clique numa casa ou use \u2190 \u2192 para escolher a posição da letra");
    }

    private void processarLetra(char c) {
        if (ocupado || jogo.isTerminado()) return;
        jogo.digitarLetra(c);
        msg.setText(" ");
        atualizarGrelha();
    }

    private void processarApagar() {
        if (ocupado || jogo.isTerminado()) return;
        jogo.retrocederEApagar();
        atualizarGrelha();
    }

    private void processarDelete() {
        if (ocupado || jogo.isTerminado()) return;
        jogo.apagarLetraCursor();
        atualizarGrelha();
    }

    private void processarSeta(int direcao) {
        if (ocupado || jogo.isTerminado()) return;
        jogo.moverCursor(direcao);
        atualizarGrelha();
    }

    private void processarEnter() {
        if (jogo.isTerminado()) { novoJogo(); return; }
        if (ocupado) return;
        if (jogo.tentativaIncompleta()) {
            msg.setText("Preencha todas as 5 letras");
            return;
        }
        String palpite = jogo.obterTentativaString();
        if (!jogo.isTentativaValida(palpite)) {
            msg.setText("Essa palavra não é aceite");
            return;
        }
        revelar(palpite);
    }

    private void atualizarGrelha() {
        for (int r = 0; r < TermoJ.TENTATIVAS; r++) {
            for (int c = 0; c < TermoJ.TAMANHO; c++) {
                boolean ativo = (!jogo.isTerminado() && r == jogo.getLinhaAtual() && c == jogo.getColunaAtual());

                if (r == jogo.getLinhaAtual()) {
                    char letra = jogo.getPalavraAtual()[c];
                    grelha[r][c].atualizar(letra == '\0' ? ' ' : letra, letra == '\0' ? EstadoLetra.VAZIA : EstadoLetra.DIGITADA, ativo);
                } else if (r > jogo.getLinhaAtual()) {
                    grelha[r][c].atualizar(' ', EstadoLetra.VAZIA, ativo);
                }
            }
        }
    }

    private void revelar(String palpite) {
        ocupado = true;
        msg.setText(" ");
        for (Design t : grelha[jogo.getLinhaAtual()]) {
            t.atualizar(t.getCharExibido(), t.getEstado(), false);
        }

        final String mostrar = jogo.getDicionario().obterDisplayCorreto(palpite);
        final EstadoLetra[] resultado = jogo.avaliar(palpite);
        final int r = jogo.getLinhaAtual();
        final int[] i = { 0 };

        final Timer t = new Timer(280, null);
        t.addActionListener(e -> {
            Design design = grelha[r][i[0]];
            design.atualizar(mostrar.charAt(i[0]), resultado[i[0]], false);
            if (++i[0] == TermoJ.TAMANHO) {
                t.stop();
                finalizarLinha(palpite, resultado);
            }
        });
        t.start();
    }

    private void finalizarLinha(String palpite, EstadoLetra[] resultado) {
        boolean vitoria = true;
        for (int i = 0; i < TermoJ.TAMANHO; i++) {
            char c = palpite.charAt(i);
            EstadoLetra antigo = estadoTeclado.get(c);
            if (antigo == null || classificar(resultado[i]) > classificar(antigo)) {
                estadoTeclado.put(c, resultado[i]);
            }
            if (resultado[i] != EstadoLetra.CERTA) vitoria = false;
        }
        atualizarCoresTeclado();
        jogo.avancarLinha();

        if (vitoria) {
            jogo.setTerminado(true);
            String[] elogios = { "Genial!", "Magnífico!", "Impressionante!", "Muito bom!", "Bom!", "Ufa!" };
            msg.setText(elogios[jogo.getLinhaAtual() - 1] + " Acertou em " + jogo.getLinhaAtual() +
                    (jogo.getLinhaAtual() == 1 ? " tentativa" : " tentativas") + " — Enter para jogar de novo");
        } else if (jogo.getLinhaAtual() == TermoJ.TENTATIVAS) {
            jogo.setTerminado(true);
            msg.setText("A palavra era \"" + jogo.getRespostaOriginal().toUpperCase(Locale.ROOT) + "\" — Enter para jogar de novo");
        }
        ocupado = false;
        atualizarGrelha();
    }

    private static int classificar(EstadoLetra s) {
        return s == EstadoLetra.CERTA ? 3 : s == EstadoLetra.LUGAR ? 2 : 1;
    }

    private void atualizarCoresTeclado() {
        for (Map.Entry<Character, JLabel> e : botoesTeclado.entrySet()) {
            EstadoLetra s = estadoTeclado.get(e.getKey());
            Color cor = s == EstadoLetra.CERTA ? Design.CERTO :
                    s == EstadoLetra.LUGAR ? Design.LUGAR :
                            s == EstadoLetra.ERRADA ? Design.ERRADO : TECLA_COR;
            e.getValue().setBackground(cor);
        }
    }

    public static void main(String[] args) {
        try { UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName()); }
        catch (Exception ignored) { }

        SwingUtilities.invokeLater(() -> {
            FonteDicionario fonte = new DicionarioF("/palavras.txt");
            Dicionario dicionario = new Dicionario(fonte);
            TermoJ jogo = new TermoJ(dicionario);
            new UI(jogo).setVisible(true);
        });
    }
}
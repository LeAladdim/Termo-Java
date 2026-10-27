import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.text.Normalizer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

/**
 * Clone do Termo (Wordle em português) em Java Swing.
 *
 * Compilar: javac -encoding UTF-8 Termo.java
 * Executar: java Termo
 */
public class Termo extends JFrame {

  static final int LEN = 5, TRIES = 6;

  enum S { EMPTY, TYPED, RIGHT, PLACE, WRONG }

  // Cores
  static final Color BG = new Color(0x3b3336);
  static final Color TILE_EMPTY = new Color(0x4a4145);
  static final Color BORDER = new Color(0x6e5c62);
  static final Color ACTIVE = new Color(0xe8d9de);
  static final Color RIGHT = new Color(0x3aa394);
  static final Color PLACE = new Color(0xd3ad69);
  static final Color WRONG = new Color(0x241f21);
  static final Color KEY = new Color(0x5e5156);

  // Dicionário: normalizada (sem acento, maiúscula) -> forma acentuada
  private final Map<String, String> dict = new HashMap<>();
  private final List<String> answers = new ArrayList<>();
  private final Random rnd = new Random();

  private final Tile[][] grid = new Tile[TRIES][LEN];
  private final Map<Character, S> keyState = new HashMap<>();
  private final Map<Character, JLabel> keyBtn = new HashMap<>();
  private final char[] cur = new char[LEN]; // '\0' = casa vazia
  private final JLabel msg = new JLabel(" ", SwingConstants.CENTER);

  private int row, col; // col = casa selecionada (0..4)
  private boolean busy, over;
  private String answer, answerN;
  private boolean strict = true;

  // ---------- Tile ----------
  static class Tile extends JComponent {
    char ch = ' ';
    S st = S.EMPTY;
    boolean active;

    Tile() { setPreferredSize(new Dimension(62, 62)); }

    @Override protected void paintComponent(Graphics g0) {
      Graphics2D g = (Graphics2D) g0.create();
      g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
      int w = getWidth(), h = getHeight();
      Color c = st == S.RIGHT ? RIGHT : st == S.PLACE ? PLACE : st == S.WRONG ? WRONG : TILE_EMPTY;
      g.setColor(c);
      g.fillRoundRect(2, 2, w - 4, h - 4, 14, 14);
      if (st == S.EMPTY || st == S.TYPED) {
        g.setColor(active ? ACTIVE : BORDER);
        g.setStroke(new BasicStroke(3f));
        g.drawRoundRect(3, 3, w - 6, h - 6, 14, 14);
      }
      if (active && (st == S.EMPTY || st == S.TYPED)) {
        g.setColor(ACTIVE);
        g.fillRoundRect(w / 2 - 12, h - 12, 24, 3, 3, 3);
      }
      if (ch != ' ') {
        g.setFont(new Font("SansSerif", Font.BOLD, 30));
        g.setColor(Color.WHITE);
        FontMetrics fm = g.getFontMetrics();
        String s = String.valueOf(ch);
        g.drawString(s, (w - fm.stringWidth(s)) / 2, (h - fm.getHeight()) / 2 + fm.getAscent());
      }
      g.dispose();
    }
  }

  // ---------- Construção da janela ----------
  public Termo() {
    super("Termo");
    loadWords();

    setDefaultCloseOperation(EXIT_ON_CLOSE);
    getContentPane().setBackground(BG);
    setLayout(new BorderLayout(0, 10));

    JPanel top = new JPanel(new GridLayout(2, 1));
    top.setBackground(BG);
    top.setBorder(BorderFactory.createEmptyBorder(14, 10, 0, 10));
    JLabel title = new JLabel("TERMO", SwingConstants.CENTER);
    title.setFont(new Font("SansSerif", Font.BOLD, 34));
    title.setForeground(Color.WHITE);
    msg.setFont(new Font("SansSerif", Font.PLAIN, 14));
    msg.setForeground(ACTIVE);
    top.add(title);
    top.add(msg);
    add(top, BorderLayout.NORTH);

    JPanel board = new JPanel(new GridLayout(TRIES, LEN, 6, 6));
    board.setBackground(BG);
    board.setBorder(BorderFactory.createEmptyBorder(0, 24, 0, 24));
    for (int r = 0; r < TRIES; r++)
      for (int c = 0; c < LEN; c++) {
        final int rr = r, cc = c;
        Tile t = new Tile();
        t.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        t.addMouseListener(new MouseAdapter() {
          @Override public void mousePressed(MouseEvent e) {
            if (!busy && !over && rr == row) { col = cc; refreshActive(); }
          }
        });
        grid[r][c] = t;
        board.add(t);
      }

    JPanel boardContainer = new JPanel(new GridBagLayout());
    boardContainer.setBackground(BG);
    boardContainer.add(board);
    add(boardContainer, BorderLayout.CENTER);

    add(buildKeyboard(), BorderLayout.SOUTH);

    KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(e -> {
      if (e.getID() == KeyEvent.KEY_PRESSED) {
        switch (e.getKeyCode()) {
          case KeyEvent.VK_ENTER: enter(); break;
          case KeyEvent.VK_BACK_SPACE: back(); break;
          case KeyEvent.VK_DELETE: deleteHere(); break;
          case KeyEvent.VK_LEFT: moveCursor(-1); break;
          case KeyEvent.VK_RIGHT: moveCursor(1); break;
          default: break;
        }
      } else if (e.getID() == KeyEvent.KEY_TYPED) {
        String n = norm(String.valueOf(e.getKeyChar()));
        if (n.length() == 1 && n.charAt(0) >= 'A' && n.charAt(0) <= 'Z') type(n.charAt(0));
      }
      return false;
    });

    newGame();
    pack();
    setMinimumSize(getSize());
    setExtendedState(JFrame.MAXIMIZED_BOTH);
    setResizable(true);
    setLocationRelativeTo(null);
  }

  private JPanel buildKeyboard() {
    JPanel kb = new JPanel();
    kb.setLayout(new BoxLayout(kb, BoxLayout.Y_AXIS));
    kb.setBackground(BG);
    kb.setBorder(BorderFactory.createEmptyBorder(0, 8, 14, 8));
    String[] rows = { "QWERTYUIOP", "ASDFGHJKL", "ZXCVBNM" };
    for (int i = 0; i < rows.length; i++) {
      JPanel p = new JPanel(new FlowLayout(FlowLayout.CENTER, 4, 3));
      p.setBackground(BG);
      if (i == 2) p.add(makeKey("ENTER", 70, () -> enter()));
      for (char c : rows[i].toCharArray()) {
        final char cc = c;
        JLabel b = makeKey(String.valueOf(c), 42, () -> type(cc));
        keyBtn.put(c, b);
        p.add(b);
      }
      if (i == 2) p.add(makeKey("\u232B", 70, () -> back()));
      kb.add(p);
    }
    return kb;
  }

  private JLabel makeKey(String text, int w, Runnable action) {
    JLabel b = new JLabel(text, SwingConstants.CENTER);
    b.setPreferredSize(new Dimension(w, 50));
    b.setFont(new Font("SansSerif", Font.BOLD, text.length() > 1 ? 12 : 16));
    b.setForeground(Color.WHITE);
    b.setBackground(KEY);
    b.setOpaque(true);
    b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    b.addMouseListener(new MouseAdapter() {
      @Override public void mousePressed(MouseEvent e) { action.run(); }
    });
    return b;
  }

  // ---------- Palavras ----------
  static String norm(String s) {
    return Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}", "").toUpperCase(Locale.ROOT);
  }

  private void loadWords() {
    Path file = Paths.get("src/palavras.txt");

    if (!Files.exists(file)) {
      JOptionPane.showMessageDialog(null,
          "Arquivo 'palavras.txt' não encontrado!\n\n" +
              "Crie um arquivo de texto com esse nome na mesma pasta do jogo.\n" +
              "Coloque uma palavra de 5 letras por linha e salve em UTF-8.",
          "Erro - Dicionário Ausente",
          JOptionPane.ERROR_MESSAGE);
      System.exit(0);
    }

    try {
      for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
        String w = line.trim();
        String n = norm(w);
        if (n.matches("[A-Z]{5}") && dict.putIfAbsent(n, w.toLowerCase()) == null) {
          answers.add(w);
        }
      }
    } catch (Exception e) {
      JOptionPane.showMessageDialog(null, "Falha ao ler o arquivo palavras.txt.", "Erro", JOptionPane.ERROR_MESSAGE);
      System.exit(1);
    }

    if (answers.isEmpty()) {
      JOptionPane.showMessageDialog(null, "Nenhuma palavra válida de 5 letras foi encontrada no arquivo palavras.txt.", "Erro", JOptionPane.ERROR_MESSAGE);
      System.exit(1);
    }
  }

  // ---------- Lógica do jogo ----------
  private void newGame() {
    answer = answers.get(rnd.nextInt(answers.size()));
    answerN = norm(answer);
    row = 0; col = 0; busy = false; over = false;
    Arrays.fill(cur, '\0');
    for (Tile[] r : grid) for (Tile t : r) { t.ch = ' '; t.st = S.EMPTY; t.active = false; }
    keyState.clear();
    refreshKeys();
    refreshActive();
    msg.setText("Clique numa casa ou use \u2190 \u2192 para escolher a posição da letra");
  }

  private void clearCell(int i) {
    cur[i] = '\0';
    Tile t = grid[row][i];
    t.ch = ' '; t.st = S.EMPTY;
  }

  private int nextEmpty(int from) {
    for (int k = 1; k < LEN; k++) {
      int i = (from + k) % LEN;
      if (cur[i] == '\0') return i;
    }
    return -1;
  }

  private void type(char c) {
    if (busy || over) return;
    cur[col] = c;
    Tile t = grid[row][col];
    t.ch = c; t.st = S.TYPED;
    msg.setText(" ");
    int next = nextEmpty(col);
    if (next >= 0) col = next;
    refreshActive();
  }

  private void back() {
    if (busy || over) return;
    if (cur[col] != '\0') clearCell(col);
    else if (col > 0) { col--; clearCell(col); }
    refreshActive();
  }

  private void deleteHere() {
    if (busy || over) return;
    if (cur[col] != '\0') clearCell(col);
    refreshActive();
  }

  private void moveCursor(int d) {
    if (busy || over) return;
    col = Math.max(0, Math.min(LEN - 1, col + d));
    refreshActive();
  }

  private void enter() {
    if (over) { newGame(); return; }
    if (busy) return;
    for (int i = 0; i < LEN; i++)
      if (cur[i] == '\0') { msg.setText("Preencha todas as 5 letras"); return; }
    String g = new String(cur);
    if (strict && !dict.containsKey(g)) { msg.setText("Essa palavra não é aceita"); return; }
    reveal(g);
  }

  private S[] evaluate(String g, String a) {
    S[] r = new S[LEN];
    int[] cnt = new int[26];
    for (int i = 0; i < LEN; i++) {
      if (g.charAt(i) == a.charAt(i)) r[i] = S.RIGHT;
      else cnt[a.charAt(i) - 'A']++;
    }
    for (int i = 0; i < LEN; i++) {
      if (r[i] != null) continue;
      int k = g.charAt(i) - 'A';
      if (cnt[k] > 0) { r[i] = S.PLACE; cnt[k]--; }
      else r[i] = S.WRONG;
    }
    return r;
  }

  private void reveal(String g) {
    busy = true;
    msg.setText(" ");
    for (Tile t : grid[row]) t.active = false;
    final String show = dict.getOrDefault(g, g.toLowerCase()).toUpperCase(Locale.ROOT);
    final S[] res = evaluate(g, answerN);
    final int r = row;
    final int[] i = { 0 };
    final javax.swing.Timer t = new javax.swing.Timer(280, null);
    t.addActionListener(e -> {
      Tile tile = grid[r][i[0]];
      tile.ch = show.charAt(i[0]);
      tile.st = res[i[0]];
      tile.repaint();
      if (++i[0] == LEN) {
        t.stop();
        finishRow(g, res);
      }
    });
    t.start();
  }

  private void finishRow(String g, S[] res) {
    boolean win = true;
    for (int i = 0; i < LEN; i++) {
      char c = g.charAt(i);
      S old = keyState.get(c);
      if (old == null || rank(res[i]) > rank(old)) keyState.put(c, res[i]);
      if (res[i] != S.RIGHT) win = false;
    }
    refreshKeys();
    row++; col = 0;
    Arrays.fill(cur, '\0');
    if (win) {
      over = true;
      String[] praise = { "Genial!", "Magnífico!", "Impressionante!", "Muito bom!", "Bom!", "Ufa!" };
      msg.setText(praise[row - 1] + " Acertou em " + row + (row == 1 ? " tentativa" : " tentativas")
          + " — Enter para jogar de novo");
    } else if (row == TRIES) {
      over = true;
      msg.setText("A palavra era \"" + answer.toUpperCase(Locale.ROOT) + "\" — Enter para jogar de novo");
    }
    busy = false;
    refreshActive();
  }

  private static int rank(S s) { return s == S.RIGHT ? 3 : s == S.PLACE ? 2 : 1; }

  // ---------- Atualização visual ----------
  private void refreshActive() {
    for (Tile[] r : grid) for (Tile t : r) t.active = false;
    if (!over && row < TRIES) grid[row][col].active = true;
    for (Tile[] r : grid) for (Tile t : r) t.repaint();
  }

  private void refreshKeys() {
    for (Map.Entry<Character, JLabel> e : keyBtn.entrySet()) {
      S s = keyState.get(e.getKey());
      e.getValue().setBackground(s == S.RIGHT ? RIGHT : s == S.PLACE ? PLACE : s == S.WRONG ? WRONG : KEY);
    }
  }

  public static void main(String[] args) {
    try { UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName()); }
    catch (Exception ignored) { }
    SwingUtilities.invokeLater(() -> new Termo().setVisible(true));
  }
}

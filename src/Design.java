import javax.swing.JComponent;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

public class Design extends JComponent {
    private char charExibido = ' ';
    private EstadoLetra estado = EstadoLetra.VAZIA;
    private boolean ativo = false;

    public static final Color TILE_VAZIO = new Color(0x4a4145);
    public static final Color BORDA = new Color(0x6e5c62);
    public static final Color ATIVO = new Color(0xe8d9de);
    public static final Color CERTO = new Color(0x3aa394);
    public static final Color LUGAR = new Color(0xd3ad69);
    public static final Color ERRADO = new Color(0x241f21);

    public Design() {
        Dimension d = new Dimension(62, 62);
        setPreferredSize(d);
        setMinimumSize(d);
        setMaximumSize(d);
    }

    public void atualizar(char c, EstadoLetra est, boolean atv) {
        this.charExibido = c;
        this.estado = est;
        this.ativo = atv;
        repaint();
    }

    public char getCharExibido() { return charExibido; }
    public EstadoLetra getEstado() { return estado; }

    @Override
    protected void paintComponent(Graphics g0) {
        Graphics2D g = (Graphics2D) g0.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        int w = getWidth(), h = getHeight();
        Color corFundo = estado == EstadoLetra.CERTA ? CERTO :
                estado == EstadoLetra.LUGAR ? LUGAR :
                        estado == EstadoLetra.ERRADA ? ERRADO : TILE_VAZIO;

        g.setColor(corFundo);
        g.fillRoundRect(2, 2, w - 4, h - 4, 14, 14);

        if (estado == EstadoLetra.VAZIA || estado == EstadoLetra.DIGITADA) {
            g.setColor(ativo ? ATIVO : BORDA);
            g.setStroke(new BasicStroke(3f));
            g.drawRoundRect(3, 3, w - 6, h - 6, 14, 14);
        }

        if (ativo && (estado == EstadoLetra.VAZIA || estado == EstadoLetra.DIGITADA)) {
            g.setColor(ATIVO);
            g.fillRoundRect(w / 2 - 12, h - 12, 24, 3, 3, 3);
        }

        if (charExibido != ' ') {
            g.setFont(new Font("SansSerif", Font.BOLD, 30));
            g.setColor(Color.WHITE);
            FontMetrics fm = g.getFontMetrics();
            String s = String.valueOf(charExibido);
            g.drawString(s, (w - fm.stringWidth(s)) / 2, (h - fm.getHeight()) / 2 + fm.getAscent());
        }
        g.dispose();
    }
}
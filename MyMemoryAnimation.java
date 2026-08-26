package CG_Assignment1;
import javax.swing.*;
import java.awt.*;
import java.awt.geom.*;

/**
 * "My Memory" — 5 second animation, 3 scenes:
 *   Scene 1 (0.0-1.2s): desktop, Angry Birds 2013 menu, cursor moves & clicks PLAY, fade to black
 *   Scene 2 (1.2-3.4s): in-game — slingshot shot clears the pig, "LEVEL COMPLETE" + 3 stars pop up
 *   Scene 3 (3.4-5.0s): cut to the player's adult face, smiling — flashback-crossfades into their childhood face
 *
 * Loops automatically with a short pause between cycles.
 */
public class MyMemoryAnimation extends JPanel {

    private final long startTime;
    private static final int W = 600, H = 600;

    private static final int SCENE1_END = 2000;  // desktop -> click -> fade out
    private static final int SCENE2_END = 6000;  // gameplay -> 3 stars (+ hold)
    private static final int SCENE3_END = 9500;  // adult -> child flashback (+ hold)
    private static final int LOOP_PAUSE = 1000;

    public MyMemoryAnimation() {
        setPreferredSize(new Dimension(W, H));
        setBackground(Color.BLACK);
        startTime = System.currentTimeMillis();
        new Timer(16, e -> repaint()).start(); // ~60fps
    }

    // ---------- small math helpers ----------
    private float clamp(float v, float lo, float hi) { return Math.max(lo, Math.min(hi, v)); }
    private float lerp(float a, float b, float t) { return a + (b - a) * t; }
    private float easeInOutQuad(float t) { return t < 0.5f ? 2 * t * t : 1 - (float) Math.pow(-2 * t + 2, 2) / 2; }
    private float easeOutQuad(float t) { return 1 - (1 - t) * (1 - t); }
    private float easeOutBack(float t) {
        float c1 = 1.70158f, c3 = c1 + 1f;
        float x = t - 1f;
        return 1 + c3 * x * x * x + c1 * x * x;
    }

    private Polygon buildStar(int cx, int cy, int outerR, int innerR) {
        Polygon p = new Polygon();
        double angle = -Math.PI / 2;
        double step = Math.PI / 5;
        for (int i = 0; i < 10; i++) {
            int r = (i % 2 == 0) ? outerR : innerR;
            p.addPoint((int) (cx + Math.cos(angle) * r), (int) (cy + Math.sin(angle) * r));
            angle += step;
        }
        return p;
    }

    private void drawStar(Graphics2D g2, int cx, int cy, float scale, boolean filled) {
        int outerR = (int) (18 * scale), innerR = (int) (7 * scale);
        if (scale <= 0.01f) return;
        Polygon star = buildStar(cx, cy, outerR, innerR);
        if (filled) {
            g2.setColor(new Color(255, 205, 40));
            g2.fillPolygon(star);
        }
        g2.setColor(new Color(150, 100, 10));
        g2.setStroke(new BasicStroke(2));
        g2.drawPolygon(star);
    }

    private Polygon buildCursor(float x, float y, float scale) {
        int[][] pts = {{0, 0}, {0, 16}, {4, 12}, {7, 19}, {10, 17}, {7, 11}, {12, 11}};
        Polygon p = new Polygon();
        for (int[] pt : pts) p.addPoint((int) (x + pt[0] * scale), (int) (y + pt[1] * scale));
        return p;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        long elapsed = System.currentTimeMillis() - startTime;
        long cycle = SCENE3_END + LOOP_PAUSE;
        long t = elapsed % cycle;

        if (t < SCENE1_END) {
            drawScene1(g2, t / (float) SCENE1_END);
        } else if (t < SCENE2_END) {
            drawScene2(g2, (t - SCENE1_END) / (float) (SCENE2_END - SCENE1_END));
        } else if (t < SCENE3_END) {
            drawScene3(g2, (t - SCENE2_END) / (float) (SCENE3_END - SCENE2_END));
        } else {
            drawScene3(g2, 1f); // hold last frame during the pause
        }
    }

    // ========== SCENE 1 (0-1.2s): desktop -> click PLAY -> fade out ==========
    private void drawScene1(Graphics2D g2, float t) {
        int mw = (int) (W * 0.8f);              // 480
        int mh = (int) (mw * (360 / 640.0f));   // 270
        int mx = (W - mw) / 2;                  // 60
        int my = (int) (H * 0.20f);             // 120
        int deskY = my + mh + 14 + 60;          // 464

        // dim room behind the monitor + desk
        g2.setColor(new Color(30, 30, 36));
        g2.fillRect(0, 0, W, H);
        g2.setColor(new Color(60, 45, 35));
        g2.fillRect(0, deskY, W, H - deskY);
        g2.setColor(new Color(45, 32, 24));
        g2.fillRect(0, deskY, W, 6);

        // monitor bezel & stand
        g2.setColor(new Color(35, 35, 38));
        g2.fillRoundRect(mx - 14, my - 14, mw + 28, mh + 28, 16, 16);
        g2.fillRect(W / 2 - 20, my + mh + 14, 40, deskY - (my + mh + 14)); // stand neck
        g2.fillRoundRect(W / 2 - 70, deskY - 14, 140, 14, 8, 8);           // stand base

        // screen: Angry Birds 2013 menu
        g2.setPaint(new GradientPaint(mx, my, new Color(120, 175, 220), mx, my + mh, new Color(200, 225, 235)));
        g2.fillRect(mx, my, mw, mh);
        g2.setColor(new Color(255, 255, 255, 160));
        g2.fillOval(mx + 40, my + 40, 60, 22);
        g2.fillOval(mx + mw - 120, my + 30, 70, 24);

        g2.setColor(new Color(180, 30, 20));
        g2.setFont(new Font("SansSerif", Font.BOLD, 32));
        FontMetrics fm = g2.getFontMetrics();
        String title = "ANGRY BIRDS";
        g2.drawString(title, mx + (mw - fm.stringWidth(title)) / 2, my + 72);
        g2.setColor(new Color(255, 210, 40));
        g2.setFont(new Font("SansSerif", Font.BOLD, 15));
        fm = g2.getFontMetrics();
        g2.drawString("2013", mx + (mw - fm.stringWidth("2013")) / 2, my + 94);

        int btnCx = mx + mw / 2, btnCy = my + mh / 2 + 32, btnR = 36;
        boolean clicking = t > 0.5f && t < 0.62f;
        g2.setColor(clicking ? new Color(255, 90, 60) : new Color(210, 40, 30));
        g2.fillOval(btnCx - btnR, btnCy - btnR, btnR * 2, btnR * 2);
        g2.setColor(Color.WHITE);
        int[] tx = {btnCx - 11, btnCx - 11, btnCx + 18};
        int[] ty = {btnCy - 18, btnCy + 18, btnCy};
        g2.fillPolygon(tx, ty, 3);

        // click ripple
        if (t > 0.5f) {
            float rt = clamp((t - 0.5f) / 0.3f, 0, 1);
            int rippleR = (int) (btnR + rt * 35);
            g2.setColor(new Color(255, 255, 255, (int) (200 * (1 - rt))));
            g2.setStroke(new BasicStroke(3));
            g2.drawOval(btnCx - rippleR, btnCy - rippleR, rippleR * 2, rippleR * 2);
        }

        // cursor: eases in from bottom-left of screen to the button, "presses" on click
        float moveT = easeInOutQuad(clamp(t / 0.5f, 0, 1));
        float startX = mx + 40, startY = my + mh - 20;
        float curX = lerp(startX, btnCx - 4, moveT);
        float curY = lerp(startY, btnCy - 4, moveT);
        float pressScale = clicking ? 0.85f : 1f;
        g2.setColor(Color.WHITE);
        g2.fillPolygon(buildCursor(curX, curY, pressScale));
        g2.setColor(Color.BLACK);
        g2.setStroke(new BasicStroke(1.2f));
        g2.drawPolygon(buildCursor(curX, curY, pressScale));

        // fade to black — the "cut" into the game
        if (t > 0.62f) {
            int alpha = (int) (255 * clamp((t - 0.62f) / 0.38f, 0, 1));
            g2.setColor(new Color(0, 0, 0, alpha));
            g2.fillRect(0, 0, W, H);
        }
    }

    // ========== SCENE 2 (1.2-3.4s): gameplay -> clear pig -> 3 stars ==========
    private void drawScene2(Graphics2D g2, float t) {
        g2.setPaint(new GradientPaint(0, 0, new Color(140, 190, 230), 0, H, new Color(210, 230, 240)));
        g2.fillRect(0, 0, W, H);
        int groundH = 80;
        g2.setColor(new Color(120, 160, 90));
        g2.fillRect(0, H - groundH, W, groundH);

        int slingX = 110, slingBaseY = H - groundH;
        g2.setColor(new Color(90, 60, 30));
        g2.setStroke(new BasicStroke(10));
        g2.drawLine(slingX - 18, slingBaseY, slingX - 18, slingBaseY - 110);
        g2.drawLine(slingX + 18, slingBaseY, slingX + 18, slingBaseY - 110);

        int towerX = 450;
        Rectangle[] blocks = {
                new Rectangle(towerX, H - groundH - 50, 35, 50),
                new Rectangle(towerX + 45, H - groundH - 50, 35, 50),
                new Rectangle(towerX, H - groundH - 95, 80, 45)
        };
        int pigX = towerX + 40, pigY = H - groundH - 130;

        float shotT = clamp(t / 0.5f, 0, 1); // shot sequence packed into first half of the scene
        float birdX, birdY;
        boolean impactDone = t >= 0.5f;

        if (shotT < 0.18f) {
            float pt = easeOutQuad(shotT / 0.18f);
            birdX = lerp(slingX, slingX - 55, pt);
            birdY = lerp(slingBaseY - 75, slingBaseY - 50, pt);
            g2.setColor(new Color(60, 40, 20));
            g2.setStroke(new BasicStroke(5));
            g2.drawLine(slingX - 18, slingBaseY - 105, (int) birdX, (int) birdY);
            g2.drawLine(slingX + 18, slingBaseY - 105, (int) birdX, (int) birdY);
        } else if (shotT < 0.55f) {
            float ft = (shotT - 0.18f) / 0.37f;
            float startX = slingX - 55, startY = slingBaseY - 50;
            birdX = lerp(startX, pigX, ft);
            float straightY = lerp(startY, pigY, ft);
            birdY = straightY - (float) Math.sin(ft * Math.PI) * 180;
        } else {
            float it = clamp((shotT - 0.55f) / 0.45f, 0, 1);
            birdX = pigX; birdY = pigY;

            g2.setColor(new Color(150, 100, 50));
            for (int i = 0; i < blocks.length; i++) {
                Rectangle b = blocks[i];
                float fall = it * (14 + i * 8);
                float rot = it * (i % 2 == 0 ? 0.35f : -0.35f);
                Graphics2D g3 = (Graphics2D) g2.create();
                g3.translate(b.x + b.width / 2f, b.y + b.height / 2f + fall);
                g3.rotate(rot);
                g3.fillRect(-b.width / 2, -b.height / 2, b.width, b.height);
                g3.dispose();
            }
            if (it > 0.05f) {
                g2.setColor(new Color(120, 190, 90));
                for (int i = 0; i < 8; i++) {
                    double ang = i * (Math.PI * 2 / 8);
                    float dist = it * 50;
                    g2.fillOval((int) (pigX + Math.cos(ang) * dist) - 5, (int) (pigY + Math.sin(ang) * dist) - 5, 10, 10);
                }
            }
            g2.setColor(new Color(200, 30, 20));
            g2.fillOval((int) birdX - 12, (int) birdY - 12, 24, 24);
        }

        if (shotT < 0.55f) {
            // pre-impact: draw intact blocks, pig, bird
            g2.setColor(new Color(150, 100, 50));
            for (Rectangle b : blocks) g2.fillRect(b.x, b.y, b.width, b.height);
            g2.setColor(new Color(120, 190, 90));
            g2.fillOval(pigX - 18, pigY - 18, 36, 36);
            g2.setColor(new Color(200, 30, 20));
            g2.fillOval((int) birdX - 14, (int) birdY - 14, 28, 28);
        }

        // "LEVEL COMPLETE" panel + 3 stars, once the shot has landed
        if (impactDone) {
            float panelT = clamp((t - 0.5f) / 0.12f, 0, 1);
            float panelScale = easeOutBack(panelT);
            int pw = 340, ph = 170;
            int pcx = W / 2, pcy = H / 2;

            Graphics2D gp = (Graphics2D) g2.create();
            gp.translate(pcx, pcy);
            gp.scale(Math.max(panelScale, 0.01f), Math.max(panelScale, 0.01f));

            gp.setColor(new Color(235, 210, 160));
            gp.fillRoundRect(-pw / 2, -ph / 2, pw, ph, 24, 24);
            gp.setColor(new Color(150, 100, 40));
            gp.setStroke(new BasicStroke(6));
            gp.drawRoundRect(-pw / 2, -ph / 2, pw, ph, 24, 24);

            gp.setColor(new Color(90, 55, 20));
            gp.setFont(new Font("SansSerif", Font.BOLD, 24));
            FontMetrics fm = gp.getFontMetrics();
            String title = "LEVEL COMPLETE!";
            gp.drawString(title, -fm.stringWidth(title) / 2, -ph / 2 + 48);

            // stars pop in one by one at fixed points along the scene timeline
            float[] starTimes = {0.58f, 0.66f, 0.74f};
            int[] starX = {-70, 0, 70};
            for (int i = 0; i < 3; i++) {
                float st = clamp((t - starTimes[i]) / 0.09f, 0, 1);
                if (st > 0f) {
                    float sScale = easeOutBack(st);
                    drawStar(gp, starX[i], 12, sScale * 1.2f, true);
                }
            }
            gp.dispose();
        }
    }

    // ========== SCENE 3 (3.4-5.0s): adult face -> flashback -> child face ==========
    private void drawScene3(Graphics2D g2, float t) {
        // background shifts from a dim, cool "screen-lit room" to a warm sepia memory tone
        float warm = clamp((t - 0.40f) / 0.40f, 0, 1);
        int br = (int) lerp(28, 235, warm), bgc = (int) lerp(30, 215, warm), bb = (int) lerp(38, 175, warm);
        g2.setColor(new Color(br, bgc, bb));
        g2.fillRect(0, 0, W, H);

        // soft glow behind the face (screen light, later becomes a warm memory glow)
        Point2D glowCenter = new Point2D.Float(W / 2f, H / 2f - 20);
        RadialGradientPaint glow = new RadialGradientPaint(glowCenter, 240,
                new float[]{0f, 1f},
                new Color[]{new Color(255, 255, 255, (int) lerp(60, 30, warm)), new Color(0, 0, 0, 0)});
        g2.setPaint(glow);
        g2.fillOval((int) (glowCenter.getX() - 240), (int) (glowCenter.getY() - 240), 480, 480);

        float adultAlpha = 1f - clamp((t - 0.40f) / 0.35f, 0, 1);
        float childAlpha = clamp((t - 0.40f) / 0.35f, 0, 1);
        float smile = clamp(t / 0.35f, 0, 1);

        if (adultAlpha > 0f) drawFace(g2, W / 2f, H / 2f + 15, 1.25f, false, smile, adultAlpha);
        if (childAlpha > 0f) drawFace(g2, W / 2f, H / 2f + 25, 1.05f, true, 0.6f + 0.4f * smile, childAlpha);

        // memory-flash rings during the crossfade
        if (t > 0.40f && t < 0.80f) {
            float rt = (t - 0.40f) / 0.40f;
            for (int i = 0; i < 2; i++) {
                float ringT = clamp(rt - i * 0.18f, 0, 1);
                if (ringT <= 0f || ringT >= 1f) continue;
                int r = (int) (ringT * 280);
                g2.setColor(new Color(255, 255, 255, (int) (120 * (1 - ringT))));
                g2.setStroke(new BasicStroke(2f));
                g2.drawOval(W / 2 - r, (int) glowCenter.getY() - r, r * 2, r * 2);
            }
        }

        // quick white flash at the very start — the "cut" from gameplay to the player's face
        if (t < 0.05f) {
            int alpha = (int) (200 * (1 - t / 0.05f));
            g2.setColor(new Color(255, 255, 255, alpha));
            g2.fillRect(0, 0, W, H);
        }
    }

    private void drawFace(Graphics2D g2, float cx, float cy, float scale, boolean isChild, float smile, float alpha) {
        Graphics2D gf = (Graphics2D) g2.create();
        gf.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, clamp(alpha, 0, 1)));
        gf.translate(cx, cy);
        gf.scale(scale, scale);

        Color skin = isChild ? new Color(255, 215, 180) : new Color(235, 190, 150);
        Color hair = isChild ? new Color(90, 55, 30) : new Color(45, 35, 30);

        // shoulders / collar
        gf.setColor(isChild ? new Color(230, 90, 70) : new Color(70, 90, 120));
        gf.fillRoundRect(-55, 55, 110, 45, 20, 20);

        // head
        int headW = isChild ? 100 : 90, headH = isChild ? 100 : 105;
        gf.setColor(skin);
        gf.fillOval(-headW / 2, -headH / 2 + 5, headW, headH);

        // hair
        if (isChild) {
            gf.setColor(hair);
            gf.fillArc(-headW / 2 - 4, -headH / 2 - 6, headW + 8, 60, 0, 180);
            gf.fillOval(-8, -headH / 2 - 12, 20, 20); // little cowlick
        } else {
            gf.setColor(hair);
            gf.fillArc(-headW / 2 - 2, -headH / 2 - 4, headW + 4, 55, 10, 165);
        }

        // eyes
        int eyeR = isChild ? 9 : 7;
        int eyeY = -8;
        int eyeDX = isChild ? 22 : 20;
        gf.setColor(Color.WHITE);
        gf.fillOval(-eyeDX - eyeR, eyeY - eyeR, eyeR * 2, eyeR * 2);
        gf.fillOval(eyeDX - eyeR, eyeY - eyeR, eyeR * 2, eyeR * 2);
        gf.setColor(new Color(40, 30, 25));
        int pupilR = isChild ? 4 : 3;
        gf.fillOval(-eyeDX - pupilR, eyeY - pupilR, pupilR * 2, pupilR * 2);
        gf.fillOval(eyeDX - pupilR, eyeY - pupilR, pupilR * 2, pupilR * 2);

        if (!isChild) {
            gf.setColor(hair);
            gf.setStroke(new BasicStroke(2.5f));
            gf.drawLine(-eyeDX - 8, eyeY - 12, -eyeDX + 8, eyeY - 14);
            gf.drawLine(eyeDX - 8, eyeY - 14, eyeDX + 8, eyeY - 12);
        }

        // mouth: curve grows with `smile`
        gf.setColor(new Color(150, 70, 55));
        gf.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        QuadCurve2D mouth = new QuadCurve2D.Float(-18, 22, 0, 22 + smile * 22, 18, 22);
        gf.draw(mouth);

        gf.dispose();
    }

    public static void main(String[] args) {
        JFrame frame = new JFrame("My Memory - 5s Animation");
        frame.add(new MyMemoryAnimation());
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setResizable(false);
        frame.setVisible(true);
    }
}
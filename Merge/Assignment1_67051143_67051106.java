package CG_Assignment1.Merge;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.util.LinkedList;
import java.util.Queue;

/**
 * ลำดับการเล่น (ตามไฟล์เก่า mergeAll.java):
 *   1. GameEnter_Scene  (0.0s - 3.2s)
 *   2. Game_Scene       (3.2s - 9.2s)
 *   3. AdultChild_Scene (9.2s - 14.0s)
 */
public class Assignment1_67051143_67051106 extends JPanel {
    private static final int W = 600, H = 600;

    private final BufferedImage buf = new BufferedImage(W, H, BufferedImage.TYPE_INT_ARGB);
    private int penRGB = Color.BLACK.getRGB();

    private static final Color TRANSPARENT = new Color(0, 0, 0, 0);
    private static final Color OUTLINE     = new Color(30, 24, 20);
    private static final int   PAD         = 2;   // ขอบเผื่อรอบสไปรต์กันเส้นขอบโดนตัด

    // ---------- Timeline (ms) ----------
    private static final int T_GE = 3200;                 // GameEnter
    private static final int T_G  = 6000;                 // Game
    private static final int T_AC = 4800;                 // AdultChild
    private static final int TOTAL = T_GE + T_G + T_AC;    // 14000

    private static final int GE_CLICK = 1900, GE_FADE = 2600;
    private static final int G_PULL = 300, G_SHOT = 800, G_HIT = 1800, G_PANEL = 2400, G_HOLD = 5200;
    private static final int A_MORPH = 1200, A_CHILD = 2800, A_END = 4200, A_FADE = 4800;

    // ---------- Monitor / Scene2 geometry ----------
    private static final int SW = 640, SH = 360;                 // ความละเอียดเชิงตรรกะของหน้าจอ
    private static final int MW = 480, MH = 270, MX = 60, MY = 120;
    private static final int DESK_Y = MY + MH + 14 + 60;

    // ---------- AdultChild framing ----------
    private static final float SCALE = 1.65f;
    private final int figX, figY, figW, figH;

    // ---------- แอสเซทที่เรนเดอร์ล่วงหน้า ----------
    private final BufferedImage adultImg, childImg, birdImg, pigImg, cursorImg;
    private final BufferedImage gameEnterImg, gameBgImg;

    private final long startTime;

    public Assignment1_67051143_67051106() {
        setPreferredSize(new Dimension(W, H));
        setBackground(Color.BLACK);

        this.figW = Math.round(240 * SCALE);
        this.figH = Math.round(440 * SCALE);
        this.figX = (W - figW) / 2;
        this.figY = 40 - Math.round(30 * SCALE);

        // เรนเดอร์สไปรต์และฉากนิ่งครั้งเดียว (อัลกอริทึมทำงานตอนนี้ ไม่ต้องทำซ้ำทุกเฟรม)
        this.adultImg = renderAdult(240, 440, true);
        this.childImg = renderChild(240, 440, true);
        this.birdImg  = renderBird(120, 120);
        this.pigImg   = renderPig(120, 120);
        this.cursorImg = renderCursor();
        this.gameEnterImg = buildGameEnterStatic();
        this.gameBgImg    = buildGameStatic();

        this.startTime = System.currentTimeMillis();
        new Timer(16, e -> repaint()).start();
    }

    public void bresenhamLine(Graphics g, int x1, int y1, int x2, int y2) {
        int dx = Math.abs(x2 - x1);
        int dy = Math.abs(y2 - y1);
        int sx = (x1 < x2) ? 1 : -1;
        int sy = (y1 < y2) ? 1 : -1;
        boolean isSwap = false;
        if (dy > dx) { int temp = dx; dx = dy; dy = temp; isSwap = true; }
        int D = 2 * dy - dx;
        int x = x1, y = y1;
        for (int i = 0; i <= dx; i++) {
            plot(g, x, y);
            if (D >= 0) {
                if (isSwap) x += sx; else y += sy;
                D -= 2 * dx;
            }
            if (isSwap) y += sy; else x += sx;
            D += 2 * dy;
        }
    }

    public Point cubicBerzierCurve(double t, Point[] controlPoints) {
        if (controlPoints == null || controlPoints.length == 0)
            throw new IllegalArgumentException("Control points cannot be null or empty");
        if (t < 0.0 || t > 1.0)
            System.err.println("t value is outside [0,1] range");
        Point p1 = controlPoints[0];
        Point p2 = controlPoints[1];
        Point p3 = controlPoints[2];
        Point p4 = controlPoints[3];
        double x = (Math.pow((1-t), 3) * p1.x) + (3 * t * Math.pow(1-t, 2) * p2.x)
                 + (3 * Math.pow(t, 2) * (1 - t) * p3.x) + (Math.pow(t, 3) * p4.x);
        double y = (Math.pow((1-t), 3) * p1.y) + (3 * t * Math.pow(1-t, 2) * p2.y)
                 + (3 * Math.pow(t, 2) * (1 - t) * p3.y) + (Math.pow(t, 3) * p4.y);
        return new Point((int) Math.round(x), (int) Math.round(y));
    }

    private void drawCubicBezier(Graphics g, int[] c, int steps) {
        Point[] cps = new Point[]{
                new Point(c[0], c[1]), new Point(c[2], c[3]),
                new Point(c[4], c[5]), new Point(c[6], c[7])
        };
        Point prev = cubicBerzierCurve(0.0, cps);
        for (int i = 1; i <= steps; i++) {
            Point cur = cubicBerzierCurve(i / (double) steps, cps);
            bresenhamLine(g, prev.x, prev.y, cur.x, cur.y);
            prev = cur;
        }
    }

    public BufferedImage floodFill(BufferedImage m, int x, int y, Color target_colour, Color replacement_Colour) {
        if (target_colour.getRGB() == replacement_Colour.getRGB()) return m;
        if (x < 0 || x >= m.getWidth() || y < 0 || y >= m.getHeight()) return m;
        if (m.getRGB(x, y) != target_colour.getRGB()) return m;
        Queue<Point> q = new LinkedList<>();
        m.setRGB(x, y, replacement_Colour.getRGB());
        q.add(new Point(x, y));
        while (!q.isEmpty()) {
            Point currentPoint = q.poll();
            int currentX = currentPoint.x;
            int currentY = currentPoint.y;
            if (currentY + 1 < m.getHeight() && m.getRGB(currentX, currentY + 1) == target_colour.getRGB()) {
                m.setRGB(currentX, currentY + 1, replacement_Colour.getRGB());
                q.add(new Point(currentX, currentY + 1));
            }
            if (currentY - 1 >= 0 && m.getRGB(currentX, currentY - 1) == target_colour.getRGB()) {
                m.setRGB(currentX, currentY - 1, replacement_Colour.getRGB());
                q.add(new Point(currentX, currentY - 1));
            }
            if (currentX + 1 < m.getWidth() && m.getRGB(currentX + 1, currentY) == target_colour.getRGB()) {
                m.setRGB(currentX + 1, currentY, replacement_Colour.getRGB());
                q.add(new Point(currentX + 1, currentY));
            }
            if (currentX - 1 >= 0 && m.getRGB(currentX - 1, currentY) == target_colour.getRGB()) {
                m.setRGB(currentX - 1, currentY, replacement_Colour.getRGB());
                q.add(new Point(currentX - 1, currentY));
            }
        }
        return m;
    }

    public void midpointCircle(Graphics g, int xc, int yc, int r) {
        int x = 0, y = r, Dx = 2 * x, Dy = 2 * y, D = 1 - r;
        while (x <= y) {
            plot(g,  x + xc,  y + yc); plot(g, -x + xc,  y + yc);
            plot(g,  x + xc, -y + yc); plot(g, -x + xc, -y + yc);
            plot(g,  y + xc,  x + yc); plot(g, -y + xc,  x + yc);
            plot(g,  y + xc, -x + yc); plot(g, -y + xc, -x + yc);
            x++; Dx += 2; D += Dx + 1;
            if (D >= 0) { y--; Dy -= 2; D -= Dy; }
        }
    }

    public void midpointEllipse(Graphics g, int xc, int yc, int a, int b) {
        if (a <= 0 || b <= 0) return;
        int a2 = a * a, b2 = b * b, twoA2 = 2 * a2, twoB2 = 2 * b2;
        int x = 0, y = b;
        int D = (int) Math.round(b2 - a2 * b + (a2 / 4.0));
        int Dx = 0, Dy = twoA2 * y;
        while (Dx <= Dy) {
            plot(g, x + xc, y + yc); plot(g, x + xc, -y + yc);
            plot(g, -x + xc, y + yc); plot(g, -x + xc, -y + yc);
            x++; Dx += twoB2; D += Dx + b2;
            if (D >= 0) { y--; Dy -= twoA2; D -= Dy; }
        }
        x = a; y = 0;
        D = (int) Math.round(a2 - b2 * a + (b2 / 4.0));
        Dx = twoB2 * x; Dy = 0;
        while (Dx >= Dy) {
            plot(g, x + xc, y + yc); plot(g, x + xc, -y + yc);
            plot(g, -x + xc, y + yc); plot(g, -x + xc, -y + yc);
            y++; Dy += twoA2; D += Dy + a2;
            if (D >= 0) { x--; Dx -= twoB2; D -= Dx; }
        }
    }

    // ===================== plot / สี / รูปทรงเติมสี =====================
    private void plot(Graphics g, int x, int y) {
        g.fillRect(x, y, 1, 1);
        if (x >= 0 && x < W && y >= 0 && y < H) buf.setRGB(x, y, penRGB);
    }

    private void useColor(Graphics g, Color c) { 
        g.setColor(c); 
        penRGB = c.getRGB(); 
    }

    private static int sgn(int v) { 
        return Integer.compare(v, 0); 
    }

    private static BufferedImage newImg(int w, int h) 
    { return new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB); }

    // วงรีทึบ วาดเส้นขอบด้วย midpointEllipse (ซ้อนหลายรอบให้ทึบ) แล้ว floodFill ภายใน
    private BufferedImage ellipseSprite(int a, int b, Color fill, boolean rim) {
        int w = 2 * (a + PAD) + 1, h = 2 * (b + PAD) + 1, cx = a + PAD, cy = b + PAD;
        BufferedImage s = newImg(w, h);
        Graphics gs = s.getGraphics();
        useColor(gs, fill);
        midpointEllipse(gs, cx, cy, a, b);
        midpointEllipse(gs, cx, cy, a - 1, b);
        midpointEllipse(gs, cx, cy, a, b - 1);
        midpointEllipse(gs, cx, cy, a - 1, b - 1);
        floodFill(s, cx, cy, TRANSPARENT, fill);
        if (rim) { useColor(gs, OUTLINE); midpointEllipse(gs, cx, cy, a, b); }
        gs.dispose();
        return s;
    }

    private void fillEllipse(Graphics2D g, int cx, int cy, int a, int b, Color fill, boolean rim) {
        if (a <= 0 || b <= 0) return;
        g.drawImage(ellipseSprite(a, b, fill, rim), cx - a - PAD, cy - b - PAD, null);
    }

    private BufferedImage circleSprite(int r, Color fill, boolean rim) {
        int w = 2 * (r + PAD) + 1, c = r + PAD;
        BufferedImage s = newImg(w, w);
        Graphics gs = s.getGraphics();
        useColor(gs, fill);
        midpointCircle(gs, c, c, r);
        midpointCircle(gs, c, c, r - 1);
        floodFill(s, c, c, TRANSPARENT, fill);
        if (rim) { useColor(gs, OUTLINE); midpointCircle(gs, c, c, r); }
        gs.dispose();
        return s;
    }

    private void fillCircle(Graphics2D g, int cx, int cy, int r, Color fill, boolean rim) {
        if (r <= 0) return;
        g.drawImage(circleSprite(r, fill, rim), cx - r - PAD, cy - r - PAD, null);
    }

    // รูปหลายเหลี่ยมทึบ วาดขอบด้วย bresenham  แล้ว floodFill
    private void fillPoly(Graphics2D g, int[] xs, int[] ys, Color fill, boolean rim) {
        int n = xs.length;
        int minx = xs[0], miny = ys[0], maxx = xs[0], maxy = ys[0];
        for (int i = 1; i < n; i++) {
            minx = Math.min(minx, xs[i]); maxx = Math.max(maxx, xs[i]);
            miny = Math.min(miny, ys[i]); maxy = Math.max(maxy, ys[i]);
        }
        int ox = minx - PAD, oy = miny - PAD;
        int w = maxx - minx + 2 * PAD + 1, h = maxy - miny + 2 * PAD + 1;
        int[] lx = new int[n], ly = new int[n];
        long sx = 0, sy = 0;
        for (int i = 0; i < n; i++) { lx[i] = xs[i] - ox; ly[i] = ys[i] - oy; sx += lx[i]; sy += ly[i]; }
        int cx = (int) (sx / n), cy = (int) (sy / n);
        BufferedImage s = newImg(w, h);
        Graphics gs = s.getGraphics();
        useColor(gs, fill);
        for (int i = 0; i < n; i++) {
            int j = (i + 1) % n;
            bresenhamLine(gs, lx[i], ly[i], lx[j], ly[j]);
            bresenhamLine(gs, lx[i] + sgn(cx - lx[i]), ly[i] + sgn(cy - ly[i]),
                              lx[j] + sgn(cx - lx[j]), ly[j] + sgn(cy - ly[j]));
        }
        floodFill(s, cx, cy, TRANSPARENT, fill);
        if (rim) {
            useColor(gs, OUTLINE);
            for (int i = 0; i < n; i++) { int j = (i + 1) % n; bresenhamLine(gs, lx[i], ly[i], lx[j], ly[j]); }
        }
        gs.dispose();
        g.drawImage(s, ox, oy, null);
    }

    // สี่เหลี่ยมทึบด้วยการสแกนไลน์ด้วย bresenham
    private void fillRectPrim(Graphics g, Color c, int x, int y, int w, int h) {
        if (w <= 0 || h <= 0) return;
        useColor(g, c);
        for (int yy = y; yy < y + h; yy++) bresenhamLine(g, x, yy, x + w - 1, yy);
    }

    private void strokeLine(Graphics g, Color c, int x1, int y1, int x2, int y2) { 
        useColor(g, c); 
        bresenhamLine(g, x1, y1, x2, y2); 
    }

    private void strokeCircle(Graphics g, Color c, int cx, int cy, int r) { 
        useColor(g, c); 
        midpointCircle(g, cx, cy, r); 
    }

    // ===================== ลูปวาดหลัก =====================
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

        long t = (System.currentTimeMillis() - startTime) % TOTAL;
        if (t < T_GE)            drawGameEnter(g2, t);
        else if (t < T_GE + T_G) drawGame(g2, t - T_GE);
        else                     drawAdultChild(g2, t - T_GE - T_G);
    }

    // ===================== SCENE 1: GameEnter =====================
    private void drawGameEnter(Graphics2D g2, long t) {
        g2.drawImage(gameEnterImg, 0, 0, null);

        boolean clicking = t >= GE_CLICK && t < GE_CLICK + 350;
        double sxs = MW / (double) SW, sys = MH / (double) SH;
        int btnX = (int) (MX + (SW / 2) * sxs);
        int btnY = (int) (MY + 178 * sys);
        int btnR = (int) (42 * sxs);

        if (clicking) {
            fillCircle(g2, btnX, btnY, btnR, new Color(255, 90, 60), true);
            fillPoly(g2, 
                new int[]{btnX - 11, btnX - 11, btnX + 16}
                , new int[]{btnY - 18, btnY + 18, btnY}
                , Color.WHITE, false);
        }
        if (t >= GE_CLICK) {
            float rt = clamp01((t - GE_CLICK) / 500f);
            int r = (int) (btnR + 6 + rt * 32);
            strokeCircle(g2, new Color(255, 255, 255, (int) (200 * (1 - rt))), btnX, btnY, r);
        }

        // เคอร์เซอร์เมาส์วิ่งไปยังปุ่ม PLAY
        float moveT = easeInOutQuad(clamp01(t / (float) GE_CLICK));
        int curX = Math.round(lerp(MX + 60, btnX - 4, moveT));
        int curY = Math.round(lerp(MY + MH - 30, btnY + 6, moveT));
        g2.drawImage(cursorImg, curX, curY, null);

        if (t < 400) fadeFromBlack(g2, 1f - clamp01(t / 400f));
        if (t > GE_FADE) fadeToBlack(g2, clamp01((t - GE_FADE) / (float) (T_GE - GE_FADE)));
    }

    // ===================== SCENE 2: Game =====================
    private void drawGame(Graphics2D g2, long t) {
        g2.drawImage(gameBgImg, 0, 0, null);

        int groundH = 80, slingX = 110, slingBaseY = H - groundH;
        int towerX = 450;
        int[][] blocks = {
                {towerX, H - groundH - 50, 35, 50},
                {towerX + 45, H - groundH - 50, 35, 50},
                {towerX, H - groundH - 95, 80, 45}
        };
        int pigX = towerX + 40, pigY = H - groundH - 130;
        Color wood = new Color(150, 100, 50);

        float birdX, birdY;
        boolean impactDone = t >= G_HIT;

        if (t < G_SHOT) {
            float pt = easeOutQuad(clamp01((t - G_PULL) / (float) (G_SHOT - G_PULL)));
            birdX = lerp(slingX, slingX - 55, pt);
            birdY = lerp(slingBaseY - 75, slingBaseY - 50, pt);
            strokeLine(g2, new Color(60, 40, 20), slingX - 18, slingBaseY - 105, (int) birdX, (int) birdY);
            strokeLine(g2, new Color(60, 40, 20), slingX + 18, slingBaseY - 105, (int) birdX, (int) birdY);
            for (int[] b : blocks) fillRectPrim(g2, wood, b[0], b[1], b[2], b[3]);
            drawSprite(g2, pigImg, pigX, pigY, 56);
            drawSprite(g2, birdImg, birdX, birdY, 48);
        } else if (t < G_HIT) {
            float ft = clamp01((t - G_SHOT) / (float) (G_HIT - G_SHOT));
            float startX = slingX - 55, startY = slingBaseY - 50;
            birdX = lerp(startX, pigX, ft);
            float straightY = lerp(startY, pigY, ft);
            birdY = straightY - (float) Math.sin(ft * Math.PI) * 180;
            for (int[] b : blocks) fillRectPrim(g2, wood, b[0], b[1], b[2], b[3]);
            drawSprite(g2, pigImg, pigX, pigY, 56);
            drawSprite(g2, birdImg, birdX, birdY, 48);
        } else {
            float it = clamp01((t - G_HIT) / 800f);
            for (int i = 0; i < blocks.length; i++) {
                int[] b = blocks[i];
                double fall = it * (14 + i * 8);
                double rot = it * (i % 2 == 0 ? 0.35 : -0.35);
                double bcx = b[0] + b[2] / 2.0, bcy = b[1] + b[3] / 2.0 + fall;
                int[] xs = new int[4], ys = new int[4];
                int[][] corner = {{-b[2] / 2, -b[3] / 2}, {b[2] / 2, -b[3] / 2}, {b[2] / 2, b[3] / 2}, {-b[2] / 2, b[3] / 2}};
                for (int k = 0; k < 4; k++) {
                    double rx = corner[k][0] * Math.cos(rot) - corner[k][1] * Math.sin(rot);
                    double ry = corner[k][0] * Math.sin(rot) + corner[k][1] * Math.cos(rot);
                    xs[k] = (int) Math.round(bcx + rx);
                    ys[k] = (int) Math.round(bcy + ry);
                }
                fillPoly(g2, xs, ys, wood, false);
            }
            if (it > 0.05f && it < 1.0f) {
                for (int i = 0; i < 8; i++) {
                    double ang = i * (Math.PI * 2 / 8);
                    double dist = it * 50;
                    fillCircle(g2, (int) (pigX + Math.cos(ang) * dist), (int) (pigY + Math.sin(ang) * dist), 5, new Color(120, 190, 90), false);
                }
            }
            drawSprite(g2, birdImg, pigX, pigY, 48);
        }

        if (impactDone) drawWinPanel(g2, t);

        if (t < 400) fadeFromBlack(g2, 1f - clamp01(t / 400f));
        if (t > G_HOLD) fadeToBlack(g2, clamp01((t - G_HOLD) / (float) (T_G - G_HOLD)));
    }

    private void drawWinPanel(Graphics2D g2, long t) {
        float panelT = clamp01((t - G_PANEL) / 400f);
        float sc = easeOutBack(panelT);
        if (sc <= 0.02f) return;
        int pw = (int) (340 * sc), ph = (int) (170 * sc);
        int px = W / 2 - pw / 2, py = H / 2 - ph / 2;
        fillRectPrim(g2, new Color(235, 210, 160), px, py, pw, ph);
        // ขอบป้าย (4 เส้น bresenham)
        Color brd = new Color(150, 100, 40);
        strokeLine(g2, brd, px, py, px + pw, py);
        strokeLine(g2, brd, px, py + ph, px + pw, py + ph);
        strokeLine(g2, brd, px, py, px, py + ph);
        strokeLine(g2, brd, px + pw, py, px + pw, py + ph);

        // ตัวอักษร
        if (panelT > 0.6f) {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(new Color(90, 55, 20));
            g2.setFont(new Font("SansSerif", Font.BOLD, 24));
            FontMetrics fm = g2.getFontMetrics();
            String title = "LEVEL COMPLETE!";
            g2.drawString(title, W / 2 - fm.stringWidth(title) / 2, py + 46);
        }

        // ดาว 3 ดวง (fillPoly + floodFill)
        int[] delays = {G_PANEL + 300, G_PANEL + 650, G_PANEL + 1000};
        int[] starX = {W / 2 - 70, W / 2, W / 2 + 70};
        int starY = H / 2 + 20;
        for (int i = 0; i < 3; i++) {
            if (t >= delays[i]) {
                float st = easeOutBack(clamp01((t - delays[i]) / 350f));
                drawStar(g2, starX[i], starY, st * 1.25f);
            }
        }
    }

    private void drawStar(Graphics2D g2, int cx, int cy, float scale) {
        if (scale <= 0.05f) return;
        int outerR = (int) (18 * scale), innerR = (int) (7 * scale);
        if (outerR < 2) return;
        int[] xs = new int[10], ys = new int[10];
        double ang = -Math.PI / 2, step = Math.PI / 5;
        for (int i = 0; i < 10; i++) {
            int r = (i % 2 == 0) ? outerR : innerR;
            xs[i] = (int) (cx + Math.cos(ang) * r);
            ys[i] = (int) (cy + Math.sin(ang) * r);
            ang += step;
        }
        fillPoly(g2, xs, ys, new Color(255, 205, 40), true);
    }

    // ===================== SCENE 3: AdultChild =====================
    private void drawAdultChild(Graphics2D g2, long t) {
        float adultAlpha, childAlpha, morph;
        if (t < A_MORPH) { 
            adultAlpha = 1f; 
            childAlpha = 0f; 
            morph = 0f; 
        }
        else if (t < A_CHILD) { 
            morph = (t - A_MORPH) / (float) (A_CHILD - A_MORPH); 
            adultAlpha = 1f - morph; 
            childAlpha = morph; 
        }
        else {
            adultAlpha = 0f; 
            childAlpha = 1f; 
            morph = 1f; 
        }
        float smile = clamp01(t / 900f);
        float warm = morph;

        // พื้นหลัง (compositing เต็มเฟรม) + แสงฟุ้ง (gradient)
        int br = (int) lerp(40, 235, warm), bgc = (int) lerp(44, 215, warm), bb = (int) lerp(62, 175, warm);
        g2.setColor(new Color(br, bgc, bb));
        g2.fillRect(0, 0, W, H);
        int gcx = W / 2, gcy = figY + Math.round(110 * SCALE);
        RadialGradientPaint glow = new RadialGradientPaint(new Point2D.Float(gcx, gcy), 240,
                new float[]{0f, 1f},
                new Color[]{new Color(255, 255, 255,
                (int) lerp(70, 40, warm)), 
                new Color(0, 0, 0, 0)}
            );
        g2.setPaint(glow);
        g2.fillOval(gcx - 240, gcy - 240, 480, 480);

        int amx = figX + Math.round(120 * SCALE), amy = figY + Math.round(136 * SCALE);
        int cmx = figX + Math.round(121 * SCALE), cmy = figY + Math.round(176 * SCALE);
        drawFigure(g2, adultImg, adultAlpha);
        drawSmile(g2, amx, amy, Math.round(8 * SCALE), smile, adultAlpha, new Color(190, 105, 90));
        drawFigure(g2, childImg, childAlpha);
        drawSmile(g2, cmx, cmy, Math.round(10 * SCALE), 0.6f + 0.4f * smile, childAlpha, new Color(90, 35, 30));

        if (morph > 0f && morph < 1f) {
            float flash = (float) Math.sin(morph * Math.PI);
            g2.setColor(new Color(255, 255, 255, (int) (200 * flash)));
            g2.fillRect(0, 0, W, H);
            for (int i = 0; i < 3; i++) {
                float rt = clamp01(morph - i * 0.18f);
                if (rt <= 0f || rt >= 1f) continue;
                int r = (int) (rt * 280);
                if (r < 3) continue;
                int a = (int) (150 * (1 - rt));
                strokeCircle(g2, new Color(255, 255, 255, a), gcx, gcy, r);
                strokeCircle(g2, new Color(255, 255, 255, a), gcx, gcy, r - 1);
            }
        }
        if (t > A_END) fadeToBlack(g2, clamp01((t - A_END) / (float) (A_FADE - A_END)));
    }

    private void drawFigure(Graphics2D g2, BufferedImage img, float alpha) {
        if (alpha <= 0.001f) return;
        Graphics2D gf = (Graphics2D) g2.create();
        gf.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, clamp01(alpha)));
        gf.drawImage(img, figX, figY, figW, figH, null);
        gf.dispose();
    }

    // รอยยิ้ม: วาดเส้นโค้ง Bezier ลงเลเยอร์เล็ก แล้ว composite ตามค่า alpha ของตัวละคร
    private void drawSmile(Graphics2D g2, int cx, int cy, int hw, float smile, float alpha, Color col) {
        if (alpha <= 0.02f) return;
        int depth = Math.max(1, Math.round(smile * hw * 1.1f));
        int pad = depth + 4;
        int w = 2 * hw + 2 * pad + 1, h = depth + 2 * pad + 1;
        int ox = cx - hw - pad, oy = cy - pad;
        int lx = hw + pad, ly = pad;
        BufferedImage s = newImg(w, h);
        Graphics gs = s.getGraphics();
        useColor(gs, col);
        int d = (int) (depth * 1.4);
        for (int o = 0; o < 3; o++) { 
            drawCubicBezier(gs, new int[]{lx - hw, ly + o, lx - hw / 2, ly + d + o, lx + hw / 2, ly + d + o, lx + hw, ly + o}, 24);
        }
        gs.dispose();
        Graphics2D gf = (Graphics2D) g2.create();
        gf.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, clamp01(alpha)));
        gf.drawImage(s, ox, oy, null);
        gf.dispose();
    }

    private void drawSprite(Graphics2D g2, BufferedImage img, float cx, float cy, int size) {
        g2.drawImage(
            img, 
            Math.round(cx - size / 2f), 
            Math.round(cy - size / 2f), 
            size, 
            size, null);
    }

    // ===================== ฉากนิ่งที่เรนเดอร์ครั้งเดียว =====================
    private BufferedImage buildGameEnterStatic() {
        BufferedImage img = newImg(W, H);
        Graphics2D g = (Graphics2D) img.getGraphics();
        // ห้อง + โต๊ะ (fill ด้วยสแกนไลน์ bresenham)
        fillRectPrim(g, new Color(30, 30, 36), 0, 0, W, H);
        fillRectPrim(g, new Color(60, 45, 35), 0, DESK_Y, W, H - DESK_Y);
        fillRectPrim(g, new Color(45, 32, 24), 0, DESK_Y, W, 6);
        // ตัวจอ + ขาตั้ง + ฐาน
        fillRectPrim(g, new Color(35, 35, 38), MX - 14, MY - 14, MW + 28, MH + 28);
        fillRectPrim(g, new Color(35, 35, 38), W / 2 - 20, MY + MH + 14, 40, DESK_Y - (MY + MH + 14));
        fillEllipse(g, W / 2, DESK_Y - 6, 70, 8, new Color(35, 35, 38), false);
        // เนื้อหาเมนูในจอ
        BufferedImage menu = buildMenuScreen();
        g.drawImage(menu, MX, MY, MW, MH, null);
        g.dispose();
        return img;
    }

    private BufferedImage buildMenuScreen() {
        BufferedImage img = newImg(SW, SH);
        Graphics2D g = (Graphics2D) img.getGraphics();
        // ท้องฟ้าไล่เฉด
        g.setPaint(new GradientPaint(0, 0, new Color(140, 200, 225), 0, SH, new Color(205, 234, 238)));
        g.fillRect(0, 0, SW, SH);
        // พุ่มไม้ไกล
        for (int bx = 40; bx < SW; bx += 150) fillEllipse(g, bx + 45, 285, 45, 35, new Color(170, 205, 216), false);
        // พื้นหญ้า + ยอดหญ้า + ดิน
        int grassY = 300;
        fillRectPrim(g, new Color(120, 185, 70), 0, grassY, SW, 26);
        for (int gx = 0; gx < SW; gx += 16)
            fillPoly(g, new int[]{gx, gx + 5, gx + 10}, new int[]{grassY + 2, grassY - 8, grassY + 2}, new Color(95, 160, 55), false);
        fillRectPrim(g, new Color(45, 52, 72), 0, grassY + 26, SW, SH - grassY - 26);
        for (int px = 30; px < SW; px += 70) fillEllipse(g, px + 7, grassY + 38, 7, 4, new Color(70, 78, 100), false);
        // ตัวละครตกแต่ง
        menuRedBird(g, 180, 60);
        menuYellowBird(g, 560, 175);
        menuPig(g, 500, 300, false);
        menuPig(g, 600, 298, true);
        // โลโก้ (Java2D text)
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setFont(new Font("SansSerif", Font.BOLD, 48));
        String logo = "ANGRY BIRDS";
        FontMetrics fm = g.getFontMetrics();
        int lx = (SW - fm.stringWidth(logo)) / 2, ly = 100;
        g.setColor(new Color(70, 100, 150));
        for (int dx = -2; dx <= 2; dx++) for (int dy = -2; dy <= 2; dy++) if (dx != 0 || dy != 0) g.drawString(logo, lx + dx, ly + dy);
        g.setColor(Color.WHITE);
        g.drawString(logo, lx, ly);
        // ปุ่ม PLAY (วงกลม + สามเหลี่ยม)
        int bcx = SW / 2, bcy = 178, r = 42;
        fillCircle(g, bcx, bcy, r, new Color(210, 40, 30), true);
        strokeCircle(g, new Color(150, 22, 16), bcx, bcy, r - 1);
        fillPoly(g, new int[]{bcx - 14, bcx - 14, bcx + 22}, new int[]{bcy - 24, bcy + 24, bcy}, Color.WHITE, false);
        g.dispose();
        return img;
    }

    private void menuRedBird(Graphics2D g, int cx, int cy) {
        fillPoly(g, new int[]{cx - 10, cx + 2, cx - 10}, new int[]{cy - 24, cy - 26, cy - 14}, new Color(220, 30, 20), false);
        fillPoly(g, new int[]{cx + 12, cx, cx + 12}, new int[]{cy - 24, cy - 26, cy - 14}, new Color(220, 30, 20), false);
        fillCircle(g, cx, cy, 22, new Color(220, 30, 20), true);
        fillCircle(g, cx - 4, cy - 5, 6, Color.WHITE, false);
        fillCircle(g, cx + 4, cy - 5, 6, Color.WHITE, false);
        fillCircle(g, cx - 3, cy - 4, 2, Color.BLACK, false);
        fillCircle(g, cx + 5, cy - 4, 2, Color.BLACK, false);
        fillPoly(g, new int[]{cx + 2, cx + 20, cx + 2}, new int[]{cy, cy + 4, cy + 9}, new Color(245, 180, 30), true);
    }

    private void menuYellowBird(Graphics2D g, int cx, int cy) {
        fillPoly(g, new int[]{cx - 18, cx + 16, cx - 8}, new int[]{cy - 14, cy, cy + 16}, new Color(250, 210, 40), true);
        fillCircle(g, cx - 6, cy - 4, 5, Color.WHITE, false);
        fillCircle(g, cx + 2, cy - 4, 5, Color.WHITE, false);
        fillCircle(g, cx - 5, cy - 3, 2, Color.BLACK, false);
        fillCircle(g, cx + 3, cy - 3, 2, Color.BLACK, false);
        fillPoly(g, new int[]{cx + 2, cx + 14, cx + 2}, new int[]{cy - 2, cy + 2, cy + 6}, new Color(230, 140, 20), false);
    }

    private void menuPig(Graphics2D g, int cx, int cy, boolean king) {
        fillCircle(g, cx - 14, cy - 16, 6, new Color(120, 200, 80), true);
        fillCircle(g, cx + 14, cy - 16, 6, new Color(120, 200, 80), true);
        fillCircle(g, cx, cy, 20, new Color(120, 200, 80), true);
        fillCircle(g, cx - 8, cy - 4, 4, Color.WHITE, false);
        fillCircle(g, cx + 8, cy - 4, 4, Color.WHITE, false);
        fillCircle(g, cx - 7, cy - 3, 2, Color.BLACK, false);
        fillCircle(g, cx + 9, cy - 3, 2, Color.BLACK, false);
        fillEllipse(g, cx, cy + 8, 8, 6, new Color(150, 220, 110), true);
        fillEllipse(g, cx - 3, cy + 8, 1, 3, new Color(40, 90, 30), false);
        fillEllipse(g, cx + 3, cy + 8, 1, 3, new Color(40, 90, 30), false);
        if (king)
            fillPoly(g, new int[]{cx - 12, cx - 12, cx - 6, cx, cx + 6, cx + 12, cx + 12},
                    new int[]{cy - 22, cy - 34, cy - 26, cy - 36, cy - 26, cy - 34, cy - 22}, new Color(245, 205, 40), true);
    }

    private BufferedImage buildGameStatic() {
        BufferedImage img = newImg(W, H);
        Graphics2D g = (Graphics2D) img.getGraphics();
        g.setPaint(new GradientPaint(0, 0, new Color(140, 190, 230), 0, H, new Color(210, 230, 240)));
        g.fillRect(0, 0, W, H);
        int groundH = 80, slingX = 110, slingBaseY = H - groundH;
        fillRectPrim(g, new Color(120, 160, 90), 0, H - groundH, W, groundH);
        // เสาหนังสติ๊ก
        fillRectPrim(g, new Color(90, 60, 30), slingX - 22, slingBaseY - 110, 8, 110);
        fillRectPrim(g, new Color(90, 60, 30), slingX + 14, slingBaseY - 110, 8, 110);
        g.dispose();
        return img;
    }

    private BufferedImage renderCursor() {
        int[] xs = {0, 0, 4, 7, 10, 7, 12};
        int[] ys = {0, 16, 12, 19, 17, 11, 11};
        BufferedImage img = newImg(20, 24);
        Graphics2D g = (Graphics2D) img.getGraphics();
        fillPoly(g, xs, ys, Color.WHITE, true);
        g.dispose();
        return img;
    }

    // ===================== สไปรต์ตัวละคร (วาดด้วยอัลกอริทึมที่แปะไว้) =====================
    private BufferedImage renderAdult(int w, int h, boolean clean) {
        boolean R = !clean;
        BufferedImage out = newImg(w, h);
        Graphics2D g = (Graphics2D) out.getGraphics();
        Color skin = new Color(120, 72, 45), skinSh = new Color(104, 60, 36), hair = new Color(55, 35, 22);
        Color beard = new Color(46, 30, 20), shirt = new Color(120, 170, 210), jeans = new Color(30, 45, 75);
        Color shoe = new Color(70, 45, 30), white = new Color(245, 245, 245), dark = new Color(35, 28, 24), mouth = new Color(120, 50, 45);

        fillEllipse(g, 100, 420, 26, 12, shoe, R);
        fillEllipse(g, 148, 420, 26, 12, shoe, R);
        fillPoly(g, new int[]{84, 156, 150, 90}, new int[]{268, 268, 414, 414}, jeans, R);
        if (R) strokeLine(g, OUTLINE, 120, 330, 120, 414);
        fillPoly(g, new int[]{74, 166, 160, 80}, new int[]{168, 168, 272, 272}, shirt, R);
        fillPoly(g, new int[]{74, 96, 86, 64}, new int[]{168, 170, 258, 250}, shirt, R);
        fillPoly(g, new int[]{166, 144, 154, 176}, new int[]{168, 170, 258, 250}, shirt, R);
        fillEllipse(g, 120, 260, 28, 15, skin, R);
        fillPoly(g, new int[]{110, 130, 130, 110}, new int[]{150, 150, 170, 170}, skinSh, false);

        fillEllipse(g, 120, 86, 52, 56, hair, false);
        fillEllipse(g, 120, 98, 45, 50, skin, R);
        fillCircle(g, 73, 104, 9, skin, R);
        fillCircle(g, 167, 104, 9, skin, R);
        for (int[] c : new int[][]{{86, 44, 13}, {104, 36, 14}, {122, 34, 14}, {140, 36, 14}, {158, 44, 13}})
            fillCircle(g, c[0], c[1], c[2], hair, false);
        fillEllipse(g, 120, 126, 34, 24, beard, false);
        fillCircle(g, 102, 96, 8, white, R);
        fillCircle(g, 138, 96, 8, white, R);
        fillCircle(g, 102, 96, 4, dark, false);
        fillCircle(g, 138, 96, 4, dark, false);
        strokeLine(g, OUTLINE, 90, 86, 112, 84);
        strokeLine(g, OUTLINE, 128, 84, 150, 86);
        strokeLine(g, OUTLINE, 120, 104, 116, 116);
        strokeLine(g, OUTLINE, 116, 116, 124, 116);
        if (!clean) {
            BufferedImage s = newImg(w, h);
            Graphics gs = s.getGraphics();
            useColor(gs, mouth);
            drawCubicBezier(gs, new int[]{104, 130, 112, 125, 128, 125, 136, 130}, 26);
            drawCubicBezier(gs, new int[]{136, 130, 128, 144, 112, 144, 104, 130}, 26);
            floodFill(s, 120, 135, TRANSPARENT, mouth);
            gs.dispose();
            g.drawImage(s, 0, 0, null);
            fillPoly(g, new int[]{111, 129, 127, 113}, new int[]{131, 131, 137, 137}, white, false);
            BufferedImage r2 = newImg(w, h);
            Graphics gr = r2.getGraphics();
            useColor(gr, OUTLINE);
            drawCubicBezier(gr, new int[]{104, 130, 112, 125, 128, 125, 136, 130}, 26);
            drawCubicBezier(gr, new int[]{136, 130, 128, 144, 112, 144, 104, 130}, 26);
            gr.dispose();
            g.drawImage(r2, 0, 0, null);
        }
        g.dispose();
        return out;
    }

    private BufferedImage renderChild(int w, int h, boolean clean) {
        boolean R = !clean;
        BufferedImage out = newImg(w, h);
        Graphics2D g = (Graphics2D) out.getGraphics();
        Color skin = new Color(140, 88, 58), skinSh = new Color(122, 74, 48), hair = new Color(55, 35, 22);
        Color tee = new Color(245, 205, 60), jeans = new Color(70, 110, 175), cuff = new Color(120, 160, 205);
        Color red = new Color(200, 60, 50), white = new Color(245, 245, 245), dark = new Color(35, 28, 24), mouth = new Color(120, 50, 45);

        fillEllipse(g, 100, 414, 26, 13, red, R);
        fillEllipse(g, 148, 414, 26, 13, red, R);
        fillEllipse(g, 84, 416, 11, 8, white, R);
        fillEllipse(g, 164, 416, 11, 8, white, R);
        fillPoly(g, new int[]{84, 156, 150, 90}, new int[]{286, 286, 404, 404}, jeans, R);
        if (R) strokeLine(g, OUTLINE, 120, 340, 120, 404);
        fillPoly(g, new int[]{88, 118, 116, 90}, new int[]{392, 392, 406, 406}, cuff, R);
        fillPoly(g, new int[]{124, 152, 150, 122}, new int[]{392, 392, 406, 406}, cuff, R);
        fillPoly(g, new int[]{110, 130, 130, 110}, new int[]{194, 194, 210, 210}, skinSh, false);
        fillPoly(g, new int[]{76, 164, 158, 82}, new int[]{206, 206, 290, 290}, tee, R);
        fillPoly(g, new int[]{76, 102, 94, 66}, new int[]{206, 208, 248, 244}, tee, R);
        fillPoly(g, new int[]{164, 138, 146, 174}, new int[]{206, 208, 248, 244}, tee, R);
        fillPoly(g, new int[]{70, 90, 84, 66}, new int[]{244, 246, 300, 296}, skin, R);
        fillPoly(g, new int[]{170, 150, 156, 174}, new int[]{244, 246, 300, 296}, skin, R);
        fillCircle(g, 75, 304, 11, skin, R);
        fillCircle(g, 165, 304, 11, skin, R);

        fillEllipse(g, 120, 140, 50, 50, hair, false);
        fillEllipse(g, 120, 152, 43, 46, skin, R);
        fillCircle(g, 79, 158, 9, skin, R);
        fillCircle(g, 161, 158, 9, skin, R);
        for (int[] c : new int[][]{{88, 100, 12}, {106, 94, 13}, {124, 92, 13}, {142, 94, 13}, {158, 100, 12}})
            fillCircle(g, c[0], c[1], c[2], hair, false);
        fillCircle(g, 104, 150, 8, white, R);
        fillCircle(g, 138, 150, 8, white, R);
        fillCircle(g, 104, 150, 4, dark, false);
        fillCircle(g, 138, 150, 4, dark, false);
        strokeLine(g, OUTLINE, 94, 138, 112, 136);
        strokeLine(g, OUTLINE, 130, 136, 148, 138);
        if (!clean) {
            BufferedImage s = newImg(w, h);
            Graphics gs = s.getGraphics();
            useColor(gs, mouth);
            drawCubicBezier(gs, new int[]{104, 170, 112, 166, 130, 166, 138, 170}, 26);
            drawCubicBezier(gs, new int[]{138, 170, 130, 188, 112, 188, 104, 170}, 26);
            floodFill(s, 121, 178, TRANSPARENT, mouth);
            gs.dispose();
            g.drawImage(s, 0, 0, null);
            fillPoly(g, new int[]{110, 132, 130, 108}, new int[]{171, 171, 178, 178}, white, false);
            BufferedImage r2 = newImg(w, h);
            Graphics gr = r2.getGraphics();
            useColor(gr, OUTLINE);
            drawCubicBezier(gr, new int[]{104, 170, 112, 166, 130, 166, 138, 170}, 26);
            drawCubicBezier(gr, new int[]{138, 170, 130, 188, 112, 188, 104, 170}, 26);
            gr.dispose();
            g.drawImage(r2, 0, 0, null);
        }
        g.dispose();
        return out;
    }

    private BufferedImage renderBird(int w, int h) {
        BufferedImage out = newImg(w, h);
        Graphics2D g = (Graphics2D) out.getGraphics();
        Color body = new Color(220, 30, 20), belly = new Color(232, 182, 140), beak = new Color(245, 180, 30);
        Color beakD = new Color(210, 140, 20), white = new Color(245, 245, 245), dark = new Color(25, 20, 18), spot = new Color(150, 22, 16);

        fillPoly(g, new int[]{40, 52, 58}, new int[]{22, 4, 26}, body, true);
        fillPoly(g, new int[]{58, 72, 66}, new int[]{22, 10, 28}, body, true);
        fillPoly(g, new int[]{16, 6, 20}, new int[]{58, 66, 70}, body, true);
        fillPoly(g, new int[]{18, 8, 22}, new int[]{72, 80, 82}, body, true);
        fillCircle(g, 56, 64, 46, body, true);
        fillEllipse(g, 56, 96, 26, 13, belly, false);
        fillEllipse(g, 34, 84, 6, 5, spot, false);
        fillEllipse(g, 28, 72, 4, 4, spot, false);
        fillEllipse(g, 48, 52, 10, 14, white, true);
        fillEllipse(g, 68, 52, 10, 14, white, true);
        fillCircle(g, 52, 54, 4, dark, false);
        fillCircle(g, 64, 54, 4, dark, false);
        fillPoly(g, new int[]{34, 52, 54, 36}, new int[]{34, 46, 52, 40}, dark, false);
        fillPoly(g, new int[]{82, 64, 62, 80}, new int[]{34, 46, 52, 40}, dark, false);
        fillPoly(g, new int[]{56, 92, 58}, new int[]{62, 70, 72}, beak, true);
        fillPoly(g, new int[]{56, 88, 58}, new int[]{74, 80, 84}, beakD, true);
        g.dispose();
        return out;
    }

    private BufferedImage renderPig(int w, int h) {
        BufferedImage out = newImg(w, h);
        Graphics2D g = (Graphics2D) out.getGraphics();
        Color body = new Color(120, 200, 80), snout = new Color(150, 220, 110), dgreen = new Color(70, 140, 50);
        Color white = new Color(245, 245, 245), dark = new Color(30, 40, 25);

        fillCircle(g, 44, 24, 10, body, true);
        fillCircle(g, 76, 24, 10, body, true);
        fillCircle(g, 60, 66, 44, body, true);
        fillCircle(g, 46, 54, 11, white, true);
        fillCircle(g, 74, 54, 11, white, true);
        fillCircle(g, 48, 56, 4, dark, false);
        fillCircle(g, 72, 56, 4, dark, false);
        strokeLine(g, dgreen, 36, 40, 54, 44);
        strokeLine(g, dgreen, 84, 40, 66, 44);
        strokeLine(g, dgreen, 50, 92, 70, 92);
        fillEllipse(g, 60, 74, 18, 14, snout, true);
        fillEllipse(g, 53, 74, 2, 4, dark, false);
        fillEllipse(g, 67, 74, 2, 4, dark, false);
        g.dispose();
        return out;
    }

    // ===================== compositing helpers (Java2D) =====================
    private void fadeToBlack(Graphics2D g2, float a)   { 
        g2.setColor(new Color(0, 0, 0, (int) (255 * clamp01(a)))); 
        g2.fillRect(0, 0, W, H); 
    }
    private void fadeFromBlack(Graphics2D g2, float a) { 
        g2.setColor(new Color(0, 0, 0, (int) (255 * clamp01(a)))); 
        g2.fillRect(0, 0, W, H); 
    }

    // ===================== math / easing =====================
    private static float clamp(float v, float lo, float hi) { return Math.max(lo, Math.min(hi, v)); }
    private static float clamp01(float v) { return clamp(v, 0f, 1f); }
    private static float lerp(float a, float b, float t) { return a + (b - a) * t; }
    private static float easeInOutQuad(float t) { return t < 0.5f ? 2 * t * t : 1 - (float) Math.pow(-2 * t + 2, 2) / 2; }
    private static float easeOutQuad(float t) { return 1 - (1 - t) * (1 - t); }
    private static float easeOutBack(float t) {
        float c1 = 1.70158f, c3 = c1 + 1f, x = t - 1f;
        return 1 + c3 * x * x * x + c1 * x * x;
    }

    class Point {
        public int x, y;
        public Point(int x, int y) { this.x = x; this.y = y; }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Angry Birds - Memory Animation (mergeAll2)");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.add(new Assignment1_67051143_67051106());
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setResizable(false);
            frame.setVisible(true);
        });
    }
}

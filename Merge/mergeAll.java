package CG_Assignment1.Merge;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.util.LinkedList;
import java.util.Queue;

/**
 * ลำดับการเล่น (Sequence):
 *   1. AdultChild_Scene (0.0s - 4.8s): ผู้ใหญ่ยิ้ม -> Flashback / Morph กลายเป็นเด็ก พร้อมวงแหวนความทรงจำ
 *   2. GameEnter_Scene  (4.8s - 8.0s): เมนู Angry Birds 2013 บนหน้าจอ Monitor -> เลื่อนเคอร์เซอร์เมาส์ไปคลิกปุ่ม PLAY -> Fade out
 *   3. Game_Scene       (8.0s - 14.0s): หนังสติ๊กยิงนกวิถีพาราโบลา -> ทำลายหอคอยบล็อกไม้และหมู -> ป้าย LEVEL COMPLETE + ดาว 3 ดวง
 *
 * วนลูปการทำงานอัตโนมัติอย่างต่อเนื่อง
 */
public class mergeAll extends JPanel {
    private static final int W = 600, H = 600;

    // แอสเซทโมเดลรูปคนสำหรับ Scene 1
    private static final int FW = 240, FH = 440;
    private static final float SCALE = 1.65f;
    private final int figX, figY, figW, figH;
    private final BufferedImage adultImg, childImg;

    // แอสเซทสไปรต์ (Sprite) สำหรับ Scene 3
    private final BufferedImage birdImg, pigImg;

    private final long startTime;

    // มาร์กเกอร์ช่วงเวลาของ Timeline (หน่วยเป็นมิลลิวินาที / ms)
    private static final int S1_START = 0;
    private static final int S1_MORPH = 1200;
    private static final int S1_CHILD = 2800;
    private static final int S1_END   = 4200;
    private static final int S1_FADE  = 4800;

    private static final int S2_START = 4800;
    private static final int S2_CLICK = 6700;
    private static final int S2_FADE  = 7400;
    private static final int S2_END   = 8000;

    private static final int S3_START = 8000;
    private static final int S3_PULL  = 8300;
    private static final int S3_SHOT  = 8800;
    private static final int S3_HIT   = 9800;
    private static final int S3_PANEL = 10400;
    private static final int S3_HOLD  = 13200;
    private static final int TOTAL_CYCLE = 14000;

    // ขนาดความละเอียดเชิงตรรกะสำหรับหน้าจอ Monitor ใน Scene 2
    private static final int SW = 640, SH = 360;

    public mergeAll() {
        setPreferredSize(new Dimension(W, H));
        setBackground(Color.BLACK);

        // เรนเดอร์แอสเซทสไปรต์ล่วงหน้าด้วยอัลกอริทึมภายในคลาส (ทำงานได้ด้วยตัวเอง 100%)
        this.adultImg = renderAdult(FW, FH, true);
        this.childImg = renderChild(FW, FH, true);
        this.birdImg  = renderBird(120, 120);
        this.pigImg   = renderPig(120, 120);

        this.figW = Math.round(FW * SCALE);
        this.figH = Math.round(FH * SCALE);
        this.figX = (W - figW) / 2;
        this.figY = 40 - Math.round(30 * SCALE);

        this.startTime = System.currentTimeMillis();
        new Timer(16, e -> repaint()).start(); // ~60fps
    }

    // ===================== ฟังก์ชันคำนวณและ Easing Helpers =====================
    private static float clamp(float v, float lo, float hi) { return Math.max(lo, Math.min(hi, v)); }
    private static float clamp01(float v) { return clamp(v, 0f, 1f); }
    private static float lerp(float a, float b, float t) { return a + (b - a) * t; }
    private static float easeInOutQuad(float t) { return t < 0.5f ? 2 * t * t : 1 - (float) Math.pow(-2 * t + 2, 2) / 2; }
    private static float easeOutQuad(float t) { return 1 - (1 - t) * (1 - t); }
    private static float easeOutBack(float t) {
        float c1 = 1.70158f, c3 = c1 + 1f;
        float x = t - 1f;
        return 1 + c3 * x * x * x + c1 * x * x;
    }

    private static Polygon buildStar(int cx, int cy, int outerR, int innerR) {
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

    private static void drawStar(Graphics2D g2, int cx, int cy, float scale, boolean filled) {
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

    private static Polygon buildCursor(float x, float y, float scale) {
        int[][] pts = {{0, 0}, {0, 16}, {4, 12}, {7, 19}, {10, 17}, {7, 11}, {12, 11}};
        Polygon p = new Polygon();
        for (int[] pt : pts) p.addPoint((int) (x + pt[0] * scale), (int) (y + pt[1] * scale));
        return p;
    }

    private static void drawSprite(Graphics2D g2, BufferedImage img, float cx, float cy, int size) {
        g2.drawImage(img, Math.round(cx - size / 2f), Math.round(cy - size / 2f), size, size, null);
    }

    // ===================== ลูปการวาดหลัก (Main Paint Loop) =====================
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

        long elapsed = System.currentTimeMillis() - startTime;
        long t = elapsed % TOTAL_CYCLE;

        if (t < S1_FADE) {
            drawScene1(g2, t);
        } else if (t < S2_END) {
            drawScene2(g2, t);
        } else {
            drawScene3(g2, t);
        }
    }

    // =========================================================================
    // SCENE 1: AdultChild_Scene (0.0s - 4.8s)
    // ผู้ใหญ่ยิ้ม -> แสงวาบสีขาว & วงแหวน Midpoint Ellipse -> Morph กลายเป็นเด็ก
    // =========================================================================
    private void drawScene1(Graphics2D g2, long t) {
        float adultAlpha, childAlpha, morph;
        if (t < S1_MORPH) {
            adultAlpha = 1f; childAlpha = 0f; morph = 0f;
        } else if (t < S1_CHILD) {
            morph = (t - S1_MORPH) / (float) (S1_CHILD - S1_MORPH);
            adultAlpha = 1f - morph;
            childAlpha = morph;
        } else {
            adultAlpha = 0f; childAlpha = 1f; morph = 1f;
        }

        float smile = clamp01(t / 900f);

        // พื้นหลัง: โทนเย็นสลัว -> เปลี่ยนเป็นโทนอุ่นซีเปียแห่งความทรงจำ (Warm sepia memory tone)
        float warm = morph;
        int br = (int) lerp(40, 235, warm), bgc = (int) lerp(44, 215, warm), bb = (int) lerp(62, 175, warm);
        g2.setColor(new Color(br, bgc, bb));
        g2.fillRect(0, 0, W, H);

        // แสงฟุ้งรอบใบหน้า (Radial Glow)
        int gcx = W / 2, gcy = figY + Math.round(110 * SCALE);
        RadialGradientPaint glow = new RadialGradientPaint(new Point2D.Float(gcx, gcy), 240,
                new float[]{0f, 1f},
                new Color[]{new Color(255, 255, 255, (int) lerp(70, 40, warm)), new Color(0, 0, 0, 0)});
        g2.setPaint(glow);
        g2.fillOval(gcx - 240, gcy - 240, 480, 480);

        // วาดตัวละคร + เลเยอร์รอยยิ้มแบบแอนิเมชัน (Morph / Cross-fade)
        float amx = figX + 120 * SCALE, amy = figY + 136 * SCALE;
        float cmx = figX + 121 * SCALE, cmy = figY + 176 * SCALE;
        drawFigure(g2, adultImg, adultAlpha);
        drawSmile(g2, amx, amy, 15 * SCALE, smile, adultAlpha, new Color(190, 105, 90));
        drawFigure(g2, childImg, childAlpha);
        drawSmile(g2, cmx, cmy, 17 * SCALE, 0.6f + 0.4f * smile, childAlpha, new Color(90, 35, 30));

        // แสงวาบสีขาว (White flash) & วงแหวนคลื่นความทรงจำ (วาดด้วยอัลกอริทึม Midpoint Ellipse)
        if (morph > 0f && morph < 1f) {
            float flash = (float) Math.sin(morph * Math.PI);
            g2.setColor(new Color(255, 255, 255, (int) (200 * flash)));
            g2.fillRect(0, 0, W, H);

            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
            int cx = W / 2, cy = gcy;
            for (int i = 0; i < 3; i++) {
                float rt = clamp01(morph - i * 0.18f);
                if (rt <= 0f || rt >= 1f) continue;
                int r = (int) (rt * 280);
                if (r < 3) continue;
                int a = (int) (150 * (1 - rt));
                g2.setColor(new Color(255, 255, 255, a));
                midpointEllipse(g2, cx, cy, r, r);
                midpointEllipse(g2, cx, cy, r - 1, r - 1);
                midpointEllipse(g2, cx, cy, r - 2, r - 2);
            }
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        }

        // ค่อย ๆ ดับมืด (Fade to black) ตอนท้ายของ Scene 1 ก่อนตัดเข้า Scene 2
        if (t > S1_END) {
            float fadeT = clamp01((t - S1_END) / (float) (S1_FADE - S1_END));
            g2.setColor(new Color(0, 0, 0, (int) (255 * fadeT)));
            g2.fillRect(0, 0, W, H);
        }
    }

    private void drawFigure(Graphics2D g2, BufferedImage img, float alpha) {
        if (alpha <= 0.001f) return;
        Graphics2D gf = (Graphics2D) g2.create();
        gf.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, clamp01(alpha)));
        gf.drawImage(img, figX, figY, figW, figH, null);
        gf.dispose();
    }

    private void drawSmile(Graphics2D g2, float cx, float cy, float halfW, float smile, float alpha, Color col) {
        if (alpha <= 0.001f) return;
        Graphics2D gf = (Graphics2D) g2.create();
        gf.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        gf.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, clamp01(alpha)));
        gf.setColor(col);
        gf.setStroke(new BasicStroke(3.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        float depth = smile * halfW * 1.1f;
        gf.draw(new QuadCurve2D.Float(cx - halfW, cy, cx, cy + depth, cx + halfW, cy));
        gf.dispose();
    }

    // =========================================================================
    // SCENE 2: GameEnter_Scene (4.8s - 8.0s)
    // หน้าจอคอมพิวเตอร์ -> หน้าเมนู Angry Birds 2013 -> เคอร์เซอร์เมาส์คลิกปุ่ม PLAY -> Fade Out
    // =========================================================================
    private void drawScene2(Graphics2D g2, long t) {
        boolean clicking = t >= S2_CLICK && t < S2_CLICK + 350;

        int mw = (int) (W * 0.8f);              // 480
        int mh = (int) (mw * (SH / (float) SW)); // 270
        int mx = (W - mw) / 2;                  // 60
        int my = (int) (H * 0.20f);             // 120
        int deskY = my + mh + 14 + 60;          // 464

        // บรรยากาศห้องมืดสลัว + โต๊ะไม้
        g2.setColor(new Color(30, 30, 36));
        g2.fillRect(0, 0, W, H);
        g2.setColor(new Color(60, 45, 35));
        g2.fillRect(0, deskY, W, H - deskY);
        g2.setColor(new Color(45, 32, 24));
        g2.fillRect(0, deskY, W, 6);

        // ขอบจอ Monitor + ขาตั้งจอ
        g2.setColor(new Color(35, 35, 38));
        g2.fillRoundRect(mx - 14, my - 14, mw + 28, mh + 28, 16, 16);
        g2.fillRect(W / 2 - 20, my + mh + 14, 40, deskY - (my + mh + 14));
        g2.fillRoundRect(W / 2 - 70, deskY - 14, 140, 14, 8, 8);

        // วาดเนื้อหาเมนูเกมภายในพื้นที่จอ Monitor
        double sx = mw / (double) SW, sy = mh / (double) SH;
        Graphics2D gs = (Graphics2D) g2.create(mx, my, mw, mh);
        gs.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        gs.scale(sx, sy);
        drawMenuScreenContent(gs, t, clicking);
        gs.dispose();

        // การเคลื่อนที่ของเคอร์เซอร์เมาส์ตรงไปยังปุ่ม PLAY
        float btnWX = (float) (mx + (SW / 2) * sx);
        float btnWY = (float) (my + 178 * sy);
        float moveT = easeInOutQuad(clamp01((t - S2_START) / (float) (S2_CLICK - S2_START)));
        float curX = lerp(mx + 60, btnWX - 4, moveT);
        float curY = lerp(my + mh - 30, btnWY + 6, moveT);
        float pressScale = clicking ? 0.85f : 1f;
        g2.setColor(Color.WHITE);
        g2.fillPolygon(buildCursor(curX, curY, pressScale));
        g2.setColor(Color.BLACK);
        g2.setStroke(new BasicStroke(1.2f));
        g2.drawPolygon(buildCursor(curX, curY, pressScale));

        // ค่อย ๆ สว่างขึ้นจากความมืด (Fade in) ตอนเริ่ม Scene 2
        if (t < S2_START + 400) {
            float inAlpha = 1f - clamp01((t - S2_START) / 400f);
            g2.setColor(new Color(0, 0, 0, (int) (255 * inAlpha)));
            g2.fillRect(0, 0, W, H);
        }

        // ค่อย ๆ มืดลง (Fade to black) ตอนท้าย Scene 2 เพื่อตัดเข้าสู่เกมเพลย์
        if (t > S2_FADE) {
            float outAlpha = clamp01((t - S2_FADE) / (float) (S2_END - S2_FADE));
            g2.setColor(new Color(0, 0, 0, (int) (255 * outAlpha)));
            g2.fillRect(0, 0, W, H);
        }
    }

    private void drawMenuScreenContent(Graphics2D g2, long t, boolean clicking) {
        // 1. ท้องฟ้า (Sky)
        g2.setPaint(new GradientPaint(0, 0, new Color(140, 200, 225), 0, SH, new Color(205, 234, 238)));
        g2.fillRect(-4, -4, SW + 8, SH + 8);

        // 2. พุ่มไม้ระยะไกล (Distant bushes)
        g2.setColor(new Color(170, 205, 216));
        for (int bx = 40; bx < SW; bx += 150) {
            g2.fillOval(bx, 250, 90, 70);
            g2.fillRect(bx + 30, 258, 30, 50);
        }

        // 3. ผืนดินและยอดหญ้า (Ground & Grass)
        int grassY = 300;
        g2.setColor(new Color(120, 185, 70));
        g2.fillRect(-4, grassY, SW + 8, 26);
        g2.setColor(new Color(95, 160, 55));
        for (int gx = 0; gx < SW; gx += 14) {
            int[] bx = {gx, gx + 5, gx + 10};
            int[] by = {grassY + 2, grassY - 8, grassY + 2};
            g2.fillPolygon(bx, by, 3);
        }
        g2.setColor(new Color(45, 52, 72));
        g2.fillRect(-4, grassY + 26, SW + 8, SH - grassY - 26 + 8);
        g2.setColor(new Color(70, 78, 100));
        for (int px = 30; px < SW; px += 70) g2.fillOval(px, grassY + 34, 14, 9);

        // 4. ตัวละครตกแต่งหน้าเมนู (Decorative characters)
        drawMenuRedBird(g2, 180, 55, 1.0f);
        drawMenuWhiteBird(g2, 108, 288);
        drawMenuYellowBird(g2, 560, 175);
        drawMenuPig(g2, 500, 300, false);
        drawMenuPig(g2, 600, 298, true);

        // 5. โลโก้เกม (Logo)
        g2.setFont(new Font("SansSerif", Font.BOLD, 48));
        String logo = "ANGRY BIRDS";
        FontMetrics fm = g2.getFontMetrics();
        int lx = (SW - fm.stringWidth(logo)) / 2, ly = 100;
        g2.setColor(new Color(60, 90, 120, 120));
        g2.drawString(logo, lx + 3, ly + 4);
        g2.setColor(new Color(70, 100, 150));
        for (int dx = -2; dx <= 2; dx++)
            for (int dy = -2; dy <= 2; dy++)
                if (dx != 0 || dy != 0) g2.drawString(logo, lx + dx, ly + dy);
        g2.setColor(Color.WHITE);
        g2.drawString(logo, lx, ly);

        // 6. ปุ่ม PLAY สีแดง
        int bcx = SW / 2, bcy = 178, br = 42;
        g2.setColor(clicking ? new Color(255, 90, 60) : new Color(210, 40, 30));
        g2.fillOval(bcx - br, bcy - br, br * 2, br * 2);
        g2.setColor(new Color(150, 22, 16));
        g2.setStroke(new BasicStroke(3));
        g2.drawOval(bcx - br, bcy - br, br * 2, br * 2);
        g2.setColor(Color.WHITE);
        int[] tx = {bcx - 14, bcx - 14, bcx + 22};
        int[] ty = {bcy - 24, bcy + 24, bcy};
        g2.fillPolygon(tx, ty, 3);

        // 7. วงคลื่นกระเพื่อมจากการคลิก (Click ripple)
        if (t >= S2_CLICK) {
            float rt = clamp01((t - S2_CLICK) / 500f);
            int r = (int) (br + 8 + rt * 42);
            g2.setColor(new Color(255, 255, 255, (int) (200 * (1 - rt))));
            g2.setStroke(new BasicStroke(3));
            g2.drawOval(bcx - r, bcy - r, r * 2, r * 2);
        }
    }

    private void drawMenuRedBird(Graphics2D g2, int cx, int cy, float s) {
        int r = (int) (22 * s);
        g2.setColor(new Color(220, 30, 20));
        g2.fillOval(cx - r, cy - r, r * 2, r * 2);
        int[] tx = {cx - 6, cx, cx + 4}, ty = {cy - r + 2, cy - r - 10, cy - r + 2};
        g2.fillPolygon(tx, ty, 3);
        g2.setColor(Color.WHITE);
        g2.fillOval(cx - 8, cy - 8, 9, 11); g2.fillOval(cx + 1, cy - 8, 9, 11);
        g2.setColor(Color.BLACK);
        g2.fillOval(cx - 4, cy - 6, 4, 4); g2.fillOval(cx + 4, cy - 6, 4, 4);
        g2.fillPolygon(new int[]{cx - 10, cx + 2, cx - 10}, new int[]{cy - 12, cy - 14, cy - 6}, 3);
        g2.fillPolygon(new int[]{cx + 12, cx, cx + 12}, new int[]{cy - 12, cy - 14, cy - 6}, 3);
        g2.setColor(new Color(245, 180, 30));
        g2.fillPolygon(new int[]{cx + 2, cx + 20, cx + 2}, new int[]{cy, cy + 4, cy + 9}, 3);
    }

    private void drawMenuWhiteBird(Graphics2D g2, int cx, int cy) {
        g2.setColor(new Color(245, 245, 245));
        g2.fillOval(cx - 26, cy - 30, 52, 58);
        g2.setColor(new Color(230, 230, 230));
        g2.fillOval(cx - 18, cy + 2, 36, 22);
        g2.setColor(Color.BLACK);
        g2.fillPolygon(new int[]{cx - 6, cx, cx + 2}, new int[]{cy - 26, cy - 40, cy - 26}, 3);
        g2.setColor(Color.WHITE);
        g2.fillOval(cx - 12, cy - 20, 11, 13); g2.fillOval(cx + 1, cy - 20, 11, 13);
        g2.setColor(Color.BLACK);
        g2.fillOval(cx - 7, cy - 16, 4, 4); g2.fillOval(cx + 6, cy - 16, 4, 4);
        g2.setColor(new Color(245, 170, 30));
        g2.fillPolygon(new int[]{cx - 2, cx - 20, cx - 2}, new int[]{cy - 8, cy - 4, cy}, 3);
    }

    private void drawMenuYellowBird(Graphics2D g2, int cx, int cy) {
        g2.setColor(new Color(250, 210, 40));
        g2.fillPolygon(new int[]{cx - 18, cx + 16, cx - 8}, new int[]{cy - 14, cy, cy + 16}, 3);
        g2.setColor(Color.WHITE);
        g2.fillOval(cx - 10, cy - 8, 8, 9); g2.fillOval(cx - 2, cy - 8, 8, 9);
        g2.setColor(Color.BLACK);
        g2.fillOval(cx - 6, cy - 5, 3, 3); g2.fillOval(cx + 2, cy - 5, 3, 3);
        g2.setColor(new Color(230, 140, 20));
        g2.fillPolygon(new int[]{cx + 2, cx + 14, cx + 2}, new int[]{cy - 2, cy + 2, cy + 6}, 3);
    }

    private void drawMenuPig(Graphics2D g2, int cx, int cy, boolean king) {
        int r = 20;
        g2.setColor(new Color(120, 200, 80));
        g2.fillOval(cx - r - 7, cy - r - 4, 12, 12);
        g2.fillOval(cx + r - 5, cy - r - 4, 12, 12);
        g2.fillOval(cx - r, cy - r, r * 2, r * 2);
        g2.setColor(Color.WHITE);
        g2.fillOval(cx - 12, cy - 8, 8, 8); g2.fillOval(cx + 4, cy - 8, 8, 8);
        g2.setColor(Color.BLACK);
        g2.fillOval(cx - 9, cy - 6, 3, 3); g2.fillOval(cx + 7, cy - 6, 3, 3);
        g2.setColor(new Color(150, 220, 110));
        g2.fillOval(cx - 8, cy + 2, 16, 12);
        g2.setColor(new Color(40, 90, 30));
        g2.fillOval(cx - 4, cy + 5, 2, 5); g2.fillOval(cx + 2, cy + 5, 2, 5);
        if (king) {
            g2.setColor(new Color(245, 205, 40));
            g2.fillPolygon(new int[]{cx - 12, cx - 12, cx - 6, cx, cx + 6, cx + 12, cx + 12},
                    new int[]{cy - r - 2, cy - r - 14, cy - r - 6, cy - r - 16, cy - r - 6, cy - r - 14, cy - r - 2}, 7);
        }
    }

    // =========================================================================
    // SCENE 3: Game_Scene (8.0s - 14.0s)
    // หนังสติ๊กยิงนก -> วิถีพาราโบลา -> หอคอยไม้พัง & กำจัดหมู -> ป้าย LEVEL COMPLETE + ดาว 3 ดวง
    // =========================================================================
    private void drawScene3(Graphics2D g2, long t) {
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

        float birdX, birdY;
        boolean impactDone = t >= S3_HIT;

        if (t < S3_SHOT) {
            // จังหวะดึงหนังสติ๊กถอยหลัง (Pullback)
            float pt = easeOutQuad(clamp01((t - S3_PULL) / (float) (S3_SHOT - S3_PULL)));
            birdX = lerp(slingX, slingX - 55, pt);
            birdY = lerp(slingBaseY - 75, slingBaseY - 50, pt);
            g2.setColor(new Color(60, 40, 20));
            g2.setStroke(new BasicStroke(5));
            g2.drawLine(slingX - 18, slingBaseY - 105, (int) birdX, (int) birdY);
            g2.drawLine(slingX + 18, slingBaseY - 105, (int) birdX, (int) birdY);
            drawSprite(g2, birdImg, birdX, birdY, 48);
            drawSprite(g2, pigImg, pigX, pigY, 56);
            g2.setColor(new Color(150, 100, 50));
            for (Rectangle b : blocks) g2.fillRect(b.x, b.y, b.width, b.height);
        } else if (t < S3_HIT) {
            // จังหวะนกพุ่งลอยในอากาศตามวิถีโค้งพาราโบลา (Parabolic Flight)
            float ft = clamp01((t - S3_SHOT) / (float) (S3_HIT - S3_SHOT));
            float startX = slingX - 55, startY = slingBaseY - 50;
            birdX = lerp(startX, pigX, ft);
            float straightY = lerp(startY, pigY, ft);
            birdY = straightY - (float) Math.sin(ft * Math.PI) * 180;
            drawSprite(g2, birdImg, birdX, birdY, 48);
            drawSprite(g2, pigImg, pigX, pigY, 56);
            g2.setColor(new Color(150, 100, 50));
            for (Rectangle b : blocks) g2.fillRect(b.x, b.y, b.width, b.height);
        } else {
            // จังหวะพุ่งชนและเกิดการทำลายล้าง (Impact & Destruction)
            float it = clamp01((t - S3_HIT) / 800f);
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

            if (it > 0.05f && it < 1.0f) {
                g2.setColor(new Color(120, 190, 90));
                for (int i = 0; i < 8; i++) {
                    double ang = i * (Math.PI * 2 / 8);
                    float dist = it * 50;
                    g2.fillOval((int) (pigX + Math.cos(ang) * dist) - 5, (int) (pigY + Math.sin(ang) * dist) - 5, 10, 10);
                }
            }
            drawSprite(g2, birdImg, birdX, birdY, 48);
        }

        // ป้าย LEVEL COMPLETE! และดาวทอง 3 ดวง (3 Stars)
        if (impactDone) {
            float panelT = clamp01((t - S3_PANEL) / 400f);
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

            int[] starDelays = {S3_PANEL + 300, S3_PANEL + 650, S3_PANEL + 1000};
            int[] starX = {-70, 0, 70};
            for (int i = 0; i < 3; i++) {
                if (t >= starDelays[i]) {
                    float st = clamp01((t - starDelays[i]) / 350f);
                    float sScale = easeOutBack(st);
                    drawStar(gp, starX[i], 16, sScale * 1.2f, true);
                }
            }
            gp.dispose();
        }

        // ค่อย ๆ สว่างขึ้น (Fade in) ตอนเริ่ม Scene 3
        if (t < S3_START + 400) {
            float inAlpha = 1f - clamp01((t - S3_START) / 400f);
            g2.setColor(new Color(0, 0, 0, (int) (255 * inAlpha)));
            g2.fillRect(0, 0, W, H);
        }

        // ค่อย ๆ มืดลง (Fade to black) ตอนจบรอบ เพื่อวนลูปกลับไป Scene 1 ใหม่อย่างราบรื่น
        if (t > S3_HOLD) {
            float outAlpha = clamp01((t - S3_HOLD) / (float) (TOTAL_CYCLE - S3_HOLD));
            g2.setColor(new Color(0, 0, 0, (int) (255 * outAlpha)));
            g2.fillRect(0, 0, W, H);
        }
    }

    // =========================================================================
    // กลไกเรนเดอร์สไปรต์แบบฝังในตัว (Sprite Rendering Engine)
    // (รวบรวมฟังก์ชันวาดของ DrawAdult, DrawChild, DrawBird, DrawPig ไว้ภายในไฟล์เดียว)
    // =========================================================================
    private static final Color TRANSPARENT = new Color(0, 0, 0, 0);
    private static final Color OUTLINE     = new Color(30, 24, 20);

    private static int sgn(int v) { return Integer.compare(v, 0); }

    private static BufferedImage makeEllipseLayer(int w, int h, int cx, int cy, int a, int b, Color fill, boolean rim) {
        BufferedImage s = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics gs = s.getGraphics();
        gs.setColor(fill);
        midpointEllipse(gs, cx, cy, a, b);
        midpointEllipse(gs, cx, cy, a - 1, b);
        midpointEllipse(gs, cx, cy, a, b - 1);
        midpointEllipse(gs, cx, cy, a - 1, b - 1);
        floodFillImg(s, cx, cy, TRANSPARENT, fill);
        if (rim) { gs.setColor(OUTLINE); midpointEllipse(gs, cx, cy, a, b); }
        gs.dispose();
        return s;
    }

    private static BufferedImage makeCircleLayer(int w, int h, int cx, int cy, int r, Color fill, boolean rim) {
        BufferedImage s = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics gs = s.getGraphics();
        gs.setColor(fill);
        midpointCircle(gs, cx, cy, r);
        midpointCircle(gs, cx, cy, r - 1);
        floodFillImg(s, cx, cy, TRANSPARENT, fill);
        if (rim) { gs.setColor(OUTLINE); midpointCircle(gs, cx, cy, r); }
        gs.dispose();
        return s;
    }

    private static BufferedImage makePolyLayer(int w, int h, int[] xs, int[] ys, Color fill, boolean rim) {
        BufferedImage s = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics gs = s.getGraphics();
        int n = xs.length;
        long sx = 0, sy = 0;
        for (int i = 0; i < n; i++) { sx += xs[i]; sy += ys[i]; }
        int cx = (int) (sx / n), cy = (int) (sy / n);
        gs.setColor(fill);
        for (int i = 0; i < n; i++) {
            int j = (i + 1) % n;
            bresenham(gs, xs[i], ys[i], xs[j], ys[j]);
            bresenham(gs, xs[i] + sgn(cx - xs[i]), ys[i] + sgn(cy - ys[i]),
                          xs[j] + sgn(cx - xs[j]), ys[j] + sgn(cy - ys[j]));
        }
        floodFillImg(s, cx, cy, TRANSPARENT, fill);
        if (rim) {
            gs.setColor(OUTLINE);
            for (int i = 0; i < n; i++) { int j = (i + 1) % n; bresenham(gs, xs[i], ys[i], xs[j], ys[j]); }
        }
        gs.dispose();
        return s;
    }

    private static BufferedImage renderAdult(int w, int h, boolean clean) {
        boolean R = !clean;
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = (Graphics2D) out.getGraphics();

        Color skin   = new Color(120, 72, 45);
        Color skinSh = new Color(104, 60, 36);
        Color hair   = new Color(55, 35, 22);
        Color beard  = new Color(46, 30, 20);
        Color shirt  = new Color(120, 170, 210);
        Color jeans  = new Color(30, 45, 75);
        Color shoe   = new Color(70, 45, 30);
        Color white  = new Color(245, 245, 245);
        Color dark   = new Color(35, 28, 24);
        Color mouth  = new Color(120, 50, 45);

        // ส่วนลำตัวผู้ใหญ่ (Body)
        g.drawImage(makeEllipseLayer(w, h, 100, 420, 26, 12, shoe, R), 0, 0, null);
        g.drawImage(makeEllipseLayer(w, h, 148, 420, 26, 12, shoe, R), 0, 0, null);
        g.drawImage(makePolyLayer(w, h, new int[]{84,156,150,90}, new int[]{268,268,414,414}, jeans, R), 0, 0, null);
        if (R) {
            BufferedImage s = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
            Graphics gs = s.getGraphics();
            gs.setColor(OUTLINE);
            bresenham(gs, 120, 330, 120, 414);
            gs.dispose();
            g.drawImage(s, 0, 0, null);
        }
        g.drawImage(makePolyLayer(w, h, new int[]{74,166,160,80}, new int[]{168,168,272,272}, shirt, R), 0, 0, null);
        g.drawImage(makePolyLayer(w, h, new int[]{74,96,86,64}, new int[]{168,170,258,250}, shirt, R), 0, 0, null);
        g.drawImage(makePolyLayer(w, h, new int[]{166,144,154,176}, new int[]{168,170,258,250}, shirt, R), 0, 0, null);
        g.drawImage(makeEllipseLayer(w, h, 120, 260, 28, 15, skin, R), 0, 0, null);
        g.drawImage(makePolyLayer(w, h, new int[]{110,130,130,110}, new int[]{150,150,170,170}, skinSh, false), 0, 0, null);

        // ส่วนศีรษะและใบหน้าผู้ใหญ่ (Head)
        g.drawImage(makeEllipseLayer(w, h, 120, 86, 52, 56, hair, false), 0, 0, null);
        g.drawImage(makeEllipseLayer(w, h, 120, 98, 45, 50, skin, R), 0, 0, null);
        g.drawImage(makeCircleLayer(w, h, 73, 104, 9, skin, R), 0, 0, null);
        g.drawImage(makeCircleLayer(w, h, 167, 104, 9, skin, R), 0, 0, null);
        for (int[] c : new int[][]{{86,44,13},{104,36,14},{122,34,14},{140,36,14},{158,44,13}})
            g.drawImage(makeCircleLayer(w, h, c[0], c[1], c[2], hair, false), 0, 0, null);
        g.drawImage(makeEllipseLayer(w, h, 120, 126, 34, 24, beard, false), 0, 0, null);
        g.drawImage(makeCircleLayer(w, h, 102, 96, 8, white, R), 0, 0, null);
        g.drawImage(makeCircleLayer(w, h, 138, 96, 8, white, R), 0, 0, null);
        g.drawImage(makeCircleLayer(w, h, 102, 96, 4, dark, false), 0, 0, null);
        g.drawImage(makeCircleLayer(w, h, 138, 96, 4, dark, false), 0, 0, null);
        {
            BufferedImage s = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
            Graphics gs = s.getGraphics();
            gs.setColor(OUTLINE);
            bresenham(gs, 90, 86, 112, 84);
            bresenham(gs, 128, 84, 150, 86);
            bresenham(gs, 120, 104, 116, 116);
            bresenham(gs, 116, 116, 124, 116);
            gs.dispose();
            g.drawImage(s, 0, 0, null);
        }
        if (!clean) {
            // รอยยิ้มแบบเปิดปาก (วาดเส้นโค้ง Bezier + Flood Fill เติมสีริมฝีปากและฟัน)
            BufferedImage s = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
            Graphics gs = s.getGraphics();
            gs.setColor(mouth);
            drawBezier(gs, new int[]{104,130, 112,125, 128,125, 136,130}, 26);
            drawBezier(gs, new int[]{136,130, 128,144, 112,144, 104,130}, 26);
            floodFillImg(s, 120, 135, TRANSPARENT, mouth);
            gs.dispose();
            g.drawImage(s, 0, 0, null);
            g.drawImage(makePolyLayer(w, h, new int[]{111,129,127,113}, new int[]{131,131,137,137}, white, false), 0, 0, null);
            BufferedImage r2 = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
            Graphics gr = r2.getGraphics();
            gr.setColor(OUTLINE);
            drawBezier(gr, new int[]{104,130, 112,125, 128,125, 136,130}, 26);
            drawBezier(gr, new int[]{136,130, 128,144, 112,144, 104,130}, 26);
            gr.dispose();
            g.drawImage(r2, 0, 0, null);
        }
        g.dispose();
        return out;
    }

    private static BufferedImage renderChild(int w, int h, boolean clean) {
        boolean R = !clean;
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = (Graphics2D) out.getGraphics();

        Color skin   = new Color(140, 88, 58);
        Color skinSh = new Color(122, 74, 48);
        Color hair   = new Color(55, 35, 22);
        Color tee    = new Color(245, 205, 60);
        Color jeans  = new Color(70, 110, 175);
        Color cuff   = new Color(120, 160, 205);
        Color red    = new Color(200, 60, 50);
        Color white  = new Color(245, 245, 245);
        Color dark   = new Color(35, 28, 24);
        Color mouth  = new Color(120, 50, 45);

        // ส่วนลำตัวเด็ก (Body)
        g.drawImage(makeEllipseLayer(w, h, 100, 414, 26, 13, red, R), 0, 0, null);
        g.drawImage(makeEllipseLayer(w, h, 148, 414, 26, 13, red, R), 0, 0, null);
        g.drawImage(makeEllipseLayer(w, h, 84, 416, 11, 8, white, R), 0, 0, null);
        g.drawImage(makeEllipseLayer(w, h, 164, 416, 11, 8, white, R), 0, 0, null);
        g.drawImage(makePolyLayer(w, h, new int[]{84,156,150,90}, new int[]{286,286,404,404}, jeans, R), 0, 0, null);
        if (R) {
            BufferedImage s = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
            Graphics gs = s.getGraphics();
            gs.setColor(OUTLINE);
            bresenham(gs, 120, 340, 120, 404);
            gs.dispose();
            g.drawImage(s, 0, 0, null);
        }
        g.drawImage(makePolyLayer(w, h, new int[]{88,118,116,90}, new int[]{392,392,406,406}, cuff, R), 0, 0, null);
        g.drawImage(makePolyLayer(w, h, new int[]{124,152,150,122}, new int[]{392,392,406,406}, cuff, R), 0, 0, null);
        g.drawImage(makePolyLayer(w, h, new int[]{110,130,130,110}, new int[]{194,194,210,210}, skinSh, false), 0, 0, null);
        g.drawImage(makePolyLayer(w, h, new int[]{76,164,158,82}, new int[]{206,206,290,290}, tee, R), 0, 0, null);
        g.drawImage(makePolyLayer(w, h, new int[]{76,102,94,66}, new int[]{206,208,248,244}, tee, R), 0, 0, null);
        g.drawImage(makePolyLayer(w, h, new int[]{164,138,146,174}, new int[]{206,208,248,244}, tee, R), 0, 0, null);
        g.drawImage(makePolyLayer(w, h, new int[]{70,90,84,66}, new int[]{244,246,300,296}, skin, R), 0, 0, null);
        g.drawImage(makePolyLayer(w, h, new int[]{170,150,156,174}, new int[]{244,246,300,296}, skin, R), 0, 0, null);
        g.drawImage(makeCircleLayer(w, h, 75, 304, 11, skin, R), 0, 0, null);
        g.drawImage(makeCircleLayer(w, h, 165, 304, 11, skin, R), 0, 0, null);

        // ส่วนศีรษะและใบหน้าเด็ก (Head)
        g.drawImage(makeEllipseLayer(w, h, 120, 140, 50, 50, hair, false), 0, 0, null);
        g.drawImage(makeEllipseLayer(w, h, 120, 152, 43, 46, skin, R), 0, 0, null);
        g.drawImage(makeCircleLayer(w, h, 79, 158, 9, skin, R), 0, 0, null);
        g.drawImage(makeCircleLayer(w, h, 161, 158, 9, skin, R), 0, 0, null);
        for (int[] c : new int[][]{{88,100,12},{106,94,13},{124,92,13},{142,94,13},{158,100,12}})
            g.drawImage(makeCircleLayer(w, h, c[0], c[1], c[2], hair, false), 0, 0, null);
        g.drawImage(makeCircleLayer(w, h, 104, 150, 8, white, R), 0, 0, null);
        g.drawImage(makeCircleLayer(w, h, 138, 150, 8, white, R), 0, 0, null);
        g.drawImage(makeCircleLayer(w, h, 104, 150, 4, dark, false), 0, 0, null);
        g.drawImage(makeCircleLayer(w, h, 138, 150, 4, dark, false), 0, 0, null);
        {
            BufferedImage s = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
            Graphics gs = s.getGraphics();
            gs.setColor(OUTLINE);
            bresenham(gs, 94, 138, 112, 136);
            bresenham(gs, 130, 136, 148, 138);
            gs.dispose();
            g.drawImage(s, 0, 0, null);
        }
        if (!clean) {
            BufferedImage s = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
            Graphics gs = s.getGraphics();
            gs.setColor(mouth);
            drawBezier(gs, new int[]{104,170, 112,166, 130,166, 138,170}, 26);
            drawBezier(gs, new int[]{138,170, 130,188, 112,188, 104,170}, 26);
            floodFillImg(s, 121, 178, TRANSPARENT, mouth);
            gs.dispose();
            g.drawImage(s, 0, 0, null);
            g.drawImage(makePolyLayer(w, h, new int[]{110,132,130,108}, new int[]{171,171,178,178}, white, false), 0, 0, null);
            BufferedImage r2 = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
            Graphics gr = r2.getGraphics();
            gr.setColor(OUTLINE);
            drawBezier(gr, new int[]{104,170, 112,166, 130,166, 138,170}, 26);
            drawBezier(gr, new int[]{138,170, 130,188, 112,188, 104,170}, 26);
            gr.dispose();
            g.drawImage(r2, 0, 0, null);
        }
        g.dispose();
        return out;
    }

    private static BufferedImage renderBird(int w, int h) {
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = (Graphics2D) out.getGraphics();

        Color body  = new Color(220, 30, 20);
        Color belly = new Color(232, 182, 140);
        Color beak  = new Color(245, 180, 30);
        Color beakD = new Color(210, 140, 20);
        Color white = new Color(245, 245, 245);
        Color dark  = new Color(25, 20, 18);
        Color spot  = new Color(150, 22, 16);

        // ขนหางและหงอนบนหัวด้านหลัง (Feathers behind body)
        g.drawImage(makePolyLayer(w, h, new int[]{40,52,58}, new int[]{22,4,26}, body, true), 0, 0, null);
        g.drawImage(makePolyLayer(w, h, new int[]{58,72,66}, new int[]{22,10,28}, body, true), 0, 0, null);
        g.drawImage(makePolyLayer(w, h, new int[]{16,6,20}, new int[]{58,66,70}, body, true), 0, 0, null);
        g.drawImage(makePolyLayer(w, h, new int[]{18,8,22}, new int[]{72,80,82}, body, true), 0, 0, null);

        // ลำตัว + ท้อง + จุดกระบนแก้ม (Body + belly + cheek spots)
        g.drawImage(makeCircleLayer(w, h, 56, 64, 46, body, true), 0, 0, null);
        g.drawImage(makeEllipseLayer(w, h, 56, 96, 26, 13, belly, false), 0, 0, null);
        g.drawImage(makeEllipseLayer(w, h, 34, 84, 6, 5, spot, false), 0, 0, null);
        g.drawImage(makeEllipseLayer(w, h, 28, 72, 4, 4, spot, false), 0, 0, null);

        // ดวงตา (Eyes)
        g.drawImage(makeEllipseLayer(w, h, 48, 52, 10, 14, white, true), 0, 0, null);
        g.drawImage(makeEllipseLayer(w, h, 68, 52, 10, 14, white, true), 0, 0, null);
        g.drawImage(makeCircleLayer(w, h, 52, 54, 4, dark, false), 0, 0, null);
        g.drawImage(makeCircleLayer(w, h, 64, 54, 4, dark, false), 0, 0, null);

        // คิ้วโกรธ (Angry eyebrows)
        g.drawImage(makePolyLayer(w, h, new int[]{34,52,54,36}, new int[]{34,46,52,40}, dark, false), 0, 0, null);
        g.drawImage(makePolyLayer(w, h, new int[]{82,64,62,80}, new int[]{34,46,52,40}, dark, false), 0, 0, null);

        // จงอยปาก (Beak)
        g.drawImage(makePolyLayer(w, h, new int[]{56,92,58}, new int[]{62,70,72}, beak, true), 0, 0, null);
        g.drawImage(makePolyLayer(w, h, new int[]{56,88,58}, new int[]{74,80,84}, beakD, true), 0, 0, null);

        g.dispose();
        return out;
    }

    private static BufferedImage renderPig(int w, int h) {
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = (Graphics2D) out.getGraphics();

        Color body  = new Color(120, 200, 80);
        Color snout = new Color(150, 220, 110);
        Color dgreen= new Color(70, 140, 50);
        Color white = new Color(245, 245, 245);
        Color dark  = new Color(30, 40, 25);

        // หูและศีรษะหมู (Ears & head)
        g.drawImage(makeCircleLayer(w, h, 44, 24, 10, body, true), 0, 0, null);
        g.drawImage(makeCircleLayer(w, h, 76, 24, 10, body, true), 0, 0, null);
        g.drawImage(makeCircleLayer(w, h, 60, 66, 44, body, true), 0, 0, null);

        // ดวงตา (Eyes)
        g.drawImage(makeCircleLayer(w, h, 46, 54, 11, white, true), 0, 0, null);
        g.drawImage(makeCircleLayer(w, h, 74, 54, 11, white, true), 0, 0, null);
        g.drawImage(makeCircleLayer(w, h, 48, 56, 4, dark, false), 0, 0, null);
        g.drawImage(makeCircleLayer(w, h, 72, 56, 4, dark, false), 0, 0, null);

        // คิ้วและปาก (Brows + mouth)
        {
            BufferedImage s = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
            Graphics gs = s.getGraphics();
            gs.setColor(dgreen);
            bresenham(gs, 36, 40, 54, 44);
            bresenham(gs, 84, 40, 66, 44);
            bresenham(gs, 50, 92, 70, 92);
            gs.dispose();
            g.drawImage(s, 0, 0, null);
        }

        // จมูกหมูและรูจมูก (Snout + nostrils)
        g.drawImage(makeEllipseLayer(w, h, 60, 74, 18, 14, snout, true), 0, 0, null);
        g.drawImage(makeEllipseLayer(w, h, 53, 74, 2, 4, dark, false), 0, 0, null);
        g.drawImage(makeEllipseLayer(w, h, 67, 74, 2, 4, dark, false), 0, 0, null);

        g.dispose();
        return out;
    }

    // =========================================================================
    // อัลกอริทึมกราฟิกคอมพิวเตอร์พื้นฐาน (Computer Graphics Primitive Algorithms)
    // =========================================================================
    public static void bresenham(Graphics g, int x1, int y1, int x2, int y2) {
        int dx = Math.abs(x2 - x1);
        int dy = Math.abs(y2 - y1);
        int sx = (x1 < x2) ? 1 : -1;
        int sy = (y1 < y2) ? 1 : -1;
        boolean isSwap = false;
        if (dy > dx) { int temp = dx; dx = dy; dy = temp; isSwap = true; }
        int D = 2 * dy - dx;
        int x = x1, y = y1;
        for (int i = 0; i <= dx; i++) {
            g.fillRect(x, y, 1, 1);
            if (D >= 0) {
                if (isSwap) x += sx; else y += sy;
                D -= 2 * dx;
            }
            if (isSwap) y += sy; else x += sx;
            D += 2 * dy;
        }
    }

    public static void midpointCircle(Graphics g, int xc, int yc, int r) {
        int x = 0, y = r, Dx = 2 * x, Dy = 2 * y, D = 1 - r;
        while (x <= y) {
            g.fillRect( x + xc,  y + yc, 1, 1); g.fillRect(-x + xc,  y + yc, 1, 1);
            g.fillRect( x + xc, -y + yc, 1, 1); g.fillRect(-x + xc, -y + yc, 1, 1);
            g.fillRect( y + xc,  x + yc, 1, 1); g.fillRect(-y + xc,  x + yc, 1, 1);
            g.fillRect( y + xc, -x + yc, 1, 1); g.fillRect(-y + xc, -x + yc, 1, 1);
            x++; Dx += 2; D += Dx + 1;
            if (D >= 0) { y--; Dy -= 2; D -= Dy; }
        }
    }

    public static void midpointEllipse(Graphics g, int xc, int yc, int a, int b) {
        if (a <= 0 || b <= 0) return;
        int a2 = a * a, b2 = b * b, twoA2 = 2 * a2, twoB2 = 2 * b2;
        int x = 0, y = b;
        int D = (int) Math.round(b2 - a2 * b + (a2 / 4.0));
        int Dx = 0, Dy = twoA2 * y;
        while (Dx <= Dy) {
            g.fillRect( x + xc,  y + yc, 1, 1); g.fillRect( x + xc, -y + yc, 1, 1);
            g.fillRect(-x + xc,  y + yc, 1, 1); g.fillRect(-x + xc, -y + yc, 1, 1);
            x++; Dx += twoB2; D += Dx + b2;
            if (D >= 0) { y--; Dy -= twoA2; D -= Dy; }
        }
        x = a; y = 0;
        D = (int) Math.round(a2 - b2 * a + (b2 / 4.0));
        Dx = twoB2 * x; Dy = 0;
        while (Dx >= Dy) {
            g.fillRect( x + xc,  y + yc, 1, 1); g.fillRect( x + xc, -y + yc, 1, 1);
            g.fillRect(-x + xc,  y + yc, 1, 1); g.fillRect(-x + xc, -y + yc, 1, 1);
            y++; Dy += twoA2; D += Dy + a2;
            if (D >= 0) { x--; Dx -= twoB2; D -= Dx; }
        }
    }

    public static Point cubicBezierPoint(double t, Point[] cp) {
        Point p1 = cp[0], p2 = cp[1], p3 = cp[2], p4 = cp[3];
        double x = (Math.pow((1-t), 3) * p1.x) + (3 * t * Math.pow(1-t, 2) * p2.x)
                 + (3 * Math.pow(t, 2) * (1 - t) * p3.x) + (Math.pow(t, 3) * p4.x);
        double y = (Math.pow((1-t), 3) * p1.y) + (3 * t * Math.pow(1-t, 2) * p2.y)
                 + (3 * Math.pow(t, 2) * (1 - t) * p3.y) + (Math.pow(t, 3) * p4.y);
        return new Point((int) Math.round(x), (int) Math.round(y));
    }

    public static void drawBezier(Graphics g, int[] c, int steps) {
        Point[] cps = new Point[]{
                new Point(c[0], c[1]), new Point(c[2], c[3]),
                new Point(c[4], c[5]), new Point(c[6], c[7])
        };
        Point prev = cubicBezierPoint(0.0, cps);
        for (int i = 1; i <= steps; i++) {
            Point cur = cubicBezierPoint(i / (double) steps, cps);
            bresenham(g, prev.x, prev.y, cur.x, cur.y);
            prev = cur;
        }
    }

    public static BufferedImage floodFillImg(BufferedImage m, int x, int y, Color target_colour, Color replacement_Colour) {
        if (target_colour.getRGB() == replacement_Colour.getRGB()) return m;
        if (x < 0 || x >= m.getWidth() || y < 0 || y >= m.getHeight()) return m;
        if (m.getRGB(x, y) != target_colour.getRGB()) return m;
        Queue<Point> q = new LinkedList<>();
        m.setRGB(x, y, replacement_Colour.getRGB());
        q.add(new Point(x, y));
        while (!q.isEmpty()) {
            Point p = q.poll();
            int px = p.x, py = p.y;
            if (py + 1 < m.getHeight() && m.getRGB(px, py + 1) == target_colour.getRGB()) {
                m.setRGB(px, py + 1, replacement_Colour.getRGB());
                q.add(new Point(px, py + 1));
            }
            if (py - 1 >= 0 && m.getRGB(px, py - 1) == target_colour.getRGB()) {
                m.setRGB(px, py - 1, replacement_Colour.getRGB());
                q.add(new Point(px, py - 1));
            }
            if (px + 1 < m.getWidth() && m.getRGB(px + 1, py) == target_colour.getRGB()) {
                m.setRGB(px + 1, py, replacement_Colour.getRGB());
                q.add(new Point(px + 1, py));
            }
            if (px - 1 >= 0 && m.getRGB(px - 1, py) == target_colour.getRGB()) {
                m.setRGB(px - 1, py, replacement_Colour.getRGB());
                q.add(new Point(px - 1, py));
            }
        }
        return m;
    }

    static class Point {
        public int x, y;
        public Point(int x, int y) { this.x = x; this.y = y; }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Angry Birds - Memory Animation (600x600)");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.add(new mergeAll());
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setResizable(false);
            frame.setVisible(true);
        });
    }
}

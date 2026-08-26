package CG_Assignment1;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.LinkedList;
import java.util.Queue;

/**
 * Game_Scene — ฉากการเล่นเกม (Gameplay)
 * การยิงหนังสติ๊กเพื่อกำจัดหมูบนหอคอยไม้ แล้วแสดงป้าย "LEVEL COMPLETE!" พร้อมดาวทอง 3 ดวง
 * วาดด้วย Sprite ของนกแดง (DrawBird) และหมูเขียว (DrawPig)
 * ที่เรนเดอร์ขึ้นจากอัลกอริทึมกราฟิกพื้นฐานทั้งหมด วนลูปการทำงาน
 */
public class Game_Scene extends JPanel {
    private final int W, H;
    private final BufferedImage buf;
    private int penRGB = Color.BLACK.getRGB();

    private final BufferedImage birdImg, pigImg;
    private final long startTime;
    private static final int SCENE_MS = 4200;   // เวลาเล่นแอนิเมชัน 1 รอบ
    private static final int LOOP_PAUSE = 900;  // ค้างหน้า LEVEL COMPLETE ไว้ชั่วขณะก่อนวนลูป

    public Game_Scene(int width, int height) {
        this.W = width;
        this.H = height;
        this.buf = new BufferedImage(W, H, BufferedImage.TYPE_INT_ARGB);
        setPreferredSize(new Dimension(W, H));
        setBackground(Color.BLACK);

        this.birdImg = new DrawBird(120, 120).render();
        this.pigImg  = new DrawPig(120, 120).render();

        this.startTime = System.currentTimeMillis();
        new Timer(16, e -> repaint()).start(); // ~60fps
    }

    // ---------- ฟังก์ชันคำนวณและ Easing Helpers ----------
    private float clamp(float v, float lo, float hi) { return Math.max(lo, Math.min(hi, v)); }
    private float lerp(float a, float b, float t) { return a + (b - a) * t; }
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

    // วาดรูปสไปรต์โดยจัดให้อยู่กึ่งกลางพิกัด (cx, cy) และปรับสเกลตามขนาดที่กำหนด
    private void drawSprite(Graphics2D g2, BufferedImage img, float cx, float cy, int size) {
        g2.drawImage(img, Math.round(cx - size / 2f), Math.round(cy - size / 2f), size, size, null);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

        long cycle = SCENE_MS + LOOP_PAUSE;
        long phase = (System.currentTimeMillis() - startTime) % cycle;
        float t = clamp(phase / (float) SCENE_MS, 0f, 1f); // คงค่าไว้ที่ 1 ในช่วงหยุดพัก

        drawGameplay(g2, t);
    }

    // ========== เกมเพลย์: ยิงหนังสติ๊กเพื่อกำจัดหมู แล้วแสดงป้าย LEVEL COMPLETE + ดาว ==========
    private void drawGameplay(Graphics2D g2, float t) {
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

        float shotT = clamp(t / 0.5f, 0, 1); // ลำดับการยิงจัดอยู่ในช่วงครึ่งแรกของฉาก
        float birdX, birdY;
        boolean impactDone = t >= 0.5f;

        if (shotT < 0.18f) {
            // ดึงหนังสติ๊กถอยหลัง (Pullback)
            float pt = easeOutQuad(shotT / 0.18f);
            birdX = lerp(slingX, slingX - 55, pt);
            birdY = lerp(slingBaseY - 75, slingBaseY - 50, pt);
            g2.setColor(new Color(60, 40, 20));
            g2.setStroke(new BasicStroke(5));
            g2.drawLine(slingX - 18, slingBaseY - 105, (int) birdX, (int) birdY);
            g2.drawLine(slingX + 18, slingBaseY - 105, (int) birdX, (int) birdY);
        } else if (shotT < 0.55f) {
            // นกพุ่งลอยในอากาศตามวิถีโค้งพาราโบลา (Parabolic flight)
            float ft = (shotT - 0.18f) / 0.37f;
            float startX = slingX - 55, startY = slingBaseY - 50;
            birdX = lerp(startX, pigX, ft);
            float straightY = lerp(startY, pigY, ft);
            birdY = straightY - (float) Math.sin(ft * Math.PI) * 180;
        } else {
            // การพุ่งชนและเกิดการทำลายล้าง (Impact & Destruction)
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
            drawSprite(g2, birdImg, birdX, birdY, 48);   // นกลงตรงจุดชนหมู
        }

        if (shotT < 0.55f) {
            // ก่อนการพุ่งชน: วาดบล็อกไม้ที่ยังสมบูรณ์, หมู, และนก
            g2.setColor(new Color(150, 100, 50));
            for (Rectangle b : blocks) g2.fillRect(b.x, b.y, b.width, b.height);
            drawSprite(g2, pigImg, pigX, pigY, 56);
            drawSprite(g2, birdImg, birdX, birdY, 48);
        }

        // ป้าย "LEVEL COMPLETE!" และดาว 3 ดวง เมื่อนกพุ่งชนเป้าหมายแล้ว
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

            float[] starTimes = {0.58f, 0.66f, 0.74f};
            int[] starX = {-70, 0, 70};
            for (int i = 0; i < 3; i++) {
                float st = clamp((t - starTimes[i]) / 0.09f, 0, 1);
                if (st > 0f) {
                    float sScale = easeOutBack(st);
                    drawStar(gp, starX[i], 16, sScale * 1.2f, true);
                }
            }
            gp.dispose();
        }
    }

    // ===================== algorithm toolkit =====================
    public void bresenhamLine(Graphics g,int x1, int y1, int x2, int y2) {
        int dx = Math.abs(x2 - x1);
        int dy = Math.abs(y2 - y1);

        int sx = (x1 < x2)? 1 : -1; //Step x
        int sy = (y1 < y2)? 1 : -1; //Step y

        boolean isSwap = false;

        if(dy > dx) { // swap if slope is steep
            int temp = dx; dx = dy; dy = temp;
            isSwap = true;
        }

        int D = 2 * dy - dx;
        int x = x1, y = y1;

        for(int i = 0; i <= dx; i++) {
            plot(g,x, y);
            if(D >= 0) {
                if(isSwap) x += sx;
                else       y += sy;
                D -= 2*dx;
            }
            if(isSwap) y += sy;
            else       x += sx;

            D += 2 * dy;
        }
    }

    // Cubic Bezier
    public Point cubicBerzierCurve(double t, Point[] controlPoints) {
        if(controlPoints == null || controlPoints.length == 0)
            throw new IllegalArgumentException("Control points cannot be null or empty");
        if(t < 0.0 || t > 1.0)
            System.err.println("t value is outside [0,1] range");

        Point p1 = controlPoints[0];
        Point p2 = controlPoints[1];
        Point p3 = controlPoints[2];
        Point p4 = controlPoints[3];

        double x = (Math.pow((1-t), 3) * p1.x) + (3 * t * Math.pow(1-t, 2) * p2.x)
                 + (3 * Math.pow(t, 2) * (1 - t) * p3.x) + (Math.pow(t, 3) * p4.x);
        double y = (Math.pow((1-t), 3) * p1.y) + (3 * t * Math.pow(1-t, 2) * p2.y)
                 + (3 * Math.pow(t, 2) * (1 - t) * p3.y) + (Math.pow(t, 3) * p4.y);

        return new Point((int)Math.round(x), (int)Math.round(y));
    }

    // Stroke a Bezier by connecting sampled points with Bresenham
    private void drawCubicBezier(Graphics g,int[] c, int steps) {
        Point[] cps = new Point[]{
                new Point(c[0],c[1]), new Point(c[2],c[3]),
                new Point(c[4],c[5]), new Point(c[6],c[7])
        };
        Point prev = cubicBerzierCurve(0.0, cps);
        for (int i = 1; i <= steps; i++) {
            Point cur = cubicBerzierCurve(i/(double)steps, cps);
            bresenhamLine(g,prev.x, prev.y, cur.x, cur.y);
            prev = cur;
        }
    }

    // Flood fill on buffer
    public BufferedImage floodFill(BufferedImage m, int x, int y, Color target_colour, Color replacement_Colour) {
        if (target_colour.getRGB() == replacement_Colour.getRGB()) return m;   // no-op / avoid infinite loop
        if (x < 0 || x >= m.getWidth() || y < 0 || y >= m.getHeight()) return m;
        if (m.getRGB(x, y) != target_colour.getRGB()) return m;                // seed not on target -> nothing to do

        Queue<Point> q = new LinkedList<>();
        m.setRGB(x, y, replacement_Colour.getRGB());                           // fill the seed itself
        q.add(new Point(x, y));

        while(!q.isEmpty()) {
            Point currentPoint = q.poll();
            int currentX = currentPoint.x;
            int currentY = currentPoint.y;

            if(currentY+1 < m.getHeight() && m.getRGB(currentX,currentY+1) == target_colour.getRGB()){
                m.setRGB(currentX, currentY+1, replacement_Colour.getRGB());
                q.add(new Point(currentX,currentY+1));
            }
            if(currentY-1 >=0 && m.getRGB(currentX,currentY-1) == target_colour.getRGB()){
                m.setRGB(currentX, currentY-1, replacement_Colour.getRGB());
                q.add(new Point(currentX,currentY-1));
            }
            if(currentX+1 < m.getWidth() && m.getRGB(currentX+1,currentY) == target_colour.getRGB()){
                m.setRGB(currentX+1, currentY, replacement_Colour.getRGB());
                q.add(new Point(currentX+1,currentY));
            }
            if(currentX-1 >= 0 && m.getRGB(currentX-1,currentY) == target_colour.getRGB()){
                m.setRGB(currentX-1, currentY, replacement_Colour.getRGB());
                q.add(new Point(currentX-1,currentY));
            }
        }
        return m;
    }

    // Midpoint circle
    public void midpointCircle(Graphics g, int xc, int yc, int r) {
        int x = 0;
        int y = r;
        int Dx = 2 * x;
        int Dy = 2 * y;
        int D = 1 - r;

        while (x <= y) {
            plot(g,  x + xc,  y + yc);
            plot(g, -x + xc,  y + yc);
            plot(g,  x + xc, -y + yc);
            plot(g, -x + xc, -y + yc);
            plot(g,  y + xc,  x + yc);
            plot(g, -y + xc,  x + yc);
            plot(g,  y + xc, -x + yc);
            plot(g, -y + xc, -x + yc);

            x++;
            Dx += 2;
            D  += Dx + 1;

            if(D >= 0) {
                y--;
                Dy -= 2;
                D  -= Dy;
            }
        }
    }

    // Midpoint ellipse
    public void midpointEllipse(Graphics g,int xc, int yc, int a, int b) {
        int a2 = a * a;
        int b2 = b * b;
        int twoA2 = 2 * a2;
        int twoB2 = 2 * b2;

        // Region 1
        int x = 0;
        int y = b;

        int D  = (int) Math.round(b2 - a2 * b + (a2/4.0));
        int Dx = 0, Dy = twoA2 * y;

        while(Dx <= Dy) {
            plot(g,x+xc,y+yc);
            plot(g,x+xc,-y+yc);
            plot(g,-x+xc,y+yc);
            plot(g,-x+xc,-y+yc);

            x++;
            Dx += twoB2;
            D  += Dx + b2;

            if(D >= 0) {
                y--;
                Dy -= twoA2;
                D  -= Dy;
            }
        }

        // Region 2
        x = a; y = 0;
        D  = (int) Math.round(a2 - b2*a + (b2/4.0));
        Dx = twoB2*x;
        Dy = 0;

        while(Dx >= Dy) {
            plot(g,x+xc,y+yc);
            plot(g,x+xc,-y+yc);
            plot(g,-x+xc,y+yc);
            plot(g,-x+xc,-y+yc);

            y++;
            Dy += twoA2;
            D  += Dy + a2;

            if(D >= 0) {
                x--;
                Dx -= twoB2;
                D  -= Dx;
            }
        }
    }

    //plot
    public void plot(Graphics g, int x, int y) {
        g.fillRect(x, y, 1, 1);  // draw to screen
        if (x>=0 && x<W && y>=0 && y<H) {
            buf.setRGB(x, y, penRGB); // mirror into buffer
        }
    }

    //SetColor
    private void useColor(Graphics g,Color c) { g.setColor(c); penRGB = c.getRGB(); }

    private void fillBuffer(int argb) {
        int[] row = new int[W];
        for (int i = 0; i < W; i++) row[i] = argb;
        for (int y = 0; y < H; y++) buf.getRaster().setDataElements(0, y, W, 1, row);
    }

    class Point {
        public int x, y;
        public Point(int x, int y) { this.x = x; this.y = y; }
    }

    public static void main(String[] args) {
        JFrame frame = new JFrame("Angry Birds - Game Scene");
        frame.add(new Game_Scene(600, 600));
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setResizable(false);
        frame.setVisible(true);
    }
}

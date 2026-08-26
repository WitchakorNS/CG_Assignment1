package CG_Assignment1;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.util.LinkedList;
import java.util.Queue;

/**
 * AdultChild_Scene — แอนิเมชันแบบวนลูปที่ตัวละครผู้ใหญ่ (DrawAdult)
 * เปลี่ยนผ่าน (Morph) กลายเป็นตัวละครในวัยเด็ก (DrawChild)
 * มีเอฟเฟกต์แสงวาบสีขาว (White flash) และวงแหวนคลื่นความทรงจำ (Memory rings)
 * ที่วาดด้วยอัลกอริทึม midpointEllipse ขณะที่ตัวละครทั้งสองค่อย ๆ Cross-fade เข้าหากัน
 *
 * ช่วงเวลาของ Timeline ในแต่ละรอบ:
 *   0.0-1.2s  แสดงภาพผู้ใหญ่
 *   1.2-2.8s  ช่วงเปลี่ยนผ่าน: Cross-fade ผู้ใหญ่ -> เด็ก, แสงวาบสีขาว, วงแหวนความทรงจำ
 *   2.8-4.2s  แสดงภาพเด็ก
 *   + หยุดพักสั้น ๆ แล้ววนลูปใหม่
 */
public class AdultChild_Scene extends JPanel {
    private final int W, H;
    private final BufferedImage buf;
    private int penRGB = Color.BLACK.getRGB();

    // ขนาดของแคนวาสตัวละคร ซูมขึ้นเพื่อให้เห็นภาพครึ่งตัว (เอวขึ้นไป)
    private static final int FW = 240, FH = 440;
    private static final float SCALE = 1.65f;             // ซูมเข้าเพื่อให้เห็นช่วงลำตัวด้านบนชัดเจน
    private final int figX, figY, figW, figH;
    private final BufferedImage adultImg, childImg;

    private final long startTime;
    private static final int SHOW_ADULT_END = 1200;
    private static final int MORPH_END      = 2800;
    private static final int SHOW_CHILD_END = 4200;
    private static final int LOOP_PAUSE     = 800;

    public AdultChild_Scene(int width, int height) {
        this.W = width;
        this.H = height;
        this.buf = new BufferedImage(W, H, BufferedImage.TYPE_INT_ARGB);
        setPreferredSize(new Dimension(W, H));
        setBackground(Color.BLACK);

        this.adultImg = new DrawAdult(FW, FH).render(true);  // clean: ไม่มีเส้นขอบหนา และไม่มีปากที่วาดทับไว้ก่อน
        this.childImg = new DrawChild(FW, FH).render(true);
        this.figW = Math.round(FW * SCALE);
        this.figH = Math.round(FH * SCALE);
        this.figX = (W - figW) / 2;                 // จัดให้อยู่ตรงกลางแนวนอน
        this.figY = 40 - Math.round(30 * SCALE);    // ศีรษะอยู่ใกล้ด้านบน ส่วนขาเลยขอบล่างของเฟรม

        this.startTime = System.currentTimeMillis();
        new Timer(16, e -> repaint()).start(); // ~60fps
    }

    // ---------- ฟังก์ชันคำนวณพื้นฐาน (Math helpers) ----------
    private float clamp(float v, float lo, float hi) { return Math.max(lo, Math.min(hi, v)); }
    private float clamp01(float v) { return clamp(v, 0f, 1f); }
    private float lerp(float a, float b, float t) { return a + (b - a) * t; }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

        long cycle = SHOW_CHILD_END + LOOP_PAUSE;
        long t = (System.currentTimeMillis() - startTime) % cycle;

        // --- ความคืบหน้าของสถานะ -> Cross-fade / Flash / Ring progress ---
        float adultAlpha, childAlpha, morph;
        if (t < SHOW_ADULT_END)      { adultAlpha = 1f; childAlpha = 0f; morph = 0f; }
        else if (t < MORPH_END)      { morph = (t - SHOW_ADULT_END) / (float) (MORPH_END - SHOW_ADULT_END);
                                       adultAlpha = 1f - morph; childAlpha = morph; }
        else                         { adultAlpha = 0f; childAlpha = 1f; morph = 1f; }

        // รอยยิ้มค่อย ๆ กว้างขึ้นในช่วง 0.9s แรก แล้วคงที่ไว้
        float smile = clamp01(t / 900f);

        // --- พื้นหลัง: โทนเย็นสลัว -> เปลี่ยนเป็นโทนอุ่นซีเปียแห่งความทรงจำ (Warm sepia memory tone) ---
        float warm = morph;
        int br = (int) lerp(40, 235, warm), bgc = (int) lerp(44, 215, warm), bb = (int) lerp(62, 175, warm);
        g2.setColor(new Color(br, bgc, bb));
        g2.fillRect(0, 0, W, H);

        // --- แสงฟุ้งรอบใบหน้า (Radial Glow) ---
        int gcx = W / 2, gcy = figY + Math.round(110 * SCALE);
        RadialGradientPaint glow = new RadialGradientPaint(new Point2D.Float(gcx, gcy), 240,
                new float[]{0f, 1f},
                new Color[]{new Color(255, 255, 255, (int) lerp(70, 40, warm)), new Color(0, 0, 0, 0)});
        g2.setPaint(glow);
        g2.fillOval(gcx - 240, gcy - 240, 480, 480);

        // --- ตัวละคร + เลเยอร์รอยยิ้มแบบแอนิเมชัน (Cross-faded) ---
        float amx = figX + 120 * SCALE, amy = figY + 136 * SCALE;   // ตำแหน่งปากผู้ใหญ่ (Screen space)
        float cmx = figX + 121 * SCALE, cmy = figY + 176 * SCALE;   // ตำแหน่งปากเด็ก
        drawFigure(g2, adultImg, adultAlpha);
        drawSmile(g2, amx, amy, 15 * SCALE, smile, adultAlpha, new Color(190, 105, 90)); // สีโทนสว่างเพื่อให้เห็นบนเครา
        drawFigure(g2, childImg, childAlpha);
        drawSmile(g2, cmx, cmy, 17 * SCALE, 0.6f + 0.4f * smile, childAlpha, new Color(90, 35, 30));

        // --- แสงวาบสีขาว: สว่างสูงสุดตรงกึ่งกลางของการ Morph ---
        if (morph > 0f && morph < 1f) {
            float flash = (float) Math.sin(morph * Math.PI);
            g2.setColor(new Color(255, 255, 255, (int) (200 * flash)));
            g2.fillRect(0, 0, W, H);

            // --- วงแหวนคลื่นความทรงจำ (วาดด้วยอัลกอริทึม midpointEllipse) ---
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
            int cx = W / 2, cy = gcy;
            for (int i = 0; i < 3; i++) {
                float rt = clamp01(morph - i * 0.18f);
                if (rt <= 0f || rt >= 1f) continue;
                int r = (int) (rt * 280);
                if (r < 3) continue;   // รักษาให้ทั้ง 3 รอบ (r, r-1, r-2) >= 1
                int a = (int) (150 * (1 - rt));
                useColor(g2, new Color(255, 255, 255, a));
                midpointEllipse(g2, cx, cy, r, r);           // วาด 3 รอบซ้อนกันเพื่อให้เส้นทึบชัดเจน
                midpointEllipse(g2, cx, cy, r - 1, r - 1);
                midpointEllipse(g2, cx, cy, r - 2, r - 2);
            }
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        }
    }

    private void drawFigure(Graphics2D g2, BufferedImage img, float alpha) {
        if (alpha <= 0.001f) return;
        Graphics2D gf = (Graphics2D) g2.create();
        gf.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, clamp01(alpha)));
        gf.drawImage(img, figX, figY, figW, figH, null);
        gf.dispose();
    }

    // an animated smile curve (MyMemory style) overlaid on the figure's mouth
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
        if (a <= 0 || b <= 0) return;   // guard: a==0/b==0 would loop forever
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
        JFrame frame = new JFrame("Adult -> Child");
        frame.add(new AdultChild_Scene(600, 600));
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setResizable(false);
        frame.setVisible(true);
    }
}

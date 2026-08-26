package CG_Assignment1;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.LinkedList;
import java.util.Queue;

public class GameEnter_Scene extends JPanel {
    private final int W, H;
    private final BufferedImage buf;
    private int penRGB = Color.BLACK.getRGB();

    private final long startTime;
    private static final int MENU_MS = 2600;    // one menu -> click -> fade pass
    private static final int LOOP_PAUSE = 900;  // hold black, then loop

    public GameEnter_Scene(int width, int height) {
        this.W = width;
        this.H = height;
        this.buf = new BufferedImage(W, H, BufferedImage.TYPE_INT_ARGB);
        setPreferredSize(new Dimension(W, H));
        setBackground(Color.BLACK);
        this.startTime = System.currentTimeMillis();
        new Timer(16, e -> repaint()).start(); // ~60fps
    }

    // ---------- math / cursor helpers (from MyMemoryAnimation) ----------
    private float clamp(float v, float lo, float hi) { return Math.max(lo, Math.min(hi, v)); }
    private float lerp(float a, float b, float t) { return a + (b - a) * t; }
    private float easeInOutQuad(float t) { return t < 0.5f ? 2 * t * t : 1 - (float) Math.pow(-2 * t + 2, 2) / 2; }

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

        long cycle = MENU_MS + LOOP_PAUSE;
        long phase = (System.currentTimeMillis() - startTime) % cycle;
        float t = clamp(phase / (float) MENU_MS, 0f, 1f); // holds at 1 during the pause
        drawMenu(g2, t);
    }

    // logical resolution the menu content is drawn in, then scaled onto the monitor screen
    private static final int SW = 640, SH = 360;

    // ============ desktop: the Angry-Birds menu on a monitor, cursor clicks PLAY -> game ============
    private void drawMenu(Graphics2D g2, float t) {
        boolean clicking = t > 0.55f && t < 0.68f;

        int mw = (int) (W * 0.8f);              // 480 when W=600
        int mh = (int) (mw * (SH / (float) SW)); // 270 (16:9 aspect ratio)
        int mx = (W - mw) / 2;                  // 60
        int my = (int) (H * 0.20f);             // 120
        int deskY = my + mh + 14 + 60;          // 464 (monitor sits cleanly on desk)

        // --- dim room + desk ---
        g2.setColor(new Color(30, 30, 36));
        g2.fillRect(0, 0, W, H);
        g2.setColor(new Color(60, 45, 35));
        g2.fillRect(0, deskY, W, H - deskY);
        g2.setColor(new Color(45, 32, 24));
        g2.fillRect(0, deskY, W, 6); // desk top rim shadow

        // --- monitor bezel + stand ---
        g2.setColor(new Color(35, 35, 38));
        g2.fillRoundRect(mx - 14, my - 14, mw + 28, mh + 28, 16, 16);
        g2.fillRect(W / 2 - 20, my + mh + 14, 40, deskY - (my + mh + 14)); // stand neck
        g2.fillRoundRect(W / 2 - 70, deskY - 14, 140, 14, 8, 8);           // stand base

        // --- menu content drawn inside the screen (logical SWxSH scaled to fit) ---
        double sx = mw / (double) SW, sy = mh / (double) SH;
        Graphics2D gs = (Graphics2D) g2.create(mx, my, mw, mh); // clips to the screen
        gs.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        gs.scale(sx, sy);
        drawScreenContent(gs, t, clicking);
        gs.dispose();

        // --- cursor eases up to the PLAY button (window space), presses on click ---
        float btnWX = (float) (mx + (SW / 2) * sx);
        float btnWY = (float) (my + 178 * sy);
        float moveT = easeInOutQuad(clamp(t / 0.55f, 0, 1));
        float curX = lerp(mx + 60, btnWX - 4, moveT);
        float curY = lerp(my + mh - 30, btnWY + 6, moveT);
        float pressScale = clicking ? 0.85f : 1f;
        g2.setColor(Color.WHITE);
        g2.fillPolygon(buildCursor(curX, curY, pressScale));
        g2.setColor(Color.BLACK);
        g2.setStroke(new BasicStroke(1.2f));
        g2.drawPolygon(buildCursor(curX, curY, pressScale));

        // --- fade to black: the cut into the game ---
        if (t > 0.68f) {
            int alpha = (int) (255 * clamp((t - 0.68f) / 0.30f, 0, 1));
            g2.setColor(new Color(0, 0, 0, alpha));
            g2.fillRect(0, 0, W, H);
        }
    }

    // the Angry-Birds menu, in logical SWxSH coordinates (drawn onto the monitor screen)
    private void drawScreenContent(Graphics2D g2, float t, boolean clicking) {
        // 1. sky (fill a touch past the edges so scaling leaves no seam)
        g2.setPaint(new GradientPaint(0, 0, new Color(140, 200, 225), 0, SH, new Color(205, 234, 238)));
        g2.fillRect(-4, -4, SW + 8, SH + 8);

        // 2. distant faded bushes near the horizon
        g2.setColor(new Color(170, 205, 216));
        for (int bx = 40; bx < SW; bx += 150) {
            g2.fillOval(bx, 250, 90, 70);
            g2.fillRect(bx + 30, 258, 30, 50);
        }

        // 3. ground: grass strip + dark dirt
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

        // 4. decorative characters
        drawRedBird(g2, 180, 55, 1.0f);
        drawWhiteBird(g2, 108, 288);
        drawYellowBird(g2, 560, 175);
        drawPig(g2, 500, 300, false);
        drawPig(g2, 600, 298, true);   // king pig (crown)

        // 5. "ANGRY BIRDS" logo (white with shadow + outline)
        g2.setFont(new Font("SansSerif", Font.BOLD, 48));
        String logo = "ANGRY BIRDS";
        FontMetrics fm = g2.getFontMetrics();
        int lx = (SW - fm.stringWidth(logo)) / 2, ly = 100;
        g2.setColor(new Color(60, 90, 120, 120));
        g2.drawString(logo, lx + 3, ly + 4);               // soft shadow
        g2.setColor(new Color(70, 100, 150));
        for (int dx = -2; dx <= 2; dx++)                    // outline
            for (int dy = -2; dy <= 2; dy++)
                if (dx != 0 || dy != 0) g2.drawString(logo, lx + dx, ly + dy);
        g2.setColor(Color.WHITE);
        g2.drawString(logo, lx, ly);

        // 6. PLAY button — the original red circle + white play triangle
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

        // 7. click ripple around the button
        if (t > 0.55f) {
            float rt = clamp((t - 0.55f) / 0.28f, 0, 1);
            int r = (int) (br + 8 + rt * 42);
            g2.setColor(new Color(255, 255, 255, (int) (200 * (1 - rt))));
            g2.setStroke(new BasicStroke(3));
            g2.drawOval(bcx - r, bcy - r, r * 2, r * 2);
        }
    }

    // ---------- small inline menu characters ----------
    private void drawRedBird(Graphics2D g2, int cx, int cy, float s) {
        int r = (int) (22 * s);
        g2.setColor(new Color(220, 30, 20));
        g2.fillOval(cx - r, cy - r, r * 2, r * 2);
        int[] tx = {cx - 6, cx, cx + 4}, ty = {cy - r + 2, cy - r - 10, cy - r + 2};
        g2.fillPolygon(tx, ty, 3);                                  // tuft
        g2.setColor(Color.WHITE);
        g2.fillOval(cx - 8, cy - 8, 9, 11); g2.fillOval(cx + 1, cy - 8, 9, 11);
        g2.setColor(Color.BLACK);
        g2.fillOval(cx - 4, cy - 6, 4, 4); g2.fillOval(cx + 4, cy - 6, 4, 4);
        g2.fillPolygon(new int[]{cx - 10, cx + 2, cx - 10}, new int[]{cy - 12, cy - 14, cy - 6}, 3); // L brow
        g2.fillPolygon(new int[]{cx + 12, cx, cx + 12}, new int[]{cy - 12, cy - 14, cy - 6}, 3);     // R brow
        g2.setColor(new Color(245, 180, 30));
        g2.fillPolygon(new int[]{cx + 2, cx + 20, cx + 2}, new int[]{cy, cy + 4, cy + 9}, 3);        // beak
    }

    private void drawWhiteBird(Graphics2D g2, int cx, int cy) {
        g2.setColor(new Color(245, 245, 245));
        g2.fillOval(cx - 26, cy - 30, 52, 58);                       // body (egg-ish)
        g2.setColor(new Color(230, 230, 230));
        g2.fillOval(cx - 18, cy + 2, 36, 22);                        // belly shade
        g2.setColor(Color.BLACK);
        g2.fillPolygon(new int[]{cx - 6, cx, cx + 2}, new int[]{cy - 26, cy - 40, cy - 26}, 3); // tuft
        g2.setColor(Color.WHITE);
        g2.fillOval(cx - 12, cy - 20, 11, 13); g2.fillOval(cx + 1, cy - 20, 11, 13);
        g2.setColor(Color.BLACK);
        g2.fillOval(cx - 7, cy - 16, 4, 4); g2.fillOval(cx + 6, cy - 16, 4, 4);
        g2.setColor(new Color(245, 170, 30));
        g2.fillPolygon(new int[]{cx - 2, cx - 20, cx - 2}, new int[]{cy - 8, cy - 4, cy}, 3);   // beak
    }

    private void drawYellowBird(Graphics2D g2, int cx, int cy) {
        g2.setColor(new Color(250, 210, 40));
        g2.fillPolygon(new int[]{cx - 18, cx + 16, cx - 8}, new int[]{cy - 14, cy, cy + 16}, 3); // triangular body
        g2.setColor(Color.WHITE);
        g2.fillOval(cx - 10, cy - 8, 8, 9); g2.fillOval(cx - 2, cy - 8, 8, 9);
        g2.setColor(Color.BLACK);
        g2.fillOval(cx - 6, cy - 5, 3, 3); g2.fillOval(cx + 2, cy - 5, 3, 3);
        g2.setColor(new Color(230, 140, 20));
        g2.fillPolygon(new int[]{cx + 2, cx + 14, cx + 2}, new int[]{cy - 2, cy + 2, cy + 6}, 3); // beak
    }

    private void drawPig(Graphics2D g2, int cx, int cy, boolean king) {
        int r = 20;
        g2.setColor(new Color(120, 200, 80));
        g2.fillOval(cx - r - 7, cy - r - 4, 12, 12);                 // ears
        g2.fillOval(cx + r - 5, cy - r - 4, 12, 12);
        g2.fillOval(cx - r, cy - r, r * 2, r * 2);                   // head
        g2.setColor(Color.WHITE);
        g2.fillOval(cx - 12, cy - 8, 8, 8); g2.fillOval(cx + 4, cy - 8, 8, 8);
        g2.setColor(Color.BLACK);
        g2.fillOval(cx - 9, cy - 6, 3, 3); g2.fillOval(cx + 7, cy - 6, 3, 3);
        g2.setColor(new Color(150, 220, 110));
        g2.fillOval(cx - 8, cy + 2, 16, 12);                        // snout
        g2.setColor(new Color(40, 90, 30));
        g2.fillOval(cx - 4, cy + 5, 2, 5); g2.fillOval(cx + 2, cy + 5, 2, 5); // nostrils
        if (king) {
            g2.setColor(new Color(245, 205, 40));
            g2.fillPolygon(new int[]{cx - 12, cx - 12, cx - 6, cx, cx + 6, cx + 12, cx + 12},
                           new int[]{cy - r - 2, cy - r - 14, cy - r - 6, cy - r - 16, cy - r - 6, cy - r - 14, cy - r - 2}, 7);
        }
    }

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
        JFrame frame = new JFrame("Angry Birds - Menu");
        frame.add(new GameEnter_Scene(600, 600));
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setResizable(false);
        frame.setVisible(true);
    }
}
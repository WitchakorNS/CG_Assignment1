package CG_Assignment1;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.LinkedList;
import java.util.Queue;

public class DrawChild {
    private final int W, H;
    private final BufferedImage buf;
    private int penRGB = Color.BLACK.getRGB();

    public DrawChild(int width, int height) {
        this.W = width;
        this.H = height;
        this.buf = new BufferedImage(W, H, BufferedImage.TYPE_INT_ARGB);
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

    // ===================== figure rendering =====================
    private static final Color TRANSPARENT = new Color(0, 0, 0, 0);
    private static final Color OUTLINE     = new Color(30, 24, 20);

    private int sgn(int v) { return Integer.compare(v, 0); }
    private BufferedImage newLayer() { return new BufferedImage(W, H, BufferedImage.TYPE_INT_ARGB); }

    // A solid ellipse on its own transparent layer: seal the border with a couple of
    // concentric passes (so the 4-connected floodFill can't leak out), then flood-fill.
    private BufferedImage ellipseLayer(int cx, int cy, int a, int b, Color fill, boolean rim) {
        BufferedImage s = newLayer();
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

    private BufferedImage circleLayer(int cx, int cy, int r, Color fill, boolean rim) {
        BufferedImage s = newLayer();
        Graphics gs = s.getGraphics();
        useColor(gs, fill);
        midpointCircle(gs, cx, cy, r);
        midpointCircle(gs, cx, cy, r - 1);
        floodFill(s, cx, cy, TRANSPARENT, fill);
        if (rim) { useColor(gs, OUTLINE); midpointCircle(gs, cx, cy, r); }
        gs.dispose();
        return s;
    }

    // A solid convex polygon: each edge is drawn twice (once nudged toward the centroid)
    // so the outline is watertight, then the interior is flood-filled from the centroid.
    private BufferedImage polyLayer(int[] xs, int[] ys, Color fill, boolean rim) {
        BufferedImage s = newLayer();
        Graphics gs = s.getGraphics();
        int n = xs.length;
        long sx = 0, sy = 0;
        for (int i = 0; i < n; i++) { sx += xs[i]; sy += ys[i]; }
        int cx = (int) (sx / n), cy = (int) (sy / n);
        useColor(gs, fill);
        for (int i = 0; i < n; i++) {
            int j = (i + 1) % n;
            bresenhamLine(gs, xs[i], ys[i], xs[j], ys[j]);
            bresenhamLine(gs, xs[i] + sgn(cx - xs[i]), ys[i] + sgn(cy - ys[i]),
                              xs[j] + sgn(cx - xs[j]), ys[j] + sgn(cy - ys[j]));
        }
        floodFill(s, cx, cy, TRANSPARENT, fill);
        if (rim) {
            useColor(gs, OUTLINE);
            for (int i = 0; i < n; i++) { int j = (i + 1) % n; bresenhamLine(gs, xs[i], ys[i], xs[j], ys[j]); }
        }
        gs.dispose();
        return s;
    }

    private BufferedImage strokeLayer() { return newLayer(); }

    /** Draws the child figure into an ARGB image (transparent background) and returns it. */
    public BufferedImage render() {
        fillBuffer(0x00000000);
        BufferedImage out = newLayer();
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

        // ---------- body (back to front) ----------
        g.drawImage(ellipseLayer(100, 414, 26, 13, red, true), 0, 0, null);    // sneakers
        g.drawImage(ellipseLayer(148, 414, 26, 13, red, true), 0, 0, null);
        g.drawImage(ellipseLayer(84, 416, 11, 8, white, true), 0, 0, null);    // white toe caps
        g.drawImage(ellipseLayer(164, 416, 11, 8, white, true), 0, 0, null);
        g.drawImage(polyLayer(new int[]{84,156,150,90}, new int[]{286,286,404,404}, jeans, true), 0, 0, null); // jeans
        { BufferedImage s = strokeLayer(); Graphics gs = s.getGraphics();
          useColor(gs, OUTLINE); bresenhamLine(gs,120,340,120,404); gs.dispose(); g.drawImage(s,0,0,null); } // inseam
        g.drawImage(polyLayer(new int[]{88,118,116,90},  new int[]{392,392,406,406}, cuff, true), 0, 0, null); // cuffs
        g.drawImage(polyLayer(new int[]{124,152,150,122}, new int[]{392,392,406,406}, cuff, true), 0, 0, null);
        g.drawImage(polyLayer(new int[]{110,130,130,110}, new int[]{194,194,210,210}, skinSh, false), 0, 0, null); // neck
        g.drawImage(polyLayer(new int[]{76,164,158,82}, new int[]{206,206,290,290}, tee, true), 0, 0, null); // t-shirt
        g.drawImage(polyLayer(new int[]{76,102,94,66},  new int[]{206,208,248,244}, tee, true), 0, 0, null); // L sleeve
        g.drawImage(polyLayer(new int[]{164,138,146,174}, new int[]{206,208,248,244}, tee, true), 0, 0, null); // R sleeve
        g.drawImage(polyLayer(new int[]{70,90,84,66},  new int[]{244,246,300,296}, skin, true), 0, 0, null); // L arm
        g.drawImage(polyLayer(new int[]{170,150,156,174}, new int[]{244,246,300,296}, skin, true), 0, 0, null); // R arm
        g.drawImage(circleLayer(75, 304, 11, skin, true), 0, 0, null);         // hands
        g.drawImage(circleLayer(165, 304, 11, skin, true), 0, 0, null);

        // ---------- head ----------
        g.drawImage(ellipseLayer(120, 140, 50, 50, hair, false), 0, 0, null);  // hair (behind face)
        g.drawImage(ellipseLayer(120, 152, 43, 46, skin, true), 0, 0, null);   // face over hair -> hair rim
        g.drawImage(circleLayer(79, 158, 9, skin, true), 0, 0, null);          // ears
        g.drawImage(circleLayer(161, 158, 9, skin, true), 0, 0, null);
        for (int[] c : new int[][]{{88,100,12},{106,94,13},{124,92,13},{142,94,13},{158,100,12}})
            g.drawImage(circleLayer(c[0], c[1], c[2], hair, false), 0, 0, null); // curls
        g.drawImage(circleLayer(104, 150, 8, white, true), 0, 0, null);        // eyes (big)
        g.drawImage(circleLayer(138, 150, 8, white, true), 0, 0, null);
        g.drawImage(circleLayer(104, 150, 4, dark, false), 0, 0, null);
        g.drawImage(circleLayer(138, 150, 4, dark, false), 0, 0, null);
        { BufferedImage s = strokeLayer(); Graphics gs = s.getGraphics();
          useColor(gs, OUTLINE);
          bresenhamLine(gs, 94, 138, 112, 136);                                // brows
          bresenhamLine(gs, 130, 136, 148, 138);
          gs.dispose(); g.drawImage(s, 0, 0, null); }
        // big open smile: two Bezier lips form a closed lens, flood-filled
        { BufferedImage s = newLayer(); Graphics gs = s.getGraphics();
          useColor(gs, mouth);
          drawCubicBezier(gs, new int[]{104,170, 112,166, 130,166, 138,170}, 26); // upper lip
          drawCubicBezier(gs, new int[]{138,170, 130,188, 112,188, 104,170}, 26); // lower lip
          floodFill(s, 121, 178, TRANSPARENT, mouth);
          gs.dispose(); g.drawImage(s, 0, 0, null); }
        g.drawImage(polyLayer(new int[]{110,132,130,108}, new int[]{171,171,178,178}, white, false), 0, 0, null); // teeth
        { BufferedImage s = newLayer(); Graphics gs = s.getGraphics();
          useColor(gs, OUTLINE);
          drawCubicBezier(gs, new int[]{104,170, 112,166, 130,166, 138,170}, 26);
          drawCubicBezier(gs, new int[]{138,170, 130,188, 112,188, 104,170}, 26);
          gs.dispose(); g.drawImage(s, 0, 0, null); }

        g.dispose();
        return out;
    }

    public static void main(String[] args) {
        DrawChild d = new DrawChild(240, 440);
        final BufferedImage img = d.render();
        JFrame f = new JFrame("Child");
        f.add(new JPanel() {
            { setBackground(Color.WHITE); setPreferredSize(new Dimension(240, 440)); }
            protected void paintComponent(Graphics g) { super.paintComponent(g); g.drawImage(img, 0, 0, null); }
        });
        f.pack();
        f.setLocationRelativeTo(null);
        f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        f.setVisible(true);
    }

    class Point {
        public int x, y;
        public Point(int x, int y) { this.x = x; this.y = y; }
    }
}
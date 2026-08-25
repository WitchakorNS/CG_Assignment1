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

    public GameEnter_Scene(int width, int height) {
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

    class Point {
        public int x, y;
        public Point(int x, int y) { this.x = x; this.y = y; }
    }
}
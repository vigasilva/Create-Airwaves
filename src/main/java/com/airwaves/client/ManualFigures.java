/**
 * Readable decompilation of Create: Airwaves from the supplied binary.
 * Client-only rendering, audio, UI, Ponder, or JEI behavior.
 *
 * <p>Names present in bytecode are preserved. Local names and comments that
 * did not survive compilation cannot be recovered with certainty; verify
 * behavior against the original bytecode before changing game logic.</p>
 */
package com.airwaves.client;

import com.airwaves.cable.CableType;
import com.airwaves.logic.AntennaUtil;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

final class ManualFigures {
   static final int INK = -12899814;
   static final int SOFT = -7703984;
   static final int GRID = 574302746;
   static final int HEAD = -8765676;
   static final int LIVE = -2522581;
   static final int GREEN = -12678598;
   static final int RED = -5228500;
   static final int BLUE = -13734232;
   private static int bx0;
   private static int by0;
   private static int bx1;
   private static int by1;

   private ManualFigures() {
   }

   static void draw(GuiGraphics g, Font f, String name, int x, int y, int w, int h, float t) {
      bx0 = x + 1;
      by0 = y + 1;
      bx1 = x + w - 1;
      by1 = y + h - 1;
      g.fill(x, y, x + w, y + h, 335544320);
      switch (name) {
         case "range":
            range(g, f, x, y, w, h, t);
            break;
         case "rpm":
            rpm(g, f, x, y, w, h, t);
            break;
         case "wave":
            wave(g, f, x, y, w, h, t);
            break;
         case "falloff":
            falloff(g, f, x, y, w, h, t);
            break;
         case "cables":
            cables(g, f, x, y, w, h, t);
            break;
         case "board":
            board(g, f, x, y, w, h, t);
            break;
         case "altitude":
            altitude(g, f, x, y, w, h, t);
            break;
         case "jammer":
            jammer(g, f, x, y, w, h, t);
            break;
         case "relay":
            relay(g, f, x, y, w, h, t);
            break;
         case "power":
            power(g, f, x, y, w, h, t);
            break;
         case "dimlink":
            dimlink(g, f, x, y, w, h, t);
            break;
         case "yagi":
            yagi(g, f, x, y, w, h, t);
            break;
         default:
            g.drawString(f, "?", x + 2, y + 2, -7703984, false);
      }

      frame(g, x, y, w, h);
   }

   private static void box(GuiGraphics g, int x0, int y0, int x1, int y1, int col) {
      x0 = Math.max(x0, bx0);
      y0 = Math.max(y0, by0);
      x1 = Math.min(x1, bx1);
      y1 = Math.min(y1, by1);
      if (x1 > x0 && y1 > y0) {
         g.fill(x0, y0, x1, y1, col);
      }
   }

   private static void frame(GuiGraphics g, int x, int y, int w, int h) {
      g.fill(x, y, x + w, y + 1, -7703984);
      g.fill(x, y + h - 1, x + w, y + h, -7703984);
      g.fill(x, y, x + 1, y + h, -7703984);
      g.fill(x + w - 1, y, x + w, y + h, -7703984);
   }

   private static void small(GuiGraphics g, Font f, String s, float x, float y, int col, int maxW) {
      float k = 0.5F;

      while (f.width(s) * k > maxW && s.length() > 1) {
         s = s.substring(0, s.length() - 1);
      }

      g.pose().pushPose();
      g.pose().translate(x, y, 0.0F);
      g.pose().scale(k, k, 1.0F);
      g.drawString(f, s, 0, 0, col, false);
      g.pose().popPose();
   }

   private static void dot(GuiGraphics g, float x, float y, int s, int col) {
      int ix = Math.round(x);
      int iy = Math.round(y);
      box(g, ix - s / 2, iy - s / 2, ix - s / 2 + s, iy - s / 2 + s, col);
   }

   private static void plot(GuiGraphics g, int x0, int[] ys, int col) {
      for (int i = 0; i < ys.length; i++) {
         int a = ys[i];
         int b = i + 1 < ys.length ? ys[i + 1] : ys[i];
         box(g, x0 + i, Math.min(a, b), x0 + i + 1, Math.max(a, b) + 1, col);
      }
   }

   private static void axes(GuiGraphics g, int x, int y, int w, int h) {
      g.fill(x, y + h, x + w, y + h + 1, -12899814);
      g.fill(x, y, x + 1, y + h + 1, -12899814);

      for (int k = 1; k < 4; k++) {
         g.fill(x + 1, y + h * k / 4, x + w, y + h * k / 4 + 1, 574302746);
      }
   }

   private static float ease(float u) {
      u = Math.max(0.0F, Math.min(1.0F, u));
      return u * u * (3.0F - 2.0F * u);
   }

   private static String km(double blocks) {
      return blocks >= 1000.0 ? String.format("%.1fk", blocks / 1000.0) : String.valueOf(Math.round(blocks));
   }

   private static void range(GuiGraphics g, Font f, int x, int y, int w, int h, float t) {
      small(g, f, "Reach by mast height (FM, blocks)", x + 3, y + 3, -8765676, w - 6);
      String[] n = new String[]{"10", "20", "40", "96"};
      int[] hs = new int[]{10, 20, 40, 96};
      double max = AntennaUtil.range(96, 4, true);
      float grow = ease(t % 6.0F / 1.6F);
      int rows = 4;
      int top = y + 10;
      int rh = (h - 14) / rows;
      int bx = x + 26;
      int bw = w - 26 - 26;

      for (int i = 0; i < rows; i++) {
         double r = AntennaUtil.range(hs[i], 4, true);
         int ry = top + i * rh;
         int len = (int)(bw * r / max * grow);
         small(g, f, n[i], x + 3, ry + rh / 2.0F - 2.0F, -12899814, 22);
         g.fill(bx, ry + 2, bx + len, ry + rh - 1, -9531472);
         g.fill(bx, ry + 2, bx + len, ry + 3, -6307616);
         if (grow > 0.98F) {
            small(g, f, km(r), bx + len + 2, ry + rh / 2.0F - 2.0F, -12899814, 20);
         }
      }
   }

   private static void rpm(GuiGraphics g, Font f, int x, int y, int w, int h, float t) {
      int ax = x + 14;
      int ay = y + 10;
      int aw = w - 20;
      int ah = h - 22;
      small(g, f, "Range vs shaft speed", x + 3, y + 3, -8765676, w - 6);
      axes(g, ax, ay, aw, ah);
      double maxRpm = 256.0;
      double maxP = 1.6;
      int[] ys = new int[aw];

      for (int i = 0; i < aw; i++) {
         double r = maxRpm * i / aw;
         double p = r < 16.0 ? 0.0 : 0.5 + 0.5 * Math.min(2.0, r / 64.0);
         ys[i] = ay + ah - (int)(ah * p / maxP);
      }

      plot(g, ax, ys, -13734232);

      for (int r : new int[]{16, 64, 128, 256}) {
         int px = ax + (int)(aw * r / maxRpm) - (r == 256 ? 1 : 0);
         g.fill(px, ay + ah, px + 1, ay + ah + 2, -12899814);
         small(g, f, String.valueOf(r), px - (r == 256 ? 8 : 2), ay + ah + 3, -7703984, 12);
      }

      small(g, f, "100%", x + 1, ay + ah - (int)(ah / maxP) - 2, -7703984, 12);
      float u = (float)((Math.sin(t * 0.7) + 1.0) / 2.0);
      double r = maxRpm * u;
      double p = r < 16.0 ? 0.0 : 0.5 + 0.5 * Math.min(2.0, r / 64.0);
      int px = ax + (int)(aw * u);
      dot(g, px, ay + ah - (float)(ah * p / maxP), 3, -2522581);
      small(g, f, Math.round(r) + " RPM: " + (p == 0.0 ? "off" : Math.round(p * 100.0) + "%"), ax + 3, ay + 2, -12899814, aw - 6);
      float ang = (float)(t * r / 60.0 * Math.PI * 2.0 / 8.0);
      int cx = x + w - 10;
      int cy = ay + 6;

      for (int k = 0; k < 4; k++) {
         dot(g, cx + (float)Math.cos(ang + k * Math.PI / 2.0) * 4.0F, cy + (float)Math.sin(ang + k * Math.PI / 2.0) * 4.0F, 2, -12899814);
      }

      dot(g, cx, cy, 2, -7703984);
   }

   private static void wave(GuiGraphics g, Font f, int x, int y, int w, int h, float t) {
      int half = h / 2;
      small(g, f, "AM: the strength changes", x + 3, y + 2, -8765676, w - 6);
      small(g, f, "FM: the frequency changes", x + 3, y + half + 1, -8765676, w - 6);
      int[] am = new int[w - 6];
      int[] fm = new int[w - 6];
      double ph = 0.0;

      for (int i = 0; i < am.length; i++) {
         double s = i + t * 30.0F;
         double voice = Math.sin(s * 0.05);
         am[i] = y + 8 + (half - 10) / 2 - (int)Math.round(Math.sin(s * 0.9) * (0.35 + 0.6 * (voice + 1.0) / 2.0) * (half - 12) / 2.0);
         ph += 0.55 + 0.4 * voice;
         fm[i] = y + half + 7 + (half - 10) / 2 - (int)Math.round(Math.sin(ph + t * 25.0F) * (half - 12) / 2.0 * 0.9);
      }

      plot(g, x + 3, am, -13734232);
      plot(g, x + 3, fm, -5228500);
      g.fill(x + 2, y + half, x + w - 2, y + half + 1, 574302746);
   }

   private static void falloff(GuiGraphics g, Font f, int x, int y, int w, int h, float t) {
      int ax = x + 6;
      int ay = y + 10;
      int aw = w - 12;
      int ah = h - 20;
      small(g, f, "Signal over distance", x + 3, y + 3, -8765676, w - 6);
      axes(g, ax, ay, aw, ah);
      int[] ys = new int[aw];

      for (int i = 0; i < aw; i++) {
         double d = (double)i / aw;
         double q = Math.max(0.0, 1.0 - Math.pow(d, 2.2));
         ys[i] = ay + ah - (int)(ah * q);
         int seed = (int)(t * 12.0F) * 131 + i * 7919;
         if (((seed ^ seed >> 5) & 15) < (int)(d * d * 14.0)) {
            dot(g, ax + i, ay + ah - (int)(ah * q) + (seed >> 3) % 7 - 3, 1, -7703984);
         }
      }

      plot(g, ax, ys, -12678598);
      small(g, f, "transmitter", ax + 1, ay + ah + 2, -7703984, 40);
      small(g, f, "range", ax + aw - 13, ay + ah + 2, -7703984, 20);
      float u = (float)((Math.sin(t * 0.5) + 1.0) / 2.0) * 0.98F;
      double q = Math.max(0.0, 1.0 - Math.pow(u, 2.2));
      dot(g, ax + aw * u, ay + ah - (float)(ah * q), 3, -2522581);
      int bars = (int)Math.round(q * 5.0);

      for (int k = 0; k < 5; k++) {
         g.fill(x + w - 30 + k * 5, y + 14 - k * 2, x + w - 27 + k * 5, y + 18, k < bars ? -12678598 : 574302746);
      }
   }

   private static void cables(GuiGraphics g, Font f, int x, int y, int w, int h, float t) {
      int ax = x + 6;
      int ay = y + 10;
      int aw = w - 12;
      int ah = h - 20;
      small(g, f, "Audio left after a cable run", x + 3, y + 3, -8765676, w - 6);
      axes(g, ax, ay, aw, ah);
      double maxLen = 96.0;
      double lo = 0.75;

      for (CableType c : CableType.values()) {
         int len = (int)(aw * c.maxLen / maxLen);
         int[] ys = new int[Math.max(1, len)];

         for (int i = 0; i < ys.length; i++) {
            double q = Math.max(lo, 1.0 - c.loss * (maxLen * i / aw));
            ys[i] = ay + (int)(ah * (1.0 - q) / (1.0 - lo));
         }

         int col = c.color;
         plot(g, ax, ys, col);
         dot(g, ax + ys.length - 1, ys[ys.length - 1], 3, col);
         float u = (t * 0.4F + c.ordinal() * 0.17F) % 1.0F;
         int pi = (int)(u * (ys.length - 1));
         dot(g, ax + pi, ys[pi], 2, -2522581);
      }

      small(g, f, "0", ax - 1, ay + ah + 2, -7703984, 6);
      small(g, f, "96 blocks", ax + aw - 22, ay + ah + 2, -7703984, 24);
      small(g, f, "75%", x + 1, ay + ah - 6, -7703984, 10);
   }

   private static void board(GuiGraphics g, Font f, int x, int y, int w, int h, float t) {
      int cs = Math.min((w - 20) / 8, (h - 6) / 5);
      int bw = cs * 8;
      int bh = cs * 5;
      int bx = x + (w - bw) / 2;
      int by = y + (h - bh) / 2;
      g.fill(bx - 6, by - 2, bx + bw + 6, by + bh + 2, -14718150);
      int[] path = new int[]{8, 9, 10, 11, 12, 13, 14, 22, 23};
      String parts = "-KP-XP---";
      float step = t * 3.0F % (path.length + 6);

      for (int r = 0; r < 5; r++) {
         for (int c = 0; c < 8; c++) {
            int cx = bx + c * cs;
            int cy = by + r * cs;
            g.fill(cx + 1, cy + 1, cx + cs - 1, cy + cs - 1, -15184083);
            g.fill(cx + 1, cy + 1, cx + 2, cy + 2, -3634628);
            g.fill(cx + cs - 2, cy + cs - 2, cx + cs - 1, cy + cs - 1, -3634628);
         }
      }

      g.fill(bx - 5, by + cs + cs / 2 - 1, bx, by + cs + cs / 2 + 1, step >= 0.0F ? -19641 : -5214162);
      g.fill(bx + bw, by + 2 * cs + cs / 2 - 1, bx + bw + 5, by + 2 * cs + cs / 2 + 1, step >= path.length ? -19641 : -5214162);

      for (int k = 0; k < path.length; k++) {
         int i = path[k];
         int cx = bx + i % 8 * cs + cs / 2;
         int cy = by + i / 8 * cs + cs / 2;
         boolean live = step >= k;
         if (k + 1 < path.length) {
            int j = path[k + 1];
            int nx = bx + j % 8 * cs + cs / 2;
            int ny = by + j / 8 * cs + cs / 2;
            int col = step >= k + 1 ? -19641 : -5214162;
            g.fill(Math.min(cx, nx), Math.min(cy, ny), Math.max(cx, nx) + 1, Math.max(cy, ny) + 1, col);
         }

         char p = parts.charAt(k);
         if (p == '-') {
            if (live) {
               g.fill(cx - 1, cy - 1, cx + 1, cy + 1, -8032);
            }
         } else {
            int pc = switch (p) {
               case 'C', 'K' -> -3641288;
               case 'P' -> -12948816;
               case 'R' -> -2572144;
               case 'T' -> -14013910;
               case 'X' -> -4677408;
               default -> step >= path.length && ((int)(t * 4.0F) & 1) == 0 ? -49104 : -8771040;
            };
            g.fill(cx - cs / 3, cy - cs / 4, cx + cs / 3, cy + cs / 4 + 1, pc);
            if (live) {
               g.fill(cx - 1, cy - 1, cx + 1, cy + 1, -8032);
            }
         }
      }

      small(g, f, "IN", bx - 6, by + 2, -1515328, 8);
      small(g, f, "OUT", bx + bw - 2, by + 2, -1515328, 10);
   }

   private static void altitude(GuiGraphics g, Font f, int x, int y, int w, int h, float t) {
      small(g, f, "Airborne antennas reach farther", x + 3, y + 3, -8765676, w - 6);
      int gy = y + h - 6;
      g.fill(x + 2, gy, x + w - 2, gy + 1, -12899814);

      for (int i = x + 4; i < x + w - 4; i += 6) {
         g.fill(i, gy + 1, i + 3, gy + 2, -7703984);
      }

      float u = (float)((Math.sin(t * 0.6) + 1.0) / 2.0);
      int alt = Math.round(u * 200.0F);
      int sy = gy - 8 - (int)(u * (h - 34));
      int sx = x + 22;
      g.fill(sx - 10, sy - 3, sx + 10, sy + 3, -7576540);
      g.fill(sx - 8, sy - 4, sx + 8, sy - 3, -2644662);
      g.fill(sx - 4, sy + 3, sx + 4, sy + 6, -10798558);
      g.fill(sx, sy - 10, sx + 1, sy - 4, -12899814);
      double mult = 1.0 + 0.5 * alt / 100.0;
      int rr = (int)(8.0 + 14.0 * mult);

      for (int k = 0; k < 3; k++) {
         float ph = (t * 0.8F + k / 3.0F) % 1.0F;
         int rad = (int)(rr * ph);
         int a = (int)(200.0F * (1.0F - ph));
         box(g, sx + rad, sy - 10 - 1, sx + rad + 1, sy - 10 + 2, a << 24 | 3042984);
         box(g, sx - rad, sy - 10 - 1, sx - rad + 1, sy - 10 + 2, a << 24 | 3042984);
      }

      small(g, f, alt + " blocks up", x + w - 50, y + 14, -12899814, 48);
      small(g, f, "range x" + String.format("%.1f", mult), x + w - 50, y + 21, -12678598, 48);
   }

   private static void jammer(GuiGraphics g, Font f, int x, int y, int w, int h, float t) {
      int cx = x + w / 2;
      int cy = y + h / 2;
      int r = Math.min(w, h) / 2 - 4;

      for (int i = 0; i < 360; i += 4) {
         dot(g, cx + (float)Math.cos(Math.toRadians(i)) * r, cy + (float)Math.sin(Math.toRadians(i)) * r, 1, -5228500);
      }

      int seed = (int)(t * 15.0F);

      for (int k = 0; k < 40; k++) {
         int s = seed * 31 + k * 2654435 & 2147483647;
         double a = s % 360 * Math.PI / 180.0;
         double d = (s >> 9) % 100 / 100.0 * r;
         dot(g, cx + (float)(Math.cos(a) * d), cy + (float)(Math.sin(a) * d), 1, -7703984);
      }

      float p = t * 0.7F % 1.0F;
      int pr = (int)(r * p);

      for (int i = 0; i < 360; i += 10) {
         dot(g, cx + (float)Math.cos(Math.toRadians(i)) * pr, cy + (float)Math.sin(Math.toRadians(i)) * pr, 1, (int)(180.0F * (1.0F - p)) << 24 | 11548716);
      }

      g.fill(cx - 3, cy - 2, cx + 3, cy + 3, -10851526);
      g.fill(cx - 2, cy - 5, cx - 1, cy - 2, -12899814);
      g.fill(cx + 1, cy - 5, cx + 2, cy - 2, -12899814);
      small(g, f, "48 blocks", cx + r * 7 / 10.0F, cy - r * 7 / 10.0F - 4.0F, -5228500, 30);
   }

   private static void yagi(GuiGraphics g, Font f, int x, int y, int w, int h, float t) {
      int cx = x + w / 3;
      int cy = y + h / 2;
      float unit = Math.min(w / 2.0F, h) / 1.9F * 0.95F;

      for (int i = 0; i < 360; i += 6) {
         dot(g, cx + (float)Math.cos(Math.toRadians(i)) * unit, cy + (float)Math.sin(Math.toRadians(i)) * unit, 1, -7703984);
      }

      for (int i = 0; i < 360; i += 3) {
         double a = Math.toRadians(i);
         double dx = Math.cos(a);
         double dz = Math.sin(a);
         float r = (float)(AntennaUtil.pattern(90.0F, dx, dz) * unit);
         dot(g, cx + (float)dx * r, cy + (float)dz * r, 1, -13734232);
      }

      g.fill(cx - 3, cy - 1, cx + 4, cy, -12899814);

      for (int k = -2; k <= 3; k += 2) {
         g.fill(cx + k, cy - 3 + Math.abs(k) / 2, cx + k + 1, cy + 3 - Math.abs(k) / 2, -12899814);
      }

      double a = t * 0.8;
      float rr = unit * 1.2F;
      float px = cx + (float)Math.cos(a) * rr;
      float py = cy + (float)Math.sin(a) * rr;
      boolean heard = AntennaUtil.pattern(90.0F, Math.cos(a), Math.sin(a)) * unit >= rr;
      g.fill((int)px - 1, (int)py - 1, (int)px + 2, (int)py + 2, heard ? -12678598 : -5228500);
      int lx = x + w * 2 / 3 + 2;
      small(g, f, "ahead x1.9", lx, y + 4, -13734232, w / 3 - 4);
      small(g, f, "sides: none", lx, y + 10, -13734232, w / 3 - 4);
      small(g, f, "behind: none", lx, y + 16, -13734232, w / 3 - 4);
      small(g, f, "plain mast x1", lx, y + 24, -7703984, w / 3 - 4);
      small(g, f, heard ? "radio: heard" : "radio: silent", lx, y + h - 8, heard ? -12678598 : -5228500, w / 3 - 4);
   }

   private static void relay(GuiGraphics g, Font f, int x, int y, int w, int h, float t) {
      int gy = y + h - 6;
      int n = 4;
      g.fill(x + 2, gy, x + w - 2, gy + 1, -12899814);
      int[] px = new int[n];
      String[] lab = new String[]{"station", "relay", "relay", "radio"};

      for (int i = 0; i < n; i++) {
         px[i] = x + 10 + i * (w - 20) / (n - 1);
         int th = i == 0 ? h - 20 : (i == n - 1 ? 6 : h - 26);
         g.fill(px[i], gy - th, px[i] + 1, gy, -12899814);
         g.fill(px[i] - 2, gy - th, px[i] + 3, gy - th + 1, -12899814);
         small(g, f, lab[i], px[i] - (i == n - 1 ? 14 : (i == 0 ? 0 : 6)), y + 2, -7703984, 26);
      }

      float hop = t * 0.9F % (n - 1);
      int k = (int)hop;
      float u = hop - k;
      int x0 = px[k];
      int x1 = px[k + 1];
      int sx = (int)(x0 + (x1 - x0) * u);
      int top0 = gy - (k == 0 ? h - 20 : h - 26);
      int top1 = k + 1 == n - 1 ? gy - 6 : gy - (h - 26);
      int sy = (int)(top0 + (top1 - top0) * u - Math.sin(u * Math.PI) * 8.0);
      dot(g, sx, sy, 3, -2522581);

      for (int j = 1; j < 4; j++) {
         float uu = u - j * 0.06F;
         if (uu < 0.0F) {
            break;
         }

         int tx = (int)(x0 + (x1 - x0) * uu);
         int ty = (int)(top0 + (top1 - top0) * uu - Math.sin(uu * Math.PI) * 8.0);
         dot(g, tx, ty, 1, 200 - j * 50 << 24 | 14254635);
      }
   }

   private static void power(GuiGraphics g, Font f, int x, int y, int w, int h, float t) {
      float cyc = t * 0.25F % 1.0F;
      boolean shaft = cyc < 0.55F;
      int dx = x + w / 2 - 8;
      int dy = y + h / 2 - 6;
      g.fill(dx, dy, dx + 16, dy + 12, -4617668);
      g.fill(dx + 1, dy + 1, dx + 15, dy + 11, -2644662);
      g.fill(dx + 5, dy + 3, dx + 11, dy + 9, !shaft && !(cyc < 0.97F) ? -5228500 : -12678598);
      int sy = dy + 5;
      g.fill(x + 4, sy, dx, sy + 2, -12899814);
      if (shaft) {
         for (int k = 0; k < 6; k++) {
            int sx = x + 4 + (int)((k * 8 + t * 40.0F) % (dx - x - 6));
            g.fill(sx, sy - 1, sx + 1, sy + 3, -2522581);
         }
      }

      small(g, f, shaft ? "shaft: turning" : "shaft: stopped", x + 4, sy + 6, shaft ? -12899814 : -5228500, dx - x - 6);
      int wx = dx + 16;
      int cx = x + w - 30;
      g.fill(wx, sy, wx + 3, sy + 2, -9539976);
      g.fill(wx + 3, sy, cx, sy + 1, -12899814);
      g.fill(wx + 3, sy + 2, cx, sy + 3, -12899814);
      g.fill(cx, sy - 4, cx + 10, sy + 6, -13734232);
      small(g, f, "grid", cx + 1, sy - 1, -1, 10);
      if (!shaft) {
         for (int k = 0; k < 4; k++) {
            int px = cx - 2 - (int)((k * 9 + t * 30.0F) % (cx - wx - 5));
            g.fill(px, sy, px + 2, sy + 1, -13734232);
         }
      }

      small(g, f, shaft ? "wire: waiting" : "wire: running it", wx + 2, sy + 6, shaft ? -7703984 : -13734232, x + w - wx - 4);
      small(g, f, shaft ? "running on rotation" : "running on electricity", dx - 12, dy - 7, -8765676, 60);
   }

   private static void dimlink(GuiGraphics g, Font f, int x, int y, int w, int h, float t) {
      int mid = x + w / 2;
      g.fill(x + 1, y + 1, mid - 1, y + h - 1, 440306234);
      g.fill(mid + 1, y + 1, x + w - 1, y + h - 1, 447756332);
      small(g, f, "Overworld", x + 4, y + 3, -12678598, mid - x - 8);
      small(g, f, "Nether / End", mid + 4, y + 3, -5228500, x + w - mid - 8);
      int gy = y + h - 6;
      g.fill(x + 2, gy, mid - 2, gy + 1, -12899814);
      g.fill(mid + 2, gy, x + w - 2, gy + 1, -12899814);

      for (int k = 0; k < h - 8; k += 2) {
         int off = (int)(Math.sin(t * 4.0F + k * 0.5) * 1.5);
         g.fill(mid - 1 + off, y + 4 + k, mid + 1 + off, y + 6 + k, -7588660);
      }

      int tx = x + 10;
      int r1 = mid - 16;
      int r2 = mid + 10;
      int rx = x + w - 14;
      g.fill(tx, gy - 18, tx + 1, gy, -12899814);
      g.fill(tx - 2, gy - 18, tx + 3, gy - 17, -12899814);
      g.fill(r1, gy - 7, r1 + 7, gy, -2644662);
      g.fill(r1 + 2, gy - 5, r1 + 5, gy - 2, -7588660);
      g.fill(r2, gy - 7, r2 + 7, gy, -2644662);
      g.fill(r2 + 2, gy - 5, r2 + 5, gy - 2, -7588660);
      g.fill(rx, gy - 5, rx + 6, gy, -8762838);
      g.fill(rx + 1, gy - 4, rx + 3, gy - 2, -2522581);
      small(g, f, "station", tx - 4, gy - 25, -7703984, 30);
      small(g, f, "radio", rx - 2, gy - 11, -7703984, 20);
      float u = t * 0.6F % 1.0F;
      float px;
      float py;
      if (u < 0.45F) {
         float k = u / 0.45F;
         px = tx + (r1 + 3 - tx) * k;
         py = gy - 18 + 12.0F * k - (float)Math.sin(k * Math.PI) * 6.0F;
      } else if (u < 0.55F) {
         px = mid;
         py = gy - 4;
      } else {
         float k = (u - 0.55F) / 0.45F;
         px = r2 + 3 + (rx + 3 - r2 - 3) * k;
         py = gy - 6 - (float)Math.sin(k * Math.PI) * 6.0F;
      }

      dot(g, px, py, 3, -2522581);
   }
}

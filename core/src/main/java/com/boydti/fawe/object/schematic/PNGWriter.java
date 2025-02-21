package com.boydti.fawe.object.schematic;

import com.boydti.fawe.FaweCache;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.MutableBlockVector;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardWriter;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.world.registry.WorldData;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.OutputStream;
import javax.imageio.ImageIO;
import javax.imageio.stream.ImageOutputStream;

public class PNGWriter implements ClipboardWriter {

    private final ImageOutputStream out;

    public PNGWriter(OutputStream out) throws IOException {
        this.out = ImageIO.createImageOutputStream(out);
    }


    public static Color getTopColor(int id, int data) {
        return FaweCache.getColor(id, data);
    }

    public static Color getRightColor(int id, int data) {
        return FaweCache.getColor(id, data).darker();
    }

    public static Color getLeftColor(int id, int data) {
        return FaweCache.getColor(id, data).brighter();
    }

    @Override
    public void write(Clipboard clipboard, WorldData data) throws IOException {
        Region region = clipboard.getRegion();
        int width = region.getWidth();
        int height = region.getHeight();
        int length = region.getLength();
        int imageSize = 1080;
        BufferedImage img = new BufferedImage(imageSize, imageSize, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img.createGraphics();
        double d = Math.min((double) imageSize / length, (double) imageSize / width) / 3;
        double d_2 = d / 2;
        double cx = (double) imageSize / 2;
        double cy = (double) imageSize / 2;

        int[] poly1X = new int[4];
        int[] poly1Y = new int[4];
        int[] poly2X = new int[4];
        int[] poly2Y = new int[4];
        int[] poly3X = new int[4];
        int[] poly3Y = new int[4];

        double[] dpxj = new double[length];
        double[] dpxi = new double[Math.max(256, width)];
        double[] dpyj = new double[length];
        double[] dpyi = new double[Math.max(256, width)];
        double[] hd = new double[256];
        for (int i = 0; i < hd.length; i++) {
        }
        for (int j = 0; j < dpxj.length; j++) {
            dpxj[j] = cx + j * d;
            dpyj[j] = imageSize / 2 + d + j * d_2;
        }
        for (int i = 0; i < Math.max(256, dpxi.length); i++) {
            dpxi[i] = i * d;
            dpyi[i] = i * d_2;
        }

        g2.setColor(new Color(0, 0, 0));
        g2.drawRect(0, 0, imageSize - 1, imageSize - 1);

        boolean fill = length * 4 < imageSize && width * 4 < imageSize;

        MutableBlockVector mutable = new MutableBlockVector(0, 0, 0);
        Vector mutableTop = new Vector(0, 0, 0);
        Vector mutableRight = new Vector(0, 0, 0);
        Vector mutableLeft = new Vector(0, 0, 0);

        Vector min = clipboard.getMinimumPoint();
        int y0 = min.getBlockY();
        int z0 = min.getBlockZ();
        int x0 = min.getBlockX();
        for (int x = x0; x < x0 + width; x++) {
            mutable.mutX(x);
            mutableTop.mutX(x);
            mutableRight.mutX(x);
            mutableLeft.mutX(x + 1);
            int xx = x - x0;
            double cpx1 = -dpxi[xx];
            double cpy1 = dpyi[xx];
            for (int z = z0; z < z0 + length; z++) {
                mutable.mutZ(z);
                mutableTop.mutZ(z);
                mutableRight.mutZ(z + 1);
                mutableLeft.mutZ(z);
                int zz = z - z0;
                double cpx = cpx1 + dpxj[zz];
                double cpy2 = cpy1 + dpyj[zz];
                for (int y = y0; y < y0 + height; y++) {
                    mutable.mutY(y);
                    BaseBlock block = clipboard.getBlock(mutable);
                    if (block.getId() == 0) {
                        continue;
                    }
                    mutableTop.mutY(y + 1);
                    mutableRight.mutY(y);
                    mutableLeft.mutY(y);
                    if (clipboard.getBlock(mutableTop).getId() != 0 && clipboard.getBlock(mutableRight) != EditSession.nullBlock && clipboard.getBlock(mutableLeft).getId() != 0) {
                        continue;
                    }
                    double cpy = cpy2 - dpxi[y - y0];
                    poly1X[0] = (int) (cpx);
                    poly1Y[0] = (int) (cpy);
                    poly1X[1] = (int) (cpx - d);
                    poly1Y[1] = (int) (cpy - d_2);
                    poly1X[2] = (int) (cpx);
                    poly1Y[2] = (int) (cpy - d);
                    poly1X[3] = (int) (cpx + d);
                    poly1Y[3] = (int) (cpy - d_2);

                    poly2X[0] = (int) (cpx);
                    poly2Y[0] = (int) (cpy);
                    poly2X[1] = (int) (cpx + d);
                    poly2Y[1] = (int) (cpy - d_2);
                    poly2X[2] = (int) (cpx + d);
                    poly2Y[2] = (int) (cpy + d_2 + dpxi[0]);
                    poly2X[3] = (int) (cpx);
                    poly2Y[3] = (int) (cpy + dpxi[1]);

                    poly3X[0] = (int) (cpx);
                    poly3Y[0] = (int) (cpy);
                    poly3X[1] = (int) (cpx - d);
                    poly3Y[1] = (int) (cpy - d_2);
                    poly3X[2] = (int) (cpx - d);
                    poly3Y[2] = (int) (cpy + d_2 + dpxi[0]);
                    poly3X[3] = (int) (cpx);
                    poly3Y[3] = (int) (cpy + dpxi[1]);

                    Color colorTop = getTopColor(block.getId(), block.getData());
                    Color colorRight = getRightColor(block.getId(), block.getData());
                    Color colorLeft = getLeftColor(block.getId(), block.getData());

                    if (fill) {
                        g2.setColor(colorTop);
                        g2.fillPolygon(poly1X, poly1Y, 4);
                        g2.setColor(colorRight);
                        g2.fillPolygon(poly2X, poly2Y, 4);
                        g2.setColor(colorLeft);
                        g2.fillPolygon(poly3X, poly3Y, 4);
                    } else {
                        g2.setColor(colorTop);
                        g2.drawPolygon(poly1X, poly1Y, 4);
                        g2.setColor(colorRight);
                        g2.drawPolygon(poly2X, poly2Y, 4);
                        g2.setColor(colorLeft);
                        g2.drawPolygon(poly3X, poly3Y, 4);
                    }
                }
            }
        }
        ImageIO.write(img, "png", out);
    }

    @Override
    public void close() throws IOException {
        out.close();
    }
}
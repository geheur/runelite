/*
 * Copyright (c) 2018, TheLonelyDev <https://github.com/TheLonelyDev>
 * Copyright (c) 2018, Adam <Adam@sigterm.info>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package net.runelite.client.plugins.groundmarkers;

import com.google.common.base.Strings;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.Stroke;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.util.Collection;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.Constants;
import static net.runelite.api.Constants.EXTENDED_SCENE_SIZE;
import static net.runelite.api.Constants.TILE_FLAG_BRIDGE;
import net.runelite.api.Perspective;
import static net.runelite.api.Perspective.*;
import net.runelite.api.Point;
import net.runelite.api.Scene;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayPriority;
import net.runelite.client.ui.overlay.OverlayUtil;

public class GroundMarkerOverlay extends Overlay
{
	private static final int MAX_DRAW_DISTANCE = 32;

	private final Client client;
	private final GroundMarkerConfig config;
	private final GroundMarkerPlugin plugin;

	@Inject
	private GroundMarkerOverlay(Client client, GroundMarkerConfig config, GroundMarkerPlugin plugin)
	{
		this.client = client;
		this.config = config;
		this.plugin = plugin;
		setPosition(OverlayPosition.DYNAMIC);
		setPriority(OverlayPriority.LOW);
		setLayer(OverlayLayer.ABOVE_SCENE);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		long t = System.nanoTime();
		final Collection<ColorTileMarker> points = plugin.getPoints();
		if (points.isEmpty())
		{
			return null;
		}

		Stroke stroke = new BasicStroke((float) config.borderWidth());
//		System.out.println(points.size());
		for (final ColorTileMarker point : points)
		{
			WorldPoint worldPoint = point.getWorldPoint();
			if (worldPoint.getPlane() != client.getPlane()) continue;

			Color tileColor = point.getColor();
			if (tileColor == null)
			{
				// If this is an old tile which has no color, use marker color
				tileColor = config.markerColor();
			}

			int x = worldPoint.getX();
			int y = worldPoint.getY();
			boolean n = false, e = false, s = false, w = false;
			boolean nw = false, ne = false, se = false, sw = false;
			boolean anyColorSouth = false, anyColorEast = false;
			for (ColorTileMarker colorTileMarker : points)
			{
				int xdiff = colorTileMarker.getWorldPoint().getX() - x;
				int ydiff = colorTileMarker.getWorldPoint().getY() - y;
				if (colorTileMarker.getWorldPoint().getPlane() != worldPoint.getPlane()) continue;
				boolean sameColor = colorTileMarker.getColor().equals(point.getColor());
//				if (xdiff == 1 && ydiff == 0) {
//					if (sameColor) e = true;
//					else anyColorEast = true;
//				}
//				if (xdiff == 0 && ydiff == -1) {
//					if (sameColor) s = true;
//					else anyColorSouth = true;
//				}
				if (sameColor)
				{
					if (xdiff == 1 && ydiff == 0) {
						e = true;
					}
					if (xdiff == 0 && ydiff == -1) {
						s = true;
					}
					if (xdiff == 0 && ydiff == 1) {
						n = true;
					}
					if (xdiff == -1 && ydiff == 0) {
						w = true;
					}
					if (xdiff == 1 && ydiff == 1) {
						ne = true;
					}
					if (xdiff == 1 && ydiff == -1) {
						se = true;
					}
					if (xdiff == -1 && ydiff == -1) {
						sw = true;
					}
					if (xdiff == -1 && ydiff == 1) {
						nw = true;
					}
				}
			}

			drawTile(graphics, worldPoint, tileColor, point.getLabel(), stroke, n, e, s, w, ne, nw, se, sw, anyColorSouth, anyColorEast);
		}

//		System.out.println((System.nanoTime() - t) / 1_000);
		return null;
	}

	private void drawTile(Graphics2D graphics, WorldPoint point, Color color, @Nullable String label, Stroke borderStroke, boolean n, boolean e, boolean s, boolean w, boolean ne, boolean nw, boolean se, boolean sw, boolean anyColorSouth, boolean anyColorEast)
	{
		WorldPoint playerLocation = client.getLocalPlayer().getWorldLocation();

		if (point.distanceTo(playerLocation) >= MAX_DRAW_DISTANCE)
		{
			return;
		}

		LocalPoint lp = LocalPoint.fromWorld(client, point);
		if (lp == null)
		{
			return;
		}

		graphics.setColor(color);
		final Stroke originalStroke = graphics.getStroke();
		graphics.setStroke(borderStroke);

//		graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
		Polygon poly = Perspective.getCanvasTilePoly(client, lp);
		Point2D.Float[] points = getCanvasTileAreaPoly(client, lp, 1, 1, client.getPlane(), 0);
		if (points != null && poly != null)
		{
			graphics.setColor(color);
			if (!e && !anyColorEast) {
//				graphics.setColor(Color.RED);
				Point2D.Float p1 = points[1];
				Point2D.Float p2 = points[2];
				graphics.draw(new Line2D.Double(p1.getX(), p1.getY(), p2.getX(), p2.getY()));
			}
			if (!n) {
//				graphics.setColor(Color.GREEN);
				Point2D.Float p1 = points[2];
				Point2D.Float p2 = points[3];
				graphics.draw(new Line2D.Double(p1.getX(), p1.getY(), p2.getX(), p2.getY()));
			}
			if (!w) {
//				graphics.setColor(Color.BLUE);
				Point2D.Float p1 = points[3];
				Point2D.Float p2 = points[0];
				graphics.draw(new Line2D.Double(p1.getX(), p1.getY(), p2.getX(), p2.getY()));
			}
			if (!s && !anyColorSouth) {
//				graphics.setColor(Color.YELLOW);
				Point2D.Float p1 = points[0];
				Point2D.Float p2 = points[1];
				graphics.draw(new Line2D.Double(p1.getX(), p1.getY(), p2.getX(), p2.getY()));
			}

			float l = 8;
			if (e) {
				if (!n && !ne) {
					Point2D.Float p1 = points[2];
					Point2D.Float p2 = points[1];
					double xdiff = p2.getX() - p1.getX();
					double ydiff = p2.getY() - p1.getY();
//					graphics.setColor(Color.RED);
					graphics.draw(new Line2D.Double(p1.getX(), p1.getY(), p1.getX() + (xdiff / l), p1.getY() + (ydiff / l)));
				}
				if (!s && !se) {
					Point2D.Float p1 = points[1];
					Point2D.Float p2 = points[2];
					double xdiff = p2.getX() - p1.getX();
					double ydiff = p2.getY() - p1.getY();
//					graphics.setColor(Color.RED);
					graphics.draw(new Line2D.Double(p1.getX(), p1.getY(), p1.getX() + (xdiff / l), p1.getY() + (ydiff / l)));
				}
			}
			if (n) {
				if (!e && !ne) {
					Point2D.Float p1 = points[2];
					Point2D.Float p2 = points[3];
					double xdiff = p2.getX() - p1.getX();
					double ydiff = p2.getY() - p1.getY();
//					graphics.setColor(Color.RED);
					graphics.draw(new Line2D.Double(p1.getX(), p1.getY(), p1.getX() + (xdiff / l), p1.getY() + (ydiff / l)));
				}
				if (!w && !nw) {
					Point2D.Float p1 = points[3];
					Point2D.Float p2 = points[2];
					double xdiff = p2.getX() - p1.getX();
					double ydiff = p2.getY() - p1.getY();
//					graphics.setColor(Color.RED);
					graphics.draw(new Line2D.Double(p1.getX(), p1.getY(), p1.getX() + (xdiff / l), p1.getY() + (ydiff / l)));
				}
			}

			if (n && ne && e) {
				Point2D.Float p1 = points[2];
				Point2D.Float p2 = points[1];
				double x = p1.getX();
				double y = p1.getY();
//					graphics.setColor(Color.RED);
//				int xdiff = (p2.getX() - x) / l;
//				int ydiff = (p2.getY() - y) / l;
//				graphics.drawLine(x - xdiff, y - ydiff, x + xdiff, y + ydiff);
//				graphics.drawLine(x + ydiff, y - xdiff, x - ydiff, y + xdiff);
				graphics.drawRect((int) (x - 1), (int) (y - 1), 0, 0);
			}
			graphics.setStroke(originalStroke);
//			OverlayUtil.renderPolygon(graphics, poly, new Color(0, 0, 0, 0), new Color(0, 0, 0, config.fillOpacity()), borderStroke);
		}

		if (!Strings.isNullOrEmpty(label))
		{
			Point canvasTextLocation = getCanvasTextLocation(client, graphics, lp, label, 0);
			if (canvasTextLocation != null)
			{
				OverlayUtil.renderTextLocation(graphics, canvasTextLocation, label, color);
			}
		}
	}

	public static Point2D.Float[] getCanvasTileAreaPoly(
		@Nonnull Client client,
		@Nonnull LocalPoint localLocation,
		int sizeX,
		int sizeY,
		int plane,
		int zOffset)
	{
		final int msx = localLocation.getSceneX() + ESCENE_OFFSET;
		final int msy = localLocation.getSceneY() + ESCENE_OFFSET;

		if (msx < 0 || msy < 0 || msx >= EXTENDED_SCENE_SIZE || msy >= EXTENDED_SCENE_SIZE)
		{
			// out of scene
			return null;
		}

		var scene = client.getScene();
		final byte[][][] tileSettings = scene.getExtendedTileSettings();

		int tilePlane = plane;
		if (plane < Constants.MAX_Z - 1 && (tileSettings[1][msx][msy] & TILE_FLAG_BRIDGE) == TILE_FLAG_BRIDGE)
		{
			tilePlane = plane + 1;
		}

		final int swX = localLocation.getX() - (sizeX * LOCAL_TILE_SIZE / 2);
		final int swY = localLocation.getY() - (sizeY * LOCAL_TILE_SIZE / 2);

		final int neX = localLocation.getX() + (sizeX * LOCAL_TILE_SIZE / 2);
		final int neY = localLocation.getY() + (sizeY * LOCAL_TILE_SIZE / 2);

		final int seX = swX;
		final int seY = neY;

		final int nwX = neX;
		final int nwY = swY;

		final int swHeight = getHeight(scene, swX, swY, tilePlane) - zOffset;
		final int nwHeight = getHeight(scene, nwX, nwY, tilePlane) - zOffset;
		final int neHeight = getHeight(scene, neX, neY, tilePlane) - zOffset;
		final int seHeight = getHeight(scene, seX, seY, tilePlane) - zOffset;

		Point2D.Float p1 = localToCanvas(client, swX, swY, swHeight);
		Point2D.Float p2 = localToCanvas(client, nwX, nwY, nwHeight);
		Point2D.Float p3 = localToCanvas(client, neX, neY, neHeight);
		Point2D.Float p4 = localToCanvas(client, seX, seY, seHeight);
		return new Point2D.Float[]{p1, p2, p3, p4};
//
//		if (p1 == null || p2 == null || p3 == null || p4 == null)
//		{
//			return null;
//		}
//
//		Polygon poly = new Polygon();
//		poly.addPoint(p1.getX(), p1.getY());
//		poly.addPoint(p2.getX(), p2.getY());
//		poly.addPoint(p3.getX(), p3.getY());
//		poly.addPoint(p4.getX(), p4.getY());
//
//		return poly;
	}

	private static int getHeight(@Nonnull Scene scene, int localX, int localY, int plane)
	{
		int sceneX = (localX >> LOCAL_COORD_BITS) + ESCENE_OFFSET;
		int sceneY = (localY >> LOCAL_COORD_BITS) + ESCENE_OFFSET;
		if (sceneX >= 0 && sceneY >= 0 && sceneX < Constants.EXTENDED_SCENE_SIZE && sceneY < Constants.EXTENDED_SCENE_SIZE)
		{
			int[][][] tileHeights = scene.getTileHeights();

			int x = localX & (LOCAL_TILE_SIZE - 1);
			int y = localY & (LOCAL_TILE_SIZE - 1);
			int var8 = x * tileHeights[plane][sceneX + 1][sceneY] + (LOCAL_TILE_SIZE - x) * tileHeights[plane][sceneX][sceneY] >> LOCAL_COORD_BITS;
			int var9 = tileHeights[plane][sceneX][sceneY + 1] * (LOCAL_TILE_SIZE - x) + x * tileHeights[plane][sceneX + 1][sceneY + 1] >> LOCAL_COORD_BITS;
			return (LOCAL_TILE_SIZE - y) * var8 + y * var9 >> LOCAL_COORD_BITS;
		}

		return 0;
	}

	private static final int ESCENE_OFFSET = (Constants.EXTENDED_SCENE_SIZE - Constants.SCENE_SIZE) / 2;

	@Nullable
	public static Point2D.Float localToCanvas(@Nonnull Client client, @Nonnull LocalPoint point, int plane)
	{
		return localToCanvas(client, point, plane, 0);
	}

	/**
	 * Translates two-dimensional ground coordinates within the 3D world to
	 * their corresponding coordinates on the game screen.
	 *
	 * @param client the game client
	 * @param point ground coordinate
	 * @param plane ground plane on the z axis
	 * @param zOffset distance from ground on the z axis
	 * @return a {@link Point} on screen corresponding to the position in
	 * 3D-space
	 */
	@Nullable
	public static Point2D.Float localToCanvas(@Nonnull Client client, @Nonnull LocalPoint point, int plane, int zOffset)
	{
		final int tileHeight = getTileHeight(client, point, plane);
		return localToCanvas(client, point.getX(), point.getY(), tileHeight - zOffset);
	}

	/**
	 * Translates three-dimensional local coordinates within the 3D world to
	 * their corresponding coordinates on the game screen.
	 *
	 * @param client the game client
	 * @param x ground coordinate on the x axis
	 * @param y ground coordinate on the y axis
	 * @param z
	 * @return a {@link Point} on screen corresponding to the position in
	 * 3D-space
	 */
	public static Point2D.Float localToCanvas(@Nonnull Client client, int x, int y, int z)
	{
		return client.isGpu() ? localToCanvasGpu(client, x, y, z) : localToCanvasCpu(client, x, y, z);
	}

	private static Point2D.Float localToCanvasCpu(Client client, int x, int y, int z)
	{
		if (x >= -ESCENE_OFFSET << LOCAL_COORD_BITS && y >= -ESCENE_OFFSET << LOCAL_COORD_BITS &&
			x <= SCENE_SIZE + ESCENE_OFFSET << LOCAL_COORD_BITS && y <= SCENE_SIZE + ESCENE_OFFSET << LOCAL_COORD_BITS)
		{
			x -= client.getCameraX();
			y -= client.getCameraY();
			z -= client.getCameraZ();

			final int cameraPitch = client.getCameraPitch();
			final int cameraYaw = client.getCameraYaw();

			final int pitchSin = SINE[cameraPitch];
			final int pitchCos = COSINE[cameraPitch];
			final int yawSin = SINE[cameraYaw];
			final int yawCos = COSINE[cameraYaw];

			final int
				x1 = x * yawCos + y * yawSin >> 16,
				y1 = y * yawCos - x * yawSin >> 16,
				y2 = z * pitchCos - y1 * pitchSin >> 16,
				z1 = y1 * pitchCos + z * pitchSin >> 16;

			if (z1 >= 50)
			{
				final int scale = client.getScale();
				final int pointX = client.getViewportWidth() / 2 + x1 * scale / z1;
				final int pointY = client.getViewportHeight() / 2 + y2 * scale / z1;
				return new Point2D.Float(
					pointX + client.getViewportXOffset(),
					pointY + client.getViewportYOffset()
				);
			}
		}

		return null;
	}

	private static Point2D.Float localToCanvasGpu(Client client, int x, int y, int z)
	{
		if (x >= -ESCENE_OFFSET << LOCAL_COORD_BITS && y >= -ESCENE_OFFSET << LOCAL_COORD_BITS &&
			x <= SCENE_SIZE + ESCENE_OFFSET << LOCAL_COORD_BITS && y <= SCENE_SIZE + ESCENE_OFFSET << LOCAL_COORD_BITS)
		{
			final double
				cameraPitch = client.getCameraFpPitch(),
				cameraYaw = client.getCameraFpYaw();

			final float
				fx = x - (float) client.getCameraFpX(),
				fy = y - (float) client.getCameraFpY(),
				fz = z - (float) client.getCameraFpZ(),
				pitchSin = (float) Math.sin(cameraPitch),
				pitchCos = (float) Math.cos(cameraPitch),
				yawSin = (float) Math.sin(cameraYaw),
				yawCos = (float) Math.cos(cameraYaw);

			final float
				x1 = fx * yawCos + fy * yawSin,
				y1 = fy * yawCos - fx * yawSin,
				y2 = fz * pitchCos - y1 * pitchSin,
				z1 = y1 * pitchCos + fz * pitchSin;

			if (z1 >= 50f)
			{
				final int scale = client.getScale();
				final float pointX = client.getViewportWidth() / 2f + x1 * scale / z1;
				final float pointY = client.getViewportHeight() / 2f + y2 * scale / z1;
				return new Point2D.Float(
					pointX + client.getViewportXOffset(),
					pointY + client.getViewportYOffset()
				);
			}
		}

		return null;
	}

}

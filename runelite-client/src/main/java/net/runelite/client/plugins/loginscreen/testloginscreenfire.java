package net.runelite.client.plugins.loginscreen;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.Timer;

public class testloginscreenfire
{
	private static final int FIRE_WIDTH = 128;
	private static final int FIRE_HEIGHT = 256;
	private static final int FIRE_DATA_ARRAY_SIZE = FIRE_WIDTH * FIRE_HEIGHT;
	private static final int FIRE_CLIP_ENDS = 1;
	private static final int FIRE_TOP = 9 - FIRE_CLIP_ENDS;

	private static final int PALETTE_SIZE = 256;

	private static final int SCREEN_WIDTH = 1000;

	//	static GraphicsDefaults spriteIds;
//	IndexedSprite[] sprites;
	int[] waviness = new int[FIRE_HEIGHT]; // horizontal distortion. One value per row.
	int wavinessCounter = 0; // x coordinate of a composition of sines function that is used to generate waviness.

	int[] colorPalette;
	int[] blackRedYellowWhite; // black -> red -> yellow -> white.
	int[] blackGreenTurquoiseWhite;
	int[] blackBluePurpleWhite;
	int greenFireTimer = 0;
	int blueFireTimer = 0;

	int[] fireIntensity; // Each value is between 0 and 255 inclusive and is used as both an index into colorPalette to determine the color and the transparency (high values are more transparent).
	int[] fireFadeoutTemporary; // Could more or less be an instance variable, but being a field prevents it from being allocated every client tick I guess?
	int[] spriteData; // Contains random noise and the sprite that appears in the flame. The implementation is that its values are subtracted from fireIntensity every tick, so a low value means that that section of the fire will fade out slower.
	int[] spriteTemporary; // Could more or less be an instance variable, but being a field prevents it from being allocated every 5ish seconds I guess?
	int spriteDataScrollPosition = 0;
	int ticksSinceLastFadeoutPass = 0;
	int lastClientTickDrawnOn = 0;

	testloginscreenfire(/*IndexedSprite[] var1*/) {
//		sprites = var1;
		initColors();
	}

	void initColors() {
		blackRedYellowWhite = new int[PALETTE_SIZE];

		// 64 shades of increasingly intense red.
		for (int i = 0; i < 64; ++i) {
			blackRedYellowWhite[i] = i * 0x00040000;
		}

		// Add green.
		for (int i = 0; i < 64; ++i) {
			blackRedYellowWhite[i + 64] = i * 0x00000400 + 0xff0000;
		}

		// Add blue
		for (int i = 0; i < 64; ++i) {
			blackRedYellowWhite[i + 128] = i * 4 + 0xffff00;
		}

		// pad.
		for (int i = 0; i < 64; ++i) {
			blackRedYellowWhite[i + 192] = 0xffffff;
		}

		blackGreenTurquoiseWhite = new int[PALETTE_SIZE];

		for (int i = 0; i < 64; ++i) {
			blackGreenTurquoiseWhite[i] = i * 0x00000400;
		}

		for (int i = 0; i < 64; ++i) {
			blackGreenTurquoiseWhite[i + 64] = i * 4 + 0x00ff00;
		}

		for (int i = 0; i < 64; ++i) {
			blackGreenTurquoiseWhite[i + 128] = i * 0x00040000 + 65535;
		}

		for (int i = 0; i < 64; ++i) {
			blackGreenTurquoiseWhite[i + 192] = 0xffffff;
		}

		blackBluePurpleWhite = new int[PALETTE_SIZE];

		for (int i = 0; i < 64; ++i) {
			blackBluePurpleWhite[i] = i * 4;
		}

		for (int i = 0; i < 64; ++i) {
			blackBluePurpleWhite[i + 64] = i * 0x00040000 + 255;
		}

		for (int i = 0; i < 64; ++i) {
			blackBluePurpleWhite[i + 128] = i * 0x00000400 + 0xff00ff;
		}

		for (int i = 0; i < 64; ++i) {
			blackBluePurpleWhite[i + 192] = 0xffffff;
		}

		colorPalette = new int[PALETTE_SIZE];
		spriteDataScrollPosition = 0;
		spriteData = new int[FIRE_DATA_ARRAY_SIZE];
		spriteTemporary = new int[FIRE_DATA_ARRAY_SIZE];
//		method2215((IndexedSprite)null);
		generateFirePattern();
		fireIntensity = new int[FIRE_DATA_ARRAY_SIZE];
		fireFadeoutTemporary = new int[FIRE_DATA_ARRAY_SIZE];
	}

	void releaseMemory() {
		blackRedYellowWhite = null;
		blackGreenTurquoiseWhite = null;
		blackBluePurpleWhite = null;
		colorPalette = null;
		spriteData = null;
		spriteTemporary = null;
		fireIntensity = null;
		fireFadeoutTemporary = null;
		spriteDataScrollPosition = 0;
		ticksSinceLastFadeoutPass = 0;
	}

	void draw(int xPos, int clientCycle, Graphics g) {
		if (fireIntensity == null) {
			initColors();
		}

		if (lastClientTickDrawnOn == 0) {
			lastClientTickDrawnOn = clientCycle;
		}

		int ticksElapsed = clientCycle - lastClientTickDrawnOn;
		// CPU saving measure?
		if (ticksElapsed >= FIRE_HEIGHT) {
			ticksElapsed = 0;
		}

		lastClientTickDrawnOn = clientCycle;
		if (ticksElapsed > 0) {
			updateFire(ticksElapsed);
		}

		colorAndDrawFire(xPos, g);
	}

	final void updateFire(int clientTicksSinceLastDraw) {
		int stepsProgressed = clientTicksSinceLastDraw * FIRE_WIDTH;
		spriteDataScrollPosition += stepsProgressed;
		// every 256 client ticks, do a rune.
		if (spriteDataScrollPosition > FIRE_DATA_ARRAY_SIZE) {
			spriteDataScrollPosition -= FIRE_DATA_ARRAY_SIZE;
//			method2215(sprites[(int)(Math.random() * 12.0D)]);
			generateFirePattern();
		}

		// pull data "up" (relative to flame direction) to its new position, in field1225.
		// Does not fill the "bottom" with new data.
		// Reduces intensity in parts of the fire that are not part of the sprite.
		int var2 = 0;
		int var4 = (FIRE_HEIGHT - clientTicksSinceLastDraw) * FIRE_WIDTH;
		for (int i = 0; i < var4; ++i) {
			int var6 = fireIntensity[stepsProgressed + var2] - spriteData[var2 + spriteDataScrollPosition & FIRE_DATA_ARRAY_SIZE - 1] * clientTicksSinceLastDraw / 6;
			if (var6 < 0) {
				var6 = 0;
			}

			fireIntensity[var2++] = var6;
		}

		// fill bottom of field1225.
		// The gutters (10 px on each side) are filled with 0, the pixels inbetween are a 50% chance to be 255 and a 50% chance to be 0.
		int rightSideMinusTen = FIRE_WIDTH - 10;
		for (int row = FIRE_HEIGHT - clientTicksSinceLastDraw; row < FIRE_HEIGHT; ++row) {
			int rowIndex = row * FIRE_WIDTH;

			for (int col = 0; col < FIRE_WIDTH; ++col) {
				int var10 = (int)(Math.random() * 100.0D);
				if (var10 < 50 && col > 10 && col < rightSideMinusTen) {
					fireIntensity[rowIndex + col] = 255;
				} else {
					fireIntensity[rowIndex + col] = 0;
				}
			}
		}

		if (greenFireTimer > 0) {
			greenFireTimer -= clientTicksSinceLastDraw * 4;
		}

		if (blueFireTimer > 0) {
			blueFireTimer -= clientTicksSinceLastDraw * 4;
		}

		if (greenFireTimer == 0 && blueFireTimer == 0) {
			int var7 = (int)(Math.random() * (double)(2000 / clientTicksSinceLastDraw));
			if (var7 == 0) {
				greenFireTimer = 1024;
			}

			if (var7 == 1) {
				blueFireTimer = 1024;
			}
		}

		// Pull waviness data "up".
		for (int i = 0; i < FIRE_HEIGHT - clientTicksSinceLastDraw; ++i) {
			waviness[i] = waviness[i + clientTicksSinceLastDraw];
		}

		// some kind of taylor series? https://www.desmos.com/calculator/m4cgbrwe8u
		// sin(x / 14) * 16 + sin(x / 15) * 14 + sin(x / 16) * 12
		for (int i = FIRE_HEIGHT - clientTicksSinceLastDraw; i < FIRE_HEIGHT; ++i) {
			waviness[i] = (int)(
				Math.sin((double) wavinessCounter / 14.0D) * 16.0D +
				Math.sin((double) wavinessCounter / 15.0D) * 14.0D +
				Math.sin((double) wavinessCounter / 16.0D) * 12.0D
			);
			++wavinessCounter;
		}

		ticksSinceLastFadeoutPass += clientTicksSinceLastDraw;
		// rate limit to every other tick?
		int var7 = ((lastClientTickDrawnOn & 1) + clientTicksSinceLastDraw) / 2;
		if (var7 > 0) {
			// randomly insert 192s in the bottom half of the flame not within 2px of the sides.
			short var16 = FIRE_HEIGHT - 128;
			byte var17 = 2;
			int var10 = FIRE_WIDTH - var17 - var17;
			for (int i = 0; i < ticksSinceLastFadeoutPass * 100; ++i) {
				int random2To126 = (int)(Math.random() * (double)var10) + var17;
				int random128To256 = (int)(Math.random() * (double)var16) + var16;
				fireIntensity[random2To126 + (random128To256 << 7)] = 192;
			}

			ticksSinceLastFadeoutPass = 0;

			for (int row = 0; row < FIRE_HEIGHT; ++row) {
				int var12 = 0;
				int rowOffset = row * FIRE_WIDTH;

				for (int col = -var7; col < FIRE_WIDTH; ++col) {
					int offset = rowOffset + col;
					if (col + var7 < FIRE_WIDTH) {
						var12 += fireIntensity[offset + var7];
					}

					if (col - (var7 + 1) >= 0) {
						var12 -= fireIntensity[offset - (var7 + 1)];
					}

					if (col >= 0) {
						fireFadeoutTemporary[offset] = var12 / (var7 * 2 + 1);
					}
				}
			}

			for (int col = 0; col < FIRE_WIDTH; ++col) {
				int var12 = 0;

				for (int row = -var7; row < FIRE_HEIGHT; ++row) {
					int var14 = row * FIRE_WIDTH;
					if (var7 + row < FIRE_HEIGHT) {
						var12 += fireFadeoutTemporary[col + var14 + var7 * FIRE_WIDTH];
					}

					if (row - (var7 + 1) >= 0) {
						var12 -= fireFadeoutTemporary[var14 + col - (var7 + 1) * FIRE_WIDTH];
					}

					if (row >= 0) {
						fireIntensity[var14 + col] = var12 / (var7 * 2 + 1);
					}
				}
			}
		}

	}

	final void colorAndDrawFire(int xPos, Graphics g) {
		if (greenFireTimer > 0) {
			updateColorPalette(greenFireTimer, blackGreenTurquoiseWhite);
		} else if (blueFireTimer > 0) {
			updateColorPalette(blueFireTimer, blackBluePurpleWhite);
		} else {
			for (int i = 0; i < PALETTE_SIZE; ++i) {
				colorPalette[i] = blackRedYellowWhite[i];
			}
		}

		drawToScreen(xPos, g);
	}

	/**
	 * Controls fading to and from alt colors, and holding alt color.
	 */
	final void updateColorPalette(int fireTimer, int[] altPalette) {
		for (int i = 0; i < PALETTE_SIZE; ++i) {
			if (fireTimer > 768) {
				colorPalette[i] = interpolateColor(blackRedYellowWhite[i], altPalette[i], 1024 - fireTimer);
			} else if (fireTimer > 256) {
				colorPalette[i] = altPalette[i];
			} else {
				colorPalette[i] = interpolateColor(altPalette[i], blackRedYellowWhite[i], 256 - fireTimer);
			}
		}

	}

	/**
	 * @param from color
	 * @param to color
	 * @param progress 0-255
	 */
	final int interpolateColor(int from, int to, int progress) {
		int antiProgress = 256 - progress;
		return (progress * (to & 0x00ff00) + antiProgress * (from & 0x00ff00) & 0xff0000) +
			(antiProgress * (from & 0xff00ff) + progress * (to & 0xff00ff) & 0xff00ff00) >> 8;
	}

	private final BufferedImage image = new BufferedImage(SCREEN_WIDTH, 400, BufferedImage.TYPE_INT_RGB);
	{
		Graphics graphics = image.getGraphics();
		graphics.setColor(Color.BLACK);
		graphics.fillRect(0, 0, image.getWidth(), image.getHeight());
	}

	void drawToScreen(int xPos, Graphics g) {
		int var2 = 0;
		Graphics graphics = image.getGraphics();
		graphics.setColor(Color.BLACK);
		graphics.fillRect(0, 0, image.getWidth(), image.getHeight());

		Graphics g2 = image.getGraphics();
		// 1 is the top 254 is the bottom of the fire.
		for (int row = FIRE_CLIP_ENDS; row < FIRE_HEIGHT - FIRE_CLIP_ENDS; ++row) {
			// (256 - row) / 256 is a coefficient that makes the fire wavier at the bottom.
			int xOffsetWaviness = (FIRE_HEIGHT - row) * waviness[row] / FIRE_HEIGHT;
			int fireXPosition = xOffsetWaviness + xPos;

			int rowClipLeft = 0;
			int rowClipRight = FIRE_WIDTH;
//			// I think we are skipping drawing the offscreen part here.
//			if (fireXPosition < 0) {
//				rowClipLeft = -fireXPosition;
//				fireXPosition = 0;
//			}
//			// skip the part clipped off by the left side of the screen.
//			var2 += rowClipLeft;
//			if (fireXPosition + FIRE_WIDTH /* fire width */ >= SCREEN_WIDTH) {
//				rowClipRight = SCREEN_WIDTH - fireXPosition;
//			}

			int rasterIndex = fireXPosition + (row + FIRE_TOP - FIRE_CLIP_ENDS) * SCREEN_WIDTH;

			for (int col = rowClipLeft; col < rowClipRight; ++col) {
				int intensity = fireIntensity[var2++];
				int x = rasterIndex % SCREEN_WIDTH;
				if (intensity != 0/* && x >= Rasterizer2D.Rasterizer2D_xClipStart && x < Rasterizer2D.Rasterizer2D_xClipEnd*/) {
					int notIntensity = 256 - intensity;
					int fireColor = colorPalette[intensity];
					int y = rasterIndex / 1000;
					int currentColor = image.getRGB(x, y);
					int color = 0xff000000 | (intensity * (fireColor & 0x00ff00) + notIntensity * (currentColor & 0x00ff00) & 0xff0000) + ((fireColor & 0xff00ff) * intensity + (currentColor & 0xff00ff) * notIntensity & 0xff00ff00) >> 8;
					g2.setColor(new Color(color));
					g2.fillRect(x, y, 1, 1);
//				Message.rasterProvider.pixels[var8++] = color;
				}
				++rasterIndex;
			}

			// skip the section clipped by the right side of the screen.
//			var2 += FIRE_WIDTH - rowClipRight;
		}
		g.drawImage(image, 0, 0, null);

		g.setColor(Color.BLACK);
		g.fillRect(200, FIRE_TOP - 1, 200, 300);
		for (int row = 0; row < FIRE_HEIGHT; row++)
		{
			for (int col = 0; col < FIRE_WIDTH; col++)
			{
				int index = row * FIRE_WIDTH + col;
				g.setColor(new Color(colorPalette[spriteData[(index + spriteDataScrollPosition) % FIRE_DATA_ARRAY_SIZE]]));
				g.fillRect(col + 200, row + FIRE_TOP - 1, 1, 1);
			}
		}

	}

	int spriteIndex = 0;
	final void generateFirePattern(/*IndexedSprite var1*/) {
		Arrays.fill(spriteData, 0);
//		for (int i = 0; i < spriteData.length; ++i) {
//			spriteData[i] = 0;
//		}

		// add random noise.
		for (int c = 0; c < 5000; ++c) {
			int i = (int)(Math.random() * FIRE_WIDTH * FIRE_HEIGHT);
			spriteData[i] = (int)(Math.random() * 256.0D);
		}

		// Blur the random noise by assigning each pixel to the average of its 4 neighbors, 20 times.
		for (int passes = 0; passes < 20; ++passes) {
			for (int row = 1; row < FIRE_HEIGHT - 1; ++row) {
				for (int col = 1; col < FIRE_WIDTH - 1; ++col) {
					int i = col + row * FIRE_WIDTH;
					spriteTemporary[i] = (spriteData[i + FIRE_WIDTH] + spriteData[i - FIRE_WIDTH] + spriteData[i + 1] + spriteData[i - 1]) / 4;
				}
			}

			int[] temp = spriteData;
			spriteData = spriteTemporary;
			spriteTemporary = temp;
		}

		BufferedImage read = null;
//		try
//		{
//			read = ImageIO.read(this.getClass().getResource("./Runelite.png"));
//				int var2 = 0;
//
//			for (int var3 = 0; var3 < read.getHeight(); ++var3) {
//				for (int var4 = 0; var4 < read.getWidth(); ++var4) {
//					int rgb = read.getRGB(var4, var3) & 0xffffff;
//					int var5 = var4 + 0;
//					int var6 = var3 + 0;
//					int var7 = var5 + (var6 << 7);
//					if (rgb == 0x0000ff) {
//						spriteData[var7] = 0;
//					} else if (rgb == 0xff0000) {
//						spriteData[var7] = 255;
//					}
//				}
//			}
//		}
//		catch (IOException e)
//		{
//			e.printStackTrace();
//		}
//		for (int i = 0; i < 100; i++)
//		{
//			for (int i1 = 0; i1 < 100; i1++)
//			{
//				spriteData[i * 128 + i1] = 0;
//			}
//		}

		if (true/*var1 != null*/) {
			try {
				read = ImageIO.read(this.getClass().getResource("./501-" + (spriteIndex++ % 12) + ".png"));
				System.out.println("in here " + spriteIndex + " " + read.getHeight() + " " + read.getWidth());
				int var2 = 0;

				for (int var3 = 0; var3 < read.getHeight(); ++var3) {
					for (int var4 = 0; var4 < read.getWidth(); ++var4) {
//						System.out.println("x " + var3 + " y " + var4 + " " + read.getRGB(var4, var3));
						var2++;
						if (read.getRGB(var4, var3) != 0) {
							int var5 = var4 + 16;
							int var6 = var3 + 16;
							int var7 = var5 + (var6 << 7);
							spriteData[var7] = 0;
						}
					}
				}
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}
		}

	}

	public static void main(String[] args) throws IOException
	{
		List<String> strings = Files.readAllLines(Path.of("C:\\Users\\samue\\.runelite\\profiles2\\essential plugin dev-76235643409700.properties"));
		for (String string : strings)
		{

		}
		JFrame fire_test = new JFrame("fire test");
		fire_test.setSize(500, 500);
		testloginscreenfire testloginscreenfire = new testloginscreenfire();
		JPanel jPanel = new JPanel() {
			@Override
			public void paintComponent(Graphics g) {
				testloginscreenfire.draw(0, (int) (System.currentTimeMillis() / 20), g);
			}
		};

		Timer timer=new Timer(20, (e) -> fire_test.repaint());
		timer.start();// Start the timer here.

		fire_test.add(jPanel);
		fire_test.setVisible(true);
	}
}

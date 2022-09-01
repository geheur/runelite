package net.runelite.client.plugins.loginscreen;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
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
	int[] waviness; // horizontal distortion?
	int wavinessCounter; // related to waviness. Does not reset at 256.

	int[] colorPalette;
	int[] blackRedYellowWhite; // black -> red -> yellow -> white.
	int[] blackGreenTurquoiseWhite;
	int[] blackBluePurpleWhite;
	int greenFireTimer;
	int blueFireTimer;

	int[] fireIntensity;
	int[] fireFadeoutTemporary;
	int[] spriteData;
	int[] spriteTemporary; // Could more or less be an instance variable, but being a field prevents it from being allocated every 5ish seconds I guess?
	int fireDataRowIndex;
	int ticksSinceLastFadeoutPass;
	int lastClientTickDrawnOn;

	testloginscreenfire(/*IndexedSprite[] var1*/) {
		waviness = new int[FIRE_HEIGHT]; // L: 13
		wavinessCounter = 0; // L: 14
		greenFireTimer = 0;
		blueFireTimer = 0; // L: 20
		fireDataRowIndex = 0;
		ticksSinceLastFadeoutPass = 0;
		lastClientTickDrawnOn = 0;
//		sprites = var1; // L: 30
		initColors(); // L: 31
	} // L: 32

	void initColors() {
		blackRedYellowWhite = new int[FIRE_HEIGHT]; // L: 35

		// 64 shades of increasingly intense red.
		for (int i = 0; i < 64; ++i) { // L: 36
			blackRedYellowWhite[i] = i * 0x00040000;
		}

		// Add green.
		for (int i = 0; i < 64; ++i) { // L: 37
			blackRedYellowWhite[i + 64] = i * 0x00000400 + 0xff0000;
		}

		// Add blue
		for (int i = 0; i < 64; ++i) { // L: 38
			blackRedYellowWhite[i + 128] = i * 4 + 0xffff00;
		}

		// pad.
		for (int i = 0; i < 64; ++i) { // L: 39
			blackRedYellowWhite[i + 192] = 0xffffff;
		}

		blackGreenTurquoiseWhite = new int[FIRE_HEIGHT]; // L: 40

		for (int i = 0; i < 64; ++i) { // L: 41
			blackGreenTurquoiseWhite[i] = i * 0x00000400;
		}

		for (int i = 0; i < 64; ++i) { // L: 42
			blackGreenTurquoiseWhite[i + 64] = i * 4 + 0x00ff00;
		}

		for (int i = 0; i < 64; ++i) { // L: 43
			blackGreenTurquoiseWhite[i + 128] = i * 0x00040000 + 65535;
		}

		for (int i = 0; i < 64; ++i) { // L: 44
			blackGreenTurquoiseWhite[i + 192] = 0xffffff;
		}

		blackBluePurpleWhite = new int[FIRE_HEIGHT]; // L: 45

		for (int i = 0; i < 64; ++i) { // L: 46
			blackBluePurpleWhite[i] = i * 4;
		}

		for (int i = 0; i < 64; ++i) { // L: 47
			blackBluePurpleWhite[i + 64] = i * 0x00040000 + 255;
		}

		for (int i = 0; i < 64; ++i) { // L: 48
			blackBluePurpleWhite[i + 128] = i * 0x00000400 + 0xff00ff;
		}

		for (int i = 0; i < 64; ++i) { // L: 49
			blackBluePurpleWhite[i + 192] = 0xffffff;
		}

		colorPalette = new int[PALETTE_SIZE]; // L: 50
		fireDataRowIndex = 0; // L: 51
		spriteData = new int[FIRE_DATA_ARRAY_SIZE]; // L: 52
		spriteTemporary = new int[FIRE_DATA_ARRAY_SIZE]; // L: 53
//		method2215((IndexedSprite)null); // L: 54
		method2215();
		fireIntensity = new int[FIRE_DATA_ARRAY_SIZE]; // L: 55
		fireFadeoutTemporary = new int[FIRE_DATA_ARRAY_SIZE]; // L: 56
	} // L: 57

	void reset() {
		blackRedYellowWhite = null; // L: 60
		blackGreenTurquoiseWhite = null; // L: 61
		blackBluePurpleWhite = null; // L: 62
		colorPalette = null; // L: 63
		spriteData = null; // L: 64
		spriteTemporary = null; // L: 65
		fireIntensity = null; // L: 66
		fireFadeoutTemporary = null; // L: 67
		fireDataRowIndex = 0; // L: 68
		ticksSinceLastFadeoutPass = 0; // L: 69
	} // L: 70

	void draw(int xPos, int clientCycle, Graphics g) {
		if (fireIntensity == null) { // L: 73
			initColors(); // L: 74
		}

		if (lastClientTickDrawnOn == 0) { // L: 76
			lastClientTickDrawnOn = clientCycle; // L: 77
		}

		int ticksElapsed = clientCycle - lastClientTickDrawnOn; // L: 79
		// CPU saving measure?
		if (ticksElapsed >= 256) { // L: 80
			ticksElapsed = 0;
		}

		lastClientTickDrawnOn = clientCycle; // L: 81
		if (ticksElapsed > 0) { // L: 82
			updateFire(ticksElapsed); // L: 83
		}

		colorAndDrawFire(xPos, g); // L: 85
	} // L: 86

	final void updateFire(int clientTicksSinceLastDraw) {
		int stepsProgressed = clientTicksSinceLastDraw * FIRE_WIDTH; // L: 96
		fireDataRowIndex += stepsProgressed; // L: 89
		// every 256 client ticks, do a rune.
		if (fireDataRowIndex > FIRE_DATA_ARRAY_SIZE) { // L: 90
			fireDataRowIndex -= FIRE_DATA_ARRAY_SIZE; // L: 91
//			method2215(sprites[(int)(Math.random() * 12.0D)]); // L: 93
			method2215();
		}

		// pull data "up" (relative to flame direction) to its new position, in field1225.
		// Does not fill the "bottom" with new data.
		// Also applies some kind of transformation.
		int var2 = 0; // L: 95
		int var4 = (FIRE_HEIGHT - clientTicksSinceLastDraw) * FIRE_WIDTH; // L: 97
		for (int i = 0; i < var4; ++i) { // L: 98
			int var6 = fireIntensity[stepsProgressed + var2] - spriteData[var2 + fireDataRowIndex & FIRE_DATA_ARRAY_SIZE - 1] * clientTicksSinceLastDraw / 6; // L: 99
			if (var6 < 0) { // L: 100
				var6 = 0;
			}

			fireIntensity[var2++] = var6; // L: 101
		}

		// fill bottom of field1225.
		// The gutters (10 px on each side) are filled with 0, the pixels inbetween are a 50% chance to be 255 and a 50% chance to be 0.
		int rightSideMinusTen = FIRE_WIDTH - 10; // L: 104
		for (int row = FIRE_HEIGHT - clientTicksSinceLastDraw; row < FIRE_HEIGHT; ++row) { // L: 105
			int rowIndex = row * FIRE_WIDTH; // L: 106

			for (int col = 0; col < FIRE_WIDTH; ++col) { // L: 107
				int var10 = (int)(Math.random() * 100.0D); // L: 108
				if (var10 < 50 && col > 10 && col < rightSideMinusTen) { // L: 109
					fireIntensity[rowIndex + col] = 255;
				} else {
					fireIntensity[rowIndex + col] = 0; // L: 110
				}
			}
		}

		if (greenFireTimer > 0) { // L: 113
			greenFireTimer -= clientTicksSinceLastDraw * 4;
		}

		if (blueFireTimer > 0) { // L: 114
			blueFireTimer -= clientTicksSinceLastDraw * 4;
		}

		if (greenFireTimer == 0 && blueFireTimer == 0) { // L: 115
			int var7 = (int)(Math.random() * (double)(2000 / clientTicksSinceLastDraw)); // L: 116
			if (var7 == 0) { // L: 117
				greenFireTimer = 1024;
			}

			if (var7 == 1) { // L: 118
				blueFireTimer = 1024;
			}
		}

		// Pull waviness data "up".
		for (int i = 0; i < 256 - clientTicksSinceLastDraw; ++i) { // L: 120
			waviness[i] = waviness[i + clientTicksSinceLastDraw];
		}

		// some kind of taylor series? https://www.desmos.com/calculator/m4cgbrwe8u
		// sin(x / 14) * 16 + sin(x / 15) * 14 + sin(x / 16) * 12
		for (int i = 256 - clientTicksSinceLastDraw; i < 256; ++i) { // L: 121
			waviness[i] = (int)(
				Math.sin((double) wavinessCounter / 14.0D) * 16.0D +
				Math.sin((double) wavinessCounter / 15.0D) * 14.0D +
				Math.sin((double) wavinessCounter / 16.0D) * 12.0D
			); // L: 122
			++wavinessCounter; // L: 123
		}

		ticksSinceLastFadeoutPass += clientTicksSinceLastDraw; // L: 125
		// rate limit to every other tick?
		int var7 = ((lastClientTickDrawnOn & 1) + clientTicksSinceLastDraw) / 2; // L: 126
		if (var7 > 0) { // L: 127
			// randomly insert 192s in the bottom half of the flame not within 2px of the sides.
			short var16 = 128; // L: 128
			byte var17 = 2; // L: 129
			int var10 = 128 - var17 - var17; // L: 130
			for (int i = 0; i < ticksSinceLastFadeoutPass * 100; ++i) { // L: 131
				int random2To126 = (int)(Math.random() * (double)var10) + var17; // L: 132
				int random128To256 = (int)(Math.random() * (double)var16) + var16; // L: 133
				fireIntensity[random2To126 + (random128To256 << 7)] = 192; // L: 134
			}

			ticksSinceLastFadeoutPass = 0; // L: 136

			for (int row = 0; row < FIRE_HEIGHT; ++row) { // L: 137
				int var12 = 0; // L: 138
				int rowOffset = row * FIRE_WIDTH; // L: 139

				for (int col = -var7; col < FIRE_WIDTH; ++col) { // L: 140
					int offset = rowOffset + col;
					if (col + var7 < FIRE_WIDTH) { // L: 141
						var12 += fireIntensity[offset + var7];
					}

					if (col - (var7 + 1) >= 0) { // L: 142
						var12 -= fireIntensity[offset - (var7 + 1)];
					}

					if (col >= 0) { // L: 143
						fireFadeoutTemporary[offset] = var12 / (var7 * 2 + 1);
					}
				}
			}

			for (int col = 0; col < FIRE_WIDTH; ++col) { // L: 146
				int var12 = 0; // L: 147

				for (int row = -var7; row < FIRE_HEIGHT; ++row) { // L: 148
					int var14 = row * FIRE_WIDTH; // L: 149
					if (var7 + row < FIRE_HEIGHT) { // L: 150
						var12 += fireFadeoutTemporary[col + var14 + var7 * 128];
					}

					if (row - (var7 + 1) >= 0) { // L: 151
						var12 -= fireFadeoutTemporary[var14 + col - (var7 + 1) * 128];
					}

					if (row >= 0) { // L: 152
						fireIntensity[var14 + col] = var12 / (var7 * 2 + 1);
					}
				}
			}
		}

	} // L: 156

	final void colorAndDrawFire(int xPos, Graphics g) {
		if (greenFireTimer > 0) { // L: 165
			updateColorPalette(greenFireTimer, blackGreenTurquoiseWhite); // L: 166
		} else if (blueFireTimer > 0) { // L: 168
			updateColorPalette(blueFireTimer, blackBluePurpleWhite); // L: 169
		} else {
			for (int i = 0; i < PALETTE_SIZE; ++i) { // L: 172
				colorPalette[i] = blackRedYellowWhite[i];
			}
		}

		drawToScreen(xPos, g); // L: 174
	} // L: 175

	/**
	 * Controls fading to and from alt colors, and holding alt color.
	 */
	final void updateColorPalette(int fireTimer, int[] altPalette) {
		for (int i = 0; i < PALETTE_SIZE; ++i) { // L: 179
			if (fireTimer > 768) { // L: 180
				colorPalette[i] = interpolateColor(blackRedYellowWhite[i], altPalette[i], 1024 - fireTimer);
			} else if (fireTimer > 256) {
				colorPalette[i] = altPalette[i]; // L: 181
			} else {
				colorPalette[i] = interpolateColor(altPalette[i], blackRedYellowWhite[i], 256 - fireTimer); // L: 182
			}
		}

	} // L: 184

	/**
	 * @param from color
	 * @param to color
	 * @param progress 0-255
	 */
	final int interpolateColor(int from, int to, int progress) {
		int antiProgress = 256 - progress; // L: 159
		return (progress * (to & 0x00ff00) + antiProgress * (from & 0x00ff00) & 0xff0000) +
			(antiProgress * (from & 0xff00ff) + progress * (to & 0xff00ff) & 0xff00ff00) >> 8; // L: 160
	}

	private final BufferedImage image = new BufferedImage(SCREEN_WIDTH, 400, BufferedImage.TYPE_INT_RGB);
	{
		Graphics graphics = image.getGraphics();
		graphics.setColor(Color.BLACK);
		graphics.fillRect(0, 0, image.getWidth(), image.getHeight());
	}

	void drawToScreen(int xPos, Graphics g) {
		int var2 = 0; // L: 187
//		Graphics graphics = image.getGraphics();
//		graphics.setColor(Color.BLACK);
//		graphics.fillRect(0, 0, image.getWidth(), image.getHeight());

		Graphics g2 = image.getGraphics();
		// 1 is the top 254 is the bottom of the fire.
		for (int row = FIRE_CLIP_ENDS; row < FIRE_HEIGHT - FIRE_CLIP_ENDS; ++row) { // L: 188
			// (256 - row) / 256 is a coefficient that makes the fire wavier at the bottom.
			int xOffsetWaviness = (FIRE_HEIGHT - row) * waviness[row] / FIRE_HEIGHT; // L: 189
			int fireXPosition = xOffsetWaviness + xPos; // L: 190

			int rowClipLeft = 0; // L: 191
			int rowClipRight = FIRE_WIDTH; // L: 192
//			// I think we are skipping drawing the offscreen part here.
//			if (fireXPosition < 0) { // L: 193
//				rowClipLeft = -fireXPosition; // L: 194
//				fireXPosition = 0; // L: 195
//			}
//			// skip the part clipped off by the left side of the screen.
//			var2 += rowClipLeft; // L: 201
//			if (fireXPosition + FIRE_WIDTH /* fire width */ >= SCREEN_WIDTH) { // L: 197
//				rowClipRight = SCREEN_WIDTH - fireXPosition; // L: 198
//			}

			int rasterIndex = fireXPosition + (row + FIRE_TOP - FIRE_CLIP_ENDS) * SCREEN_WIDTH; // L: 200

			for (int col = rowClipLeft; col < rowClipRight; ++col) { // L: 202
				int intensity = fireIntensity[var2++]; // L: 203
				int x = rasterIndex % SCREEN_WIDTH; // L: 204
				if (intensity != 0/* && x >= Rasterizer2D.Rasterizer2D_xClipStart && x < Rasterizer2D.Rasterizer2D_xClipEnd*/) { // L: 205
					int notIntensity = 256 - intensity; // L: 207
					int fireColor = colorPalette[intensity]; // L: 208
					int y = rasterIndex / 1000;
					int currentColor = image.getRGB(x, y);
					int color = 0xff000000 | (intensity * (fireColor & 0x00ff00) + notIntensity * (currentColor & 0x00ff00) & 0xff0000) + ((fireColor & 0xff00ff) * intensity + (currentColor & 0xff00ff) * notIntensity & 0xff00ff00) >> 8;
					g2.setColor(new Color(color));
					g2.fillRect(x, y, 1, 1);
//				Message.rasterProvider.pixels[var8++] = color; // L: 210
				}
				++rasterIndex; // L: 212
			}

			// skip the section clipped by the right side of the screen.
//			var2 += FIRE_WIDTH - rowClipRight; // L: 214
		}
		g.drawImage(image, 0, 0, null);

	} // L: 216

	final void method2215(/*IndexedSprite var1*/) {
		for (int i = 0; i < FIRE_DATA_ARRAY_SIZE; ++i) { // L: 219
			spriteData[i] = 0;
		}

		int var3;
		for (int i = 0; i < 5000; ++i) { // L: 220
			var3 = (int)(Math.random() * 128.0D * 256.0D); // L: 221
			spriteData[var3] = (int)(Math.random() * 256.0D); // L: 222
		}

		int var4;
		int var5;
		for (int i = 0; i < 20; ++i) { // L: 224
			for (var3 = 1; var3 < 255; ++var3) { // L: 225
				for (var4 = 1; var4 < 127; ++var4) { // L: 226
					var5 = var4 + (var3 << 7); // L: 227
					spriteTemporary[var5] = (spriteData[var5 + 128] + spriteData[var5 - 128] + spriteData[var5 + 1] + spriteData[var5 - 1]) / 4; // L: 228
				}
			}

			int[] var8 = spriteData; // L: 231
			spriteData = spriteTemporary; // L: 232
			spriteTemporary = var8; // L: 233
		}

		if (false/*var1 != null*/) { // L: 235
			int var2 = 0; // L: 236

//			for (var3 = 0; var3 < var1.subHeight; ++var3) { // L: 237
//				for (var4 = 0; var4 < var1.subWidth; ++var4) { // L: 238
//					if (var1.pixels[var2++] != 0) { // L: 239
//						var5 = var4 + var1.xOffset + 16; // L: 240
//						int var6 = var3 + var1.yOffset + 16; // L: 241
//						int var7 = var5 + (var6 << 7); // L: 242
//						spriteData[var7] = 0; // L: 243
//					}
//				}
//			}
		}

	} // L: 248

//	static final IterableNodeHashTable readStringIntParameters(Buffer var0, IterableNodeHashTable var1) {
//		int var2 = var0.readUnsignedByte(); // L: 16
//		int var3;
//		if (var1 == null) {
//			var3 = class135.method2910(var2);
//			var1 = new IterableNodeHashTable(var3); // L: 19
//		}
//
//		for (var3 = 0; var3 < var2; ++var3) { // L: 21
//			boolean var4 = var0.readUnsignedByte() == 1; // L: 22
//			int var5 = var0.readMedium(); // L: 23
//			Object var6;
//			if (var4) { // L: 25
//				var6 = new ObjectNode(var0.readStringCp1252NullTerminated());
//			} else {
//				var6 = new IntegerNode(var0.readInt()); // L: 26
//			}
//
//			var1.put((Node)var6, (long)var5); // L: 27
//		}
//
//		return var1; // L: 29
//	}
//
//	public static void method2233(AbstractArchive var0) {
//		InvDefinition.InvDefinition_archive = var0; // L: 17
//	} // L: 18
//
	public static void main(String[] args) {
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

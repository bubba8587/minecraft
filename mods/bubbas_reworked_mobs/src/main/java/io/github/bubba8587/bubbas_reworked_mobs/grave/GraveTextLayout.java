// dte:C1,B15
package io.github.bubba8587.bubbas_reworked_mobs.grave;

/** Gravestone text layout shared by the world renderer and the edit screen (block pixels, rotation 0, front = south). */
public final class GraveTextLayout {
	/** Font pixels per block pixel (a sign's text scale, 1/96 block per font pixel). */
	public static final float FONT_PX_PER_BLOCK_PX = 6.0F;
	/** Height of the text block's centre. */
	public static final float TEXT_CENTRE_Y = 7.5F;
	/** The stone's front face. */
	public static final float FACE_Z = 10.0F;
	/** Text line width and height, in font pixels. */
	public static final int MAX_LINE_WIDTH = 60;
	public static final int LINE_HEIGHT = 10;

	/** The stone's front silhouette as {x0, y0, x1, y1} rectangles: plinth, body, shoulder, crown. */
	public static final int[][] FACE_RECTS = {
			{ 1, 0, 15, 2 },
			{ 2, 2, 14, 12 },
			{ 3, 12, 13, 13 },
			{ 5, 13, 11, 14 },
	};

	private GraveTextLayout() {
	}
}

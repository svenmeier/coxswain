package svenmeier.coxswain.view;

import android.content.Context;
import android.graphics.Typeface;

import java.util.HashMap;
import java.util.Map;

public class FontCache {

	private static Map<String, Typeface> fonts = new HashMap<String, Typeface>();

	public static Typeface getFont(Context context, String name){

		Typeface font = fonts.get(name);

		if (font == null){
			font = Typeface.createFromAsset(context.getAssets(), name);

			fonts.put(name, font);
		}
		return font;
	}
}
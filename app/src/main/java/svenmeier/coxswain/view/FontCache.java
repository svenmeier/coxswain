package svenmeier.coxswain.view;

import android.content.Context;
import android.graphics.Typeface;
import android.util.Log;

import java.util.HashMap;
import java.util.Map;

import svenmeier.coxswain.Coxswain;

public class FontCache {

	private static Map<String, Typeface> fonts = new HashMap<String, Typeface>();

	public static Typeface getFont(Context context, String name){

		Typeface font = fonts.get(name);

		if (font == null){
			try {
				font = Typeface.createFromAsset(context.getAssets(), name);
			} catch (Exception ex) {
				Log.e(Coxswain.TAG, name);
			}

			fonts.put(name, font);
		}
		return font;
	}
}
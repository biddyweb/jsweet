package source.nativestructures;

import static jsweet.util.Globals.$export;

import def.js.Array;

/**
 * This test is executed without any Java runtime.
 */
public class NativeStringBuilder {

	static Array<String> trace = new Array<>();

	public static void main(String[] args) {
		StringBuilder sb = new StringBuilder();

		sb.append("a");
		
		trace.push(sb.toString());

		sb.append("bc");

		trace.push(""+sb);

		StringBuffer sb2 = new StringBuffer();

		sb2.append("a");
		
		trace.push(sb2.toString());

		sb2.append("bc");

		trace.push(""+sb2);
		
		$export("trace", trace.join(","));

	}

}

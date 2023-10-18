package sk.iway.iwcm.system.fulltext.cdb;

import java.io.UnsupportedEncodingException;
import java.lang.ref.SoftReference;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CoderResult;
import java.nio.charset.CodingErrorAction;
import java.util.Arrays;

/**
 *
 * Implementácia kódovania poľa znakov do poľa bajtov CdbUtils.java
 *
 * @Title webjet7
 * @Company Interway s.r.o. (www.interway.sk)
 * @Copyright Interway s.r.o. (c) 2001-2011
 * @author $Author: jeeff thaber $
 * @version $Revision: 1.3 $
 * @created Date: 5.5.2011 14:11:19
 * @modified $Date: 2004/08/16 06:26:11 $
 */
@SuppressWarnings({"unused", "unchecked", "rawtypes"})
public class CdbUtils
{
	private CdbUtils()
	{
	}

	public static final Charset cs = Charset.defaultCharset();
	private static ThreadLocal decoder = new ThreadLocal(); //NOSONAR
	private static ThreadLocal encoder = new ThreadLocal(); //NOSONAR

	private static Object deref(ThreadLocal tl)
	{
		SoftReference sr = (SoftReference) tl.get();
		if (sr == null)
			return null;
		return sr.get();
	}

	private static void set(ThreadLocal tl, Object ob)
	{
		tl.set(new SoftReference(ob));
	}

	private static byte[] safeTrim(byte[] ba, int len)
	{
		if (len == ba.length)
			return ba;
		else
			return Arrays.copyOf(ba, len);
	}

	  // Trim the given byte array to the given length
   //
   @SuppressWarnings("java:S3398")
   private static byte[] safeTrim(byte[] ba, int len, Charset cs) {
	if (len == ba.length )
	    return ba;
       else
           return Arrays.copyOf(ba, len);
   }


	// Trim the given char array to the given length
	//
	@SuppressWarnings("java:S3398")
	private static char[] safeTrim(char[] ca, int len, Charset cs)
	{
		if (len == ca.length)
			return ca;
		else
			return Arrays.copyOf(ca, len);
	}

	private static int scale(int len, float expansionFactor)
	{
		// We need to perform double, not float, arithmetic; otherwise
		// we lose low order bits when len is larger than 2**24.
		return (int) (len * (double) expansionFactor);
	}

	public static byte[] encode(char[] ca, int off, int len)
	{
		StringEncoder se = (StringEncoder) deref(encoder);
		se = new StringEncoder(cs, cs.name());
		set(encoder, se);
		return se.encode(ca, off, len);
	}


	public static char[] decode(byte[] ba)
	{
		StringDecoder sd = (StringDecoder) deref(decoder);
		sd = new StringDecoder(cs, cs.name());
		set(decoder, sd);
		return sd.decode(ba, 0, ba.length);
	}

	public static char[] decode(byte[] ba, int off, int len)
	{
		StringDecoder sd = (StringDecoder) deref(decoder);
		sd = new StringDecoder(cs, cs.name());
		set(decoder, sd);
		return sd.decode(ba, off, len);
	}

	// -- Decoding --
	private static class StringDecoder
	{
		private final String requestedCharsetName;
		private final Charset cs;
		private final CharsetDecoder cd;

		private StringDecoder(Charset cs, String rcn)
		{
			this.requestedCharsetName = rcn;
			this.cs = cs;
			this.cd = cs.newDecoder().onMalformedInput(CodingErrorAction.REPLACE).onUnmappableCharacter(CodingErrorAction.REPLACE);
		}

		char[] decode(byte[] ba, int off, int len)
		{
			int en = scale(len, cd.maxCharsPerByte());
			char[] ca = new char[en];
			if (len == 0)
				return ca;
			cd.reset();
			ByteBuffer bb = ByteBuffer.wrap(ba, off, len);
			CharBuffer cb = CharBuffer.wrap(ca);
			try
			{
				CoderResult cr = cd.decode(bb, cb, true);
				if (!cr.isUnderflow())
					cr.throwException();
				cr = cd.flush(cb);
				if (!cr.isUnderflow())
					cr.throwException();
			}
			catch (CharacterCodingException x)
			{
				// Substitution is always enabled,
				// so this shouldn't happen
				throw new Error(x);
			}
			return safeTrim(ca, cb.position(), cs);
		}
	}

	// -- Encoding --
	private static class StringEncoder
	{
		private Charset cs;
		private CharsetEncoder ce;
		private final String requestedCharsetName;

		private StringEncoder(Charset cs, String rcn)
		{
			this.requestedCharsetName = rcn;
			this.cs = cs;
			this.ce = cs.newEncoder().onMalformedInput(CodingErrorAction.REPLACE).onUnmappableCharacter(CodingErrorAction.REPLACE);
		}

		/*String charsetName()
		{
			if (cs instanceof HistoricallyNamedCharset)
				return ((HistoricallyNamedCharset) cs).historicalName();
			return cs.name();
		}*/

		final String requestedCharsetName()
		{
			return requestedCharsetName;
		}

		byte[] encode(char[] ca, int off, int len)
		{
			int en = scale(len, ce.maxBytesPerChar());
			byte[] ba = new byte[en];
			if (len == 0)
				return ba;
			ce.reset();
			ByteBuffer bb = ByteBuffer.wrap(ba);
			CharBuffer cb = CharBuffer.wrap(ca, off, len);
			try
			{
				CoderResult cr = ce.encode(cb, bb, true);
				if (!cr.isUnderflow())
					cr.throwException();
				cr = ce.flush(bb);
				if (!cr.isUnderflow())
					cr.throwException();
			}
			catch (CharacterCodingException x)
			{
				// Substitution is always enabled,
				// so this shouldn't happen
				throw new Error(x);
			}
			return safeTrim(ba, bb.position(), cs);
		}
	}
}

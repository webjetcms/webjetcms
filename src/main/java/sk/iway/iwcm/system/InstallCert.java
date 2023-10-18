package sk.iway.iwcm.system;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

/**
 * Install certificate into keystore
 */
public class InstallCert {

   public static void main(String[] args) throws Exception
	{
		String host;
		int port;
		char[] passphrase;
		if ((args.length == 1) || (args.length == 2))
		{
			String[] c = args[0].split(":");
			host = c[0];
			port = (c.length == 1) ? 443 : Integer.parseInt(c[1]);
			String p = (args.length == 1) ? "changeit" : args[1];
			passphrase = p.toCharArray();
		}
		else
		{
			System.out.println("Usage: java InstallCert <host>[:port] [passphrase]"); //NOSONAR
			return;
		}
		System.out.println(install(host, port, passphrase, "1")); //NOSONAR
	}

	public static String install(String host, int port, char[] passphrase, String line)
	{
		StringBuilder out = new StringBuilder();
		try
		{
			File file = new File("jssecacerts");
			if (file.isFile() == false)
			{
				char SEP = File.separatorChar;
				File dir = new File(System.getProperty("java.home") + SEP + "lib" + SEP + "security");
				file = new File(dir, "jssecacerts");
				if (file.isFile() == false)
				{
					file = new File(dir, "cacerts");
				}
			}
			out.append("Loading KeyStore " + file + "...\n");
			InputStream in = new FileInputStream(file);
			KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
			ks.load(in, passphrase);
			in.close();
			SSLContext context = SSLContext.getInstance("TLS");
			TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
			tmf.init(ks);
			X509TrustManager defaultTrustManager = (X509TrustManager) tmf.getTrustManagers()[0];
			SavingTrustManager tm = new SavingTrustManager(defaultTrustManager);
			context.init(null, new TrustManager[]{tm}, null);
			SSLSocketFactory factory = context.getSocketFactory();
			out.append("Opening connection to " + host + ":" + port + "...\n");
			SSLSocket socket = (SSLSocket) factory.createSocket(host, port);
			socket.setSoTimeout(10000);
			try
			{
				out.append("Starting SSL handshake...\n");
				socket.startHandshake();
				socket.close();
				out.append('\n');
				out.append("No errors, certificate is already trusted\n");
			}
			catch (Exception e)
			{
				out.append('\n');
				out.append("ERROR: "+e.getMessage());
				sk.iway.iwcm.Logger.error(e);
			}
			X509Certificate[] chain = tm.chain;
			if (chain == null)
			{
				out.append("Could not obtain server certificate chain\n");
				return out.toString();
			}
			//BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
			out.append("\nServer sent " + chain.length + " certificate(s):\n");
			MessageDigest sha1 = MessageDigest.getInstance("SHA1");
			MessageDigest md5 = MessageDigest.getInstance("MD5");
			for (int i = 0; i < chain.length; i++)
			{
				X509Certificate cert = chain[i];
				out.append(" " + (i + 1) + " Subject " + cert.getSubjectDN()+"\n");
				out.append("   Issuer  " + cert.getIssuerDN()+"\n");
				sha1.update(cert.getEncoded());
				out.append("   sha1    " + toHexString(sha1.digest())+"\n");
				md5.update(cert.getEncoded());
				out.append("   md5     " + toHexString(md5.digest())+"\n\n");
			}
			//String line = reader.readLine().trim();
			int k;
			try
			{
				k = (line.length() == 0) ? 0 : Integer.parseInt(line) - 1;
			}
			catch (NumberFormatException e)
			{
				out.append("KeyStore not changed \n");
				return out.toString();
			}
			X509Certificate cert = chain[k];
			String alias = host + "-" + (k + 1);
			ks.setCertificateEntry(alias, cert);
			OutputStream fout = new FileOutputStream(file);
			ks.store(fout, passphrase);
			fout.close();
			out.append("\n"+cert.toString()+"\n");
			out.append("Added certificate to keystore "+file.getAbsolutePath()+" using alias '" + alias + "'");
		}
		catch (Exception ex)
		{
			out.append("\n\nERROR: " + ex.getMessage());
		}

		return out.toString();
	}

	private static final char[] HEXDIGITS = "0123456789abcdef".toCharArray();

	private static String toHexString(byte[] bytes)
	{
		StringBuilder sb = new StringBuilder(bytes.length * 3);
		for (int b : bytes)
		{
			b &= 0xff;
			sb.append(HEXDIGITS[b >> 4]);
			sb.append(HEXDIGITS[b & 15]);
			sb.append(' ');
		}
		return sb.toString();
	}

	private static class SavingTrustManager implements X509TrustManager
	{
		private final X509TrustManager tm;
		private X509Certificate[] chain;

		SavingTrustManager(X509TrustManager tm)
		{
			this.tm = tm;
		}
		@Override
		public X509Certificate[] getAcceptedIssuers()
		{
			throw new UnsupportedOperationException();
		}
		@Override
		public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException
		{
			throw new UnsupportedOperationException();
		}
		@Override
		public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException
		{
			this.chain = chain;
			tm.checkServerTrusted(chain, authType);
		}
	}

}
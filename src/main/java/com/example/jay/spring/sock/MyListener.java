package com.example.jay.spring.sock;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.Charset;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MyListener {
    private AppSettings appSettings;
    private ExecutorService executor;

    public MyListener(AppSettings appSettings) {
        this.appSettings = appSettings;
        executor = Executors.newCachedThreadPool();
        executor.execute(() -> listenLoop());
    }

    void listenLoop() {
        try (ServerSocket sock = new ServerSocket(appSettings.listenPort)) {
            while (true) {
                Socket srcSock = sock.accept();
                Socket dstSock = new Socket(appSettings.targetHost, appSettings.targetPort);
                executor.execute(() -> sockLoop(srcSock, dstSock));
                executor.execute(() -> sockLoop(dstSock, srcSock));
            }
        }
        catch (Exception ex) {
            log.error("listenLoop ### Exception ###: " + ex.getMessage());
        }
    }

    void sockLoop(Socket sock, Socket wSock) {
        try (InputStream s = sock.getInputStream()) {
            OutputStream os = wSock.getOutputStream();
            int recvLen = 0;
            byte[] b = new byte[4096];
            while ((recvLen = s.read(b)) > 0) {
                log.info(getHexDump(b, recvLen, 30));
                os.write(b, 0, recvLen);
                String str = new String(b, 0, recvLen, Charset.forName("UTF-8"));
                log.info("sockLoop >> " + str);
            }
        }
        catch (Exception ex) {
            log.error("sockLoop ### Exception ###: " + ex.getMessage());
        }
        finally {
            try {
                sock.close();
                wSock.close();
            }
            catch (Exception ex) {
                log.error("sockLoop ### Exception ###: close socket - " + ex.getMessage());
            }
        }
    }
	
	public static String getHexDump(byte[] src, int len, int maxLine) {
		StringBuilder lines = new StringBuilder();
		lines.append("len = ").append(len).append('\n');
		int pos = 0;
		int lineCount = 0;
		for (int i = 0; i + 16 <= len; i += 16, lineCount++) {
			StringBuilder line1 = new StringBuilder();
			for (int i2 = i; i2 < i + 16; i2 += 4) {
				line1.append(String.format("%02X", src[i2    ] & 0xFF));
				line1.append(String.format("%02X", src[i2 + 1] & 0xFF));
				line1.append(String.format("%02X", src[i2 + 2] & 0xFF));
				line1.append(String.format("%02X", src[i2 + 3] & 0xFF));
				line1.append(' ');
			}
			lines.append(String.format("%3d ", (i/16))).append(line1).append(new String(src, i, 16)).append('\n');
			pos = i + 16;
			if (maxLine > 0 && lineCount == maxLine)
				break;
		}
		if (pos < len && (maxLine <= 0 || lineCount < maxLine)) {
			StringBuilder line1 = new StringBuilder();
			for (int i = 0; i < 16; i++) {
				if (pos + i < len)
					line1.append(String.format("%02X", src[pos + i] & 0xFF));
				else
					line1.append("  ");
				if ((i%4) == 3)
					line1.append(' ');
			}
			lines.append(String.format("%3d ", (pos/16))).append(line1).append(new String(src, pos, len - pos)).append('\n');
		}
		
		return lines.toString();
	}
}

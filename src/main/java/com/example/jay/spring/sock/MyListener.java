package com.example.jay.spring.sock;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
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

                log.info("listenLoop >> new client ----- ");
                executor.execute(() -> sockLoop("####### Brower request" , srcSock, dstSock));
                executor.execute(() -> sockLoop("@@@@@@@ Server response", dstSock, srcSock));
            }
        }
        catch (Exception ex) {
            log.error("listenLoop ### Exception ###: " + ex.getMessage());
        }
    }

    void sockLoop(String label, Socket sock, Socket wSock) {
        try (InputStream s = sock.getInputStream()) {
            OutputStream os = wSock.getOutputStream();
            int recvLen = 0;
            byte[] b = new byte[4096];
            while ((recvLen = s.read(b)) > 0) {
                os.write(b, 0, recvLen);
                String str = new String(b, 0, recvLen, StandardCharsets.UTF_8);
                log.info("sockLoop >> {}\n{}", label, str);
                log.info(getHexDump(b, recvLen, 30));
                log.info("-------------------------------------------------");
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
			lines.append(String.format("%3d ", (i/16))).append(line1).append(utf8byte2str(src, i, 16)).append('\n');
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
			lines.append(String.format("%3d ", (pos/16))).append(line1).append(utf8byte2str(src, pos, len - pos)).append('\n');
		}
		
		return lines.toString();
	}

    // utf-8: byte -> String
    private static String utf8byte2str(byte[] bytes, int pos, int len) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < len; ) {
            int b = (bytes[pos + i] & 0x000000FF);
            if ((b & 0x80) == 0) {
                sb.append(0x20 <= b && b <= 0x7e ? Character.toString(b) : ".");
                i++;
            }
            // 110x xxxx, 10xx xxxx 패턴 
            else if ((b & 0xE0) == 0xC0) {
                if (i + 1 < len) {
                    int b2 = (bytes[pos + i + 1] & 0x000000FF);
                    sb.append((b2 & 0xC0) == 0x80 ? new String(bytes, pos + i, 2, StandardCharsets.UTF_8) : "??");
                    i += 2;
                }
                else {
                    sb.append(Character.toString(b));
                    i = len;
                }
            }
            // 1110 xxxx, 10xx xxxx, 10xx xxxx 패턴
            else if ((b & 0xF0) == 0xE0) {
                if (i + 2 < len) {
                    int b2 = (bytes[pos + i + 1] & 0x000000FF);
                    int b3 = (bytes[pos + i + 1] & 0x000000FF);
                    sb.append((b2 & 0xC0) == 0x80 && (b3 & 0xC0) == 0x80 ? new String(bytes, pos + i, 3, StandardCharsets.UTF_8) : "???");
                    i += 3;
                }
                else {
                    sb.append("??".substring(0, len - i));
                    i = len;
                }
            }
            // 1111 0zzz, 10zz xxxx, 10xx xxxx, 10xx xxxx 패턴
            else {
                if (i + 3 < len) {
                    sb.append("????");
                    i += 4;
                }
                else {
                    sb.append("???".substring(0, len - i));
                    i = len;
                }
            }
        }

        return sb.toString();
    }
}

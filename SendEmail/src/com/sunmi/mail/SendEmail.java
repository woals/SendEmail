package com.sunmi.mail;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.mail.Authenticator;
import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.Multipart;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

public class SendEmail {
	public static String host;
	public static String port;
	public static String userName;
	public static String password;
	public static String[] to;
	public static String resultLocation;
	public static String testTitle;
	public static String[] testDevices;
	public static int totalCases;
	public static int totalPassCases;
	public static int totalFailCases;
	public static int totalErrorCases;
	public static int totalSkippedCases;
	public static Float totalTakesTime = Float.valueOf(0.0F);
	public static String startTime;
	public static int deviceCounts;
	public static String[] deviceSN;
	public static int[] deviceCases;
	public static int[] passing;
	public static int[] failing;
	public static int[] errors;
	public static int[] skipped;
	public static Float[] takesTime;
	public static String[] whenStart;
	public static String build_Tag;
	public static String localIP;

	public static void main(String[] args) throws Exception {
		localIP = getIP();
		build_Tag = args[0];

		getEmailInfo();

		String[] deviceJunitName = getFileName(resultLocation + args[0] + "/junit-reports");

		String testTitleLine = findTargetInfoLineFromFile(resultLocation + args[0] + "/index.html", "<title>");
		testTitle = (String) match(testTitleLine, "<title>", "</title>").get(0);

		deviceCounts = deviceJunitName.length;

		String startTimeLine = findTargetInfoLineFromFile(resultLocation + args[0] + "/index.html",
				"meta name=\"description\"");
		startTime = (String) match(startTimeLine, "at ", "\">").get(0);

		int junitCounts = deviceJunitName.length;
		deviceSN = new String[junitCounts];
		deviceCases = new int[junitCounts];
		failing = new int[junitCounts];
		errors = new int[junitCounts];
		skipped = new int[junitCounts];
		passing = new int[junitCounts];
		takesTime = new Float[junitCounts];
		whenStart = new String[junitCounts];
		for (int i = 0; i < deviceJunitName.length; i++) {
			deviceSN[i] = deviceJunitName[i].split("\\.")[0];
			String line = findTargetInfoLineFromFile(
					resultLocation + args[0] + "/junit-reports" + "/" + deviceJunitName[i], "testsuite");
			deviceCases[i] = Integer.valueOf((String) match(line, "tests=\"", "\"").get(0)).intValue();
			failing[i] = Integer.valueOf((String) match(line, "failures=\"", "\"").get(0)).intValue();
			errors[i] = Integer.valueOf((String) match(line, "errors=\"", "\"").get(0)).intValue();
			skipped[i] = Integer.valueOf((String) match(line, "skipped=\"", "\"").get(0)).intValue();
			passing[i] = (deviceCases[i] - failing[i] - errors[i] - skipped[i]);
			takesTime[i] = Float.valueOf((String) match(line, "time=\"", "\"").get(0));
			whenStart[i] = ((String) match(line, "timestamp=\"", "\"").get(0));
			totalCases += deviceCases[i];
			totalPassCases += passing[i];
			totalFailCases += failing[i];
			totalErrorCases += errors[i];
			totalSkippedCases += skipped[i];
			totalTakesTime = Float.valueOf(totalTakesTime.floatValue() + takesTime[i].floatValue());
		}
		sendHtmlMail(args[0]);
	}
	
	/** 
     * 多IP处理，可以得到最终ip 
     * @return 
     */  
    public static String getIP() {  
        String localip = null;// 本地IP，如果没有配置外网IP则返回它  
        String netip = null;// 外网IP  
        try {  
            Enumeration<NetworkInterface> netInterfaces = NetworkInterface  
                    .getNetworkInterfaces();  
            InetAddress ip = null;  
            boolean finded = false;// 是否找到外网IP  
            while (netInterfaces.hasMoreElements() && !finded) {  
                NetworkInterface ni = netInterfaces.nextElement();  
                Enumeration<InetAddress> address = ni.getInetAddresses();  
                while (address.hasMoreElements()) {  
                    ip = address.nextElement();  
//                  System.out.println(ni.getName() + ";" + ip.getHostAddress()  
//                          + ";ip.isSiteLocalAddress()="  
//                          + ip.isSiteLocalAddress()  
//                          + ";ip.isLoopbackAddress()="  
//                          + ip.isLoopbackAddress());  
                    if (!ip.isSiteLocalAddress() && !ip.isLoopbackAddress()  
                            && ip.getHostAddress().indexOf(":") == -1) {// 外网IP  
                        netip = ip.getHostAddress();  
                        finded = true;  
                        break;  
                    } else if (ip.isSiteLocalAddress()  
                            && !ip.isLoopbackAddress()  
                            && ip.getHostAddress().indexOf(":") == -1) {// 内网IP  
                        localip = ip.getHostAddress();  
                    }  
                }  
            }  
        } catch (SocketException e) {  
            e.printStackTrace();  
        }  
        if (netip != null && !"".equals(netip)) {  
            return netip;  
        } else {  
            return localip;  
        }  
    }

//	public static String getIP() {
//		try {
//			InetAddress address = InetAddress.getLocalHost();
//			if (address.isLoopbackAddress()) {
//				Enumeration<NetworkInterface> allNetInterfaces = NetworkInterface.getNetworkInterfaces();
//				Enumeration<InetAddress> addresses;
//				for (; allNetInterfaces.hasMoreElements(); addresses.hasMoreElements()) {
//					NetworkInterface netInterface = (NetworkInterface) allNetInterfaces.nextElement();
//					addresses = netInterface.getInetAddresses();
//					// continue;
//					InetAddress ip = (InetAddress) addresses.nextElement();
//					if ((!ip.isLinkLocalAddress()) && (!ip.isLoopbackAddress()) && ((ip instanceof Inet4Address))) {
//						return ip.getHostAddress();
//					}
//				}
//			}
//			return address.getHostAddress();
//		} catch (UnknownHostException e) {
//			e.printStackTrace();
//			return null;
//		} catch (SocketException e) {
//			e.printStackTrace();
//		}
//		return null;
//	}

	public static void getEmailInfo() {
		Properties properties = new Properties();
		try {
			InputStream is = SendEmail.class.getResourceAsStream("/info.properties");
			properties.load(is);
		} catch (IOException e) {
			System.out.println("info.properties中指定路径下未读取到信息");
		} catch (NullPointerException e) {
			System.out.println("info.properties中指定路径下未读取到信息空指针");
		}
		host = properties.getProperty("host");
		System.out.println(host);
		port = properties.getProperty("port");
		userName = properties.getProperty("userName");
		password = properties.getProperty("password");
		String toSomebody = properties.getProperty("to");
		to = toSomebody.split(",");
		resultLocation = properties.getProperty("spoon_output_dir");
	}

	public static String[] getFileName(String path) {
		File file = new File(path);
		if (!file.exists()) {
			System.out.println("目录不存在");
			return null;
		}
		File[] devices = file.listFiles();
		testDevices = new String[devices.length];
		for (int i = 0; i < devices.length; i++) {
			if (devices[i].isDirectory()) {
				System.out.println(devices[i] + "是目录");
			} else {
				testDevices[i] = devices[i].getName();
			}
		}
		return testDevices;
	}

	public static List<String> match(String s, String s1, String s2) {
		List<String> results = new ArrayList<String>();
		Pattern p = Pattern.compile(s1 + "(.*?)" + s2);
		Matcher m = p.matcher(s);
		while ((!m.hitEnd()) && (m.find())) {
			results.add(m.group(1));
		}
		return results;
	}

	public static String findTargetInfoLineFromFile(String path, String targetInfo) {
		File file = new File(path);
		BufferedReader reader = null;
		String tempString = null;
		try {
			System.out.println("以行为单位读取文件内容，一次读一整行：");
			reader = new BufferedReader(new FileReader(file));
			while ((tempString = reader.readLine()) != null) {
				if (tempString.contains(targetInfo)) {
					break;
				}
			}
			reader.close();
		} catch (IOException e) {
			e.printStackTrace();
			if (reader != null) {
				try {
					reader.close();
				} catch (IOException e1) {
					e1.printStackTrace();
				}
			}
		} finally {
			if (reader != null) {
				try {
					reader.close();
				} catch (IOException e1) {
					e1.printStackTrace();
				}
			}
		}
		return tempString;
	}

	public static void sendHtmlMail(String s) throws Exception {
		Properties pro = System.getProperties();
		pro.put("mail.smtp.host", host);
		pro.put("mail.smtp.port", port);
		pro.put("mail.smtp.auth", "true");
		Session sendMailSession = Session.getDefaultInstance(pro, new Authenticator() {
			protected PasswordAuthentication getPasswordAuthentication() {
				return new PasswordAuthentication(SendEmail.userName, SendEmail.password);
			}
		});
		Message mailMessage = new MimeMessage(sendMailSession);
		mailMessage.setFrom(new InternetAddress(userName));
		InternetAddress[] sendTo = new InternetAddress[to.length];
		for (int i = 0; i < to.length; i++) {
			System.out.println("发送到:" + to[i]);
			sendTo[i] = new InternetAddress(to[i]);
		}
		mailMessage.setRecipients(Message.RecipientType.TO, sendTo);
		mailMessage.setSubject(testTitle + "_" + build_Tag + "_" + Calendar.getInstance().getTime());
		mailMessage.setSentDate(new Date());
		Multipart mainPart = new MimeMultipart();
		BodyPart html = new MimeBodyPart();
		StringBuffer htmlContent = new StringBuffer();
		htmlContent.append("<!DOCTYPE html>");
		htmlContent.append("<html>");
		htmlContent.append("<head>");
		htmlContent.append("    <meta charset=\"UTF-8\">");
		htmlContent.append("    <title>表格</title>");
		htmlContent.append("</head>");
		htmlContent.append("<body>");
		htmlContent.append("<table border=\"1\" width=\"80%\">");
		htmlContent.append("    <thead>");
		htmlContent.append("        <tr>");
		htmlContent.append(
				"            <td colspan=\"3\" style=\"text-align:left;font-weight:bold;font-size:30px;background:#DDDDDD;\">SunmiAutoTestReport</td>");
		htmlContent.append("        </tr>");
		htmlContent.append("    </thead>");
		htmlContent.append("    <tbody>");
		htmlContent.append("        <tr>");
		htmlContent.append(
				"            <td rowspan=\"8\" style=\"text-align:center;font-weight:bold;font-size:20px;background:#CCCCCC;\">汇总</td>");
		htmlContent.append(
				"            <td style=\"text-align:center;font-weight:normal;font-size:15px;background:#CCCCCC;\">用例总数</td>");
		htmlContent.append(
				"            <td style=\"text-align:center;font-weight:normal;font-size:15px;background:#CCCCCC;\">"
						+ totalCases + "</td>");
		htmlContent.append("");
		htmlContent.append("        </tr>");
		htmlContent.append("        <tr>");
		htmlContent.append(
				"            <td style=\"text-align:center;font-weight:normal;font-size:15px;background:#CCCCCC;\">被测设备数量</td>");
		htmlContent.append(
				"            <td style=\"text-align:center;font-weight:normal;font-size:15px;background:#CCCCCC;\">"
						+ deviceCounts + "</td>");
		htmlContent.append("        </tr>");
		htmlContent.append("        <tr>");
		htmlContent.append(
				"            <td style=\"text-align:center;font-weight:normal;font-size:15px;background:#CCCCCC;\">通过数量</td>");
		htmlContent.append(
				"            <td style=\"text-align:center;font-weight:normal;font-size:15px;background:#CCCCCC;\">"
						+ totalPassCases + "</td>");
		htmlContent.append("        </tr>");
		htmlContent.append("        <tr>");
		htmlContent.append(
				"            <td style=\"text-align:center;font-weight:normal;font-size:15px;background:#CCCCCC;\">失败数量</td>");
		if (totalFailCases != 0) {
			htmlContent.append(
					"            <td style=\"text-align:center;font-weight:normal;font-size:15px;background:#FF0000;\">"
							+ totalFailCases + "</td>");
		} else {
			htmlContent.append(
					"            <td style=\"text-align:center;font-weight:normal;font-size:15px;background:#00FF00;\">"
							+ totalFailCases + "</td>");
		}
		htmlContent.append("        </tr>");
		htmlContent.append("        <tr>");
		htmlContent.append(
				"            <td style=\"text-align:center;font-weight:normal;font-size:15px;background:#CCCCCC;\">错误数量</td>");
		htmlContent.append(
				"            <td style=\"text-align:center;font-weight:normal;font-size:15px;background:#CCCCCC;\">"
						+ totalErrorCases + "</td>");
		htmlContent.append("        </tr>");
		htmlContent.append("        <tr>");
		htmlContent.append(
				"            <td style=\"text-align:center;font-weight:normal;font-size:15px;background:#CCCCCC;\">跳过数量</td>");
		htmlContent.append(
				"            <td style=\"text-align:center;font-weight:normal;font-size:15px;background:#CCCCCC;\">"
						+ totalSkippedCases + "</td>");
		htmlContent.append("        </tr>");
		htmlContent.append("        <tr>");
		htmlContent.append(
				"            <td style=\"text-align:center;font-weight:normal;font-size:15px;background:#CCCCCC;\">开始时间</td>");
		htmlContent.append(
				"            <td style=\"text-align:center;font-weight:normal;font-size:15px;background:#CCCCCC;\">"
						+ startTime + "</td>");
		htmlContent.append("        </tr>");
		htmlContent.append("    </tbody>");
		htmlContent.append("</table>");
		htmlContent.append("<br>");

		htmlContent.append("<h1 style=\"text-align:left;font-weight:bold;font-size:15px;\">以下是各台测试设备的测试结果:</h1>");
		for (int i = 0; i < deviceCounts; i++) {
			htmlContent.append("<table border=\"1\" width=\"80%\">");
			htmlContent.append("    <thead>");
			htmlContent.append("        <tr>");
			htmlContent.append(
					"            <td colspan=\"2\" style=\"text-align:left;font-weight:bold;font-size:15px;background:#DDDDDD;\">"
							+ deviceSN[i] + "</td>");
			htmlContent.append("        </tr>");
			htmlContent.append("    </thead>");
			htmlContent.append("    <tbody>");
			htmlContent.append("        <tr>");
			htmlContent.append(
					"            <td style=\"text-align:center;font-weight:normal;font-size:15px;background:#CCCCCC;\">用例总数</td>");
			htmlContent.append(
					"            <td style=\"text-align:center;font-weight:normal;font-size:15px;background:#CCCCCC;\">"
							+ deviceCases[i] + "</td>");
			htmlContent.append("");
			htmlContent.append("        </tr>");
			htmlContent.append("        <tr>");
			htmlContent.append(
					"            <td style=\"text-align:center;font-weight:normal;font-size:15px;background:#CCCCCC;\">通过数量</td>");
			htmlContent.append(
					"            <td style=\"text-align:center;font-weight:normal;font-size:15px;background:#CCCCCC;\">"
							+ passing[i] + "</td>");
			htmlContent.append("        </tr>");
			htmlContent.append("        <tr>");
			htmlContent.append(
					"            <td style=\"text-align:center;font-weight:normal;font-size:15px;background:#CCCCCC;\">失败数量</td>");
			if (failing[i] != 0) {
				htmlContent.append(
						"            <td style=\"text-align:center;font-weight:normal;font-size:15px;background:#FF0000;\">"
								+ failing[i] + "</td>");
			} else {
				htmlContent.append(
						"            <td style=\"text-align:center;font-weight:normal;font-size:15px;background:#00FF00;\">"
								+ failing[i] + "</td>");
			}
			htmlContent.append("        </tr>");
			htmlContent.append("        <tr>");
			htmlContent.append(
					"            <td style=\"text-align:center;font-weight:normal;font-size:15px;background:#CCCCCC;\">错误数量</td>");
			htmlContent.append(
					"            <td style=\"text-align:center;font-weight:normal;font-size:15px;background:#CCCCCC;\">"
							+ errors[i] + "</td>");
			htmlContent.append("        </tr>");
			htmlContent.append("        <tr>");
			htmlContent.append(
					"            <td style=\"text-align:center;font-weight:normal;font-size:15px;background:#CCCCCC;\">跳过数量</td>");
			htmlContent.append(
					"            <td style=\"text-align:center;font-weight:normal;font-size:15px;background:#CCCCCC;\">"
							+ skipped[i] + "</td>");
			htmlContent.append("        </tr>");
			htmlContent.append("        <tr>");
			htmlContent.append(
					"            <td style=\"text-align:center;font-weight:normal;font-size:15px;background:#CCCCCC;\">结束时间(格林尼治时间)</td>");
			htmlContent.append(
					"            <td style=\"text-align:center;font-weight:normal;font-size:15px;background:#CCCCCC;\">"
							+ whenStart[i] + "</td>");
			htmlContent.append("        </tr>");
			htmlContent.append("    </tbody>");
			htmlContent.append("</table>");
			htmlContent.append("<br>");
		}
		htmlContent.append("<h1>报告详细地址</h1>");
		htmlContent.append("<b><font color=\"#00FF00\">http://" + localIP + ":8090/spoon-output_" + build_Tag
				+ "/index.html</font></b>");
		htmlContent.append("</body>");
		htmlContent.append("</html>");

		html.setContent(htmlContent.toString(), "text/html; charset=utf-8");
		mainPart.addBodyPart(html);

		mailMessage.setContent(mainPart);

		Transport.send(mailMessage);
	}
}

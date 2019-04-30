package com.dsubires.lunchtime;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Date;
import java.util.HashMap;

import org.apache.commons.text.WordUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;

@Service
public class ScheduledTaskService {

	private static final String user = "user";
	private static final String passwd = "passwd";
	private static final String loginFormUrl = "https://timechef.serunion.com/account/login?ReturnUrl=/menus/hoy/7";
	private static final String loginActionUrl = "https://timechef.serunion.com/account/login";
	private static final String USER_AGENT = "\"Mozilla/5.0 (Windows NT\" +\n"
			+ "          \" 6.1; WOW64) AppleWebKit/535.2 (KHTML, like Gecko) Chrome/15.0.874.120 Safari/535.2\"";
	private static String primeros, segundos, plancha, postres, guarniciones;
	private Logger logger = LogManager.getLogger("informationDumping");

	@Scheduled(cron = "0 45 9 * * MON-FRI")
	public void scheduleTask() {
		
		HashMap<String, String> cookies = new HashMap<String, String>();
		HashMap<String, String> formData = new HashMap<String, String>();
		primeros = new String();
		segundos = new String();
		plancha = new String();
		postres = new String();
		guarniciones = new String();
		LocalDate localDate = LocalDate.now();
		int mes = localDate.getMonthValue();
		int dia = localDate.getDayOfMonth();
		Connection.Response loginForm;
		
		try {
			// 1. GET LOGIN FORM
			loginForm = Jsoup.connect(loginFormUrl).method(Connection.Method.GET).userAgent(USER_AGENT).execute();
			Document loginDoc = loginForm.parse(); // this is the document that contains response html
			cookies.putAll(loginForm.cookies()); // save the cookies, this will be passed on to next request
			//Obtain the verification token
			String vtoken = loginDoc.select("input").first().attr("value");
			
			

			// 2. POST LOGIN FORM

			formData.put("__RequestVerificationToken", vtoken);
			formData.put("Login.Email", user);
			formData.put("Login.Password", passwd);

			Connection.Response homePage = Jsoup.connect(loginActionUrl).cookies(cookies).data(formData)
					.method(Connection.Method.POST).userAgent(USER_AGENT).execute();

			// 3. GET MENU
			
			//Once we have made the login, we can access the URL of the menu
			homePage = Jsoup.connect("https://timechef.serunion.com/menus/hoy/7").cookies(cookies)
					.method(Connection.Method.GET).userAgent(USER_AGENT).execute();

			// Only 4 debug
			// System.out.println("Login ok: " + html.contains("DAVID SUBIRES"));

			// 4. PARSE MENU INFO
			String html = homePage.parse().html();
			Document doc = Jsoup.parse(html);

			Elements datos = doc.select("#acordeonmenuIngesta1 > ul > li:first-child > div > div.dish > p");

			primeros += "Primeros:\n";
			for (Element element : datos)
				primeros += WordUtils.capitalizeFully(element.text()) + "\n";

			datos = doc.select("#acordeonmenuIngesta1 > ul > li:nth-child(2) > div > div.dish > p");
			segundos += "Segundos:\n";
			for (Element element : datos)
				segundos += WordUtils.capitalizeFully(element.text()) + "\n";

			datos = doc.select("#acordeonmenuIngesta7 > ul > li p");
			guarniciones += "Guarniciones:\n";
			for (Element element : datos)
				guarniciones += WordUtils.capitalizeFully(element.text()) + "\n";

			datos = doc.select("#acordeonmenuIngesta6 > ul > li p");
			plancha += "Plancha:\n";
			for (Element element : datos)
				plancha += WordUtils.capitalizeFully(element.text()) + "\n";

			datos = doc.select("#acordeonmenuIngesta1 > ul > li:nth-child(3) > div > div.dish > p");
			postres += "Postres:\n";
			for (Element element : datos)
				postres += WordUtils.capitalizeFully(element.text()) + "\n";

			// 5. PUBLISH INFORMATION

			String msgTelegram = "Menú " + dia + "/" + mes + "\n" + primeros + "\n" + segundos + "\n" + guarniciones
					+ "\n" + plancha + "\n" + postres;
			String msgTwitter1 = "Menú " + dia + "/" + mes + "\n" + primeros + "\n" + segundos + "\n" + guarniciones;
			String msgTwitter2 = "Menú " + dia + "/" + mes + "\n" + plancha + "\n" + postres;

			sendToTelegram(msgTelegram);
			sendToTwitter(msgTwitter1);
			sendToTwitter(msgTwitter2);

		} catch (Exception e) {
			logger.info("Error al publicar el menú.\nFecha: " + new Date() + "\nInfo: " + e.getMessage() );
		}

		logger.info("Menú " + dia + "/" + mes + " publicado correctamente. " + new Date() );
		
	}

	private static void sendToTwitter(String str) {
		Twitter twitter = TwitterFactory.getSingleton();
		try {
			twitter.updateStatus(str);
		} catch (TwitterException e) {
			e.printStackTrace();
		}

	}

	@SuppressWarnings("unused")
	private static void sendToTelegram(String str) {
		String urlString = "https://api.telegram.org/bot%s/sendMessage?chat_id=%s&text=%s";

		// Add Telegram token (given Token is fake)
		String apiToken = "800000000:AAGxLYq5czZ_asOkDsWJyKOddbiG6ig8Xt4";

		// Add chatId (given chatId is fake)
		String chatId = "-1001400000000";

		urlString = String.format(urlString, apiToken, chatId, encodeValue(str));

		try {
			URL url = new URL(urlString);
			URLConnection conn = url.openConnection();
			InputStream is = new BufferedInputStream(conn.getInputStream());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	// Method to encode a string value using UTF-8 encoding scheme
	//

	private static String encodeValue(String value) {
		try {
			return URLEncoder.encode(value, StandardCharsets.UTF_8.toString());
		} catch (UnsupportedEncodingException ex) {
			throw new RuntimeException(ex.getCause());
		}
	}

}

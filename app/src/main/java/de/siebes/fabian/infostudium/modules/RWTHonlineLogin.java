package de.siebes.fabian.infostudium.modules;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;

import java.io.IOException;
import java.util.Map;

import de.siebes.fabian.infostudium.Const;
import de.siebes.fabian.infostudium.LoginData;

class RWTHonlineLogin {
    static Map<String, String> login(LoginData loginData) throws IOException {
        Connection.Response resMoodleLogin = Jsoup
                .connect("https://moodle.rwth-aachen.de/login/index.php")
                .method(Connection.Method.GET)
                .timeout(Const.TIMEOUT)
                .execute();


        Connection.Response resLogin = Jsoup
                .connect("https://moodle.rwth-aachen.de/auth/shibboleth/index.php")
                .method(Connection.Method.POST)
                .timeout(Const.TIMEOUT)
                .cookies(resMoodleLogin.cookies())
                .execute();

        Connection.Response resAccept = Jsoup
                .connect("https://sso.rwth-aachen.de/idp/profile/SAML2/Redirect/SSO?execution=e1s1")
                .cookies(resMoodleLogin.cookies())
                .cookies(resLogin.cookies())
                .data("j_username", loginData.getBenutzer())
                .data("j_password", loginData.getPasswort())
                .data("donotcache", "1")
                .data("_eventId_proceed", "Anmeldung")
                .data("_shib_idp_revokeConsent", "true")
                .timeout(Const.TIMEOUT)
                .execute();

        Connection con = Jsoup.connect("https://sso.rwth-aachen.de/idp/profile/SAML2/Redirect/SSO?execution=e1s2")
                .timeout(Const.TIMEOUT);

        Connection.Response res = con
                .data("_shib_idp_consentIds", "rwthSystemIDs")
                .data("_shib_idp_consentOptions", "_shib_idp_rememberConsent")
                .data("_eventId_proceed", "Akzeptieren")
                .cookies(resMoodleLogin.cookies())
                .cookies(resLogin.cookies())
                .cookies(resAccept.cookies())
                .method(Connection.Method.POST)
                .timeout(Const.TIMEOUT)
                .execute();

        Connection connection = Jsoup
                .connect("https://moodle.rwth-aachen.de/Shibboleth.sso/SAML2/POST")
                .method(Connection.Method.POST)
                .timeout(Const.TIMEOUT)
                .cookies(resMoodleLogin.cookies())
                .cookies(resLogin.cookies())
                .cookies(res.cookies());
        for (Element e : res.parse().getElementsByTag("input")) {
            if (!e.attr("name").equals("")) { // Solange das name attribute gefüllt ist
                connection.data(e.attr("name"), e.attr("value"));
            }
        }
        Connection.Response resDashboard = connection.execute();

        Map<String, String> cookies=  resMoodleLogin.cookies();
        cookies.putAll(resLogin.cookies());
        cookies.putAll(resDashboard.cookies());
        return cookies;
    }
}

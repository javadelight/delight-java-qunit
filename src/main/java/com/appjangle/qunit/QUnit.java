package com.appjangle.qunit;

import com.appjangle.qunit.internal.SslUtils;
import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.DomElement;
import com.gargoylesoftware.htmlunit.html.HtmlElement;
import com.gargoylesoftware.htmlunit.html.HtmlPage;

import be.roam.hue.doj.Doj;
import junit.framework.Assert;

public class QUnit {

    public static void run(final Object context, final String resourcePath) {
        run("file:///" + context.getClass().getResource("/" + resourcePath).getFile());
    }

    /**
     * Will load the specified HTML page, run QUnit tests and throw JUnit
     * assertion errors if any of the QUnit test cases failed.
     * 
     * @param pageUrl
     */
    public static void run(final String pageUrl) {
    	
        try {
            WebClient webClient = null;
            try {
                System.getProperties().put("org.apache.commons.logging.simplelog.defaultlog", "error");
                webClient = new WebClient(BrowserVersion.CHROME);

                /*
                 * Temporarily required since Godaddy certificates are not added
                 * to Java correctly.
                 */
                SslUtils.disableSslCertificateValidation();
                SslUtils.allowInsecureSsl(webClient);

                webClient.getOptions().setTimeout(120 * 1000);

                final HtmlPage page = webClient.getPage(pageUrl);

                Thread.sleep(1000);

                /*
                 * Set a limit of how long to wait for test completion.
                 */
                int retriesLeft = 1024;
                while (!isTestComplete(page)) {
                    Thread.sleep(1000);
                    retriesLeft--;
                    if (retriesLeft == 0) {
                        Assert.fail("Tests took longer than 1024 s to execute.");
                    }
                }

                final HtmlElement element = page.getHtmlElementById("qunit");

                if (element.getTextContent().indexOf("0 tests of 0") != -1) {
                    Assert.fail("No QUnit tests ran for [" + pageUrl + "]");
                }

                for (final HtmlElement liElement : Doj.on(element).get("li").allElements()) {
                    if (liElement.getAttribute("class").equals("fail")) {
                        for (final HtmlElement spanElement : Doj.on(liElement).get("span").allElements()) {
                            if (spanElement.getAttribute("class").equals("test-name")) {

                                for (final HtmlElement embeddedLi : Doj.on(liElement).get("ol li").allElements()) {

                                    if (embeddedLi.getAttribute("class").equals("fail")) {

                                        final Doj testMessage = Doj.on(embeddedLi).get("span");

                                        // final HtmlElement source = Doj.on(
                                        // embeddedLi).allElements()[1];

                                        Assert.fail("QUnit test failed: " + spanElement.asText() + "\nAssertion: "
                                                + testMessage.firstElement().asText() + "\nSource:\n"
                                                + embeddedLi.asText());
                                    }
                                }

                                Assert.fail("QUnit test failed: " + spanElement.asText());

                            }
                            Assert.fail("QUnit test failed. Could not determine name.");
                        }
                    }
                }

            } finally {
                if (webClient != null) {

                    webClient.close();
                }

            }
        } catch (final Throwable t) {
            throw new RuntimeException(t);
        }

    }

    public static boolean isTestComplete(final HtmlPage page) {
        final DomElement testResult = page.getElementById("qunit-testresult");

        return testResult.asText().contains("Tests completed");
    }

}

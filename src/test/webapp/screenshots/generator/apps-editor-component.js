Feature('apps-editor-component');

var basePath = "/../../src/main/webapp";

Before(({ I, login, DT }) => {
    login('admin');
    DT.addContext("recipients","#datatableFieldDTE_Field_recipientsTab_wrapper");
});

function tabLinkNone(Document, I, DT, DTE) {

}
function tabLink1(Document, I, DT, DTE) {
    I.clickCss("#tabLink1");
}
function tabLink1Spring(Document, I, DT, DTE) {
    I.clickCss("#pills-dt-editor-component-datatable li:nth-child(1) a");
}
function tabLink2(Document, I, DT, DTE) {
    I.clickCss("#tabLink2");
}
function tabLink2Spring(Document, I, DT, DTE) {
    I.clickCss("#pills-dt-editor-component-datatable li:nth-child(2) a");
}
function tabLink3Spring(Document, I, DT, DTE) {
    I.clickCss("#pills-dt-editor-component-datatable li:nth-child(3) a");
}
function tabLink2Wait(Document, I, DT, DTE) {
    I.clickCss("#tabLink2");
    I.wait(5);
}
function tabLink3(Document, I, DT, DTE) {
    I.clickCss("#tabLink3");
}
function tabLink3Spring(Document, I, DT, DTE) {
    I.clickCss("#pills-dt-editor-component-datatable li:nth-child(3) a");
}

function tabLinkCustom(Document, I, DT, DTE, tabId) {
    I.clickCss("#" + tabId);
}

function screenshotWebAndApp(I, Document, docId, path, webSelector, callback1=null, callback2=null, width, height, callbackWeb=null) {
    I.amOnPage("/showdoc.do?docid="+docId+"&NO_WJTOOLBAR=true");
    var counter = 1;
    if ("/components/app-docsembed"==path) {
        I.waitForInvisible(".loader", 80);
    }
    if ("/components/search"==path) {
        I.fillField("input[name=words]", "trhy");
        I.clickCss(".smallSearchSubmit");
        I.wait(5);
    }
    if ("/components/calendar"==path) {
        I.executeScript(function() {
         window.location.href="/apps/kalendar-udalosti/?month=8&year=2023&NO_WJTOOLBAR=true";
        });
    }
    if (typeof callbackWeb == "function" && callbackWeb != null) {
        callbackWeb(Document, I);
        I.wait(2);
    }

    let lngSuffix = I.getConfLng();
    if ("sk"==lngSuffix) lngSuffix = "";
    else lngSuffix = "-" + lngSuffix;

    if (webSelector!=null) Document.screenshotElement(webSelector, basePath+path+"/screenshot-"+(counter++)+lngSuffix+".jpg", width, height);
    if(callback1!=null) Document.screenshotAppEditor(docId, basePath+path+"/screenshot-"+(counter++)+lngSuffix+".jpg", callback1);
    if (callback2!=null) Document.screenshotAppEditor(docId, basePath+path+"/screenshot-"+(counter++)+lngSuffix+".jpg", callback2);
}

Scenario('apps screenshot for editor-components.jsp', ({ I, DT, DTE, Document }) => {

    screenshotWebAndApp(I, Document, 77668, "/apps/file-archive/mvc/", ".ly-content .container", tabLinkNone, tabLink2Spring, 1000, 1000, function(Document, I) {
        I.executeScript(() => {
            $(".collapsible").click();
        });
    });
    screenshotWebAndApp(I, Document, 77766, "/components/content-block", ".blueBox", tabLinkNone, null, 640, 480);
    screenshotWebAndApp(I, Document, 25210, "/components/app-cookiebar", ".cookiebar", tabLinkNone, null, 640, 480);
    screenshotWebAndApp(I, Document, 77874, "/components/app-docsembed", ".ly-content .container", tabLinkNone, null);
    screenshotWebAndApp(I, Document, 77873, "/components/app-htmlembed", ".ly-content .container", tabLinkNone, null);
    screenshotWebAndApp(I, Document, 77770, "/components/app-vyhladavanie", ".ly-content .container", tabLinkNone, null, 640, 480);
    screenshotWebAndApp(I, Document, 77775, "/components/app-weather", ".app-weather", tabLinkNone, null, 800, 600);
    screenshotWebAndApp(I, Document, 77766, "/components/content-block", ".blueBox", tabLinkNone, null, 640, 480);
    screenshotWebAndApp(I, Document, 45926, "/components/gallery", "#thumbs1", tabLink1Spring, tabLink2Spring, 1000, 1000);

    I.amOnPage("/apps/gdpr-cookies/");
    Document.screenshotElement(".cookies-bar-wrapper.cookies-top", basePath+"/components/gdpr/screenshot-1.jpg");
    I.click(".btn.more");
    Document.screenshotElement(".cookies-list", basePath+"/components/gdpr/screenshot-2.jpg");
    I.amOnPage("/apps/gdpr/admin/");
    DT.filterContains("cookieName", "__");
    Document.screenshot(basePath+"/components/gdpr/screenshot-3.jpg");

    screenshotWebAndApp(I, Document, 25209, "/components/menu", "header", tabLinkNone, null);
    I.amOnPage("/apps/menu/?NO_WJTOOLBAR=true");
    Document.screenshotElement(".ly-content .container", basePath+"/components/menu/screenshot-3.jpg");

    screenshotWebAndApp(I, Document, 77875, "/components/search", ".ly-content .container", tabLinkNone, null);

    screenshotWebAndApp(I, Document, 77774, "/components/send_link", ".ly-content .container", tabLinkNone, null, 800, 600);
    I.amOnPage("/apps/poslat-stranku-emailom/?NO_WJTOOLBAR=true");
    I.clickCss("a.sendLink");
    I.wait(3);
    Document.screenshot(basePath+"/components/send_link/screenshot-1.jpg");
    //if (1==1) return;

    screenshotWebAndApp(I, Document, 48204, "/components/inquiry", "#resultsDiv-1", tabLink1Spring);
    screenshotWebAndApp(I, Document, 77667, "/components/inquirysimple", ".inquiryBoxDefault", tabLink1, tabLink2);
    screenshotWebAndApp(I, Document, 21343, "/components/banner", ".banner-content", tabLinkNone, tabLink3, 640, 480);
    screenshotWebAndApp(I, Document, 60029, "/components/quiz", "#quiz", tabLink1Spring, null, 800, 600);
    screenshotWebAndApp(I, Document, 77767, "/components/app-date", ".ly-content .container", tabLinkNone, null, 800, 600);

    I.amOnPage("/admin/v9/webpages/web-pages-list/?docid=22");
    DTE.waitForEditor();
    Document.screenshot(basePath+"/components/dmail/screenshot-1.jpg");
    I.amOnPage("/apps/dmail/admin/?id=1");
    DTE.waitForEditor("campaingsDataTable");
    Document.screenshot(basePath+"/components/dmail/screenshot-2.jpg");

    I.clickCss("#pills-dt-campaingsDataTable-receivers-tab");
    I.click(DT.btn.recipients_group_button);
    I.waitForElement("#modalIframeIframeElement", 10);
    Document.screenshot(basePath+"/components/dmail/screenshot-3.jpg");
    I.click(locate("#modalIframe button").withText("OK"));

    I.amOnPage("/admin/v9/webpages/web-pages-list/?docid=77768");
    DTE.waitForEditor();
    I.wait(4);
    I.click("a.cke_button__components");
    I.waitForElement("div.cke_editor_data_dialog");
    I.wait(2);
    I.switchTo("iframe.cke_dialog_ui_iframe");
    I.switchTo("#editorComponent")
    I.clickCss("#components-emoticon-title");
    I.clickCss("div.app-components-emoticon-title div.app-buy a");
    Document.screenshot(basePath+"/components/emoticon/screenshot-1.jpg");

    screenshotWebAndApp(I, Document, 26180, "/components/formsimple", "#formMailForm-formular-lahko", tabLink1, tabLink2, 800, 600);
    screenshotWebAndApp(I, Document, 36038, "/components/calendar", ".ly-content .container", tabLink1Spring, tabLink2Spring, 800, 600);
    screenshotWebAndApp(I, Document, 120026, "/components/news-calendar", ".ly-content .container", tabLink1Spring, tabLink2Spring, 800, 600);
    screenshotWebAndApp(I, Document, 59889, "/components/map", "#map1", tabLink1, tabLink2, 1000, 1200);
    screenshotWebAndApp(I, Document, 24217, "/components/sitemap", ".sitemaptest", tabLinkNone, null, 640, 480);


    screenshotWebAndApp(I, Document, 24008, "/components/media", ".ly-content .container", tabLinkNone, null, 800, 600);

    screenshotWebAndApp(I, Document, 10, "/components/news", "section.md-news-subpage", tabLink1, tabLink2);
    screenshotWebAndApp(I, Document, 77772, "/components/app-social_icon", ".ly-content .container", tabLink2Spring, null);
    screenshotWebAndApp(I, Document, 77773, "/components/app-testimonials", ".ly-content .container", tabLink1, tabLink2, 800, 600);
    screenshotWebAndApp(I, Document, 17322, "/components/qa", ".ly-content .container", tabLink1, tabLink3, 800, 600);


    I.amOnPage("/admin/v9/webpages/web-pages-list/?docid=25");
    DTE.waitForEditor();
    I.wait(5);
    I.clickCss("a.cke_button.cke_button__htmlbox");
    I.switchTo("iframe.cke_dialog_ui_iframe");
    I.switchTo("#editorComponent");
    I.wait(4);
    Document.screenshot(basePath+"/components/htmlbox/screenshot-1.jpg");
    I.click("#tabLink2");
    Document.screenshot(basePath+"/components/htmlbox/screenshot-2.jpg");

    screenshotWebAndApp(I, Document, 77776, "/components/related-pages", ".ly-content .container", tabLinkNone, null, 800, 600);
    screenshotWebAndApp(I, Document, 70839, "/components/rating", ".ly-content .container", tabLinkNone, null, 800, 600);
    screenshotWebAndApp(I, Document, 39096, "/components/reservation", ".ly-content .container", tabLink2Spring, tabLink3Spring);

    I.amOnPage("/apps/restaurant-menu/?NO_WJTOOLBAR=true&week=2023-W48");
    Document.screenshotElement(".restaurant-menu", basePath+"/components/restaurant_menu/screenshot-1.jpg");
    Document.screenshotAppEditor(74621, basePath+"/components/restaurant_menu/screenshot-2.jpg", function(Document, I, DT, DTE) {
        tabLink2Spring(Document, I, DT, DTE);
        Document.screenshot(basePath+"/components/restaurant_menu/screenshot-3.jpg");

        tabLink3Spring(Document, I, DT, DTE);
        I.switchTo("#DTE_Field_iframe2");
        I.fillField("input.dt-filter-from-dayDate", "28.11.2023");
        I.clickCss("button.dt-filtrujem-dayDate");
        Document.screenshot(basePath+"/components/restaurant_menu/screenshot-4.jpg");

        I.switchTo();
        I.switchTo('.cke_dialog_ui_iframe');
        I.switchTo('#editorComponent');
        tabLink1Spring(Document, I, DT, DTE);
    });

    I.amOnPage("/admin/v9/webpages/web-pages-list/?docid=57");
    DTE.waitForEditor();
    I.wait(8);
    I.switchTo("#DTE_Field_data-pageBuilderIframe");
    I.clickCss("a.cke_button__components");
    I.wait(6);
    I.switchTo();
    I.switchTo("#DTE_Field_data-pageBuilderIframe");
    I.switchTo("iframe.cke_dialog_ui_iframe");
    I.switchTo("#editorComponent")
    I.forceClickCss("#components-seo-title");
    I.wait(3);
    I.click("Vložiť do stránky");
    I.wait(3);
    Document.screenshot(basePath+"/components/seo/screenshot-1.png");

    screenshotWebAndApp(I, Document, 72299, "/components/site_browser", ".ly-content .container", tabLink1, tabLink2Wait, 800, 600);
});

Scenario('apps screenshot for editor-components.jsp apps to spring v4', ({ I, DT, DTE, Document }) => {
    screenshotWebAndApp(I, Document, 64542, "/components/user", ".ly-content .container", tabLinkNone, tabLink2Spring, 800, 600);
    screenshotWebAndApp(I, Document, 77868, "/components/app-impress_slideshow", "#jms-slideshow", tabLink1Spring, tabLink2Spring, 800, 600);

    screenshotWebAndApp(I, Document, 63761, "/components/forum", "#forumContentDiv", tabLink1Spring, tabLink2Spring, 800, 600);
    screenshotWebAndApp(I, Document, 63761, "/components/forum", "#forumContentDiv", tabLink1Spring, tabLink3Spring, 800, 600);

    screenshotWebAndApp(I, Document, 77869, "/components/slider", "#amazingslider-wrapper-1", tabLink1Spring, tabLink3Spring);
    screenshotWebAndApp(I, Document, 77870, "/components/app-slit_slider", "#slider", tabLink1Spring, tabLink2Spring);

    screenshotWebAndApp(I, Document, 77871, "/components/video", ".videoBox", tabLinkNone, null);
    I.switchTo();
    I.switchTo(".cke_dialog_ui_iframe");
    I.switchTo("#editorComponent");
    I.clickCss(".image_radio_item > label[for=DTE_Field_field_0]")
    Document.screenshot(basePath+"/components/video/screenshot-3.png");
    I.switchTo();


    screenshotWebAndApp(I, Document, 77872, "/components/carousel_slider", "#amazingcarousel-container-1", tabLink1Spring, tabLink3Spring);
});
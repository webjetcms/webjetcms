package sk.iway.iwcm.components.basket.mvc;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.Getter;
import lombok.Setter;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.components.WebjetComponentAbstract;
import sk.iway.iwcm.components.basket.jpa.BasketInvoiceEntity;
import sk.iway.iwcm.components.basket.jpa.BasketInvoiceItemsRepository;
import sk.iway.iwcm.components.basket.jpa.BasketInvoicePaymentsRepository;
import sk.iway.iwcm.components.basket.jpa.BasketInvoicesRepository;
import sk.iway.iwcm.system.annotations.DefaultHandler;
import sk.iway.iwcm.system.annotations.WebjetAppStore;
import sk.iway.iwcm.system.annotations.WebjetComponent;
import sk.iway.iwcm.users.UsersDB;
import sk.iway.tags.CurrencyTag;

@WebjetComponent("sk.iway.iwcm.components.basket.mvc.BasketInvoicesListApp")
@WebjetAppStore(nameKey = "apps.basket.invoiceslist.title", descKey="apps.basket.invoiceslist.desc", imagePath = "ti ti-receipt-euro", galleryImages = "/apps/basket/mvc/", commonSettings = true)
@Getter
@Setter
public class BasketInvoicesListApp extends WebjetComponentAbstract {

    @JsonIgnore
    private BasketInvoicesRepository bir;

    @JsonIgnore
    BasketInvoiceItemsRepository biir;

    @JsonIgnore
    private BasketInvoicePaymentsRepository bipr;

    private static final String VIEW_PATH = "/apps/basket/mvc/basket-invoices-list"; //NOSONAR
    private static final String NOT_FOUND = "/404.jsp"; //NOSONAR

    private static final String ACTION_VIEW = "view";
    private static final String ACTION_PAY = "pay";
    private static final String ACTION_AFTER_PAY = "afterpay";

    @Autowired
    public BasketInvoicesListApp(BasketInvoicesRepository bir, BasketInvoiceItemsRepository biir, BasketInvoicePaymentsRepository bipr) {
        this.bir = bir;
        this.biir = biir;
        this.bipr = bipr;
    }

    @Override
    public void init(HttpServletRequest request, HttpServletResponse response) {
        Logger.debug(BasketInvoicesListApp.class, "Init of BasketInvoicesListApp app");
    }

    @DefaultHandler
	public String view(Model model, HttpServletRequest request, HttpServletResponse response)
	{
        Identity loggedUser = UsersDB.getCurrentUser(request);
        //list of invoices is available only for logged users
        if(loggedUser == null) return NOT_FOUND;

        String act = Tools.getStringValue(request.getParameter("act"), ACTION_VIEW);

        if(ACTION_VIEW.equalsIgnoreCase(act)) {
            List<BasketInvoiceEntity> invoices = bir.findAllByLoggedUserIdAndDomainIdOrderByCreateDateDesc(loggedUser.getUserId(), CloudToolsForCore.getDomainId());

            model.addAttribute("act", ACTION_VIEW);
            model.addAttribute("dataList", invoices);
            model.addAttribute("toPayOrder", new BasketInvoiceEntity());
        }
        else if(ACTION_AFTER_PAY.equalsIgnoreCase(act)) {
            model.addAttribute("act", ACTION_AFTER_PAY);
            model.addAttribute("dataList", new ArrayList<>());
            model.addAttribute("toPayOrder", new BasketInvoiceEntity());

            //Needed params to call order_payment_reply.jsp
            model.addAttribute("invoicePaymentId", Tools.getStringValue(request.getParameter("invoicePaymentId"), ""));
            model.addAttribute("realPaymentId", Tools.getStringValue(request.getParameter("id"), ""));
        } else {
            return NOT_FOUND;
        }

        model.addAttribute("tools", Tools.class);
        model.addAttribute("currencyTag", CurrencyTag.class);

        return VIEW_PATH;
	}

    public String saveForm(@ModelAttribute("entity") BasketInvoiceEntity entity, BindingResult result, Model model, HttpServletRequest request) {

        model.addAttribute("act", ACTION_PAY);
        model.addAttribute("dataList", new ArrayList<>());
        model.addAttribute("toPayOrder", entity);

        return VIEW_PATH;
    }
}

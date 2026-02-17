package sk.iway.iwcm.components.crypto;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import sk.iway.iwcm.*;
import sk.iway.iwcm.users.UsersDB;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

/**
 * Zakladny controller pre spravu sifrovacich klucov
 * [#26639]JPA - SIFROVANIE DAT
 */
@Controller
@RequestMapping("/components/crypto/")
@PreAuthorize("@WebjetSecurityService.isAdmin()")
public class CryptoController
{
   @GetMapping("admin/keymanagement")
   public String generateKeys()
   {
      return "/components/crypto/admin_keymanagement";
   }

   @GetMapping("admin/keymanagement/generate")
   public String generateKeys(Model model, HttpSession session)
   {
      Identity user = UsersDB.getCurrentUser(session);
      //npe na user neosetrujem, padne to do Exception a tam sa zaznamena chyba
      try {
         CryptoFactory cryptoFactory = new CryptoFactory();
         CryptoKeys generatedKeys = cryptoFactory.generateKeys(user.getLogin());
         model.addAttribute("generatedKeys", generatedKeys);
      } catch (Exception ex) {
         Logger.error(CryptoController.class, "Error generating keys", ex);
         model.addAttribute("errorText", "Error generating keys: "+ex.getMessage());
         return "/components/maybeError";
      }
      return "/components/crypto/admin_keymanagement";
   }

   /**
    * Nastavy zadany kluc do session pouzivatela.
    * @param key
    * @param model
    * @param request
    * @return
    */
   @PostMapping("admin/keymanagement/setkey")
   public String setKey(String key, Model model, HttpServletRequest request)
   {
      CryptoFactory cryptoFactory = new CryptoFactory();
      boolean success = cryptoFactory.setPrivateKeyToSession(key, request.getSession());
      model.addAttribute("keySetToSession", Boolean.valueOf(success));
      return "/components/crypto/admin_keymanagement";
   }

//   /**
//    * Metoda ktora je mozna pouzit iba pri hybridnych klucoch. Z privatneho kluca mozem znova vygenerovat verejny kluc.
//    * @param key
//    * @param model
//    * @return
//    */
//   @PostMapping("admin/keymanagement/generate/public")
//   public String generateKeys(String key, Model model) {
//      KeysetHandle privateKey = CryptoToolsV2.getInstance().loadPrivateKeyBase64(key);
//      KeysetHandle publicKey = null;
//      String prefix = CryptoFactory.getPrefix(key);
//      try {
//         publicKey = privateKey.getPublicKeysetHandle();
//      } catch (Exception ex) {
//         Logger.error(CryptoController.class, "Error generating keys", ex);
//         model.addAttribute("errorText", "Error generating keys: "+ex.getMessage());
//         return "/components/maybeError";
//      }
////      CryptoKeys generatedKeys = new CryptoKeys(null, prefix + ":" + CryptoToolsV2.getInstance().getKeyBase64(publicKey), prefix + ":" + CryptoToolsV2.getInstance().getKeyBase64(privateKey));
////      model.addAttribute("generatedKeys", generatedKeys);
//      return "/components/crypto/admin_keymanagement";
//   }

   /*private boolean isCryptoAlg() {
      return "v2".equalsIgnoreCase(Constants.getString("cryptoAlg"));
   }*/
}

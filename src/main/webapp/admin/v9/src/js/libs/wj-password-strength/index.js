import WJ from "../../webjet";
import $, { type } from 'jquery';
import { zxcvbn, zxcvbnOptions } from '@zxcvbn-ts/core'

/**
 * Trieda zapuzdruje kniznicu zxcvbn-ts do WJ API s moznostou bindnutia na DT.editor element
 * https://zxcvbn-ts.github.io/zxcvbn/guide/#other-implementation
 */
export class WjPasswordStrength {

    zxcvbn = null;
    zxcvbnOptions = null;
    element = null;

    constructor(options) {
        //console.log("WjPasswordStrength constructor");

        this.zxcvbn=zxcvbn;
        this.zxcvbnOptions = zxcvbnOptions;
        this.element = options.element ? options.element : null;

        if (typeof this.element === "string") this.element = document.querySelector(this.element);
        //console.log("element=", this.element);
    }

    load() {
      //console.log("WjPasswordStrength load, zxcvbn=", zxcvbn);

      let that = this;

      const asyncLoad = async () => {
          const loadZxcvbnOptions = async () => {
              const zxcvbnCommonPackage = await import(
                /* webpackChunkName: "zxcvbnCommonPackage" */ '@zxcvbn-ts/language-common'
              )
              const zxcvbnEnPackage = await import(
                /* webpackChunkName: "zxcvbnEnPackage" */ '@zxcvbn-ts/language-en'
              )

              //console.log("zxcvbnCommonPackage.default=", zxcvbnCommonPackage, "zxcvbnEnPackage.default=", zxcvbnEnPackage);

              return {
                dictionary: {
                  ...zxcvbnCommonPackage.dictionary,
                  ...zxcvbnEnPackage.dictionary,
                },
                graphs: zxcvbnCommonPackage.adjacencyGraphs
                //translations: zxcvbnEnPackage.default.translations,
              }
          }

          const options = await loadZxcvbnOptions();
          zxcvbnOptions.setOptions(options);

          if (that.element != null) that.bindToElement(that.element);
      }
      asyncLoad();
    }

    bindToElement(element) {
        //console.log("WjPasswordStrength bindToElement, zxcvbn=", zxcvbn);

        this.element = element;
        let that = this;

        let $element = $(element);
        let infoMessageDiv = $element.parents("div.DTE_Field").find("div[data-dte-e=msg-info]");
        //console.log("infoMessageDiv=", infoMessageDiv);

        let timeout = null;
        $element.on("keyup", function(e) {
          clearTimeout(timeout);
          timeout = setTimeout(()=>{
            try {
              let result = that.checkPassword($(this).val());
              //console.log("Keydown, score=", result.score, "warning=", result.feedback.warning, "suggestions=", result.feedback.suggestions);
              let feedback = result.feedback.warning;
              if (feedback!=null && feedback != "") feedback = "<br/>" + WJ.translate("wj-password-strength.warnings."+feedback+".js");
              else feedback = "";
              let message = WJ.translate("wj-password-strength.rating.js") + " " + WJ.translate("wj-password-strength.rating."+result.score+".js") + " " + feedback;
              infoMessageDiv.html(message);
            } catch (e) {}
          }, 100);
        });
    }

    checkPassword(password) {
        //console.log("Check password: ", password);
        let result = zxcvbn(password);
        //console.log("Result: ", result);
        return result;
    }
}
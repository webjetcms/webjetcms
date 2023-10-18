const { I } = inject();

/**
 * Funkcie pre pracu s Browsermi
 */

module.exports = {

    getBrowserType() {
        let browserType = process.env.CODECEPT_BROWSER || "chromium";
        I.say("browserType="+browserType);
        return browserType;
    },
  
    isFirefox() {
        return "firefox" == this.getBrowserType();
    },
  
    isChromium() {
        return "chromium" == this.getBrowserType();
    },

}
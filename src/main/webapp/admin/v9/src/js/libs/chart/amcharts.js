import * as am5 from "@amcharts/amcharts5";
import * as am5xy from "@amcharts/amcharts5/xy";
import * as am5percent from "@amcharts/amcharts5/percent";
import * as am5themes_Animated from "@amcharts/amcharts5/themes/Animated";
import * as dark from "@amcharts/amcharts5/themes/Dark";
import { Color } from "@amcharts/amcharts5/.internal/core/util/Color";

export class WebjetTheme extends am5.Theme {
  setupDefaultRules() {
    this.rule("InterfaceColors").setAll({
      background: Color.fromHex(0x2b303b),
      primaryButton: Color.fromHex(0x0090FC),
      primaryButtonHover: Color.fromHex(0x0054d5),
      primaryButtonDown: Color.fromHex(0x0054d5),
      secondaryButton: Color.fromHex(0x646979),
      secondaryButtonHover: Color.fromHex(0x0090FC),
      secondaryButtonDown: Color.fromHex(0x0054d5)
    });

    this.rule("ColorSet").setAll({
        colors : [
          am5.color("#0061f5"),
          am5.color("#00a186"),
          am5.color("#b48700"),
          am5.color("#e00028"),
          am5.color("#009cb4"),
          am5.color("#003288"),
          am5.color("#005e4d"),
          am5.color("#6a4e00"),
          am5.color("#a9001c"),
          am5.color("#005b6a")
        ]
    });
    this.rule("Label").setAll({
      fontSize: "12px",
      fontFamily: "Asap, sans-serif"
    });
    this.rule("Legend").setAll({
      paddingTop: 4
    });
    this.rule("Container", ["legend", "marker"]).setAll({
      width: 11,
      height: 11
    });

    //scrollbar width
    this.rule("Scrollbar", ["horizontal"]).setAll({
      minHeight: 6,
      fillOpacity: 1,
    });
    this.rule("Scrollbar", ["vertical"]).setAll({
      minWidth: 6
    });

    //scrollbar start/end buttons
    this.rule("Button", ["resize"]).setAll({
      paddingTop: 6,
      paddingBottom: 6,
      paddingLeft: 7,
      paddingRight: 7,
    });
    this.rule("RoundedRectangle", ["background", "resize", "button"]).setAll({
      cornerRadiusBL: 6,
      cornerRadiusBR: 6,
      cornerRadiusTL: 6,
      cornerRadiusTR: 6,
    });
    this.rule("Graphics", ["resize", "button", "icon"]).setAll({
      draw: (display) => {
        display.moveTo(0, 0.5);
        display.lineTo(0, 6.5);
        display.moveTo(4, 0.5);
        display.lineTo(4, 6.5);
      }
    });
  }
}

window.am5 = am5
window.am5xy = am5xy;
window.am5percent = am5percent;
window.am5themes_Animated = am5themes_Animated.default;
window.am5_dark = dark.default;
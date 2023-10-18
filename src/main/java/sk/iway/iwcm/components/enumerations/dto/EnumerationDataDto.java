package sk.iway.iwcm.components.enumerations.dto;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * Date: 09.04.2018
 * Time: 14:57
 * Project: webjet8
 * Company: InterWay a. s. (www.interway.sk)
 * Copyright: InterWay a. s. (c) 2001-2018
 *
 * @author mpijak
 */
public class EnumerationDataDto implements Serializable {
    private static final long serialVersionUID = 1L;

    private int id;
    private String string1;
    private String string2;
    private String string3;
    private BigDecimal decimal1;
    private BigDecimal decimal2;
    private BigDecimal decimal3;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getString1() {
        return string1;
    }

    public void setString1(String string1) {
        this.string1 = string1;
    }

    public String getString2() {
        return string2;
    }

    public void setString2(String string2) {
        this.string2 = string2;
    }

    public String getString3() {
        return string3;
    }

    public void setString3(String string3) {
        this.string3 = string3;
    }

    public BigDecimal getDecimal1() {
        return decimal1;
    }

    public void setDecimal1(BigDecimal decimal1) {
        this.decimal1 = decimal1;
    }

    public BigDecimal getDecimal2() {
        return decimal2;
    }

    public void setDecimal2(BigDecimal decimal2) {
        this.decimal2 = decimal2;
    }

    public BigDecimal getDecimal3() {
        return decimal3;
    }

    public void setDecimal3(BigDecimal decimal3) {
        this.decimal3 = decimal3;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof EnumerationDataDto)) {
            return false;
        }
        EnumerationDataDto obj2 = (EnumerationDataDto) obj;
        return this.getId() == obj2.getId();
    }
}

package sk.iway.iwcm.components.restaurant_menu.jpa;

public class AlergenBean {
    
    Integer alergenNumber;
    String alergenName;

    public AlergenBean(){}

    public AlergenBean(Integer alergenNumber, String alergenName) {
        this.alergenNumber = alergenNumber;
        this.alergenName = alergenName;
    }

    public Integer getAlergenNumber() {
        return alergenNumber;
    }
    
    public void setAlergenNumber(Integer alergenNumber) {
        this.alergenNumber = alergenNumber;
    }

    public String getAlergenName() {
        return alergenName;
    }

    public void setAlergenName(String alergenName) {
        this.alergenName = alergenName;
    }
}

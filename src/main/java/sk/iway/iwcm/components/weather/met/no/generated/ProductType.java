package sk.iway.iwcm.components.weather.met.no.generated;

import java.util.ArrayList;
import java.util.List;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;


/**
 * Element describing a weatherproduct by
 * 	             time-elements, location-elements and a set of weather-elements.
 * 	        
 * 
 * <p>Java class for productType complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="productType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="time" type="{}timeType" maxOccurs="unbounded"/>
 *       &lt;/sequence>
 *       &lt;attribute name="class" use="required">
 *         &lt;simpleType>
 *           &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *             &lt;enumeration value="pointData"/>
 *             &lt;enumeration value="extremes"/>
 *             &lt;enumeration value="forestfireindex"/>
 *             &lt;enumeration value="uvforecast"/>
 *             &lt;enumeration value="tidalwater"/>
 *             &lt;enumeration value="buoy"/>
 *             &lt;enumeration value="stavernodden"/>
 *             &lt;enumeration value="seaapproachforecast"/>
 *             &lt;enumeration value="temperatureverification"/>
 *           &lt;/restriction>
 *         &lt;/simpleType>
 *       &lt;/attribute>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "productType", propOrder = {
    "time"
})
public class ProductType {

    @XmlElement(required = true)
    protected List<TimeType> time;
    @XmlAttribute(name = "class", required = true)
    protected String clazz;

    /**
     * Gets the value of the time property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the time property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getTime().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link TimeType }
     * 
     * 
     */
    public List<TimeType> getTime() {
        if (time == null) {
            time = new ArrayList<TimeType>();
        }
        return this.time;
    }

    /**
     * Gets the value of the clazz property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getClazz() {
        return clazz;
    }

    /**
     * Sets the value of the clazz property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setClazz(String value) {
        this.clazz = value;
    }

}
